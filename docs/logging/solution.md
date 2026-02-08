# Phase 2 Runtime Fixes and Solutions

This document records what was broken at runtime, why it failed, what was changed to fix it, and what remains intentionally out of scope (Phase 3).

---

## 1. Backend container filesystem and entrypoint

### What was broken

- **Risk:** When a Docker volume is mounted at `/apps/logs`, the mount point is typically owned by root. The application runs as `appuser` (uid 1001). Without fixing ownership, the app could not create or write to files under `/apps/logs`, leading to "Permission denied" or "Could not create directory" at Log4j2 init.
- **Entrypoint:** The entrypoint used `su -s /bin/sh appuser -c "..."`. On Alpine (BusyBox) `su` the argument order can matter: some versions expect `-s /bin/sh -c "command"` before the username. Wrong order can cause "unrecognized option" and prevent the JVM from starting as appuser.

### Why it could fail at runtime

1. **Volume ownership:** First time the backend starts with `backend-logs` mounted at `/apps/logs`, the directory is created by Docker and owned by root. The image’s pre-created `/apps/logs` (owned by appuser) is replaced by the volume. So at runtime `/apps/logs` is root-owned until the entrypoint runs.
2. **su argument order:** If the entrypoint’s `su` invocation is wrong for this image, the container could exit immediately with a su/exec error, or the JVM might run as root instead of appuser.

### What was fixed

1. **Dockerfile (unchanged for ownership):** Already had `RUN mkdir -p /apps/logs && chown -R appuser:appuser /app`. That handles the case when no volume is used. For the volume case, the entrypoint must fix ownership at startup.
2. **docker-entrypoint.sh:**  
   - Kept: `chown -R appuser:appuser /apps/logs` so that when a volume is mounted, the appuser can write.  
   - **Change:** `su` invocation updated from  
     `exec su -s /bin/sh appuser -c "exec java -jar /app/app.jar"`  
     to  
     `exec su -s /bin/sh -c "exec java -jar /app/app.jar" appuser`  
     so that the shell and command come before the username, matching Alpine/BusyBox `su` behaviour.

### Why this is correct

- The entrypoint runs as root (Dockerfile `USER root`). It can chown `/apps/logs` before dropping to appuser. After chown, appuser owns the directory and can create `application.log`.
- The JVM is started via `su ... appuser`, so the main process is appuser (confirmed in logs: "started by appuser in /app"). No application code or logging config was changed; only container startup and user switch.
- Verification: `docker compose run --no-deps --rm --entrypoint "" backend ls -la /apps/logs` shows `drwxr-xr-x 2 appuser appuser` and `application.log` owned by appuser. Log content is NDJSON as expected.

---

## 2. Docker Compose volume wiring

### Before vs after

**Before (Phase 2 as initially added):**
- backend: volume `backend-logs:/apps/logs`
- filebeat: volumes `backend-logs:/apps/logs:ro`, `filebeat-data:/usr/share/filebeat/data`, filebeat.yml bind mount
- volumes: `backend-logs`, `filebeat-data`
- Compose file included `version: '3.8'`

**After (current):**
- Same volume wiring: backend mounts `backend-logs` at `/apps/logs` (read-write). Filebeat mounts the same `backend-logs` at `/apps/logs` (read-only) and `filebeat-data` at `/usr/share/filebeat/data`.
- **Change:** Removed the top-level `version: '3.8'` from docker-compose.yml to clear the "attribute version is obsolete" warning. No other compose fields were removed or changed.

### Why the shared volume is mandatory

- Phase 0 contract: the app writes to `/apps/logs/application.log` and Filebeat reads from that path. Both must see the same files.
- A single named volume (`backend-logs`) mounted at `/apps/logs` in both containers provides one shared filesystem. The backend writes; Filebeat reads. Without this shared volume, Filebeat would have nothing to read and the pipeline would be broken.
- `filebeat-data` is separate so Filebeat’s registry and data persist across restarts and are not mixed with application logs.

---

## 3. Backend restart (DataSource) — intentionally not fixed in Phase 2

### What happens

The backend container exits during Spring context initialization with: "Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured." The restart policy causes it to restart repeatedly.

### Why it is not fixed here

