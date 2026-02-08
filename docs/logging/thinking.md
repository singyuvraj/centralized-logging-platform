# Logging Pipeline: Thinking and Reasoning by Phase

This document records the thinking, reasoning, and trade-offs behind every phase of the centralized logging pipeline. It is maintained so that:

- Anyone can see why choices were made, not only what was decided.
- Future phases can reuse the same reasoning style and avoid repeating mistakes.
- When something breaks or is revisited, the original intent is on record.

**How to use this doc:** For each phase there is a short goal, the main questions we asked, the reasoning we followed, and what we explicitly deferred. For future phases (Phase 3 and beyond), use the same structure and append a new section.

---

## Phase 0: Alignment and Contract (No Implementation)

### Goal

Agree on the pipeline shape, log contract, and path before writing any code or config. No implementation.

### Questions we asked

- Where do logs live (path) and in what format?
- Who reads them and who ships them (sidecar vs central shipper)?
- Do we put something between Filebeat and Elasticsearch?
- What fields must every log event have?

### Thinking

- **Why alignment first:** Changing path or format after rollout is expensive. Locking them in docs first is cheap. Phase 0 exists so Phase 1 and 2 have a single target.
- **Why one path:** Multiple paths mean multiple Filebeat inputs and per-service config. One path (`/apps/logs/application.log`) keeps config and runbooks the same for all services.
- **Why JSON (NDJSON):** Filebeat and Logstash can parse JSON without regex. Elasticsearch can index fields. Plain text would push parsing and inconsistency into every consumer.
- **Why five mandatory fields:** `timestamp`, `level`, `message` are the minimum to sort and filter. `service.name` and `env` are needed to separate services and environments in one cluster. We did not add optional fields to keep the contract small.
- **Why sidecar Filebeat:** One shipper per app instance; no central host that must see all container logs. The shipper moves with the app. We accepted more processes (one Filebeat per instance) for simpler deployment and clear ownership.
- **Why Logstash in the middle:** A single place to parse, filter, and route. We can change Elasticsearch or index naming later without touching Filebeat or the app. We accepted running Logstash as an extra component.
- **What we did not do:** No TLS, certs, Elasticsearch users, ILM, dashboards, or any code. All of that is explicitly deferred so Phase 0 stays “agreement only.”

### Deferred to later phases

TLS, certificates, Elasticsearch users, index lifecycle, Kibana dashboards, Filebeat/Logstash/application implementation.

---

## Phase 1: Application JSON Logging

### Goal

Make the Spring Boot app emit structured JSON logs that satisfy the Phase 0 contract. All changes local to the application. No Filebeat, Docker, or TLS.

### Questions we asked

- Logback or Log4j2? How do we get ECS-style JSON?
- Where does the log file live and how do we support local runs?
- How do we get trace.id into logs without adding distributed tracing?
- How do we remove the commons-logging warning?

### Thinking

- **Why Log4j2:** We already had `log4j2-ecs-layout`. ECS layout gives `@timestamp`, `log.level`, `message`, `service.name`, and supports `trace.id` and `env` via KeyValuePair. Moving to Log4j2 and excluding Logback kept one logging engine and one layout. Trade-off: we must exclude `spring-boot-starter-logging` from every starter that brings it.
- **Why ECS layout:** Phase 0 required structured JSON. ECS is a standard; the official layout avoids hand-rolled JSON. We emit both `env` (Phase 0) and `service.environment` (ECS) so both conventions work.
- **Why fixed path + LOG_PATH override:** Phase 0 fixes `/apps/logs/application.log` for containers. For local runs where `/app` does not exist, we allow `-DLOG_PATH=...` so the same config works. We did not use application.properties for path because Log4j2 loads before Spring.
- **Why trace via MDC only:** Requirement was trace.id when available, no new tracing libs. A filter that sets a trace ID in MDC (from header or generated) and EcsLayout KeyValuePair for `trace.id` was enough. Trade-off: no cross-service propagation; that stays for a later phase.
- **Why TraceIdFilter first in chain:** Trace ID must be set before security or business logic so every log in the request has it. We used highest precedence and put the filter in `config` with other cross-cutting setup.
- **Why console and file same format:** Requirement was “same JSON on console and file.” One layout for both avoids drift and matches what Filebeat will read.
- **Why env from ENV variable:** We needed a value before Spring is up and per-environment. Process env var `ENV` with default `dev` works in Log4j2 at config load. No Spring property in Phase 1.
- **Why exclude commons-logging:** It came from AWS Parameter Store (httpclient). With both commons-logging and spring-jcl on the classpath we got the “remove commons-logging” warning. Excluding it on that dependency left only spring-jcl. No new deps; Log4j2 unchanged.

