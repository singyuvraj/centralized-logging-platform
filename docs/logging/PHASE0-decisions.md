# Phase 0: Decisions and Rationale

This document records the decisions made for the centralized logging pipeline and the reasons behind them. Trade-offs are stated in plain language. We are not speculating about future phases; we are documenting what we decided for Phase 0.

## Filebeat as Sidecar (Not Daemon Set or Single Global Filebeat)

**Decision:** Run Filebeat as a sidecar container next to each application container, reading from a shared volume.

**Why:**

- Each app instance writes to its own log file on a volume that only that instance (and its sidecar) sees. There is no need for a central host to aggregate files from many containers, and no complex path layout on shared storage.
- If an app container is scheduled somewhere else, its Filebeat travels with it and keeps shipping that instance’s logs. We get natural “one shipper per app instance” behavior.
- Resource and permission isolation: the shipper runs in the same pod/task as the app, so we don’t need to open host paths or shared NFS to all nodes.

**Trade-off:** More Filebeat instances (one per app instance) mean more processes and a bit more memory per instance. We accepted that in exchange for simpler deployment and clearer ownership of logs per instance.

## JSON Logs (Not Plain Text)

**Decision:** Applications emit structured JSON (one object per line), not free-form plain text.

**Why:**

- Filebeat and Logstash can parse JSON and extract fields without regex. That keeps parsing logic simple and consistent.
- Elasticsearch can index fields like `level`, `service.name`, and `env` and use them in filters and aggregations. Plain text would require ingest pipelines or Logstash grok patterns and would be easier to get wrong.
- JSON is a common format. Many logging libraries support it, and future consumers (dashboards, alerts, other tools) can rely on the same field names.

**Trade-off:** Developers and operators must produce valid JSON and stick to the contract. In return, we avoid brittle and service-specific parsing downstream.

## Shared Docker Volume Between App and Filebeat

**Decision:** App and Filebeat sidecar share a Docker volume; the app writes to `/apps/logs/application.log`, Filebeat reads from the same path.

**Why:**

- The app keeps “logging to a file” as its only concern. It does not need to know about Logstash, Elasticsearch, or network ports. That keeps app config and code simple.
- Filebeat is built for tailing files. Using a shared volume gives it a real file path to tail, with normal file semantics (rotation, truncation, etc.) that Filebeat already handles.
- We avoid pushing logs over the network from app to shipper (e.g. syslog or HTTP from app to Filebeat). Fewer moving parts and no extra in-app log shipping code.

**Trade-off:** We depend on the orchestration layer (e.g. Compose, Kubernetes) to define the volume and mount it correctly for both containers. That’s a small, explicit contract at deploy time.

## Logstash Between Filebeat and Elasticsearch (Not Direct Filebeat to Elasticsearch)

**Decision:** Filebeat sends to Logstash; Logstash sends to Elasticsearch. We do not ship directly from Filebeat to Elasticsearch in this pipeline.

**Why:**

- Logstash gives a single place to add parsing, filtering, and routing. If we need to normalize fields, drop noisy events, or split by environment or service, we do it once in Logstash instead of in every Filebeat config or in Elasticsearch ingest pipelines.
- We can change Elasticsearch version, index naming, or index templates later and absorb those changes in Logstash without touching Filebeat.
- In later phases, we can add TLS, auth, or different outputs in Logstash without changing how the app or Filebeat work.

**Trade-off:** We run and operate Logstash as an extra component. We accepted that in exchange for flexibility and a clear “ingestion layer” between shippers and storage.

## Fixed Log Path: `/apps/logs/application.log`

**Decision:** Every application container writes its main log file to `/apps/logs/application.log` inside the container.

**Why:**

- Filebeat config can assume one path. We don’t need per-service or per-env path rules.
- New services get the same contract: “write your app log here.” Onboarding is consistent.
- Debugging and runbooks stay simple: “check `/apps/logs/application.log`” is the same for all services.

**Trade-off:** Services that today write elsewhere must be configured (or code must be updated) to use this path. We decided the benefit of uniformity outweighs that one-time change.

## Five Mandatory Fields: timestamp, level, message, service.name, env

**Decision:** Every log event must include `timestamp`, `level`, `message`, `service.name`, and `env`.

**Why:**

- `timestamp`, `level`, and `message` are the minimum useful set for sorting, filtering, and understanding what happened.
- `service.name` identifies which service produced the log. Without it, we cannot reliably filter or group by service in Kibana when many services share the same cluster.
- `env` separates dev, staging, and prod. Without it, we risk mixing environments in one view or failing to scope alerts and dashboards.

**Trade-off:** Every producer must set these five fields. We have not added optional fields to the contract in Phase 0; extra fields are allowed but not mandated.

---

These are the decisions we have locked in for Phase 0. Implementation will follow them. Changes to these decisions would require an explicit revision of this document and agreement from the team.
