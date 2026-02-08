To run the stack locally with copy-paste commands, see the repository root **README.md**, section "Run locally (logging stack)".

---

## 1. Context and Problem Statement

Modern systems produce a large volume of logs: every request, every error, every background job. If those logs stay on a single server’s disk, they are almost useless the moment you scale out or a machine dies.

**Problem statement**

- **Visibility across many instances:** Our backend will run on multiple containers or nodes. We need a single place to see what is happening across all of them.
- **Durability:** If a container dies or a node is replaced, we do not want to lose the logs that explain why.
- **Troubleshooting speed:** When an incident happens, we need to search, filter, and correlate logs quickly, not SSH into random boxes and `grep` text files.
- **Compliance / audit:** For some environments we need to be able to show what happened, when, and by whom. That means logs must be preserved and queryable.

**Why local logs are not enough in production**

- Containers are ephemeral. When a pod or container restarts, its local filesystem is often wiped.
- Even with persistent disks, it is hard to search logs across many machines.
- Local logs are hard to secure and back up; every node becomes a “log server.”
- Diagnosing cross-service issues (e.g. a request that flows through multiple services) is almost impossible with only local files.

**Target guarantees**

- **Durability:** Once a log line is written by the app and picked up by Filebeat, it should be very hard to lose, even if services restart or Elasticsearch is temporarily down.
- **Security:** Logs can contain sensitive data. Transport between components is encrypted with TLS; Elasticsearch is not exposed to the public Internet; users authenticate via Kibana.
- **Observability:** Every important event ends up in a central place (Elasticsearch) where it can be searched, filtered, and visualized via Kibana.

---

## 2. High‑Level Architecture Overview

At a high level, the logging flow is:

> **Backend App** → **Filebeat sidecar** → **Logstash (TLS)** → **Elasticsearch (HTTPS, secured)** → **Kibana (via NGINX HTTPS)**

### Components and their roles

- **Backend (Spring Boot):** Writes structured log lines to a file. It knows business context (user IDs, request IDs, service name, environment, etc.).
- **Filebeat (sidecar):** Lightweight log shipper running next to the app container. It tails the log file from a shared Docker volume and sends entries to Logstash over TLS.
- **Logstash:** Central ingestion and routing component. It receives events from Filebeat, buffers them on disk (Persistent Queue), optionally enriches or transforms them (currently minimal), and sends them to Elasticsearch. Events that cannot be indexed are written to a Dead Letter Queue.
- **Elasticsearch:** Distributed search and storage engine. Stores logs in index-per-service-per-day indices, with security enabled and HTTPS enforced.
- **Kibana:** Web UI used by humans to search and visualize logs. It never talks directly to the database; it uses the `kibana_system` service account and runs behind an NGINX reverse proxy.
- **NGINX:** Terminates public HTTPS and forwards requests to Kibana. Only port 443 is exposed to the outside world.

### Why this order?

- **App → Filebeat:** The app should not know how to talk to Elasticsearch or Logstash. It only knows how to write logs. Filebeat is built specifically for safe, efficient log shipping.
- **Filebeat → Logstash:** Logstash can implement routing, buffering, and enrichment, and connect to Elasticsearch with TLS and authentication. Filebeat stays simple.
- **Logstash → Elasticsearch:** Central place to apply output logic and control write behavior (index naming, retry, PQ, DLQ).
- **Elasticsearch → Kibana:** Search and visualization are separate from ingest. Kibana reads from Elasticsearch without affecting ingestion.
- **Kibana → NGINX → users:** NGINX gives us a single hardened TLS entrypoint and lets us keep Elasticsearch completely private.

---

## 3. Application Logging (Spring Boot / Log4j2)

### How logs are produced

The backend is a Spring Boot application using **Log4j2** as its logging engine (via `spring-boot-starter-log4j2`). All application logs are emitted through SLF4J API calls, which Log4j2 implements.

The central configuration file is:

- **`src/main/resources/log4j2-spring.xml`**

This file defines:

