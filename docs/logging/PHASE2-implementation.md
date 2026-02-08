# Phase 2 Implementation

This document describes what was implemented and where it lives.

## 0. Log path contract (canonical)

- **Agreed path:** `/apps/logs` (directory). Application log file: `/apps/logs/application.log`. Filebeat reads from `/apps/logs/*.log`.
- **Enforced everywhere:** Log4j2 default `LOG_PATH` is `/apps/logs/application.log`; Dockerfile creates `/apps/logs`; docker-compose mounts the shared volume at `/apps/logs` for backend and Filebeat; Filebeat input paths are `/apps/logs/*.log`. No other path is canonical. See PHASE0-contract.md. `/apps/logs` is the HARD CONTRACT; no other path is supported.

## 0.1. Event field contract

- **Authoritative identity (from application only):** `service.name`, `service.environment`, `env`. The application emits these via Log4j2 EcsLayout (serviceName, serviceEnvironment) and KeyValuePair (env). Filebeat does not add or overwrite these; it adds only metadata (e.g. `agent`, `log.file.path`).
- **Mandatory fields (per Phase 0):** timestamp (or @timestamp), level (or log.level), message, service.name, service.environment, env (legacy/transitional, same value as service.environment).
- **No redundant identity from Filebeat:** The previous `fields.app` and `fields.env` in Filebeat were removed so that the application is the single source of identity. No ECS fields are renamed or transformed by processors.

## 1. Filebeat Configuration (filebeat/filebeat.yml)

- **Input:** `type: log` with `allow_deprecated_use: true` (required in Filebeat 8 for the log input). Paths: `/apps/logs/*.log`. Matches the canonical path and any rotated files (e.g. `application.log.1`).

- **JSON:** `json.keys_under_root: true`, `json.overwrite_keys: true`, `json.add_error_key: true`. Each line is decoded as JSON; keys are placed at the root. We do not add processors that change ECS fields.

- **Fields:** No `fields` block. Identity (service.name, service.environment, env) comes from the application only.

- **Paths:** `path.data: /usr/share/filebeat/data`, `path.logs: /usr/share/filebeat/logs`. Registry lives under `path.data` and is persisted via a Docker volume.

- **Output:** `output.logstash.hosts: ["logstash:5044"]`, `ssl.enabled: true`, `ssl.certificate_authorities: ["/usr/share/filebeat/certs/ca.crt"]`. Filebeat connects over TLS; the CA cert is used to verify the Logstash server certificate. Certificates are mounted from the host `tls/` directory (see TLS below).

- **Modules:** `filebeat.config.modules` points at the default modules path with `reload.enabled: false` so we do not load extra inputs by default.

## 2. Docker Compose

- **Volumes:**  
  - `backend-logs`: shared between backend and Filebeat. Backend mounts it at `/apps/logs` (read-write). Filebeat mounts it at `/apps/logs` (read-only).  
  - `filebeat-data`: Filebeat registry and data. Mounted at `/usr/share/filebeat/data`.

- **Backend service:**  
  - Volume `backend-logs:/apps/logs` added.  
  - No `version` key (deprecated in Compose; removed to avoid warnings).  
  - No other backend changes (environment, ports, healthcheck unchanged).

- **Filebeat service:**  
  - Image: `docker.elastic.co/beats/filebeat:8.15.0`.  
  - `user: root` so Filebeat can read log files created by the app user (uid 1001).  
  - Volumes: `backend-logs:/apps/logs:ro`, `filebeat-data:/usr/share/filebeat/data`, `./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro`, `./tls:/usr/share/filebeat/certs:ro`. The `tls/` mount provides `ca.crt` for TLS server verification.  
  - Command: `filebeat -e -strict.perms=false` so logs go to stderr and mounted config is accepted.  
  - `depends_on: backend, logstash` so the backend and Logstash start first.  
  - Same network as backend and Logstash so `logstash` resolves.

- **Logstash service:**  
  - Image: `docker.elastic.co/logstash/logstash:8.15.0`. Ports: `5044:5044`.  
  - Volumes: `./logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro`, `./tls:/usr/share/logstash/certs:ro`. The Beats input uses TLS; server certificate and key are at `/usr/share/logstash/certs/logstash.crt` and `logstash.key`.  
  - Output remains stdout (rubydebug) for verification; no Elasticsearch output.  
  - Environment: `LS_JAVA_OPTS=-Xms256m -Xmx256m`. Same network as backend and Filebeat.

## 3. Backend Container (Dockerfile and entrypoint)

- **Dockerfile:**  
  - `RUN chown -R appuser:appuser /app && mkdir -p /apps/logs && chown -R appuser:appuser /apps/logs` so `/apps/logs` exists and is owned by the app user when no volume is used. Do not create or reference `/app/logs`.  
  - `COPY docker-entrypoint.sh`, `RUN chmod +x`, then `USER root` and `ENTRYPOINT ["/app/docker-entrypoint.sh"]`. The JVM is no longer started directly; the entrypoint does.

- **docker-entrypoint.sh:**  
  - Runs as root. Runs `chown -R appuser:appuser /apps/logs` so that when a volume is mounted at `/apps/logs` (and is initially root-owned), the app user can write.  
  - Then `exec su -s /bin/sh -c "exec java -jar /app/app.jar" appuser` so the main process is the JVM as appuser. The argument order (`-s /bin/sh -c "..."` before the username) is required for Alpine/BusyBox `su`.  
  - No application or logging code changes; Phase 1 behaviour is unchanged.

## 4. TLS (Filebeat to Logstash)

- **Purpose:** Encrypt Beats traffic on port 5044. No plaintext Beats; required before production or Elasticsearch.
- **What (no secrets in docs):** A self-signed CA and a Logstash server certificate (SAN: logstash, localhost, 127.0.0.1) are generated by `tls/gen-certs.sh`. Outputs: `tls/ca.crt`, `tls/ca.key`, `tls/logstash.crt`, `tls/logstash.key`. Private keys are gitignored; run the script before first `docker compose up`.
- **Where:** Logstash Beats input: `ssl_enabled => true`, `ssl_certificate => "/usr/share/logstash/certs/logstash.crt"`, `ssl_key => "/usr/share/logstash/certs/logstash.key"`. No client certificate verification (mTLS deferred). Filebeat output: `ssl.enabled: true`, `ssl.certificate_authorities: ["/usr/share/filebeat/certs/ca.crt"]`. Both containers mount `./tls` read-only at `/usr/share/logstash/certs` and `/usr/share/filebeat/certs` respectively.

## 5. No Application Code Changes

Logging setup (Log4j2, ECS layout, path, trace filter) is unchanged. The application still writes to `/apps/logs/application.log` (or `LOG_PATH`) and to stdout. Phase 2 adds Filebeat, Logstash (stdout only), TLS between them, and the documented path and field contracts.
