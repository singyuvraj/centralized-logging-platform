# Phase 1 Known Issues and Limitations

This document lists known limitations of the Phase 1 logging implementation. None of these are addressed in Phase 1; they are recorded so we do not forget them and so operators know what to expect.

## Application Does Not Start Without a Writable Log Path

If the log file path is not writable, the application fails at startup when Log4j2 initializes the file appender.

- **Default path:** `/apps/logs/application.log`.  
- **When it fails:** For example, when running locally without `/app` or without write access to `/apps/logs`.  
- **Workaround:** Set the JVM system property `LOG_PATH` to a writable path, e.g.  
  `-DLOG_PATH=./logs/application.log`.  
  For `mvn spring-boot:run`, use  
  `-Dspring-boot.run.jvmArguments="-DLOG_PATH=./logs/application.log"`.  
- **In containers:** Ensure the container runtime mounts a volume or creates `/apps/logs` so the app can write there. Filebeat and deployment are out of scope for Phase 1.

## No Filebeat (Logs Not Shipped)

Phase 1 does not run or configure Filebeat. Logs are only:

- Written to the configured file (default `/apps/logs/application.log`), and  
- Printed to stdout in the same JSON form.

Nothing is shipped to Logstash or Elasticsearch. Adding Filebeat (and any sidecar or host configuration) is a later phase.

## Database Failure Can Prevent Startup

The application requires a configured DataSource. If the database is unavailable or misconfigured, Spring Boot fails during context initialization and the process exits. This is unrelated to logging but affects “application starts” in practice.

- **Expectation:** “Application starts (even if DB fails)” was a goal; we did not change database or auto-configuration in Phase 1.  
- **Current behaviour:** Without a valid DB (or an embedded DB / test profile), the application does not start.  
- **Workaround:** Use a running database or a profile that enables an embedded DB (e.g. H2 in tests). Phase 1 does not add a “run without DB” mode.

## trace.id Only Present During HTTP Requests

The field `trace.id` is taken from MDC and is therefore only set while handling an HTTP request.

- **When it appears:** For log events emitted during request handling (after `TraceIdFilter` has run).  
- **When it is missing or empty:** For startup, shutdown, scheduled tasks, or any logging that happens outside a request. In those cases `trace.id` may be blank or absent.  
- **No propagation:** Trace IDs are not sent to or received from other services. Cross-service correlation is out of scope for Phase 1.

## No TLS, Certificates, or Elasticsearch Users

Phase 1 does not introduce:

- TLS or certificates between the app and any external service,
- Elasticsearch users or role-based access,
- Index lifecycle or retention.

Those will be handled in later phases when Filebeat, Logstash, and Elasticsearch are configured.

## No Kibana Dashboards or Saved Objects

We do not define or ship Kibana dashboards, visualizations, or saved searches. The app only produces JSON logs; how they are visualized is left to a later phase.

## ParameterStoreConfig Logs Before Log4j2

Log output from `ParameterStoreConfig` (e.g. “Loading Parameter Store config…”, “Failed to load…”) can appear before Log4j2 is fully initialized. Those lines may use a different format (e.g. plain text or a fallback). That behaviour comes from the order in which Spring Boot and AWS/Parameter Store components start; Phase 1 did not change that ordering. Once Log4j2 is active, all application logs use the ECS JSON layout.