- A **file appender** that writes to:  
  `fileName="/apps/logs/application.log"`  
  `filePattern="/apps/logs/application-%d{yyyy-MM-dd}.log.gz"` (for rotation, if configured).
- A **console appender** that writes the same JSON to stdout (useful for local debugging and container logs).
- A **JSON layout** compatible with Elastic Common Schema (ECS), producing **NDJSON** (newline-delimited JSON):
  - Each log entry is a single JSON object on one line.
  - No multi-line stack traces; exceptions are encoded as structured fields.

### Why JSON (NDJSON)?

- **Machine readable:** Elasticsearch and Logstash can parse fields without brittle regex parsing.
- **Human readable:** A single log line is still understandable if you pretty-print the JSON.
- **Consistent:** Every log record has the same key set (e.g. `@timestamp`, `log.level`, `message`, `service.name`, `env`, `trace.id`).
- **Efficient:** NDJSON is streaming‑friendly and works well with Filebeat’s line‑oriented tailing.

### The hard log path contract: `/apps/logs/*.log`

We enforce a **single canonical path**:

- All backend containers write to:  
  ` /apps/logs/application.log`
- Filebeat only reads ` /apps/logs/*.log`.

This is a **hard contract** for two reasons:

- **Shared volume addressing:** The Docker volume between backend and Filebeat must mount to the same path inside both containers. If the app writes to `/tmp/logs` but Filebeat reads `/apps/logs`, no logs will flow.
- **Downstream assumptions:** Logstash and later phases (Elasticsearch/Kibana, index patterns) assume that `log.file.path` will look like `/apps/logs/...`. Changing this path breaks filters, dashboards, and test scripts.

The documentation explicitly calls out `/apps/logs/*.log` as the only supported log path in:

- `docs/logging/PHASE0-contract.md` (Phase 0)
- `docs/logging/PHASE2-implementation.md` (Filebeat + volume wiring)

### Field ownership: `service.name`, `env`, `trace.id`

The **application** is the source of truth for identity fields:

- `service.name` – logical service name (e.g. `suljhaoo-backend-service`).
- `service.environment` – environment (e.g. `dev`, `staging`, `prod`).
- `env` – human‑friendly environment shorthand, kept for convenience.
- `trace.id` – when present, allows correlation across services/requests.

These fields are injected via Log4j2’s configuration and MDC:

- We use a servlet filter / interceptor (in app code, implemented in Phase 1) to put a request/trace ID into the MDC (Mapped Diagnostic Context).
- Log4j2’s ECS layout is configured to emit `trace.id` when present.

Downstream components (Filebeat, Logstash) **do not try to guess** or overwrite these fields. They pass them through as-is to Elasticsearch.

---

## 4. Dockerization of the Backend

### Why Docker?

- **Consistency:** Every developer, CI job, and production node runs the same image.
- **Isolation:** The app’s dependencies (JDK, OS libraries) are controlled and versioned.
- **Sidecar pattern:** Running Filebeat next to the app is straightforward with Docker Compose/Kubernetes.

### Dockerfile decisions

The `Dockerfile` does three important things:

- **Non‑root user (`appuser`):**
  - Creates a user and group (e.g. UID 1001).
  - Copies the built JAR into `/app/app.jar`.
  - Sets `USER appuser` for runtime security (limits damage if app is compromised).

- **Filesystem layout:**
  - Creates `/apps/logs` directory at build time.
  - Sets ownership to `appuser:appuser`, ensuring the app can write logs there.
  - Leaves application code in `/app`, separate from logs.

- **Base image:**
  - Uses a slim JDK runtime image (e.g. `eclipse-temurin:17-jre-alpine`) to keep image size and attack surface smaller.

### docker-entrypoint.sh – root → appuser

The entrypoint script (`docker-entrypoint.sh`) is responsible for safe startup:

1. Container starts as **root**.
2. It ensures `/apps/logs` exists and is owned by `appuser`:
   - `chown -R appuser:appuser /apps/logs`
3. It then drops privileges and execs the JVM as `appuser`:
   - `exec su -s /bin/sh -c "exec java -jar /app/app.jar" appuser`

Why this matters:

