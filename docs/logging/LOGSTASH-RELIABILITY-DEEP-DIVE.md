# Logstash Reliability Deep Dive – PQ, DLQ, and Back-pressure

This document explains how Logstash handles back-pressure, retries, and failures in this project. The focus is on **correctness and durability**, not on adding new features.

It assumes:

- Filebeat → Logstash → Elasticsearch → Kibana is already working.
- TLS and HTTPS are already configured.
- Only Logstash is being changed in this phase.

---

## 1. Why the Logstash Persistent Queue Exists

Without a persistent queue, Logstash keeps events **in memory** between input and output:

- Filebeat sends events to Logstash.
- Logstash parses and forwards them to Elasticsearch.
- If Elasticsearch is down or slow, events pile up in memory.
- A Logstash restart or crash can lose in-flight events.

The **Persistent Queue (PQ)** fixes this by:

- Writing events to disk **before** they are acknowledged to the input.
- Surviving Logstash restarts (disk-backed, not RAM-only).
- Providing a bounded buffer so Elasticsearch outages don’t immediately drop events.

In short:

- **Without PQ:** Events are held in memory; back-pressure is limited by heap; a restart can lose in-flight events.
- **With PQ:** Events are written to disk before ack; restarts replay from the queue; events are not lost as long as the queue volume is intact.

---

## 2. Filebeat vs Logstash Back-pressure

There are two levels of back-pressure:

- **Filebeat-side back-pressure:**
  - Filebeat talks to Logstash over Beats protocol.
  - When Logstash is busy, Filebeat **slows down** (TCP + application-level acks).
  - This protects Logstash from being overloaded, but does not persist events beyond Filebeat’s own buffers and its registry.

- **Logstash-side back-pressure:**
  - Logstash now has a **Persistent Queue** on disk.
  - Inputs write into the PQ.
  - Filters and outputs read from the PQ.
  - If Elasticsearch is slow or down, events accumulate in the PQ until the configured limit.

Relationship:

- Filebeat back-pressure prevents Logstash from being overwhelmed.
- Logstash PQ ensures that **once events reach Logstash**, they are not lost if Logstash restarts or Elasticsearch is temporarily unavailable.

---

## 3. Internal Logstash Flow (with PQ and DLQ)

With persistent queue and dead-letter queue enabled, the flow is:

1. **input (Beats):**
   - Receives events from Filebeat.
   - Writes events into the **Persistent Queue** on disk.
   - Acknowledges to Filebeat after the event is accepted into the queue.

2. **Persistent Queue (PQ):**
   - On-disk queue at `/usr/share/logstash/data/queue` (backed by `logstash-queue` Docker volume).
   - Segmented log files, with periodic checkpoints for durability and crash recovery.

3. **filter (pipeline):**
   - Reads events from the PQ.
   - Applies any filters (currently minimal; main work is routing to Elasticsearch).

4. **output (Elasticsearch):**
   - Attempts to send events to Elasticsearch over HTTPS.
   - If ES is up and healthy, events are indexed and removed from the PQ.
   - If ES fails on specific events (e.g., mapping conflicts), these events may be written to the **Dead Letter Queue** (DLQ) instead of being dropped.

5. **retry and DLQ:**
   - Logstash output plugins retry failed batches based on internal policies.
   - If an event is consistently rejected for a non-transient reason (e.g., “bad” document), and the plugin supports DLQ, the event is written to the DLQ.
   - DLQ lives on disk at `/usr/share/logstash/data/dead_letter_queue` (backed by `logstash-dlq` Docker volume).

6. **DLQ consumer (future ops tooling):**
   - This phase does **not** implement DLQ replay.
   - In production, operators can inspect DLQ files to understand and optionally reprocess bad events.

---

## 4. Failure Matrix

### 4.1 Elasticsearch DOWN

- **What happens:**
  - Logstash’s Elasticsearch output cannot connect or gets 5xx errors.
  - Output retries; events are **not** acknowledged as successfully delivered.
  - Events remain in the Persistent Queue, consuming disk space under `logstash-queue` volume.
  - Filebeat eventually experiences back-pressure from Logstash when PQ nears capacity.

- **Guarantees:**
  - Events already accepted into PQ are **not lost** when ES is down.
  - When ES comes back, Logstash drains the PQ and continues indexing.

- **Limits:**
  - If PQ fills (`queue.max_bytes`), Logstash will back-pressure inputs harder.
  - If upstream (Filebeat/app) keeps producing and PQ is full, new events may be rejected/upstream-throttled depending on the Beats protocol.

### 4.2 Elasticsearch SLOW

- **What happens:**
  - Elasticsearch accepts requests but at reduced throughput.
  - PQ drains more slowly; PQ depth may grow.
  - Back-pressure spreads:
    - Output → PQ → input → Filebeat.

- **Guarantees:**
  - As long as PQ has free space, events are retained on disk until ES catches up.
  - Once ES performance improves, Logstash drains the accumulated queue.

- **Limits:**
  - Sustained slow ES + continuous high load can still fill the PQ.
  - Disk sizing and `queue.max_bytes` must be chosen based on expected peak buffering needs.

