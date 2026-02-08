# Phase 2 Architecture

Runtime verification and fixes (entrypoint, volumes, verification commands) are documented in `docs/logging/testing.md` and `docs/logging/solution.md`.

## Pipeline After Phase 2

- **Application container:** Writes logs to `/apps/logs/application.log` on a mounted volume. Path and format are from Phase 0/1 (NDJSON, ECS-style fields).

- **Filebeat container:** Runs next to the app. Mounts the same volume at `/apps/logs` (read-only). Reads from `/apps/logs/*.log` and sends each line to Logstash at `logstash:5044`.

- **Logstash:** Not part of this repo. It must be run elsewhere and listen on port 5044. Phase 2 only configures Filebeat to send there.

## How Shared Volumes Work

Docker volumes are storage that outlives individual containers. A named volume (e.g. `backend-logs`) can be mounted into multiple containers at the same time.

- The **backend** mounts `backend-logs` at `/apps/logs` with read-write. The application creates and updates `application.log` (and any rotated files) under that path. The data lives on the volume.

- **Filebeat** mounts the same volume `backend-logs` at `/apps/logs` read-only. It sees the same files the app is writing. Filebeat tails them and does not modify the files.

Both containers must use the same path (`/apps/logs`) so that the path in the Phase 0 contract and in `filebeat.yml` match. The volume is the link between the writer (app) and the reader (Filebeat).

## Why NDJSON Is Used

Phase 0 and Phase 1 define one JSON object per line (NDJSON). That gives:

- **Clear event boundaries:** Each line is one event. No multiline parsing is needed for normal log lines.
- **Simple parsing:** Filebeat can decode JSON line-by-line and put fields at the root (or under a key). Logstash and downstream systems get structured fields without regex.
- **Compatibility:** ECS-style fields (`@timestamp`, `log.level`, `message`, etc.) are preserved as-is. Filebeat does not add or transform identity fields; it may add only metadata (e.g. `agent`, `log.file.path`).

## Why Registry Persistence Matters

Filebeat keeps a registry of which files it has read and the last offset per file. That state is stored under `path.data` (here, `/usr/share/filebeat/data`). We mount a dedicated volume (`filebeat-data`) there so that:

- After a Filebeat restart, it resumes from the last offset instead of re-reading from the start.
- We avoid duplicate events and avoid losing track of rotated files.
- The registry is not lost when the container is recreated.

Without a persistent registry, every restart would re-send already-shipped lines to Logstash.

## Deferred to Later Phases

The following are explicitly not done in Phase 2 and are left for later:

- **TLS** between Filebeat and Logstash.
- **Logstash** definition in this docker-compose (we only point Filebeat at `logstash:5044`).
- **Elasticsearch** and **Kibana** (no indexing or dashboards).
- **Index lifecycle**, retention, or alerting.
- **Authentication** or authorization for Logstash or Elasticsearch.
