# Phase 2 Decisions

This document records the main choices made in Phase 2 and the reasons behind them.

## Filebeat as a Sidecar (Not DaemonSet or Host-Level)

**Decision:** Run Filebeat as a separate container in the same Compose project as the backend, sharing a volume with it. Do not run Filebeat as a single host-level or cluster-level daemon that reads from many containers.

**Why:** Phase 0 and the task specify a sidecar: one Filebeat per app instance, reading from a shared volume. That keeps the model simple (one writer, one reader per instance), avoids central path configuration for many containers, and matches the “sidecar next to the app” description. DaemonSets or host-level agents would be a different design and are out of scope for Phase 2.

**Trade-off:** We run one Filebeat container per backend container. That uses more memory than a single central shipper but keeps ownership and configuration clear and avoids cross-container path and permission complexity.

## Log Input (Not Filestream) in Filebeat 8

**Decision:** Use the deprecated `log` input type with `allow_deprecated_use: true` instead of the `filestream` input.

**Why:** The task explicitly required “Input type: log”. We comply with that. In Filebeat 8 the log input is deprecated in favour of filestream; enabling it via `allow_deprecated_use: true` is the supported way to keep using it. A future phase can migrate to filestream if desired.

**Trade-off:** The log input may be removed in a later Filebeat major version. We accept that for Phase 2 and document it in known issues.

## Path /apps/logs/*.log (HARD CONTRACT)

**Decision:** Configure Filebeat with paths `/apps/logs/*.log`. This is the ONLY supported path; the description requires it.

**Why /apps/logs is enforced:** The task description specifies `/apps/logs/*.log` as a HARD CONTRACT. Log4j2, Dockerfile, docker-compose, Filebeat, and the entrypoint all use `/apps/logs`. No reference to `/app/logs` remains. Phase 1 uses a rolling file appender; the glob ensures Filebeat picks up the active file and rotated files.

**Trade-off:** If other `.log` files are ever written under `/apps/logs`, they will be shipped too. For Phase 2 only the app writes there, so the behaviour is correct.

## Filebeat Runs as Root

**Decision:** Run the Filebeat container as `user: root`.

**Why:** The application runs as a non-root user (appuser, uid 1001) and creates log files under `/apps/logs` with that ownership. Filebeat must read those files. Running Filebeat as root avoids permission issues without changing file ownership or ACLs. The task allowed “Filebeat runs as root only if required to read logs”; here it is required.

**Trade-off:** The Filebeat process has root inside the container. TLS is enabled for Filebeat to Logstash; hardening (non-root, read-only filesystem, etc.) can be done in a later phase.

## JSON Decode Without Transforming ECS Fields

**Decision:** Enable JSON decoding (`keys_under_root`, `overwrite_keys`, `add_error_key`) but do not add processors that rename, drop, or alter ECS fields from the app.

**Why:** The task said “Do not parse or transform ECS fields”. We interpret that as: decode the line as JSON and attach the decoded fields, Identity fields (`service.name`, `service.environment`, `env`) come from the application only; Filebeat does not add or overwrite them. We do not add processors that change `@timestamp`, `log.level`, `message`, `service.name`, or other ECS fields.

**Trade-off:** If Logstash or Elasticsearch expect different field names later, that can be handled in Logstash or index mapping, not in Filebeat for Phase 2.

## Logstash in Compose with TLS

**Decision:** Logstash is included in docker-compose.yml. It listens on port 5044 with TLS (Beats input). Filebeat sends to `logstash:5044` over TLS. Logstash output is stdout only (no Elasticsearch).


**Trade-off:** Logstash runs in the same stack; for production, Logstash could be scaled or moved to a dedicated host. Stdout output is for verification only; Phase 4 will add Elasticsearch output.

**Why (TLS / path / stdout):** Pipeline verification requires an endpoint for Filebeat to connect to. Running Logstash in the same Compose project allows end-to-end verification (Filebeat to Logstash over TLS, events on stdout). TLS is required so that no plaintext Beats traffic is sent. The canonical log path is `/apps/logs` (HARD CONTRACT); see PHASE0-contract.md. Logstash output remains stdout for verification only; Elasticsearch output is Phase 4. mTLS is deferred; server TLS only.

## Why /apps/logs is enforced

**Decision:** The canonical log path is `/apps/logs` (directory) and `/apps/logs/application.log` (file). No reference to `/app/logs` is allowed.

**Why:** The task description requires `/apps/logs/*.log` as a HARD CONTRACT. All implementation (Log4j2, Dockerfile, docker-compose, Filebeat, entrypoint) uses `/apps/logs` so that Filebeat, Logstash, and future Elasticsearch indexing are consistent (e.g. `log.file.path` = `/apps/logs/application.log`).

**Trade-off:** None; the contract is strict and enforced everywhere.

## Why TLS Added Now (Before Elasticsearch)

**Decision:** Enable TLS between Filebeat and Logstash in Phase 2, before adding Elasticsearch.

**Why:** The task explicitly requires secure TLS output from Filebeat to Logstash. Plaintext Beats on 5044 is a contract violation. Adding TLS now ensures the pipeline is correct and observable (handshake, connection established) before introducing Elasticsearch. Self-signed CA and server cert are sufficient for development.

**Trade-off:** Certificate generation (tls/gen-certs.sh) must be run before first `docker compose up`; private keys are gitignored.

## Why mTLS Deferred

**Decision:** Do not implement mutual TLS (client certificate verification). Logstash Beats input does not require a client certificate; only server TLS is enabled.

**Why:** The task states mutual TLS (client cert) is OPTIONAL and to not implement now. Server-side TLS (Logstash presents a certificate, Filebeat verifies it with the CA) is sufficient to encrypt the channel. Client certs would add complexity without being required for Phase 2.

**Trade-off:** Any client with network access and trust in the server cert can connect to port 5044; in a closed Docker network this is acceptable. mTLS can be added in a later phase if needed.

## Why Stdout Output Retained (Logstash)

**Decision:** Logstash outputs events to stdout (rubydebug). No Elasticsearch output.

**Why:** Phase 2 scope is Filebeat to Logstash over TLS and verification that events flow. Stdout is the minimal, observable sink. The task states Logstash output must remain stdout. Elasticsearch output is explicitly Phase 4.

**Trade-off:** Stdout is not a permanent sink; it is for verification only. Phase 4 will add Elasticsearch output.

## Entrypoint su Argument Order (Alpine/BusyBox)

**Decision:** Use `exec su -s /bin/sh -c "exec java -jar /app/app.jar" appuser` (shell and command before the username), not `su ... appuser -c "..."`.

**Why:** Alpine uses BusyBox `su`, which expects `-s /bin/sh -c "command"` before the target username. The other order can cause "unrecognized option" and the JVM would not start as appuser. This was found and fixed at runtime; see `solution.md`.

**Trade-off:** None; the fix is required for the entrypoint to work on this base image.

## Compose version Removed

**Decision:** Remove the top-level `version: '3.8'` from docker-compose.yml.

**Why:** Compose v2 ignores the attribute and warns that it is obsolete. Removing it clears the warning and avoids confusion.

**Trade-off:** None.

## Registry on a Dedicated Volume

**Decision:** Persist Filebeat’s data directory (and thus the registry) on a named volume `filebeat-data` mounted at `/usr/share/filebeat/data`.

**Why:** The task required “Registry path must be persisted”. Without that, restarts would lose read offsets and could re-send or miss lines. A dedicated volume keeps registry state across container restarts and recreations.

**Trade-off:** The volume grows with registry size and any other data under `path.data`. For Phase 2 we do not add cleanup or retention; that can be addressed later if needed.
