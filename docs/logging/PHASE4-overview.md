# Phase 4 – Elasticsearch Overview

Phase 4 adds Elasticsearch to the centralized logging pipeline. Application logs flow: **Backend** → `/apps/logs/*.log` → **Filebeat** (TLS) → **Logstash** → **Elasticsearch**. Phase 0–3 are unchanged.

---

## What Phase 4 Does

- **Elasticsearch:** Run as a Docker Compose service with security enabled, no public ports, data on a named volume. Used only for log storage and indexing.
- **Security:** Dedicated role and user `logstash_writer` with minimal privileges (write, create_index, manage_index_templates, plus `monitor` for Logstash health check). Credentials via environment variables; no secrets in the repo.
- **Logstash → Elasticsearch:** Elasticsearch output plugin; authentication as `logstash_writer`; index name derived from `service.name` and date: `<service.name>-logs-YYYY.MM.DD`.
- **Indexing:** One index per application per day; format `${service.name}-logs-%{+YYYY.MM.dd}`. No custom dynamic mappings.

---

## What Phase 4 Does Not Do (Deferred)

- **Kibana:** No UI, dashboards, or Discover. Deferred to a later phase.
- **Alerts:** No alerting rules or Watcher. Deferred.
- **Tuning:** No ILM, index lifecycle, or performance tuning. Deferred.
- **Public exposure:** Elasticsearch has no host port mapping; access only from other services on the same Docker network.

---

## Key Design Choices

| Topic | Choice | Reason |
|-------|--------|--------|
| Elasticsearch exposure | No public ports | Reduce attack surface; only Logstash (and optional admin tools on the same network) need access. |
| Indices | One per service per day | Clear separation by app and time; simpler access control and future ILM. |
| User | `logstash_writer` | Least privilege: Logstash only needs to write and manage templates for `*-logs-*`, not read or manage cluster. |
| Credentials | Environment variables | No committed secrets; `.env` (or CI secrets) supply `ELASTIC_PASSWORD` and `LOGSTASH_WRITER_PASSWORD`. |

---

## Files Touched in Phase 4

- `docker-compose.yml` – `elasticsearch`, `es-setup`, Logstash env/depends_on, `es-data` volume.
- `scripts/elasticsearch-init-security.sh` – Creates `logstash_writer` role and user.
- `logstash/logstash.conf` – Elasticsearch output (hosts, user, password, index).
- `env.example` – Phase 4 variables documented (no real values).

Phase 4 does **not** change: Filebeat config, application code, or log path contract (`/apps/logs/*.log`).