### 4.3 Logstash RESTART (planned or crash)

- **What happens:**
  - Logstash process stops; PQ files and DLQ files remain on disk (Docker volumes).
  - On restart, Logstash:
    - Reopens the PQ.
    - Resumes reading unacknowledged events from the queue.

- **Guarantees:**
  - Events already in PQ are **not** lost across restarts.
  - At-least-once semantics from Logstash to Elasticsearch:
    - An event might be retried after restart (potential duplicate at ES).
    - It should not be silently dropped by Logstash.

### 4.4 Disk FULL (queue or DLQ volumes)

- **What happens:**
  - If the underlying volume for `/usr/share/logstash/data/queue` or `/usr/share/logstash/data/dead_letter_queue` fills:
    - Logstash cannot write new PQ segments or DLQ entries.
    - Errors appear in Logstash logs about I/O failures.
    - Back-pressure cascades to inputs and upstream producers.

- **Guarantees:**
  - Logstash does not silently drop data on disk-write failure; failures are surfaced in logs.
  - Already-written segments remain; operators can recover them once disk pressure is resolved.

- **Limits:**
  - If operators ignore disk alerts, Logstash may stall or crash.
  - Capacity planning for queue and DLQ volumes is essential (see tuning section below).

---

## 5. Operational Guarantees (and Non-Guarantees)

**Guaranteed (given current config):**

- Events accepted into the PQ are durably stored on disk until:
  - Successfully sent to Elasticsearch, or
  - Moved to the DLQ.
- Logstash restarts do **not** drop events in the PQ.
- If Elasticsearch is temporarily unavailable, events will be buffered (up to `queue.max_bytes`) instead of being dropped in memory.

**Not guaranteed:**

- **Exactly-once delivery:** Duplicate events are possible, especially around retries and restarts.
- **Infinite buffering:** PQ size is bounded. When full, upstream must slow down or new events are rejected.
- **Automatic DLQ replay:** This phase does not implement any DLQ consumer or replay logic.
- **Protection against total disk exhaustion:** PQ and DLQ volumes must be sized and monitored; Logstash cannot “magically” avoid full disks.

---

## 6. Tuning Guidelines

### 6.1 Queue Size (queue.max_bytes)

- Current setting: `queue.max_bytes: 1gb`
- Rationale:
  - For development and small environments, 1 GB gives a reasonable buffer window when Elasticsearch is down or slow.
  - It avoids runaway disk usage while still allowing a meaningful backlog.

Sizing approach for production:

- Estimate peak log volume per minute (e.g. MB/min).
- Decide how many minutes of outage you want to absorb (e.g. 30–60 minutes).
- Set `queue.max_bytes` ≈ `peak_MB_per_min * minutes_of_outage * safety_factor`.

### 6.2 Checkpoint Settings

- `queue.checkpoint.acks: 1024`
- `queue.checkpoint.writes: 1024`
- `queue.checkpoint.interval: 1000` (ms)

These settings balance:

- **Durability:** Frequent checkpoints reduce window of data at risk if the host crashes between fsyncs.
- **Overhead:** Too-frequent checkpoints increase I/O and CPU overhead.

For production, adjust based on:

- Throughput.
- Disk performance.
- Acceptable risk window (how many events you can afford to redo).

### 6.3 DLQ Size (dead_letter_queue.max_bytes)

- Current setting: `dead_letter_queue.max_bytes: 256mb`

Guidance:

- DLQ is for **bad events**, not for bulk buffering.
- Size it large enough to hold a realistic number of problematic events between operational reviews.
- If DLQ fills, Logstash may need operator intervention; otherwise additional bad events may be dropped or cause failures.

---

## 7. Prerequisite for Ops Hardening

Monitors, alerts, and recovery procedures are only useful if the pipeline does not drop data on common failures (restarts, ES outages).

PQ and DLQ give:

- A clear place to **see backlogs** (PQ) and **see bad events** (DLQ).
- Durable state across restarts, which is critical when:
  - Deploying new versions of Logstash.
  - Restarting nodes for maintenance.
  - Experiencing transient Elasticsearch issues.

With PQ and DLQ enabled, ops work (alerts, runbooks, capacity planning) can assume known behaviour under failure instead of events disappearing on restart or during outages.

---

## 8. What Was Deliberately Not Implemented

This phase intentionally does **not**:

- Add new filters or transformation logic.
- Add ILM, index templates, or ES-side retention policies.
- Implement DLQ replay tooling or automation.
- Implement multi-queue or multi-pipeline tuning.
- Implement fine-grained ES retry policies beyond Logstash defaults.

The focus is strictly on:

- Enabling Logstash’s built-in disk-backed buffering (PQ).
- Enabling Logstash’s built-in Dead Letter Queue.
- Wiring these to Docker volumes for persistence.
- Understanding and documenting the behavior under common failure modes.

Future phases (Ops hardening) can build on this foundation with:

- Monitoring for PQ depth and DLQ growth.
- Runbooks for DLQ inspection and remediation.
- Capacity planning for queue and DLQ volumes.