- If we skipped the `chown`, Docker would mount `backend-logs` as root-owned, and `appuser` would get `Permission denied` when writing logs.
- If we ran the JVM as root, we’d be violating least privilege, increasing the blast radius of any compromise.
- The explicit `exec` ensures signals (SIGTERM from Docker) are delivered to the JVM process, so graceful shutdown works and logs flush correctly.

What would break if misconfigured:

- Wrong path (`/app/logs` vs `/apps/logs`) → app logs to the wrong place; Filebeat sees nothing.
- Missing `chown` → app cannot write logs to the volume → lost logs.
- Running as root → potential security issues, and inconsistent file ownership that breaks Filebeat (which runs as `root` but expects shared volume data owned by appuser).

---

## 5. Filebeat as a Sidecar (Extreme Detail)

### What “sidecar” means here

A **sidecar** is a container that runs alongside another container (the main app) in the same pod or Compose service group and shares some resources.

In our case:

- `backend` and `filebeat` share the `backend-logs` volume, both mounted at `/apps/logs`.
- They run in the same Docker network (`suljhaoo-network`) so Filebeat can reach `logstash:5044`.

The sidecar is not installed on the host. It is shipped and versioned with the app deployment so logging behavior stays consistent across environments.

### Why Filebeat is not installed on the host

- We avoid coupling our app to host configuration (systemd units, package managers).
- Upgrades to Filebeat can be tested and deployed per‑service, not system‑wide.
- In containerized environments (Kubernetes), host‑level agents may not have direct access to container filesystems, but a sidecar can share the same volume.

### Volume sharing with backend

In `docker-compose.yml`:

- `backend`:

  ```yaml
  volumes:
    - backend-logs:/apps/logs
  ```

- `filebeat`:

  ```yaml
  volumes:
    - backend-logs:/apps/logs:ro
    - filebeat-data:/usr/share/filebeat/data
    - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
    - ./tls:/usr/share/filebeat/certs:ro
  ```

This ensures:

- The app writes to `/apps/logs`.
- Filebeat sees exactly the same path, read‑only.
- Filebeat’s own state (registry, offset tracking) is stored under `filebeat-data`, so restarts don’t cause re-reading the entire file.

### How Filebeat discovers logs

The main settings in `filebeat/filebeat.yml`:

- **Input:**

  ```yaml
  filebeat.inputs:
    - type: filestream
      id: backend-logs
      paths:
        - /apps/logs/*.log
      json.add_error_key: true
      json.keys_under_root: true
  ```

  - Uses `filestream` input to tail files.
  - Reads NDJSON lines and parses them as JSON, injecting fields directly into the event.

- **Output to Logstash with TLS:**

  ```yaml
  output.logstash:
    hosts: ["logstash:5044"]
    ssl.enabled: true
    ssl.certificate_authorities: ["/usr/share/filebeat/certs/ca.crt"]
  ```

### Why Filebeat does **not** add identity fields

Earlier in Phase 2 we explicitly removed extra `fields: { app: ..., env: ... }` from Filebeat:

- The app already emits `service.name` and `service.environment` and `env`.
- Duplicating identity in Filebeat (`app`, `env`) creates ambiguity: which one is the “truth”?
- Instead, Filebeat’s job is to **transport** logs and add only transport‑level metadata (e.g. `agent.*`, `log.file.path`), not business identity.

### Back-pressure and retries at Filebeat

- If Logstash slows or becomes unavailable:
  - Filebeat will retry the connection with exponential back-off.
  - It will keep reading logs up to its internal limits and buffer small amounts in memory.
  - It will not drop data unless its own buffers overflow or the log files are rotated out from under it.

Limits:

- Filebeat is not a large buffer. Its `filebeat-data` registry stores offsets and state, not full logs.
- Long‑duration outages rely on Logstash’s Persistent Queue for durability.

### Failures Filebeat can and cannot handle

- **Handles well:**
  - Short network glitches.
  - Transient Logstash restarts.
  - Container restarts (registry ensures it resumes where it left off).

