# Phase 5 – Kibana Overview

Phase 5 adds a secure Kibana UI on top of the existing centralized logging pipeline. The data path remains:

Backend → `/apps/logs/*.log` → Filebeat (TLS) → Logstash → Elasticsearch → Kibana (via nginx).

Previous phases (Filebeat, Logstash, Elasticsearch) are unchanged except where explicitly required to support Kibana security.

---

## What Phase 5 Does

- **Kibana service**: Runs as a private container on the internal Docker network (no host ports).
- **nginx reverse proxy**: Exposes Kibana publicly via **HTTPS** on port 443 only; terminates TLS for browser traffic.
- **Internal TLS**: Kibana connects to Elasticsearch over **HTTPS** using the `kibana_system` service user, via nginx acting as an internal TLS proxy.
- **UI user**: Dedicated `kibana_user` account with read-only access to log indices (`*-logs-*`), used for Kibana login.
- **Elasticsearch security**: Role `kibana_readonly` created with `read` and `view_index_metadata` on `*-logs-*`; `kibana_user` is bound to this role only.

---

## What Phase 5 Does Not Do (Deferred)

- **Dashboards / visualizations**: No custom dashboards or saved searches are created.
- **Alerts / Watcher**: No alerting or Watcher configuration.
- **Spaces / multi-tenancy**: No Kibana spaces or complex RBAC beyond a single read‑only user.
- **ILM / index tuning**: No changes to index lifecycle, shard counts, or performance tuning.
- **Production hardening**: No WAF, rate limiting, advanced TLS ciphers, or SSO; this is a dev-friendly baseline.

---

## Key Design Choices

| Topic | Choice | Reason |
|-------|--------|--------|
| Public entrypoint | nginx on port 443 | Central place to terminate TLS and keep Kibana unexposed. |
| ES connectivity | Kibana → nginx (HTTPS) → Elasticsearch (HTTP) | Satisfies Kibana→ES TLS requirement without changing existing ES/Logstash pipeline. |
| Service user | `kibana_system` | Built-in user for Kibana’s internal operations; password managed by bootstrap script. |
| UI user | `kibana_user` with `kibana_readonly` | Enforces read-only access to `*-logs-*` indices. |
| TLS assets | Generated from local CA under `tls/` | Single dev CA used for Logstash, nginx, and Kibana to simplify trust configuration. |

Phase 5 ends when Kibana is reachable over HTTPS through nginx, Elasticsearch remains private, `kibana_system` is not used for UI login, and `kibana_user` can view (but not modify) logs.