- Phase 2 scope is: backend writes logs to `/apps/logs/application.log`, Filebeat sidecar reads from the shared volume, registry persists. Scope explicitly excludes database and application business logic.
- Fixing DataSource would require adding a DB, or a profile that disables JPA/DataSource, or similar — all outside Phase 2. The task states: "Do not modify application business logic."
- The app does start far enough to initialize Log4j2 and write logs to the file before failing on DataSource. So for Phase 2 we have confirmed: no filesystem errors, logs written, ownership correct.

### Where it could be fixed later

- Add a Postgres (or other) service and set `SPRING_DATASOURCE_*` for the backend, or
- Introduce a profile (e.g. "log-only") that configures an embedded DB or disables components that require a DB, in a later phase or separate change.

---

## 4. Filebeat — Logstash unreachable

### What happens

Filebeat logs: "Failed to connect to backoff(async(tcp://logstash:5044)): lookup logstash on 127.0.0.11:53: server misbehaving" (or similar DNS/connection errors). There is no Logstash service in this compose.

### Why it is expected in Phase 2

- Phase 2 goal is to have Filebeat read logs from the shared volume and attempt to send to Logstash. We do not add Logstash, Elasticsearch, or TLS in Phase 2.
- So Logstash is unreachable by design. Filebeat stays up, keeps harvesting (harvester running, open_files:1, events active), and retries the output. Events are buffered. When Logstash is added later (e.g. same network or extra_hosts), Filebeat will connect without code changes.

### No fix applied

- No Logstash service was added. Behaviour is documented in PHASE2-known-issues.md and in testing.md.

---

## 5. Phase 2 Contract and TLS Fixes

### Path mismatch (description vs. codebase) — fixed with /apps/logs HARD CONTRACT

- **Problem:** The task description requires docker volume path `/apps/logs/*.log`. The codebase had used `/apps/logs` (singular). Path mismatch caused contract violation and potential drift for Filebeat, Logstash, and Elasticsearch.
- **Root cause:** Phase 1 and Phase 2 were implemented with `/apps/logs`; the description requires `/apps/logs` (plural). No single canonical path matched the description.
- **Fix applied:** Unified contract to `/apps/logs` everywhere. Log4j2: default `LOG_PATH` = `/apps/logs/application.log`. Dockerfile: create `/apps/logs`, chown appuser; do not create or reference `/apps/logs`. docker-entrypoint.sh: chown `/apps/logs`. docker-compose: backend and Filebeat mount `backend-logs:/apps/logs` (and `:ro` for Filebeat). Filebeat: paths `/apps/logs/*.log`. All docs (PHASE0-contract, PHASE2-implementation, PHASE2-decisions, testing, solution, TLS, thinking) updated to state `/apps/logs` as the ONLY supported path. No reference to `/apps/logs` remains.
- **Deferred:** None; path contract is complete and matches the description exactly.

### Plaintext Beats (Filebeat to Logstash)

- **Problem:** Filebeat was sending events to Logstash on port 5044 over plain TCP. The task explicitly requires secure TLS output from Filebeat to Logstash.
- **Root cause:** Phase 2 (and Phase 3) did not enable TLS; Beats input and output were plaintext.
- **Fix applied:** Generated a local self-signed CA and Logstash server certificate using `tls/gen-certs.sh` (openssl; SAN includes DNS:logstash). Logstash Beats input: `ssl_enabled => true`, `ssl_certificate => "/usr/share/logstash/certs/logstash.crt"`, `ssl_key => "/usr/share/logstash/certs/logstash.key"`. Filebeat output: `ssl.enabled: true`, `ssl.certificate_authorities: ["/usr/share/filebeat/certs/ca.crt"]`. Docker Compose: mount `./tls` at `/usr/share/logstash/certs` and `/usr/share/filebeat/certs` (read-only). Private keys (ca.key, logstash.key) are gitignored; run gen-certs.sh before first `docker compose up`. Mutual TLS (client cert) not implemented.
- **Deferred:** mTLS (client certificate verification) is optional and not implemented. Production CA and cert lifecycle are out of Phase 2 scope.

### Field ownership (redundant app and env from Filebeat)

- **Problem:** Log events contained both application identity (service.name, service.environment, env from Log4j2 EcsLayout) and Filebeat-added fields (app, env). Redundant and ambiguous; the task required a single authoritative identity model (service.name, service.environment) and to keep app only if required.
- **Root cause:** Filebeat was configured with `fields.app` and `fields.env` with `fields_under_root: true`, adding duplicate identity to every event.
- **Fix applied:** Removed the `fields` block (app, env) from filebeat.yml. Identity now comes from the application only (service.name, service.environment, env). Updated PHASE0-contract.md and PHASE2-implementation.md with the event field contract (mandatory: service.name, service.environment, env; no redundant identity from Filebeat). No ECS fields renamed or transformed by processors.
- **Deferred:** None; field contract is documented. "app" is not added by Filebeat; service.name is the authoritative service identity.

