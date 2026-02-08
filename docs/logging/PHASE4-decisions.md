# Phase 4 – Decisions

## 1. Why Elasticsearch Has No Public Ports

- **Security:** Elasticsearch is not exposed to the host or the internet. Only services on the same Docker network (Logstash, and optionally admin tools) can reach it.
- **Least exposure:** Reduces attack surface and avoids accidental exposure of data or cluster APIs.
- **Access path:** For debugging or ad-hoc queries, use a container on the same network (e.g. `docker compose exec logstash curl ...` or a one-off with `elastic` credentials) or add a dedicated admin service later. Kibana, when added, would also run on the same network and connect to Elasticsearch without host ports.

---

## 2. Why Separate Indices Per Service

- **Isolation:** One index per application (and per day) keeps data separated by service and time.
- **Access control:** Role `logstash_writer` is scoped to `*-logs-*`; future roles can be scoped per index pattern (e.g. read-only for a single service).
- **Operability:** Easier to reason about retention, ILM, or reindexing per service later.
- **Contract:** Index name format `${service.name}-logs-%{+YYYY.MM.dd}` matches the requirement and uses the existing `service.name` field from the pipeline.

---

## 3. Why logstash_writer Exists

- **Least privilege:** Logstash does not need `elastic` or superuser. A dedicated user with only write, create_index, manage_index_templates (and `monitor` for health check) is sufficient for the pipeline.
- **Audit and safety:** If Logstash config or credentials are compromised, impact is limited to writing to `*-logs-*` indices, not cluster or other data.
- **Clarity:** Clear separation between “admin” (`elastic`) and “pipeline writer” (`logstash_writer`).

---

## 4. Why `monitor` Was Added to logstash_writer

The Logstash Elasticsearch output plugin performs a health check against the cluster (e.g. root or `_cluster/health`) at startup. That check requires the `cluster:monitor/main` privilege. The role was initially defined with only `manage_index_templates` and index privileges; the pipeline failed with 403 until `monitor` was added to the role. So `monitor` is required for the plugin to start; it does not grant read access to log data.

---

## 5. Index Name: %{service.name} vs %{[service][name]}

Events from Filebeat/ECS can have `service.name` as a top-level field (flat key `"service.name"`). Logstash sprintf for the index must match how the event stores it: `%{service.name}` works for the flat ECS field; `%{[service][name]}` is for a nested `service` object. This setup uses `%{service.name}-logs-%{+YYYY.MM.dd}` so the index resolves correctly (e.g. `suljhaoo-backend-service-logs-2026.01.28`) without adding filters.

---

## 6. What Was Deferred

- **Kibana:** No dashboards, Discover, or UI in Phase 4.
- **Alerts:** No Watcher or alerting rules.
- **Tuning:** No ILM, index lifecycle, or performance tuning.
- **HTTPS for Elasticsearch:** In this Compose setup Elasticsearch is used over HTTP on the private network; TLS for Elasticsearch can be added in a later phase if required.

Phase 4 scope ends when logs are written to Elasticsearch indices with one index per service per day, using `logstash_writer`, with Elasticsearch having no public ports and documentation complete.