- **Cannot fully protect against:**
  - If the underlying `backend-logs` volume is deleted, data is gone.
 - If the log file is rotated and removed before Filebeat reads it, those entries are lost.
  - If Logstash is down for much longer than PQ can absorb, upstream back-pressure may cause Filebeat to slow or stop, and new logs may be dropped by the app (depending on its own logging behavior).

---

## 6. Logstash – The Pipeline Brain

### Why Logstash instead of direct Filebeat → Elasticsearch

We introduced Logstash for several reasons:

- **Separation of concerns:** Keep Filebeat simple and let Logstash handle complex logic (routing, enrichment, schema changes) when needed.
- **Buffering:** Logstash’s PQ gives us disk‑backed buffering and more flexible retry behavior than direct Beats → Elasticsearch.
- **Security and roles:** Logstash uses a dedicated `logstash_writer` user with specific index permissions, rather than giving Filebeat cluster‑wide credentials.

### Beats input on port 5044 with TLS

In `logstash/logstash.conf`:

```ruby
input {
  beats {
    port => 5044
    ssl_enabled => true
    ssl_certificate => "/usr/share/logstash/certs/logstash.crt"
    ssl_key => "/usr/share/logstash/certs/logstash.key"
  }
}
```

- Listens on `0.0.0.0:5044`.
- Uses the CA-signed `logstash.crt` / `logstash.key` generated by `tls/gen-certs.sh`.
- Filebeat is configured with `ssl.certificate_authorities` pointing to the same CA (`ca.crt`), so it verifies the Logstash server certificate.

### No filters (by design)

- In Phase 3 and 4 we deliberately **did not add filters**.
- The pipeline currently forwards the parsed JSON as-is:

```ruby
filter {
  # (none in Phase 3/4; future phases may enrich)
}
```

This keeps Phase 3 focused purely on **delivery correctness** (does Filebeat → Logstash → stdout work?) and avoids coupling the ingest path to schema transformations before Elasticsearch is introduced.

### Output to Elasticsearch (Phase 4)

In `logstash/logstash.conf`:

```ruby
output {
  elasticsearch {
    hosts => ["https://${ELASTICSEARCH_HOST:elasticsearch}:9200"]
    user => "logstash_writer"
    password => "${LOGSTASH_WRITER_PASSWORD}"
    index => "%{service.name}-logs-%{+YYYY.MM.dd}"
    ssl => true
    ssl_certificate_verification => true
    cacert => "/usr/share/logstash/certs/ca.crt"
  }
  stdout {
    codec => rubydebug
  }
}
```

- **Index naming strategy:**
  - Uses `%{service.name}-logs-%{+YYYY.MM.dd}`, e.g. `suljhaoo-backend-service-logs-2026.01.28`.
  - One index per service per day.
- **Security:**
  - Authenticates as `logstash_writer` (write + index template privileges only).
  - Uses HTTPS with `cacert` pointing to the same CA used for ES.

---

## 7. Logstash Reliability (PQ + DLQ) – Deep Dive

### What back-pressure actually means

Back-pressure is the system’s way of saying **“slow down, I’m full”**:

- Without PQ:
  - Logstash holds events in memory while they are in-flight.
  - If Elasticsearch is slow, memory usage grows until JVM OOM or drops.
  - Filebeat may keep sending until TCP/back-off kicks in, but the failure modes are nasty.

- With PQ:
  - Incoming events are appended to a disk queue.
  - The output pulls from this queue at the rate Elasticsearch can handle.
  - If ES slows down, the queue grows on disk instead of in memory.

### Persistent Queue internals (at our level)

- Configured in `logstash/logstash.yml`:

  ```yaml
  queue.type: persisted
  queue.max_bytes: 1gb
  queue.checkpoint.acks: 1024
  queue.checkpoint.writes: 1024
  queue.checkpoint.interval: 1000
  queue.drain: true

  path.dead_letter_queue: /usr/share/logstash/data/dead_letter_queue

  dead_letter_queue.enable: true
  dead_letter_queue.max_bytes: 256mb
  ```

