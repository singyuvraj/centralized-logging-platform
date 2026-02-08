# Phase 1 Decisions

This document records the main choices made in Phase 1 and the reasons behind them.

## Log4j2 Instead of Logback

**Decision:** Use Log4j2 as the only logging implementation and remove Logback.

**Why:**  
Phase 1 required ECS-compatible JSON output and a single, well-supported path. The existing dependency `co.elastic.logging:log4j2-ecs-layout` targets Log4j2. Log4j2’s `EcsLayout` gives `@timestamp`, `log.level`, `message`, `service.name`, `service.environment`, and supports KeyValuePair for `trace.id` and `env` without custom code. Moving to Log4j2 and dropping Logback kept the implementation simple and aligned with the Elastic ECS layout we already had.

**Trade-off:**  
Spring Boot defaults to Logback. We had to exclude `spring-boot-starter-logging` from every relevant starter and add `spring-boot-starter-log4j2`. Any future starter that brings Logback must be excluded as well. In return we get a single logging engine and a clean ECS JSON layout.

## ECS-Compatible JSON (Elastic Common Schema)

**Decision:** Use the Elastic ECS layout so every log line is one JSON object with ECS field names.

**Why:**  
Phase 0 required structured JSON and predictable field names. ECS defines `@timestamp`, `log.level`, `message`, `service.name`, and related fields. Using the official `log4j2-ecs-layout` avoids hand-rolled JSON and keeps the format consistent with common ELK usage. Downstream (Filebeat, Logstash, Elasticsearch) can parse and index without extra parsing logic.

**Trade-off:**  
Field names differ slightly from the Phase 0 example (`@timestamp` / `log.level` vs `timestamp` / `level`). Phase 1 was explicitly specified with `@timestamp` and `log.level`. We emit both `env` (Phase 0) and `service.environment` (ECS) so both conventions are covered.

## Log Path Default and LOG_PATH Override

**Decision:** Default file path is `/apps/logs/application.log`. Allow override via system property `LOG_PATH`.

**Why:**  
Phase 0 fixes the in-container path at `/apps/logs/application.log`. Containers and deployment scripts do not need to change. For local runs, `/app` often does not exist or is not writable, so a single override (`-DLOG_PATH=...`) lets the same config work locally without a second config file.

**Trade-off:**  
Local runs must set `LOG_PATH` or ensure `/apps/logs` exists and is writable. That is documented in Phase 1 known-issues and implementation. We did not add an application.properties-based path because Log4j2 initializes before the Spring context and we did not want to split “local” vs “container” config.

## Trace ID via MDC and a Filter (No Distributed Tracing)

**Decision:** Use a servlet filter to set a trace ID in MDC per request. Do not add OpenTelemetry, Sleuth, or other distributed tracing in Phase 1.

**Why:**  
The requirement was “trace.id when available” and “traceId / requestId using MDC” without new tracing libraries. A filter that reads `X-Trace-Id` or `X-Request-Id` (or generates a UUID), puts it in MDC, and clears it in `finally` is enough. The EcsLayout KeyValuePair `$${ctx:traceId}` maps that to `trace.id` in the JSON. This stays local to the process and does not depend on external tracing infrastructure.

**Trade-off:**  
Trace IDs are request-scoped and not propagated to or from other services. Cross-service correlation would require a later phase (e.g. OpenTelemetry or similar). Phase 1 only ensures that within one request all logs can share the same `trace.id`.

## TraceIdFilter Order and Placement

**Decision:** `TraceIdFilter` is a `@Component` `OncePerRequestFilter` with `@Order(Ordered.HIGHEST_PRECEDENCE)`, in the `config` package.

**Why:**  
Trace ID must be set before any application or security logic runs so that every log in the request, including security filters, can see it. Highest precedence places it first in the filter chain. Putting it in `config` matches other cross-cutting setup (e.g. `CorsConfig`, `SecurityConfig`) and keeps it visible as application configuration, not business logic.

**Trade-off:**  
Filter order and component scanning are implicit. If other filters are added later, their order relative to `TraceIdFilter` must be considered. For Phase 1, one high-precedence filter is enough.

## Console and File Same Format

**Decision:** Console and file appenders both use EcsLayout with the same settings.

**Why:**  
Phase 1 required “also log to console using the same JSON format.” Using one layout for both avoids drift and makes local debugging (e.g. `mvn spring-boot:run`) match what is written to the file and what Filebeat will later read.

**Trade-off:**  
Console is no longer human-friendly plain text. Debugging via raw tail/console relies on JSON. That was an explicit requirement; any pretty-printing or “dev vs prod” layouts would be a later phase.

## env From ENV Variable

**Decision:** `env` and `service.environment` are driven by the process environment variable `ENV`, defaulting to `dev`.

**Why:**  
We needed a value that works before Spring is up and that can differ per environment without code or profile tricks. An environment variable is set by the runner (container, systemd, etc.) and is available to Log4j2 at config load via `${env:ENV:-dev}`. No application.properties or profile-specific logic is required.

**Trade-off:**  
Teams must set `ENV` (or rely on `dev`) in their run scripts or containers. There is no Spring property like `logging.env` in Phase 1; that could be added later if we need to bind it to `spring.profiles.active` or similar.

## Commons-logging Excluded from Classpath

**Decision:** Exclude `commons-logging` from the `spring-cloud-aws-starter-parameter-store` dependency. Do not remove or replace `spring-jcl`.

**Why:**  
`commons-logging` was pulled in transitively (AWS SDK apache-client → httpclient → commons-logging). With both `commons-logging` and Spring’s `spring-jcl` on the classpath, Spring logged: “please remove commons-logging.jar from classpath in order to avoid potential conflicts.” Excluding it on the Parameter Store starter removes that transitive and leaves `spring-jcl` as the only JCL implementation, so the warning goes away and discovery is unambiguous.

**Trade-off:**  
No new dependencies; exclusion only. Log4j2 and SLF4J are unchanged. If another dependency later brings in `commons-logging`, it would need to be excluded in the same way.
