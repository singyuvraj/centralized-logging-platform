# Phase 1 Overview: Application JSON Logging

## What Phase 1 Does

Phase 1 prepares the Spring Boot application to emit structured JSON logs that satisfy the Phase 0 logging contract. All changes are local to the application. No Filebeat, Logstash, Elasticsearch, TLS, or Docker configuration is touched.

## What Changed

- **Logging engine:** Logback was removed. Log4j2 is now the only logging implementation. SLF4J remains the API; it routes to Log4j2.

- **Log format:** Logs are structured JSON (NDJSON), one JSON object per line. The layout is Elastic ECS–compatible so downstream tools can parse fields without custom regex.

- **Outputs:** Logs go to the console and to a file. The file path is `/apps/logs/application.log` by default, overridable via the `LOG_PATH` system property for local runs.

- **Mandatory fields:** Every log event includes `@timestamp`, `log.level`, `message`, `service.name`, `env`, and `trace.id` when a request is in progress.

- **Trace correlation:** A servlet filter sets a trace identifier in MDC for each request. The value comes from the `X-Trace-Id` or `X-Request-Id` header when present, otherwise a generated UUID. No distributed tracing library was added.

- **Commons-logging removed from classpath:** `commons-logging` was pulled in transitively by the AWS Parameter Store starter (AWS SDK apache-client → httpclient → commons-logging). It is excluded on that dependency so Spring Boot uses only `spring-jcl` for the JCL bridge and the “please remove commons-logging.jar from classpath” warning no longer appears.

## Who This Is For

This doc is for engineers who need to understand what Phase 1 changed and how the application logs today. It does not describe how to run Filebeat or Elasticsearch.

## What Phase 1 Does Not Do

Phase 1 does not configure or run Filebeat, Logstash, or Elasticsearch. It does not add TLS, certificates, or index lifecycle. It does not change the database or Docker setup. Dashboards and alerting are out of scope.