- Stored on disk under `/usr/share/logstash/data/queue` (via `logstash-data` volume).
- On startup:
  - Logstash opens the PQ files and begins reading from the last committed position.
  - Any unacknowledged events from previous runs are reprocessed.

### Behavior when Elasticsearch is down

- Logstash output cannot reach ES:
  - Logs show `Elasticsearch Unreachable` and `No Available connections`.
  - PQ continues to accumulate events up to `queue.max_bytes`.
- Filebeat still writes to Logstash until back-pressure builds up:
  - Eventually, once PQ is near full, Logstash will stop accepting new events.
  - Filebeat’s connection attempts back off; ingestion rate drops gracefully.

### Behavior on restart

- If we restart Logstash (`docker compose restart logstash`):
  - PQ and DLQ directories remain because they live in `logstash-data`.
  - After restart, Logstash replays any events that were in PQ but not yet fully delivered.
  - There is no need to re‑read from `/apps/logs` – Filebeat resumes where it left off using its registry.

### Dead Letter Queue (DLQ)

- Enabled via `dead_letter_queue.enable: true`.
- Stored under `/usr/share/logstash/data/dead_letter_queue`.
- Holds events that **cannot be indexed**, even after retries, for reasons like:
  - Mapping conflicts (e.g. string vs number).
  - Invalid index names or rejected documents.
- It does **not** capture:
  - Events that never reach Logstash.
  - Events successfully indexed by Elasticsearch.
  - Events dropped by upstream services.

### Disk-backed guarantees vs non-guarantees

**Guaranteed:**

- Events accepted into PQ are on disk; they persist across Logstash restarts.
- Short/medium outages are absorbed up to `queue.max_bytes`.
- Bad events are preserved in DLQ for later analysis.

**Not guaranteed:**

- No guarantee against:
  - Disk failure or full disk.
  - Infinite retention of PQ/DLQ (bounded by `queue.max_bytes` and `dead_letter_queue.max_bytes`).
  - Perfectly ordered, exactly-once delivery (retries can produce duplicates).

### Failure matrix (Logstash focus)

For a detailed matrix, see:

- `docs/logging/LOGSTASH-RELIABILITY-DEEP-DIVE.md`
- `docs/logging/OPS-HARDENING-AND-RELIABILITY-DEEP-DIVE.md`

Those documents enumerate:

- ES down vs ES slow.
- Logstash restart vs crash.
- Disk full on PQ/DLQ vs ES disk full.

---

## 8. Elasticsearch – Secure Storage Layer

### Why Elasticsearch is private

- In `docker-compose.yml`, Elasticsearch has no `ports:` mapping to the host:

  ```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
    ...
    networks:
      - suljhaoo-network
  ```

- This means:
  - Elasticsearch is only reachable from other containers in the `suljhaoo-network`.
  - End-users cannot hit Elasticsearch directly; they must go through Kibana/NGINX.

### HTTPS-only access

- Elasticsearch HTTP layer is configured with SSL:

  ```yaml
  - xpack.security.enabled=true
  - xpack.security.http.ssl.enabled=true
  - xpack.security.http.ssl.key=/usr/share/elasticsearch/config/certs/elasticsearch.key
  - xpack.security.http.ssl.certificate=/usr/share/elasticsearch/config/certs/elasticsearch.crt
  ```

- Certs are generated by `tls/gen-certs.sh` using our local CA (`tls/ca.crt`, `tls/ca.key`) and mounted read-only into the container.
- All clients (Logstash, es-setup, Kibana) use HTTPS with CA verification:
  - Logstash: `ssl => true`, `cacert => "/usr/share/logstash/certs/ca.crt"`.
  - es-setup: `curl --cacert /scripts/ca.crt https://elasticsearch:9200/...`.
  - Kibana: `ELASTICSEARCH_HOSTS=https://elasticsearch:9200` + `ELASTICSEARCH_SSL_CERTIFICATEAUTHORITIES=/usr/share/kibana/config/certs/ca.crt`.

### Users & roles

- **`elastic`:** Built-in superuser.
  - Only used by `es-setup` script and operators.
  - Password provided via `ELASTIC_PASSWORD` in `.env` (not committed).

