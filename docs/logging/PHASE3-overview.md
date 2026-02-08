# Phase 3 Overview: Filebeat → Logstash Delivery

## What Phase 3 Does

Phase 3 introduces Logstash so that:

- **Filebeat sends logs to Logstash.** Filebeat (Phase 2) continues to read from `/apps/logs/application.log` on the shared volume and forwards events over the Beats protocol to Logstash.
- **Logstash receives events on port 5044.** A Logstash container listens on 5044 (Beats input) on the same Docker network as the backend and Filebeat.
- **Logstash outputs events to STDOUT for verification.** Events are printed to the Logstash container’s stdout using the `rubydebug` codec so that delivery from Filebeat to Logstash can be confirmed without adding Elasticsearch, Kibana, or any other sink.

The goal is **only** to prove that the path **Filebeat → Logstash** works end-to-end. No indexing, no dashboards, no TLS, and no security hardening.

## What Phase 3 Does Not Do

- **No Elasticsearch.** Logstash does not output to Elasticsearch. There is no index, no index lifecycle, and no search.
- **No Kibana.** No UI, no dashboards, no discovery.
- **No TLS or certificates.** Beats input and stdout output are plain; no encryption or mutual TLS.
- **No filters.** The Logstash pipeline has no filter block unless strictly required for basic operation. Events pass through as received.
- **No changes to Phase 2.** Application logging, Filebeat config (paths, output host), backend container, and shared volume behaviour remain as in Phase 2. Phase 3 only adds the Logstash service and its minimal config.
- **No index management, security hardening, or tuning for production load.** Those are explicitly out of scope and deferred to later phases.

## Summary

Phase 3 adds a minimal Logstash service that accepts Beats on 5044 and prints events to stdout. Once Logstash shows incoming events from `application.log` (via Filebeat), Phase 3 is complete. Everything beyond that (Elasticsearch, Kibana, TLS, filters, hardening) is left for Phase 4 or later.
