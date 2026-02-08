# Ops Hardening & Reliability Deep Dive

This document describes how the logging stack behaves under stress and failures, and what we have done to make it operationally safe for production.

Current architecture (already implemented and validated):

- `backend` (Spring Boot) → logs to `/apps/logs/application.log`
- `filebeat` sidecar → reads `/apps/logs/*.log`, ships to `logstash` over TLS (Beats 5044)
- `logstash` → Persistent Queue (PQ) + Dead Letter Queue (DLQ), Elasticsearch output over HTTPS
- `elasticsearch` → secured, HTTPS-only, no public ports
- `kibana` → HTTPS via `nginx` reverse proxy

This phase **does not** change pipeline logic. It focuses on survivability, capacity safety, and operator runbooks.

---

## 1. Architecture Flow Under Stress

### Normal flow

1. Application writes structured JSON logs to `/apps/logs/application.log`.
2. Filebeat tails `/apps/logs/*.log`, parses JSON, and forwards events to Logstash over TLS (`logstash:5044`).
3. Logstash Beats input writes events into the **Persistent Queue** on disk.
4. Logstash pipeline reads from PQ, applies minimal routing logic, and sends events to Elasticsearch over HTTPS.
5. Elasticsearch indexes events into `service.name`-scoped indices; Kibana reads from Elasticsearch for visualization.

### Under stress/failure

- If **Elasticsearch slows down**:
  - Logstash’s output throughput drops.
  - PQ depth grows; events accumulate on disk instead of being dropped.
  - Filebeat sees back-pressure as Logstash drains the PQ more slowly.

- If **Elasticsearch is down**:
  - Logstash output logs connection errors and retries.
  - Events remain in PQ on disk.
  - Once Elasticsearch is back, Logstash drains PQ and resumes normal flow.

- If **Logstash restarts**:
  - PQ and DLQ directories live on a Docker volume (`logstash-data`).
  - On restart, Logstash reopens the PQ, replays unprocessed events, and continues.

---

## 2. Back-pressure Boundaries (Filebeat vs Logstash)

### Filebeat back-pressure

- Filebeat uses the Beats protocol over TCP.
- When Logstash is slow or unreachable:
  - TCP back-pressure + application-level acks cause Filebeat to slow down.
  - Filebeat keeps a small amount of data buffered in memory and on disk (its registry).
  - It **does not** persist unbounded backlogs; it is not a long-term buffer.

### Logstash back-pressure

- Logstash Persistent Queue is the primary **disk-backed buffer**:
  - Input: Beats plugin writes events into PQ.
  - Output: Elasticsearch plugin reads from PQ.
  - PQ lives on disk under `/usr/share/logstash/data/queue` (Docker volume `logstash-data`).

Boundary:

- Filebeat handles **short-term, in-flight** back-pressure at the edge.
- Logstash PQ handles **medium-term, disk-backed** buffering when Elasticsearch is slow or down.

---

## 3. PQ vs DLQ Responsibilities

### Persistent Queue (PQ)

- Purpose:
  - Absorb transient downstream outages (Elasticsearch down/slow).
  - Survive Logstash restarts without losing accepted events.
- Characteristics:
  - Bounded by `queue.max_bytes` (currently `1gb`).
  - At-least-once semantics: events may be retried, so duplicates are possible.
  - Used by all events flowing through the pipeline.

### Dead Letter Queue (DLQ)

- Purpose:
  - Capture **bad events** that cannot be indexed, even after retries.
  - Examples: mapping conflicts, invalid field types, bad index names (when supported by the output plugin).
- Characteristics:
  - Enabled with `dead_letter_queue.enable: true`.
  - Stored under `/usr/share/logstash/data/dead_letter_queue` (same `logstash-data` volume).
  - Bounded by `dead_letter_queue.max_bytes` (currently `256mb`).
  - Not automatically replayed; requires operator action and tooling.

Separation of concerns:

- PQ is about **buffering**.
- DLQ is about **triaging** problematic events.

---

## 4. Failure Matrix – What Breaks, What Survives

### 4.1 Elasticsearch down

- **Symptoms:**
  - Logstash logs `Elasticsearch Unreachable` / `No Available connections`.
  - PQ depth grows over time.
  - Kibana shows stale data (no new documents).

- **What survives:**
  - Events already accepted into PQ remain on disk.
  - Filebeat keeps running and will back off as PQ approaches capacity.
  - When Elasticsearch returns, Logstash drains PQ and delivers buffered events.

- **What can break:**
  - If the outage is longer than what `queue.max_bytes` can absorb, PQ will fill.
  - When PQ is full, new events cannot be enqueued; upstream may be throttled or see errors.