- **`logstash_writer` user with `logstash_writer` role:**
  - Created by `scripts/elasticsearch-init-security.sh`.
  - Role permissions:
    - `cluster`: `["monitor", "manage_index_templates"]`
    - `indices`: `names: ["*-logs-*"]`, `privileges: ["write", "create_index"]`
  - Used by Logstash output to write logs and manage index templates, but cannot read/delete arbitrary data.

- **`kibana_system` user:**
  - Built-in service account for Kibana’s own needs (saved objects, index patterns, etc.).
  - Password set by `es-setup` from `KIBANA_SYSTEM_PASSWORD`.
  - Kibana uses this user to connect to Elasticsearch; it is not exposed to end users.

### Index-per-service-per-day strategy

- Indices are named: `<service.name>-logs-YYYY.MM.DD`
  - Example: `suljhaoo-backend-service-logs-2026.01.28`.

Why this scales:

- **Operational isolation:** You can delete or reindex one service’s logs without affecting others.
- **ILM-friendly:** Later, we can apply ILM policies per service, per environment.
- **Search performance:** Time-based indices reduce the working set per query and make shard allocation more predictable.

---

## 9. Kibana – Human Interface

### Why Kibana is not exposed directly

- Kibana itself is an HTTP server; exposing it directly would:
  - Put another HTTP stack on the edge.
  - Require configuring TLS inside Kibana and managing certificates per instance.
  - Complicate cross‑cutting concerns (rate limiting, headers, WAF).

Instead:

- We run Kibana only on the internal Docker network (`suljhaoo-network`).
- We place NGINX in front, listening on port 443, and proxying to Kibana.

### NGINX reverse proxy role

- In `docker-compose.yml`:

  ```yaml
  nginx:
    image: nginx:1.27-alpine
    ports:
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./tls/nginx.crt:/etc/nginx/certs/nginx.crt:ro
      - ./tls/nginx.key:/etc/nginx/certs/nginx.key:ro
  ```

- `nginx.conf`:

  ```nginx
  events {}

  http {
    upstream k2 {
      server suljhaoo-kibana:5601;
    }

    server {
      listen 443 ssl;
      server_name _;

      ssl_certificate     /etc/nginx/certs/nginx.crt;
      ssl_certificate_key /etc/nginx/certs/nginx.key;

      location / {
        proxy_pass http://k2;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        ...
      }
    }
  }
  ```

### kibana_system vs kibana_user

- **`kibana_system`:**
  - Used only by Kibana to talk to Elasticsearch.
  - Has the privileges needed to read indices, manage Kibana’s own internal indices, etc.
  - Not for humans.

- **`kibana_user`:**
  - Custom user with `kibana_readonly` role:
    - `indices: ["*-logs-*"]` with `read`, `view_index_metadata`.
  - Intended for human users of Kibana.
  - They can:
    - Search, filter, and visualize logs.
  - They cannot:
    - Write or delete documents in log indices.
    - Change cluster settings or mappings.

---

## 10. NGINX Reverse Proxy – Security Boundary

### Why NGINX exists

- Provides a single, well-understood TLS termination point.
- Allows us to:
  - Enforce HTTPS only.
  - Add headers (`X-Forwarded-Proto`) so Kibana knows it is behind HTTPS.
  - Insert rate limits, basic auth, or WAF rules in the future without touching Kibana.

### Only port 443 is exposed

- In `docker-compose.yml`, NGINX is the **only** service with a host-exposed port:

  ```yaml
  ports:
    - "443:443"
  ```

- Elasticsearch, Logstash, Kibana, and Filebeat are only reachable on the internal Docker network.

### TLS responsibilities

- NGINX:
  - Terminates TLS for browser requests using `nginx.crt` / `nginx.key` signed by our CA.
  - Presents the certificate the browser validates (self-signed for dev; production would use a real CA).
- Kibana:
  - Listens on plain HTTP on `:5601` (trusted internal network).
  - Sees the original protocol in `X-Forwarded-Proto: https`.

