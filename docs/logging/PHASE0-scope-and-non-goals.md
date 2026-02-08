# Phase 0: Scope and Non-Goals

This document spells out what Phase 0 includes and what it explicitly does not. It is meant to prevent scope creep and to set expectations for later phases.

## In Scope for Phase 0

The following are in scope for Phase 0:

- **Documentation and alignment only.** No code, no config, no deployment.
- **Agreed pipeline shape:** App (writes to shared volume) -> Filebeat (sidecar, reads from shared volume) -> Logstash -> Elasticsearch -> Kibana.
- **Agreed log contract:** Structured JSON, one line per event, with mandatory fields `timestamp`, `level`, `message`, `service.name`, `env`, and a fixed log path `/apps/logs/application.log`.
- **Agreed architecture:** Sidecar Filebeat per app instance, shared Docker volume between app and Filebeat, Logstash as the central ingestion layer.
- **Documented decisions:** Why we chose Filebeat as sidecar, JSON logs, shared volume, Logstash in the middle, fixed path, and the five mandatory fields. Rationale and trade-offs are written down.
- **Clear boundaries:** What is in scope and what is out of scope for Phase 0, and what we are deferring to later phases.

Deliverables are the four Phase 0 docs: overview, contract, decisions, and this scope document. Nothing else.

## Out of Scope for Phase 0

The following are **not** part of Phase 0. No design, no implementation, no commitment to how they will look.

- **TLS / encryption in transit.** We are not defining or implementing TLS between Filebeat and Logstash, or between Logstash and Elasticsearch, in Phase 0. That will be addressed in a later phase.
- **Certificates.** No PKI, no certificate issuance, no trust stores. Certificate management is out of scope for Phase 0.
- **Elasticsearch users and role-based access.** We are not defining or configuring Elasticsearch users, roles, or privileges in Phase 0. Who can read or write which indices will be handled in a later phase.
- **Index lifecycle management (ILM).** No policies for rollover, warm/cold tiers, or deletion. How long we keep logs and how we move or shrink indices is out of scope for Phase 0.
- **Kibana dashboards and saved objects.** We are not creating or exporting dashboards, visualizations, or saved searches. Those will be built once the pipeline is running and we have real data.
- **Logstash pipeline configuration.** Phase 0 agrees that Logstash sits in the middle; it does not define filters, outputs, or codecs. Pipeline config is implementation and belongs to a later phase.
- **Filebeat configuration.** Phase 0 agrees on path and that Filebeat talks to Logstash; it does not provide a Filebeat config file or image. That is implementation.
- **Docker Compose or orchestration changes.** No changes to Compose files, pod specs, or volume definitions. Those are implementation.
- **Application code or logging library changes.** No changes to how the Spring Boot app logs. We only document the contract it must satisfy; implementation of logging format and output path is a later phase.
- **Elasticsearch index templates or mappings.** We are not defining index names, field mappings, or templates in Phase 0. Those come when we implement the pipeline.
- **Alerting, SLOs, or on-call playbooks.** None of that is in Phase 0.

## Explicit “Not Done Yet” List

For visibility, these are explicitly **not** done in Phase 0 and will be addressed in later phases:

- **TLS** between components.
- **Certificates** for auth or encryption.
- **Elasticsearch users** and access control.
- **Index lifecycle management** (retention, rollover, tiers).
- **Dashboards** in Kibana.

Phase 0 does not design or implement any of these. Later phases will add security, retention, and usability on top of the agreed pipeline and contract.

## Why This Split

Phase 0 exists to get alignment on the pipeline shape and the log contract. If we mixed in TLS, ILM, and dashboards now, we would slow down agreement and risk unclear ownership. By keeping Phase 0 to “what we ship, where, and in what shape,” we give implementation a clear target. Security, retention, and visualization can then be phased in once the basic pipeline is agreed and, in a later phase, implemented.