---

## 6. What remains intentionally unresolved (Phase 3 and beyond)

- **Logstash:** Now in this compose with TLS on 5044; stdout output only. No Elasticsearch output.
- **TLS (Filebeat to Logstash):** Implemented; server TLS only. mTLS deferred.
- **Elasticsearch and Kibana:** Not in scope; no indexing or dashboards. Deferred to Phase 4.
- **Backend DataSource:** Backend exits on missing DB; fix belongs to app/deployment configuration, not Phase 2 logging.
- **Running Filebeat as non-root:** Phase 2 runs Filebeat as root so it can read app-owned log files; hardening can be done later.

Phase 2 is complete and ready for Elasticsearch in the next phase. The above items are explicitly left for Phase 3/4 or separate work.

---

# Phase 3 Runtime Fixes and Solutions

## 1. Container name conflict ("suljhaoo-logstash" already in use)

### What was broken

- **Error:** `docker compose up -d` failed with: "The container name \"/suljhaoo-logstash\" is already in use by container ...".
- **Cause:** A previous run (or a manually created container) had left a container named `suljhaoo-logstash` on the host. Compose tries to create a container with the same name and fails.

### Why it happened

- The Compose service uses `container_name: suljhaoo-logstash`. Docker requires container names to be unique. If an old or external container with that name exists, Compose cannot start the new one.

### What was fixed

- **Fix:** Remove the conflicting container: `docker rm -f suljhaoo-logstash`, then re-run `docker compose up -d`.
- No change to docker-compose.yml or Logstash config was required.

### Why this is correct

- After removing the old container, Compose can create and start the logstash service normally. Filebeat then connects to Logstash and events flow. No other Phase 3 behaviour was affected.

---

## 2. What is deferred to Phase 4 (and beyond)

- **Elasticsearch output:** Logstash currently outputs only to stdout. Adding an Elasticsearch output, index templates, and index lifecycle is out of Phase 3 scope and deferred to Phase 4.
- **Kibana:** No UI or dashboards; deferred to a later phase.
- **TLS (Filebeat to Logstash):** Implemented in Phase 2 contract/TLS fixes. mTLS and production cert management are deferred.
- **Filters:** No Logstash filters in Phase 3; enrichment, parsing, or transformation can be added in a later phase if needed.
- **Production hardening:** Resource limits, healthchecks, non-root Logstash, and security hardening are not in Phase 3 scope.
- **Application/Phase 2 changes:** No changes to application logging, Filebeat config (other than depends_on), or Phase 2 volumes/ownership. Any such changes would be done only to fix runtime issues, which were resolved by removing the conflicting container.

Phase 3 is complete when: Logstash receives events from Filebeat and prints them to stdout, and all Phase 3 documentation is written. The above items are explicitly left for Phase 4 or later.

---

# Phase 4 Runtime Fixes and Solutions

## 1. Logstash pipeline 403 — logstash_writer missing cluster:monitor/main

### What was broken

- **Error:** Logstash pipeline failed at startup: "Could not read Elasticsearch. Please check the privileges"; Elasticsearch main endpoint returns 403; "action [cluster:monitor/main] is unauthorized for user [logstash_writer]".
- **Cause:** The Logstash Elasticsearch output plugin performs a health check against the cluster (e.g. `GET /` or `_cluster/health`) during registration. The role `logstash_writer` had only `manage_index_templates` and index privileges on `*-logs-*`; it did not have the `monitor` cluster privilege required for that health check.

### What was fixed

- **Fix:** In `scripts/elasticsearch-init-security.sh`, the role `logstash_writer` was updated to include `monitor` in the `cluster` array: `"cluster": ["monitor", "manage_index_templates"]`.
- After re-running the stack (`docker compose down -v` then `docker compose up -d`), es-setup recreated the role with `monitor`; Logstash then started successfully and indexed events.

### Why this is correct

- `monitor` allows only cluster-level read (e.g. health, node info); it does not grant read access to log indices. So least privilege is preserved while satisfying the plugin’s startup check.
- No application or Filebeat changes; only the ES role definition.