This separation keeps TLS configuration and certificate management in one place (NGINX) for the edge, while internal mTLS and upstream TLS (to Elasticsearch) are handled separately.

---

## 11. TLS Everywhere – Uniformity

### Why mixed HTTP/HTTPS is dangerous

- If some components use HTTP and others use HTTPS:
  - Operators may think traffic is encrypted end-to-end when it is not.
  - Man‑in‑the‑middle attacks become easier on the plaintext leg.
  - Debugging becomes confusing (e.g. `curl http://...` works differently from `https://...`).

We explicitly removed mixed usage so that:

- Any attempt to hit `http://elasticsearch:9200` fails (empty reply / no listener).
- All clients use `https://...` with either `--cacert` or a configured CA bundle.

### How the CA and certs are generated

- `tls/gen-certs.sh`:
  - Creates `ca.key` + `ca.crt` (development CA).
  - Generates:
    - `logstash.key` / `logstash.crt` (CN/SAN: `logstash`).
    - `nginx.key` / `nginx.crt` (CN/SAN: `localhost`).
    - `kibana.key` / `kibana.crt` (SAN: `nginx`, used previously for Kibana→ES via nginx).
    - `elasticsearch.key` / `elasticsearch.crt` (SAN: `elasticsearch`).

Private keys and certs are gitignored (`.gitignore`), and the script is idempotent enough for dev use (reuses CA if present).

### Who trusts whom

- **Filebeat → Logstash:**
  - Filebeat trusts `ca.crt` when connecting to `logstash:5044`.
  - Logstash presents `logstash.crt` (signed by the same CA).

- **Logstash / es-setup / Kibana → Elasticsearch:**
  - All use HTTPS to `elasticsearch:9200`.
  - Each has a copy of `ca.crt` mounted and uses it to validate `elasticsearch.crt`.

- **Browser → NGINX:**
  - Browser is configured (for dev) to trust `ca.crt` so `nginx.crt` is accepted.
  - In a real environment this would be a public or corporate CA.

### What is intentionally **not** implemented (TLS)

- **No mutual TLS (mTLS):**
  - Logstash does not require client certificates from Filebeat.
  - Elasticsearch does not require client certificates from Logstash/Kibana.
  - This keeps the stack simpler for initial deployment.

- **No advanced TLS hardening yet:**
  - No strict ciphersuite tuning.
  - No HSTS, OCSP stapling, or certificate rotation automation.

Those can be layered on once the core pipeline is stable and well‑understood.

---

## 12. Operational Guarantees Summary

### What is guaranteed

- **Centralization:**
  - All app logs from `/apps/logs` are collected and sent through the pipeline to Elasticsearch (assuming Filebeat and Logstash are running).

- **Encryption in transit:**
  - Filebeat ↔ Logstash: TLS.
  - Logstash / es-setup / Kibana ↔ Elasticsearch: HTTPS with CA verification.
  - Browser ↔ NGINX: HTTPS with TLS.

- **Durability:**
  - Once a log line is picked up by Filebeat and written into Logstash’s PQ, it is stored on disk and survives Logstash restarts.
  - Short/medium outages of Elasticsearch are absorbed by PQ up to `queue.max_bytes`.

- **Access control:**
  - Elasticsearch is private, not exposed to the Internet.
  - Service accounts (`logstash_writer`, `kibana_system`) have least‑privilege roles.
  - Human users use `kibana_user` with read‑only access to log indices.

- **Observability of failures:**
  - Logstash and Filebeat logs clearly show TLS issues, connection failures, and DLQ writes.
  - testing.md and deep-dive docs capture the exact commands to reproduce and verify behavior.

### What is **not** guaranteed

- **Infinite retention or capacity:**
  - PQ and DLQ are bounded by configuration and underlying disk.
  - If disk fills, operators must intervene.

- **Exactly-once semantics:**
  - Retries, restarts, and PQ replay can produce duplicate events.
  - Consumers (dashboards, alerting) should be tolerant of duplicates.

- **Automatic remediation:**
  - No automatic DLQ replay.
  - No automatic index lifecycle management or cold storage.

