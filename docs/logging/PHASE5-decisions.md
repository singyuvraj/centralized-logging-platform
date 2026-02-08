# Phase 5 – Decisions (Kibana & nginx)

## 1. Why nginx in Front of Kibana

- **Single public entrypoint:** nginx terminates TLS and is the only service with a host port (443). Kibana and Elasticsearch remain on the internal network.
- **Flexibility:** nginx can later be extended with HSTS, redirects, or WAF-style rules without touching Kibana.
- **Separation of concerns:** Kibana focuses on the UI and API; nginx handles TLS and HTTP proxying.

---

## 2. Why kibana_system for Internal Connectivity

- **Built-in service account:** `kibana_system` is the recommended account for Kibana’s internal operations (saved objects, index patterns, status checks).
- **Least privilege for the service:** Its privileges can be tuned independently of user-facing roles (like `kibana_readonly`).
- **Clear separation:** UI users never log in as `kibana_system`; they use `kibana_user`, which has only read access to logs.

---

## 3. Why kibana_user and kibana_readonly

- **Dedicated UI user:** `kibana_user` is intended for humans using Kibana, not for internal service-to-service calls.
- **Read-only access:** Role `kibana_readonly` grants `read` and `view_index_metadata` on `*-logs-*`, and **no write privileges**, ensuring logs cannot be modified or deleted via this account.
- **Principle of least privilege:** If `kibana_user` credentials are compromised, the attacker can only read logs, not alter or remove them.

Runtime verification confirmed:

- `kibana_user` can successfully search log indices (`/_search`) and see documents.
- Attempts to index documents as `kibana_user` fail with HTTP 403 (unauthorized for `indices:data/write/index`).

---

## 4. TLS Design – nginx as Kibana→ES Proxy

- **Requirement:** TLS must be enabled both from browser → nginx and Kibana → Elasticsearch.
- **Choice:** Use nginx as an internal TLS proxy on port 9200:
  - Kibana connects to `https://nginx:9200` with CA-trusted `kibana.crt`.
  - nginx proxies to Elasticsearch over HTTP on the internal network.
- **Reasoning:** This satisfies Kibana→Elasticsearch TLS without changing the existing Elasticsearch/Logstash HTTP configuration from earlier phases, and keeps TLS concerns localized to nginx and Kibana.

---

## 5. Why Not Change Elasticsearch or Logstash

- Earlier phases deliberately ran Elasticsearch over HTTP (private network only) and configured Logstash accordingly.
- Enabling HTTPS directly on Elasticsearch would require coordinated changes to Logstash (and potentially other clients), which is outside the explicit Kibana scope and would risk destabilizing completed phases.
- Using nginx as a TLS proxy keeps Filebeat, Logstash, and Elasticsearch behaviour intact while still meeting the new Kibana TLS requirements.

---

## 6. What Was Deferred

- **Dashboards / visualizations:** No dashboards, saved searches, or index patterns were created.
- **Alerts / Watcher:** Alerting remains out of scope.
- **Spaces / advanced RBAC:** Only a single read-only user is defined; no spaces or complex roles.
- **TLS hardening:** Cipher suites, HSTS, OCSP stapling, and mutual TLS are not configured.
- **SSO / Identity providers:** No SSO (OIDC/SAML) integration; Kibana uses basic authentication for `kibana_user`.

Phase 5 concludes when Kibana is reachable over HTTPS via nginx, Elasticsearch remains private, `kibana_system` is used only internally, and `kibana_user` can view (but not modify) log data.