### Deferred to later phases

TLS, Filebeat, Logstash, Elasticsearch, Kibana, dashboards, distributed tracing.

---

## Phase 2: Filebeat Sidecar

### Goal

Add Filebeat as a sidecar that reads logs from the same Docker volume as the app and forwards them to Logstash. No TLS, Elasticsearch, or Kibana. No overengineering.

### Questions we asked

- How does Filebeat see the same files as the app?
- How do we avoid losing read position on restart?
- Do we use the deprecated log input or filestream?
- Who creates /apps/logs and who can write/read?

### Thinking

- **Why shared volume:** Phase 0 says app and Filebeat share a volume. We added a named volume `backend-logs`, mounted read-write on backend at `/apps/logs` and read-only on Filebeat at `/apps/logs`. Both see the same files; app writes, Filebeat reads.
- **Why backend entrypoint:** When a volume is mounted at `/apps/logs`, the directory is often root-owned. The app runs as appuser and could not write. We added an entrypoint that runs as root, chowns `/apps/logs` to appuser, then exec’s the JVM as appuser. At runtime we fixed the su argument order for Alpine/BusyBox: -s /bin/sh -c "..." must come before the username. See solution.md.
- **Why Filebeat as root:** App creates log files as appuser (uid 1001). Filebeat must read them. Running Filebeat as root avoided permission and ACL changes. Task allowed “root only if required”; we documented the trade-off (hardening in a later phase).
- **Why log input with allow_deprecated_use:** Task explicitly required “Input type: log”. In Filebeat 8 that input is deprecated; we use it with `allow_deprecated_use: true`. A later phase can migrate to filestream.
- **Why path /apps/logs/*.log:** Phase 1 uses rolling files (application.log, application.log.1, …). A glob lets Filebeat pick up active and rotated files without config change. Trade-off: any other .log under /apps/logs would be shipped; for Phase 2 only the app writes there.
- **Why JSON decode but no ECS transform:** Task said do not parse or transform ECS fields. We decode each line as JSON and add only `app` and `env`. No processors that rename or drop ECS fields.
- **Why registry on a volume:** Task required persisted registry. We mount `filebeat-data` at `/usr/share/filebeat/data` so read offsets survive restarts and we avoid re-sending or losing track of rotated files.
- **Why no Logstash in this compose:** Task was “introduce Filebeat as sidecar” and “forward to Logstash”, not stand up the full stack. We kept the change minimal. Validation docs explain how to run or point to Logstash (e.g. same network or extra_hosts).
- **Runtime fixes:** We removed deprecated version from docker-compose. Entrypoint su order was corrected for Alpine. Full command log and root-cause/fix details are in testing.md and solution.md.

### Deferred to later phases

TLS to Logstash, Logstash in this repo, Elasticsearch, Kibana, ILM, running Filebeat as non-root, migrating from log input to filestream.

---

## Phase 2 (Revisited): Contract and TLS between Filebeat and Logstash

### Goal

Make Phase 2 ready for Elasticsearch by:

- Fixing any contract mismatches (path and fields).
- Defining clear field ownership.
- Enabling TLS from Filebeat → Logstash on port 5044.

No Elasticsearch, Kibana, filters, or mTLS. Logstash output stays stdout.

### Questions we asked

- **Path contract:** The original description mentioned `/apps/logs/*.log` while the code used `/apps/logs`. Which path is canonical and how do we avoid future drift?
- **Field contract:** We already had `service.name`, `service.environment`, `env`, plus Filebeat-added `app` and `env`. Which fields are authoritative, and where are they set?
- **TLS design:** How do we add TLS between Filebeat and Logstash with minimal change?
  - Where do certs live?
  - How do we generate them locally?
  - How do we wire them into docker-compose, Logstash, and Filebeat?
- **Scope control:** How do we keep this strictly within Phase 2 + TLS and not drift into Elasticsearch/Phase 4 work?

### Thinking: log path contract

- **Audit first:** We searched everywhere for `/app` vs `/apps`:
  - `log4j2-spring.xml` defaulted `LOG_PATH` to `/apps/logs/application.log`.
  - The Dockerfile created `/apps/logs`.
  - docker-compose mounted `backend-logs:/apps/logs` on backend and Filebeat.
  - `filebeat/filebeat.yml` read from `/apps/logs/*.log`.
  - Phase 0 contract already said “Inside the container: `/apps/logs/application.log`”.
- **Observation:** `/apps/logs` was not present in the codebase; it only existed in the textual description. The entire implementation already followed `/apps/logs`.
- **Decision:** Make `/apps/logs` the **canonical** path and document it explicitly:
  - Updated PHASE0-contract.md with “Log File Path (Canonical Contract)” and a note “Why `/apps/logs` (not `/apps/logs`)".
  - PHASE2-implementation.md now starts with the log path contract and points back to PHASE0.
- **Trade-off:** Zero code/config change, but clear written contract. This avoids future engineers trying to “fix” the implementation to `/apps/logs` and breaking the pipeline.

### Thinking: field ownership and event contract

- **Audit of current fields:**
  - The app’s Log4j2 EcsLayout already emitted `service.name` and `service.environment`.
  - A KeyValuePair added `env` derived from `ENV` (with default `dev`).
  - Filebeat added `app` and `env` via `fields` + `fields_under_root: true`.
- **Problem:** Two sources of identity fields:
  - Application: `service.name`, `service.environment`, `env`.
  - Filebeat: `app`, `env`.
  This makes it unclear which field downstream consumers should use.
- **Contract requirement:** The original description called out `app` or `service.name` plus `env`. Combined with ECS, `service.name` and `service.environment` are the natural canonical identity.
- **Decision:**
  - **Authoritative identity:** `service.name`, `service.environment`, `env` all come from the application.
  - Filebeat **does not** add or overwrite identity fields.
  - `app` is not added by Filebeat; `service.name` is the canonical service identifier.
- **Implementation:**
  - Removed the `fields` block (`app`, `env`) from `filebeat/filebeat.yml`.
  - Documented the event field contract in PHASE0-contract.md and PHASE2-implementation.md.
  - Updated PHASE2-decisions.md to clarify that JSON decode does not introduce identity fields and that Filebeat does not change ECS fields.
- **Reasoning:** This keeps identity a pure application concern. Filebeat remains a shipper and metadata provider (e.g. `agent`, `log.file.path`), not an identity source.

### Thinking: TLS between Filebeat and Logstash

- **Requirement:** “Secure TLS output from Filebeat to Logstash on port 5044”. No mTLS. Logstash output must remain stdout.
- **Constraints:**
  - Local development, no external PKI.
  - Do not commit private keys.
  - Keep configuration simple and visible for debugging.
- **Design choice: self-signed CA in-repo (dev only):**
  - Create a small `tls/` directory with a script `gen-certs.sh`.
  - Script generates:
    - `ca.key` + `ca.crt` (Logstash Dev CA).
    - `logstash.key` + `logstash.crt` with:
      - CN `logstash`
      - SAN: `DNS:logstash`, `DNS:localhost`, `IP:127.0.0.1`
  - This ensures that when Filebeat connects to `logstash:5044`, the certificate’s SAN matches the hostname.
- **Implementation details:**
  - `tls/gen-certs.sh` uses openssl non-interactively, with a small config for SANs.
  - `.gitignore` updated so keys and certs (`ca.key`, `logstash.key`, `ca.crt`, `logstash.crt`, CSR/serial files) are never committed.
  - Documentation for running the script is in TLS.md and referenced from PHASE2-implementation.md.

### Thinking: wiring TLS into Logstash and Filebeat

- **Logstash:**
  - Mount `./tls` at `/usr/share/logstash/certs:ro`.
  - Configure the Beats input in `logstash/logstash.conf`:
    - `ssl_enabled => true`
    - `ssl_certificate => "/usr/share/logstash/certs/logstash.crt"`
    - `ssl_key => "/usr/share/logstash/certs/logstash.key"`
  - Kept output as `stdout { codec => rubydebug }`.
  - Replaced deprecated `ssl => true` with `ssl_enabled => true` to clear deprecation warnings.
- **Filebeat:**
  - Mount `./tls` at `/usr/share/filebeat/certs:ro`.
  - Update `output.logstash`:
    - `hosts: ["logstash:5044"]`
    - `ssl.enabled: true`
    - `ssl.certificate_authorities: ["/usr/share/filebeat/certs/ca.crt"]`
  - Did **not** configure client cert/key (no mTLS).
- **Reasoning:**
  - This is the minimal configuration that:
    - Encrypts traffic.
    - Verifies the server against a CA we control.
    - Avoids dealing with client certificates in this phase.
  - Mounting `./tls` into both containers keeps paths simple and obvious for debugging.

### Thinking: runtime verification and evidence

- **Commands we ran (see testing.md for full log):**
  - `cd tls && chmod +x gen-certs.sh && ./gen-certs.sh`
  - `docker compose down -v`
  - `docker compose up --build -d`
  - `docker compose ps`
  - `docker compose logs logstash`
  - `docker compose logs filebeat`
  - `docker compose exec filebeat ls -la /usr/share/filebeat/data`
- **What we looked for:**
  - **Logstash:**
    - Beats input logs mentioning TLS (ssl_enabled + cert/key paths).
    - Listener on `0.0.0.0:5044`.
    - Events from `/apps/logs/application.log` printed to stdout (rubydebug).
  - **Filebeat:**
    - Configured paths: `/apps/logs/*.log`.
    - Harvester started for `/apps/logs/application.log`.
    - “Connection to backoff(async(tcp://logstash:5044)) established”.
    - Output type `logstash` with events acked.
  - **Registry:**
    - `/usr/share/filebeat/data` contains `filebeat.lock`, `meta.json`, and `registry/`.
- **Interpretation:**
  - When Filebeat has `ssl.enabled: true` and `ssl.certificate_authorities` set, and the connection to `logstash:5044` is established without TLS errors, the link is encrypted.
  - Logstash’s Beats input configuration plus successful events confirm the TLS handshake is working end-to-end.

### Thinking: scope boundaries and deferrals

- **What we deliberately did *not* do:**
  - No Elasticsearch or Kibana.
  - No Logstash filters.
  - No mutual TLS.
  - No changes to application logging semantics beyond the existing ECS layout.
- **Why:** The goal was to make Phase 2 “contract-correct” and “TLS-correct” before Phase 3/4 (Logstash + Elasticsearch). Doing more here would blur phase boundaries and make problems harder to localize later.

### Deferred to later phases

- Elasticsearch output from Logstash and index strategy.
- Kibana dashboards and users/roles.
- Mutual TLS (client certificates) for Beats.
- Hardening (non-root Filebeat, non-root Logstash, stricter TLS policies).

---

## Future Phases: Template

When you add a new phase (Phase 3, 4, …), append a section below using this structure. Keep the same style: goal, questions, thinking, deferred.

### Phase N: [Name]

**Goal:**  
One or two sentences on what this phase achieves.

**Questions we asked:**  
- [Question 1]  
- [Question 2]  
- …

**Thinking:**  
- **Why [decision]:** [Reasoning]. Trade-off: [if any].  
- Repeat for each major choice.

**Deferred to later phases:**  
List what was explicitly not done in this phase.

---

## How to Update This Document

1. **When implementing a phase:** As you make decisions, note the question you asked and the reasoning in the relevant phase section. If you create a new phase, add it and use the “Future Phases: Template” structure.
2. **When revisiting a decision:** Add a short “Revisit (date): …” under the relevant bullet if the reasoning or trade-off was re-evaluated, and why.
3. **Keep it concise:** Short bullets and one or two sentences per point. Detailed rationale stays in the phase’s own docs (e.g. PHASE2-decisions.md); this file is the condensed “why we thought this” record.