### Failure behavior in one paragraph

If Elasticsearch goes down, Filebeat continues to send logs to Logstash until the Logstash PQ fills. Logstash writes every event to disk before acknowledging it, so as long as there is space in `logstash-data`, those events are safe even if Logstash is restarted. When Elasticsearch comes back, Logstash drains the PQ and catches up. If some events cannot be indexed (e.g. schema mismatch), they are captured in DLQ for later investigation. At no point are those “bad” events silently dropped.

---

## 13. File‑by‑File Index (Cheat Sheet)

- **`Dockerfile`** – Builds the Spring Boot application container:
  - Packages the JAR, creates `appuser`, sets up `/apps/logs`, defines entrypoint.

- **`docker-compose.yml`** – Orchestrates the whole stack:
  - Defines services: `backend`, `filebeat`, `logstash`, `elasticsearch`, `kibana`, `nginx`.
  - Wires volumes (`backend-logs`, `filebeat-data`, `es-data`, `logstash-data`).
  - Configures networks and restart policies.

- **`src/main/resources/log4j2-spring.xml`** – Application logging configuration:
  - Sets JSON (NDJSON) layout, file path `/apps/logs/application.log`, service/env/trace fields.

- **`filebeat/filebeat.yml`** – Filebeat configuration:
  - Defines file input on `/apps/logs/*.log`, JSON parsing, and TLS output to `logstash:5044`.

- **`logstash/logstash.conf`** – Logstash pipeline:
  - `input { beats { ... } }`, `output { elasticsearch { ... } stdout { ... } }`.

- **`logstash/logstash.yml`** – Logstash reliability configuration:
  - Enables PQ (`queue.type: persisted`, size, checkpoints).
  - Enables DLQ, sets `path.dead_letter_queue`.

- **`nginx/nginx.conf`** – NGINX reverse proxy:
  - Listens on 443 with TLS.
  - Proxies all HTTP requests to `suljhaoo-kibana:5601`.

- **`tls/gen-certs.sh`** – Certificate generation helper:
  - Creates CA and service certificates for Logstash, NGINX, Kibana (internal), and Elasticsearch.

- **`docs/logging/PHASE*.md`** – Phase documents:
  - `PHASE0-*` – contracts and scope for logging (what “structured logs” and `/apps/logs` mean).
  - `PHASE1-*` – application logging (Log4j2, JSON format, identity fields).
  - `PHASE2-*` – Filebeat sidecar and volume wiring (no TLS / then TLS in Phase 2 + TLS doc).
  - `PHASE3-*` – Logstash introduction, Filebeat → Logstash stdout verification.
  - `PHASE4-*` – Elasticsearch integration, indices, security roles.
  - `PHASE5-*` – Kibana behind NGINX, `kibana_system` / `kibana_user`.

- **`docs/logging/TLS.md`** – Detailed TLS design:
  - How CA and certs are generated and wired into Filebeat, Logstash, Elasticsearch, and NGINX.

- **`docs/logging/LOGSTASH-RELIABILITY-DEEP-DIVE.md`** – In-depth look at PQ/DLQ and Logstash behavior.

- **`docs/logging/OPS-HARDENING-AND-RELIABILITY-DEEP-DIVE.md`** – Operational perspective:
  - Failure modes, capacity planning, and runbooks.

- **`docs/logging/testing.md`** – Chronological log of all verification commands and their PASS/FAIL status.

- **`docs/logging/solution.md`** – Root causes and fixes for all issues found during the build-out.

---

## 14. Summary for Stakeholders

- The **application** writes logs to a local path (`/apps/logs`).
- **Filebeat** tails those files and sends events to Logstash.
- **Logstash** accepts events, buffers them on disk (Persistent Queue), and forwards them to Elasticsearch. Events that cannot be indexed go to the Dead Letter Queue for later inspection.
- **Elasticsearch** stores and indexes the logs.
- **Kibana** is the UI for searching and viewing logs; access requires login and the right permissions.
- **NGINX** terminates TLS at the edge and proxies to Kibana; only port 443 is exposed.

