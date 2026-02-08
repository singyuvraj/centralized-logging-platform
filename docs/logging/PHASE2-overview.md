# Phase 2 Overview: Filebeat Sidecar

## What Phase 2 Does

Phase 2 adds Filebeat as a sidecar container that reads application logs from a shared Docker volume and forwards them to Logstash. The application (Phase 1) is unchanged: it still writes NDJSON to `/apps/logs/application.log`. Filebeat tails that file and ships each line to Logstash on port 5044. No TLS, Elasticsearch, or Kibana are introduced.

## What Filebeat Is

Filebeat is a lightweight log shipper. It watches one or more files, reads new lines, and sends them to a configured output (here, Logstash). It does not run inside the application process; it runs as a separate process (here, a separate container). The application only writes to a file. Filebeat is responsible for reading and forwarding.

## Why a Sidecar

A sidecar is an extra container that runs next to the main application container and shares resources with it (here, a volume). We use it so that:

- The application does not need to know about Logstash or any network endpoint. It just writes to a file.
- Filebeat can be started, stopped, or upgraded independently of the application.
- Each application instance has its own Filebeat, so logs from that instance are shipped without mixing with other instances.
- The same pattern scales: one sidecar per app container.

## What Changed in Phase 2

- **Filebeat configuration:** `filebeat/filebeat.yml` defines a log input at `/apps/logs/*.log`, JSON decoding (one object per line), and two added fields (`app`, `env`). Output is Logstash on port 5044. Registry path is set so it can be persisted on a volume.

- **Docker Compose:** The backend service mounts a named volume at `/apps/logs`. A new `filebeat` service mounts the same volume (read-only) at `/apps/logs`, mounts a second volume for Filebeat registry data, and runs Filebeat as root so it can read logs written by the app user.

- **Backend container:** An entrypoint script ensures `/apps/logs` is writable by the app user when the volume is mounted, then starts the JVM as the app user. The entrypoint uses Alpine/BusyBox-compatible `su` argument order. No application or logging code was changed.

Runtime verification and any fixes applied (e.g. entrypoint `su` order, volume wiring) are recorded in `docs/logging/testing.md` and `docs/logging/solution.md`.

## What Phase 2 Does Not Do

Phase 2 does not add Logstash, Elasticsearch, or Kibana to this project. It does not add TLS or certificates. It does not change how the application logs (Phase 1 remains). Validation assumes Logstash is running somewhere reachable as `logstash:5044` (e.g. same Docker network or host mapping).
