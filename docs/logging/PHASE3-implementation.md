# Phase 3 Implementation

This document describes what was implemented for Phase 3 and where it lives.

## 1. Logstash Configuration (logstash/logstash.conf)

- **Input:** `beats { port => 5044 }`. Logstash listens on TCP port 5044 for Beats protocol connections. Filebeat connects to this port and sends events.

- **Output:** `stdout { codec => rubydebug }`. Every event received is printed to Logstash’s stdout in rubydebug format. Each event appears as a key-value block with field names and values, which is readable during verification.

- **No filter block.** Events flow from input to output unchanged. No parsing, enrichment, or transformation is applied.

- **Location:** The file is mounted read-only into the container at `/usr/share/logstash/pipeline/logstash.conf`, which is the default pipeline config path for the official Logstash image.

## 2. Docker Compose Wiring

- **Logstash service:**
  - **Image:** `docker.elastic.co/logstash/logstash:8.15.0` (matches Filebeat 8.15.0 for compatibility).
  - **Ports:** `5044:5044` so the Beats input is reachable from the host if needed; Filebeat connects via the service name `logstash` on the same network.
  - **Volumes:** `./logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro`.
  - **Environment:** `LS_JAVA_OPTS=-Xms256m -Xmx256m` to limit heap and avoid OOM in resource-constrained environments.
  - **Network:** `suljhaoo-network` (same as backend and Filebeat).

- **Filebeat:** No config change. Output remains `logstash:5044`. `depends_on: logstash` was added so Logstash starts before Filebeat, giving the Beats listener time to bind before Filebeat connects.

- **No Elasticsearch service.** Only backend, Filebeat, and Logstash are in the Compose file.

## 3. Why Stdout Output Is Used

- **Verification only.** Phase 3’s goal is to prove that Filebeat → Logstash delivery works. Writing to stdout makes it trivial to see events in `docker compose logs logstash` without adding Elasticsearch, files, or other outputs.
- **Minimal and observable.** No index lifecycle, no disk usage for Logstash output, no extra services. Success is defined by: Logstash logs show “Starting input listener” (or equivalent) on 5044, and incoming events appear in stdout.
- **No production commitment.** Stdout is not intended as a long-term sink. Phase 4 (or later) can add an Elasticsearch output and remove or replace the stdout output.

## 4. No Application or Phase 2 Changes

- Application logging (Log4j2, ECS layout, path, trace filter) is unchanged.
- Filebeat configuration (inputs, paths, JSON decoding, fields, output host) is unchanged except for the addition of `depends_on: logstash`.
- Backend container, entrypoint, and shared volume setup are unchanged. Phase 3 only adds the Logstash service and its pipeline config.