### 4.2 Elasticsearch slow

- **Symptoms:**
  - Logstash logs intermittent timeouts or retries.
  - Latency to index events increases.
  - PQ depth grows and shrinks with ES performance.

- **What survives:**
  - Short-term bursts are smoothed by PQ.
  - Events are persisted while waiting to be sent.

- **What can break:**
  - Sustained slow performance with high ingest can still fill PQ.
  - Capacity planning is needed to size PQ appropriately.

### 4.3 Logstash restart/crash

- **Symptoms:**
  - Brief gap in Logstash availability (Beats connection resets).
  - After restart, Filebeat reconnects.

- **What survives:**
  - PQ contents on `logstash-data` remain intact.
  - Unprocessed events are replayed on restart.

- **What can break:**
  - If `logstash-data` is deleted (`docker compose down -v`), PQ and DLQ are lost.
  - This is a destructive operation and must be treated as such in runbooks.

### 4.4 Disk full (PQ/DLQ or ES data disk)

- **Symptoms:**
  - Logstash logs I/O or disk full errors when writing PQ/DLQ.
  - Elasticsearch logs disk watermark or disk full warnings.
  - Ingestion stalls; PQ cannot grow; DLQ cannot accept new entries.

- **What survives:**
  - Already-written PQ/DLQ segments remain on disk.
  - No silent drops; errors are visible in logs.

- **What can break:**
  - New events may be rejected or blocked until disk space is freed.
  - If disk is fully exhausted, Elasticsearch or Logstash can stop.

---

## 5. Capacity Planning Math (PQ & DLQ)

We do **not** auto-tune PQ/DLQ sizes. Instead, we document how to size them.

### 5.1 PQ sizing

Given:

- `R` = peak ingest rate in events/second.
- `S` = average event size in bytes (including overhead).
- `T` = desired outage window in seconds (how long PQ should buffer).

Then:

- Approximate required PQ size:

  \[
  PQ\_bytes \approx R \times S \times T \times \text{safety\_factor}
  \]

Example:

- Peak: 2,000 events/sec.
- Avg size: 1.5 KB/event (1500 bytes).
- Outage window: 30 minutes = 1,800 seconds.
- Safety factor: 2 (for spikes/overhead).

\[
PQ\_bytes \approx 2000 \times 1500 \times 1800 \times 2 \approx 10.8 \text{ GB}
\]

For this project we use `1gb` as a **development/default**:

- Enough to show behavior under failure.
- Small enough not to surprise local disks.
- Production deployments should recompute `queue.max_bytes` based on real R/S/T.

### 5.2 DLQ sizing

DLQ should be **much smaller** than PQ:

- DLQ is for rare, bad events, not for general buffering.
- Current setting: `256mb` – enough to hold many problematic events while operators investigate.

Sizing guideline:

- Estimate worst-case rate of bad events (ideally very low).
- Decide how long you want to retain them before manual intervention.
- Size DLQ accordingly, but keep it much smaller than PQ.

---

## 6. Operational Guarantees vs Non-Guarantees

### Guaranteed (by design and config)

- Events accepted into PQ are stored on disk and survive Logstash restarts.
- Short/medium Elasticsearch outages are absorbed up to `queue.max_bytes`.
- Individual problematic events (where supported) are routed to DLQ, not silently dropped.
- HTTPS-only communication to Elasticsearch is enforced from Logstash, es-setup, and Kibana.

### Not guaranteed

- **Exactly-once delivery:** Duplicates can occur (e.g. retried batches, restarts).
- **Infinite buffering:** PQ and DLQ are bounded. If limits are hit, operators must act.
- **Automatic DLQ replay:** No replay tooling yet; this is a manual, future concern.
- **Self-healing from disk full:** The system cannot free disk; only operators can.

---

## 7. Failure Runbooks

These runbooks assume:

- You have CLI access to the Docker host.
- You can run `docker compose` in the project directory.

### 7.1 Elasticsearch down

- **Symptoms:**
  - Kibana stops showing new data.
  - `docker compose logs logstash` shows `Elasticsearch Unreachable` / `No Available connections`.
  - PQ depth (if monitored) increases.

- **Root cause (typical):**
  - Elasticsearch container stopped or crashed.
  - Disk, memory, or config issues on Elasticsearch node.

- **What NOT to do:**
  - Do **not** run `docker compose down -v` (this deletes volumes, including PQ/DLQ and ES data).
  - Do **not** blindly delete `logstash-data` or `es-data`.

