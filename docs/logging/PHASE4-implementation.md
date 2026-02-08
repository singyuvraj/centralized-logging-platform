# Phase 4 – Implementation Details

## 1. Elasticsearch Service (docker-compose.yml)

- **Image:** `docker.elastic.co/elasticsearch/elasticsearch:8.15.0`
- **No ports** published; Elasticsearch is reachable only from other containers on `suljhaoo-network`.
- **Environment:** `discovery.type=single-node`, `xpack.security.enabled=true`, `ELASTIC_PASSWORD` from env (required).
- **Volume:** `es-data` for `/usr/share/elasticsearch/data`.
- **Network:** `suljhaoo-network`.

The `elastic` user is used only for one-time setup (e.g. `es-setup`) and admin; normal pipeline uses `logstash_writer`.

---

## 2. es-setup (One-Off Security Bootstrap)

- **Image:** `curlimages/curl:8.5.0`
- **Role:** Wait for Elasticsearch, then create role `logstash_writer` and user `logstash_writer` via REST API.
- **Script:** `scripts/elasticsearch-init-security.sh` (mounted read-only).
- **Env:** `ELASTIC_PASSWORD`, `LOGSTASH_WRITER_PASSWORD` (required).
- **Depends on:** `elasticsearch`. Logstash depends on `es-setup` with `condition: service_completed_successfully` so the user exists before Logstash starts.
- **Restart:** `no` (run once and exit).

---

## 3. logstash_writer Role and User

**Role `logstash_writer`:**
- **Cluster:** `monitor` (for Logstash output plugin health check), `manage_index_templates`.
- **Indices:** Names `*-logs-*`; privileges `write`, `create_index`.

**User `logstash_writer`:**
- Password from `LOGSTASH_WRITER_PASSWORD`.
- Assigned role: `logstash_writer`.

`monitor` is required because the Logstash Elasticsearch output plugin calls the cluster health endpoint during startup; without it the pipeline fails with 403.

---

## 4. Logstash Elasticsearch Output

- **Hosts:** `${ELASTICSEARCH_HOST:elasticsearch}:9200` (HTTP; no TLS in this setup).
- **Auth:** `user => "logstash_writer"`, `password => "${LOGSTASH_WRITER_PASSWORD}"`.
- **Index:** `%{service.name}-logs-%{+YYYY.MM.dd}`. Events use ECS-style `service.name` (flat or nested); Logstash uses `%{service.name}` so the index resolves to e.g. `suljhaoo-backend-service-logs-2026.01.28`.
- No extra filters; only index routing. Beats input and TLS (Filebeat → Logstash) are unchanged.

---

## 5. Index Naming and Conventions

- **Format:** `<service.name>-logs-YYYY.MM.dd` (e.g. `suljhaoo-backend-service-logs-2026.01.28`).
- **One index per application (service) per day.** No dynamic mapping changes; default mappings from the plugin are used.

---

## 6. Environment Variables (No Secrets in Repo)

| Variable | Used by | Purpose |
|----------|--------|---------|
| `ELASTIC_PASSWORD` | elasticsearch, es-setup | Bootstrap password for `elastic` user. |
| `LOGSTASH_WRITER_PASSWORD` | es-setup, logstash | Password for user `logstash_writer`. |
| `ELASTICSEARCH_HOST` | logstash | Elasticsearch hostname (default: `elasticsearch`). |

Set in `.env` (or CI secrets); do not commit real passwords. `env.example` documents the variables without values.
