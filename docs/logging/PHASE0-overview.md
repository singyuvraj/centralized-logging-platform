# Phase 0 Overview: Centralized Logging Pipeline

## What Phase 0 Is and Why It Exists

Phase 0 is a documentation and alignment phase. No software is built, no config is deployed, and no containers are run. The goal is to agree on what we are building and how it fits together before anyone writes code or YAML.

We do this so that when implementation starts, everyone uses the same log path, the same field names, and the same pipeline shape. Changing these later is costly; getting them right on paper first is cheap.

## Problem Statement

Applications run in containers and write logs to local files or stdout. Those logs stay on the host or in the container. To debug across services, correlate errors, or build dashboards, we need logs in one place, in a queryable form.

Without a plan, each team may log differently, use different paths, or ship logs in ad-hoc ways. That makes it hard to build a single, consistent log store and to query it in a uniform way.

Phase 0 defines how we will collect, ship, and store logs so that we can search and analyse them in one system (Elasticsearch/Kibana) instead of SSH-ing into many hosts or opening many files.

## High-Level Pipeline

The flow we have agreed on is:

1. **Application** – Spring Boot containers write logs to a file on a shared volume. Path: `/apps/logs/application.log`.

2. **Filebeat** – A lightweight shipper that tails that file, reads new lines, and sends them to Logstash. Filebeat runs as a sidecar next to each application container.

3. **Logstash** – Receives log events from Filebeat, can parse, filter, and enrich them, then sends them to Elasticsearch. Logstash is the central ingestion layer.

4. **Elasticsearch** – Stores the log events and indexes them so they can be queried quickly.

5. **Kibana** – Connects to Elasticsearch and provides the UI for searching logs and building visualizations.

So the path is: **App -> Filebeat -> Logstash -> Elasticsearch -> Kibana**. The app never talks to Logstash or Elasticsearch directly; Filebeat does the shipping.

## The Sidecar Pattern (Plain Explanation)

A "sidecar" is an extra container that runs next to your main application container in the same pod or task group. They share things like network and storage.

For logging:

- The main container runs the Spring Boot app and writes to `/apps/logs/application.log` on a shared volume.
- The sidecar container runs Filebeat and is configured to read from that same path on the same volume.

So both containers see the same log file. The app only writes; Filebeat only reads and forwards. The app does not need to know about Logstash or Elasticsearch. If you scale the app, you scale one Filebeat per app instance, so each instance’s logs are shipped on their own.

This keeps responsibilities separate: the app logs, the sidecar ships.

## What Phase 0 Does Not Contain

Phase 0 contains **no implementation**. No Filebeat config, no Logstash pipelines, no Docker Compose changes, and no application code changes are part of Phase 0. The only deliverables are documentation and agreement on contracts, decisions, and scope. Implementation begins in a later phase.
