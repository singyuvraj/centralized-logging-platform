# Phase 3 Decisions

This document records the main choices made in Phase 3 and the reasons behind them.

## Why No Filters

**Decision:** The Logstash pipeline has no `filter` block. Events pass from the Beats input directly to the stdout output.

**Why:** Phase 3’s scope is to prove that Filebeat → Logstash delivery works. Filters (e.g. grok, mutate, date) add complexity and failure modes without being required for that proof. The application already emits structured NDJSON; Filebeat decodes it and sends ECS-style events. Logstash receives them as-is. Adding filters would be for enrichment or transformation, which is explicitly out of Phase 3 scope.

**Trade-off:** If Phase 4 or later needs to normalize, parse, or enrich events in Logstash, filters can be added then. For Phase 3, “no filters” keeps the pipeline minimal and observable.

## Why No Elasticsearch Yet

**Decision:** Logstash does not output to Elasticsearch. There is no Elasticsearch service in docker-compose.yml.

**Why:** The Phase 3 objective is only to prove Filebeat → Logstash delivery. Introducing Elasticsearch would require index management, index lifecycle, and possibly security (TLS, credentials), all of which are explicitly excluded from Phase 3. The task states: “No Elasticsearch output” and “Do NOT add Elasticsearch service.”

**Trade-off:** Phase 4 (or a later phase) can add Elasticsearch and switch Logstash output from stdout to Elasticsearch. Phase 3 stops at “Logstash receives and prints events.”

## Why Stdout First

**Decision:** Logstash output is `stdout { codec => rubydebug }` (or equivalent, e.g. json_lines). No file, no Elasticsearch, no other sink.

**Why:** Stdout is the simplest way to verify that events reach Logstash. You run `docker compose logs logstash` and see events. No index, no dashboard, no extra config. It satisfies “Logstash outputs events to STDOUT for verification” and keeps the implementation minimal and observable.

**Trade-off:** Stdout is not suitable as a permanent sink (logs are ephemeral with the container, and volume can be high). It is intentional for Phase 3 only; a real sink (e.g. Elasticsearch) is deferred to Phase 4.

## Why Beats Input on 5044

**Decision:** Logstash listens on port 5044 with the `beats` input plugin.

**Why:** Filebeat (Phase 2) is already configured to send to `logstash:5044`. Port 5044 is the conventional port for the Beats input. Using it keeps Phase 2 config unchanged and matches common ELK documentation.

**Trade-off:** None; this is standard practice.

## Why Logstash Before Filebeat (depends_on)

**Decision:** Filebeat has `depends_on: logstash` (in addition to `backend`) so that Logstash starts before Filebeat.

**Why:** If Filebeat starts first, it will try to connect to `logstash:5044` before Logstash has bound the port, leading to connection refused or backoff. Starting Logstash first gives the Beats input time to open the listener; Filebeat then connects once the listener is ready.

**Trade-off:** `depends_on` does not wait for Logstash to be “ready” (e.g. listener bound); it only waits for the container to start. In practice the Logstash image starts quickly and binds 5044 within a few seconds. If needed, a future phase could add a healthcheck and condition.

## Why Same Docker Network

**Decision:** Logstash is on the same network as backend and Filebeat (`suljhaoo-network`).

**Why:** Filebeat resolves `logstash` by DNS to the Logstash container’s IP. That only works if both are on the same Docker network. No extra_hosts or port mapping on the host is required for Filebeat → Logstash communication.

**Trade-off:** None; this is required for service discovery.