---

## 2. Index name placeholder not interpolated — %{[service][name]} vs %{service.name}

### What was broken

- **Error:** Logstash warned: "Badly formatted index, after interpolation still contains placeholder: [%{[service][name]}-logs-2026.01.28]". Events were not written to Elasticsearch; indices were empty.
- **Cause:** Events from Filebeat use ECS-style fields; `service.name` is present as a top-level (flat) field. The index was set to `%{[service][name]}-logs-%{+YYYY.MM.dd}`, which expects a nested structure `event["service"]["name"]`. With a flat `"service.name"` key, `[service][name]` did not resolve, so the placeholder remained.

### What was fixed

- **Fix:** In `logstash/logstash.conf`, the Elasticsearch output index was changed from `%{[service][name]}-logs-%{+YYYY.MM.dd}` to `%{service.name}-logs-%{+YYYY.MM.dd}`.
- After restarting Logstash, the index name resolved to e.g. `suljhaoo-backend-service-logs-2026.01.28` and documents were indexed.

### Why this is correct

- `%{service.name}` in Logstash sprintf refers to the field path `service.name` (flat or nested), which matches how Beats/ECS emit the field. No extra filters were added; only the index string was corrected. Index format remains one per service per day as required.

---

## 3. Elasticsearch over HTTP (no TLS) — then HTTPS-only fix

- **Earlier state:** Elasticsearch listened on HTTP (port 9200). Logstash and es-setup used `http://elasticsearch:9200`. Kibana used HTTPS to nginx:9200, which proxied to Elasticsearch over HTTP. This mixed HTTP/HTTPS caused confusion and violated the requirement that Elasticsearch be accessed only over HTTPS.
- **Root cause:** Elasticsearch’s HTTP layer was never configured for SSL; only security (auth) was enabled. Clients used HTTP or went through an HTTPS proxy that spoke HTTP to ES.
- **Fix (HTTPS-only):**
  - **Elasticsearch:** Added certificate signed by the existing CA (elasticsearch.key, elasticsearch.crt, SAN DNS:elasticsearch) and enabled HTTP SSL via `xpack.security.http.ssl.enabled=true`, `xpack.security.http.ssl.key`, `xpack.security.http.ssl.certificate`. Certs mounted read-only. No CA regeneration; existing CA reused.
  - **Logstash:** Elasticsearch output now uses `hosts => ["https://elasticsearch:9200"]`, `ssl => true`, `ssl_certificate_verification => true`, `cacert => "/usr/share/logstash/certs/ca.crt"`. No HTTP.
  - **es-setup:** Uses `ES_URL=https://elasticsearch:9200` and all curl calls use `--cacert /scripts/ca.crt`. CA cert mounted at `/scripts/ca.crt`.
  - **Kibana:** Switched from `https://nginx:9200` to `https://elasticsearch:9200` with existing CA mount. Removed the nginx server block that listened on 9200 and proxied to Elasticsearch (no more HTTP path to ES).
- **Verification:** `curl http://elasticsearch:9200` fails (empty reply / connection behaviour). `curl -k https://elasticsearch:9200/_cluster/health` (or with `--cacert`) and auth returns cluster health. Logstash pipeline connects to Elasticsearch over HTTPS and indexes events.

---

## 4. What was deferred (Phase 4 scope)

- **Kibana:** No UI, dashboards, or Discover.
- **Alerts:** No Watcher or alerting.
- **Tuning:** No ILM, index lifecycle, or performance tuning.
- **Verification as logstash_writer:** `_cat/indices` returns 403 for `logstash_writer` (no indices:monitor). Verification of index list and document count is done with the `elastic` user (e.g. from the elasticsearch container). This is documented in testing.md.

Phase 4 is complete when: logs appear in Elasticsearch indices, indices are separated per service (and day), logstash_writer is in use, Elasticsearch has no public ports, and documentation (PHASE4-overview, PHASE4-implementation, PHASE4-decisions, testing.md, solution.md) is updated.

---

# Phase 5 Runtime Fixes and Solutions (Kibana & nginx)

## 1. Kibana/nginx container name conflicts

### What was broken

- **Error:** `docker compose up -d` failed with: "The container name \"/suljhaoo-kibana\" is already in use" and "The container name \"/suljhaoo-nginx\" is already in use".
- **Cause:** A previous run (or manually created containers) had left containers named `suljhaoo-kibana` and `suljhaoo-nginx` on the host. Compose tries to create containers with the same names and fails.