- **Safe recovery steps:**
  1. Check ES container:
     - `docker compose ps elasticsearch`
     - `docker compose logs elasticsearch`
  2. Fix underlying cause (disk, memory, config).
  3. Restart ES:
     - `docker compose restart elasticsearch`
  4. Confirm health:
     - `docker compose exec logstash curl -sk -u "elastic:$ELASTIC_PASSWORD" https://elasticsearch:9200/_cluster/health?pretty`
  5. Monitor PQ draining and Kibana catching up.

### 7.2 Elasticsearch slow

- **Symptoms:**
  - Higher indexing latency visible in Kibana.
  - Logstash logs intermittent timeouts/retries.
  - PQ depth slowly grows.

- **Root cause (typical):**
  - High ES load (search, ingest).
  - Insufficient ES resources (CPU, heap, disks).

- **What NOT to do:**
  - Do not arbitrarily increase PQ to “infinite” sizes; this only masks issues and risks disk exhaustion.
  - Do not disable PQ to temporarily “speed things up”.

- **Safe recovery steps:**
  1. Check ES health and stats (Kibana Dev Tools or curl).
  2. Reduce ingest rate if possible (throttle upstream noise).
  3. Scale ES or add resources (out of scope for this repo, but correct long-term fix).
  4. Monitor PQ size; ensure it is not approaching `queue.max_bytes`.

### 7.3 Logstash restart

- **Symptoms:**
  - Brief interruption in Logstash logs; Filebeat reconnects.
  - After restart, events should resume.

- **Root cause (typical):**
  - Deployment/upgrade.
  - Memory/CPU pressure.

- **What NOT to do:**
  - Do not remove `logstash-data` unless you intentionally accept PQ/DLQ loss.

- **Safe recovery steps:**
  1. Restart Logstash:
     - `docker compose restart logstash`
  2. Confirm Logstash status:
     - `docker compose ps logstash`
  3. Verify PQ/DLQ directories still exist:
     - `docker compose exec logstash ls -ld /usr/share/logstash/data /usr/share/logstash/data/queue /usr/share/logstash/data/dead_letter_queue`
  4. Check logs to ensure pipeline is started and Elasticsearch connection is restored.

### 7.4 Disk full (Logstash PQ/DLQ)

- **Symptoms:**
  - Logstash logs I/O or disk errors when writing PQ/DLQ.
  - PQ stops growing; ingestion may stall.

- **Root cause (typical):**
  - `logstash-data` volume filled by PQ/DLQ or other unexpected files.

- **What NOT to do:**
  - Do not `rm -rf` PQ or DLQ directories blindly; you will discard buffered and diagnostic data.
  - Do not run `docker compose down -v` unless you accept data loss and have coordinated it.

- **Safe recovery steps:**
  1. Identify what is consuming space:
     - On the host: `docker system df`, inspect the `logstash-data` volume.
  2. Free space **outside** PQ/DLQ first (logs, other containers).
  3. If PQ is legitimately too small, plan a **controlled** resize:
     - Stop Logstash cleanly.
     - Backup PQ/DLQ if needed.
     - Adjust `queue.max_bytes` and/or add disk.
  4. Restart Logstash and verify normal operation.

### 7.5 DLQ growth

- **Symptoms:**
  - DLQ directory (`/usr/share/logstash/data/dead_letter_queue`) grows steadily.
  - Logstash logs may show recurring per-event failures (e.g. mapping errors).

- **Root cause (typical):**
  - Persistent data shape mismatch (fields not matching ES mappings).
  - Misconfigured index names or templates.

- **What NOT to do:**
  - Do not ignore DLQ growth; it is a signal that data is being rejected.
  - Do not delete DLQ files without at least understanding their contents.

- **Safe recovery steps:**
  1. Stop Logstash (optional but safer for DLQ inspection).
  2. Copy DLQ files from `logstash-data` for offline analysis.
  3. Use appropriate tools (e.g. `logstash` DLQ viewer or custom scripts) to inspect entries.
  4. Fix the underlying cause (mapping, templates, field shapes) in Elasticsearch or upstream.
  5. Decide whether to replay or discard DLQ contents in a controlled way (future tooling).

---

## 8. Summary of Operational Behaviour

With the current configuration:

- **Data durability:** Events are not lost on common failures (short ES outages, Logstash restarts). They are buffered on disk in a Persistent Queue.
- **Visibility:** Bad events are captured in a Dead Letter Queue instead of disappearing silently.
- **Security:** All traffic to Elasticsearch is over HTTPS; TLS is consistently configured.
- **Operational controls:** Restart policies, bounded queues, and clear runbooks exist for the main failure modes operators will face.

Monitoring, alerting, and tooling around DLQ/PQ are not yet in place. With the current design and capacity planning, the stack can be run in production-like environments; operators should add observability and runbooks as needed.

