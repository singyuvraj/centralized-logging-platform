# Phase 2 Known Issues and Limitations

This document lists known limitations and how to validate Phase 2. It does not describe Phase 3 or later.

For a chronological log of runtime tests and commands run, see `docs/logging/testing.md`. For root causes and fixes applied at runtime (e.g. entrypoint, volumes), see `docs/logging/solution.md`.

## Logstash Must Be Running and Reachable

Filebeat is configured to send to `logstash:5044`. There is no Logstash service in this docker-compose. To receive events you must either:

- Run Logstash in the same Docker network and name the service `logstash`, or  
- Run Logstash elsewhere and make it reachable as `logstash` (e.g. `extra_hosts` or DNS) from the Filebeat container.

If Logstash is not reachable, Filebeat will log connection errors and retry. Events are buffered for a while but can be dropped if the output stays down. We do not assume Elasticsearch exists; validation can be done by confirming Logstash receives data (e.g. stdout or a simple file output in Logstash).

## Deprecated Log Input in Filebeat 8

The `log` input type is deprecated in Filebeat 8. We use it with `allow_deprecated_use: true` as required by the task. In a future Filebeat major version the log input may be removed; migration to the `filestream` input would then be needed.

## No TLS to Logstash

Traffic from Filebeat to Logstash is plain TCP. No TLS or mutual authentication is configured. That is intentional for Phase 2 and will be addressed in a later phase.

## Filebeat Runs as Root

The Filebeat container runs as root so it can read log files created by the app user. We did not add a read-only filesystem or other hardening in Phase 2. Hardening can be done in a later phase.

## Backend May Be Restarting (DataSource)

When no database is configured, the backend container exits during Spring context init (DataSource error) and the restart policy causes it to restart repeatedly. That is out of Phase 2 scope. The app still writes logs to `/apps/logs/application.log` before failing. To inspect the shared volume when the backend is not stably running, use a one-off container with the same volume:  
`docker compose run --no-deps --rm --entrypoint "" backend ls -la /apps/logs`  
and  
`docker compose run --no-deps --rm --entrypoint "" backend tail -n 5 /apps/logs/application.log`.  
See `testing.md` for the full command log.

## How to Verify Filebeat Is Reading Logs

1. **Start backend and Filebeat:**  
   `docker compose up -d backend filebeat`  
   The backend may be Restarting if no DB is configured; Filebeat will still read whatever logs were written to the shared volume.

2. **Check that the log file exists and has content:**  
   If the backend is running: `docker compose exec backend ls -la /apps/logs` and `docker compose exec backend tail -n 5 /apps/logs/application.log`.  
   If the backend is Restarting: use `docker compose run --no-deps --rm --entrypoint "" backend ls -la /apps/logs` and `docker compose run --no-deps --rm --entrypoint "" backend tail -n 5 /apps/logs/application.log` (same volume, one-off container).  
   You should see NDJSON lines.

3. **Check Filebeat logs:**  
   `docker compose logs filebeat`  
   Look for lines indicating that Filebeat has started harvesters for `/apps/logs/application.log` (or similar). Errors about “permission denied” or “no such file” would indicate a path or volume problem.

4. **Confirm registry is updated:**  
   `docker compose exec filebeat ls -la /usr/share/filebeat/data/registry`  
   After some activity, registry files should be present and updated. That shows Filebeat is tracking the log files.

## How to Verify Events Are Being Sent to Logstash

We do not assume Elasticsearch exists. You can confirm that events reach Logstash in either of these ways:

1. **Logstash stdout output:**  
   Run Logstash with a config that listens on port 5044 (Beats input) and outputs to stdout. When you send traffic to the backend and Filebeat ships logs, you should see events printed by Logstash. That proves Filebeat is sending and Logstash is receiving.

2. **Logstash file output:**  
   Configure Logstash to write received events to a file. After generating app logs and waiting a few seconds, inspect the file. Each line should be a JSON event (possibly wrapped or formatted by Logstash) containing the fields from the app (e.g. `@timestamp`, `message`, `service.name`, `env`) plus any fields Filebeat adds (e.g. `app`, `env` from the input fields).

3. **Logstash logs:**  
   If Logstash logs incoming connection or pipeline activity, check those logs when the backend is producing logs and Filebeat is running. Connection from the Filebeat container and pipeline execution confirm that events are being received.

If Logstash is not running or not reachable as `logstash:5044`, Filebeat will log output errors. Fix connectivity or hostname resolution before relying on the above checks.

## Intentionally Deferred to Later Phases

The following are not done in Phase 2 and are left for later:

- TLS between Filebeat and Logstash (or Elasticsearch).
- Elasticsearch and Kibana (indexing, search, dashboards).
- Logstash pipeline definition in this repo (we only configure Filebeat’s output).
- Index lifecycle, retention, or alerting.
- Running Filebeat as non-root (would require log directory permissions or ACLs).
- Migration from the deprecated `log` input to `filestream`.

Phase 2 stops at “Filebeat reads from shared volume and forwards to Logstash.” Manual validation of read and send behaviour is expected before proceeding to Phase 3.