### What was fixed

- **Fix:** Remove the conflicting containers: `docker rm -f suljhaoo-kibana suljhaoo-nginx`, then re-run `docker compose up -d`.
- No change to docker-compose.yml or service definitions was required.

### Why this is correct

- After removing the old containers, Compose can create and start the kibana and nginx services normally. Kibana and nginx then start and HTTPS access to Kibana via nginx works as intended.

---

## 2. Missing Phase 5 environment variables

### What was broken

- **Error:** `docker compose down -v` (or `up`) failed with: "required variable KIBANA_SYSTEM_PASSWORD is missing a value: Set KIBANA_SYSTEM_PASSWORD in .env".
- **Cause:** Phase 5 added `KIBANA_SYSTEM_PASSWORD` and `KIBANA_USER_PASSWORD` as required env vars for es-setup and Kibana; `.env` did not yet contain them.

### What was fixed

- **Fix:** Add to `.env`: `KIBANA_SYSTEM_PASSWORD=...` and `KIBANA_USER_PASSWORD=...` (example values used for testing; do not commit real secrets). Document these in `env.example` as Phase 5 variables.
- No change to docker-compose.yml logic; only provisioning of the required values.

### Why this is correct

- es-setup must set the `kibana_system` password and create `kibana_user` with a password before Kibana starts. Kibana needs `KIBANA_SYSTEM_PASSWORD` to connect to Elasticsearch. Providing these via `.env` keeps secrets out of the repo and matches the documented pattern for Phase 4.

---

## 3. What was deferred (Phase 5 scope)

- **Dashboards / visualizations:** No dashboards, saved searches, or index patterns were created.
- **Alerts / Watcher:** Alerting remains out of scope.
- **Spaces / advanced RBAC:** Only a single read-only user (`kibana_user`) is defined; no spaces or complex roles.
- **TLS hardening:** Cipher suites, HSTS, OCSP stapling, and mutual TLS are not configured at nginx.
- **SSO / Identity providers:** No SSO (OIDC/SAML) integration; Kibana uses basic authentication for `kibana_user`.

Phase 5 is complete when: Kibana is accessible via HTTPS through nginx, Elasticsearch is not publicly exposed, `kibana_system` is used only internally (not for UI login), `kibana_user` can view logs but cannot modify them, and documentation (PHASE5-overview, PHASE5-implementation, PHASE5-decisions, testing.md, solution.md) is updated.

---

# Logstash Ops Hardening – Restart & Persistence

## 1. Incomplete restart policies

### What was broken

- Some services (e.g. Elasticsearch, Logstash, Kibana, nginx) did not have explicit restart policies in `docker-compose.yml`.
- A transient Docker or host issue could leave parts of the stack stopped until manually restarted.

### What was fixed

- Added `restart: unless-stopped` for:
  - `elasticsearch`
  - `logstash`
  - `kibana`
  - `nginx`
- Backend and Filebeat already had appropriate restart behavior.

### Why this is correct

- `restart: unless-stopped` ensures containers are restarted automatically after host reboots or engine restarts, without masking permanent configuration errors.
- It aligns all long-lived services with a consistent resilience policy.

---

## 2. Logstash persistence and survivability

### What was improved

- Ensured Logstash’s Persistent Queue and Dead Letter Queue live under a dedicated Docker volume:
  - `logstash-data` mounted at `/usr/share/logstash/data`.
- Confirmed Logstash creates:
  - `/usr/share/logstash/data/queue` (PQ)
  - `/usr/share/logstash/data/dead_letter_queue` (DLQ)
- Verified behavior when:
  - Elasticsearch is stopped and restarted (Logstash retries and resumes without crashing).
  - Logstash is restarted (PQ/DLQ directories persist and are reused).

### Why this is correct

- PQ/DLQ are now explicitly disk-backed and survive container restarts, matching the reliability goals.
- Restart policies plus persistent volumes allow the stack to handle common infra events (daemon restart, host reboot) without changing pipeline logic.

---

## 3. What remains out of scope for this phase

- No changes to Filebeat configuration, application logging, Elasticsearch mappings/ILM, or Kibana dashboards.
- No alerting or monitoring rules were added; these will be layered on later using the documented signals and runbooks.
- No automatic PQ/DLQ cleanup or replay tooling; operations remain manual and explicit by design in this phase.
