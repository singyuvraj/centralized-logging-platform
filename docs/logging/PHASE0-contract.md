# Phase 0: Logging Contract

This document defines the contract between the application, Filebeat, and Logstash. Anything that produces or consumes logs in this pipeline must adhere to it.

## Log Format

Logs are **structured JSON**. One JSON object per line (NDJSON). No plain-text log lines in the main application log file that we ship.

JSON is required so that Filebeat and Logstash can parse fields without custom regex. We get consistent field names and types, and Elasticsearch can index them as proper fields.

## Mandatory Fields

Every log event written to the pipeline must include these fields. No optionality for Phase 0.

| Field | Description |
|-------|-------------|
| `timestamp` | When the event occurred. Use an ISO-8601–style string or a format agreed with the index mapping (e.g. `2025-01-27T10:15:00.000Z`). |
| `level` | Log level: e.g. `ERROR`, `WARN`, `INFO`, `DEBUG`. |
| `message` | The main log message or description of the event. |
| `service.name` | Name of the service that produced the log (e.g. `suljhaoo-backend-service`). Authoritative service identity. |
| `service.environment` | Environment identifier (e.g. `dev`, `staging`, `prod`). Authoritative environment. |
| `env` | Environment identifier. Kept for backward compatibility; same value as `service.environment` where both exist. |

The application is responsible for emitting these fields. Filebeat does not add or overwrite identity fields (`service.name`, `service.environment`, `env`); it may add only metadata (e.g. `agent`, `log.file.path`). Logstash passes them through.

## Log File Path (Canonical Contract) — HARD CONTRACT

- **Agreed path:** `/apps/logs` (directory). The application log file is `/apps/logs/application.log`. Filebeat reads from `/apps/logs/*.log`.
- **`/apps/logs/*.log` is the ONLY supported path.** No other path is canonical. All config (Log4j2, Dockerfile, docker-compose, Filebeat) must use `/apps/logs`; remove any reference to `/app/logs`.

The contract assumes that the app and Filebeat share a Docker volume mounted so that this path is the same for both. The exact volume name and host path are deployment details; the in-container path is fixed by this contract.

**Why /apps/logs is enforced:** The description requires `/apps/logs/*.log` as a HARD CONTRACT. All implementation (Log4j2, Dockerfile, docker-compose, Filebeat, entrypoint) uses `/apps/logs` so that Filebeat, Logstash, and future Elasticsearch indexing are consistent (e.g. `log.file.path` = `/apps/logs/application.log`).

## Why Path Consistency Matters

Filebeat is configured to read from a specific path. If one service wrote to `/apps/logs/application.log` and another to `/var/log/app.log`, we would need multiple Filebeat inputs and extra logic to tell which service produced which file. Agreeing on one path per “app log” keeps configuration simple and uniform.

Using `/apps/logs/application.log` everywhere means:

- One Filebeat input config can be reused for all app containers.
- We avoid path-based bugs when new services are added.
- Debugging is easier because everyone knows where to look inside the container.

## How the Contract Is Consumed

**Filebeat** – Configured to tail `/apps/logs/*.log` on the shared volume. Events show `log.file.path` = `/apps/logs/application.log`. It reads new lines, treats each line as a JSON document, and forwards each as an event to Logstash. Filebeat may add metadata (e.g. source file, hostname); the core payload is the JSON line from the app.

**Logstash** – Receives events from Filebeat. The event body should already be parsed JSON (or Logstash will parse it from the line). Logstash uses the documented fields (e.g. `level`, `service.name`, `env`) for filtering, routing, or mapping. It does not define or require extra fields beyond this contract; any extra fields are passed through or handled in later phases.

## Example Log Event

A single line in `application.log` must look like valid NDJSON. For example:

```json
{"timestamp":"2025-01-27T10:15:00.123Z","level":"INFO","message":"Request completed","service.name":"suljhaoo-backend","env":"dev"}
```

One event, one line. Additional custom fields are allowed as long as the five mandatory fields are present and the output remains valid JSON per line.
