# Phase 1 Implementation

This document describes what was implemented and where it lives.

## 1. Dependencies (pom.xml)

- **Logback removed:** Every `spring-boot-starter-*` that pulled in `spring-boot-starter-logging` now excludes it. That removes Logback and its transitive deps from the runtime.

- **Log4j2 as engine:** `spring-boot-starter-log4j2` was added. It brings Log4j2 and the SLF4J→Log4j2 bridge. Application code still uses SLF4J (e.g. `@Slf4j`, `LoggerFactory`); the bridge sends those calls to Log4j2.

- **ECS layout:** The existing `co.elastic.logging:log4j2-ecs-layout` dependency is unchanged. It provides the `EcsLayout` used in the Log4j2 config.

- **Commons-logging excluded:** `commons-logging` was transitively brought in by `spring-cloud-aws-starter-parameter-store` (path: AWS SDK `apache-client` → `httpclient` → `commons-logging`). It is excluded on that dependency so only Spring’s `spring-jcl` remains for JCL bridging. That removes the “please remove commons-logging.jar from classpath” warning. `spring-jcl` is not removed or changed.

## 2. Log4j2 Configuration (log4j2-spring.xml)

Location: `src/main/resources/log4j2-spring.xml`.

- **Why `log4j2-spring.xml`:** Spring Boot loads this file when using Log4j2 and exposes the Spring environment to Log4j2 lookups (e.g. `${spring:spring.application.name}`). A plain `log4j2.xml` would not have Spring context at config load time.

- **Appenders:**  
  - **Console:** `SYSTEM_OUT`, EcsLayout. Same JSON format as the file.  
  - **File:** RollingFile at `${sys:LOG_PATH:-/apps/logs/application.log}`. Default path is the Phase 0 contract path. Rollover at 50 MB, up to 5 backup files.

- **EcsLayout settings:**  
  - `serviceName="${spring:spring.application.name}"` → `service.name` in JSON.  
  - `serviceEnvironment="${env:ENV:-dev}"` → `service.environment` in JSON; the `env` var defaults to `dev` when `ENV` is unset.  
  - `KeyValuePair key="trace.id" value="$${ctx:traceId}"` → `trace.id` from MDC when set.  
  - `KeyValuePair key="env" value="$${env:ENV:-dev}"` → `env` for Phase 0.

- **Loggers:** Root at INFO; `com.suljhaoo.backend.config.ParameterStoreConfig` at INFO. No other logger tuning in this phase.

## 3. How JSON Logging Works

- **Layout:** `EcsLayout` (from `log4j2-ecs-layout`) formats each log event as a single JSON line. It writes `@timestamp`, `log.level`, `message`, `ecs.version`, `service.name`, `service.environment`, `event.dataset`, `process.thread.name`, `log.logger`, and the custom `env` and `trace.id` fields.

- **NDJSON:** One line per event. No multi-line JSON in the main message. Stack traces are still written by the layout; they are part of the same JSON object.

- **No code changes for format:** Controllers and services keep using `log.info("...")`, `log.error("...")`, etc. The layout turns those into JSON. No `log.info("{}", jsonObject)` is required.

## 4. How Trace Correlation Works

- **Filter:** `TraceIdFilter` in `com.suljhaoo.backend.config` runs once per HTTP request, before the security chain. It is a `OncePerRequestFilter` with `@Order(Ordered.HIGHEST_PRECEDENCE)` so it runs first.

- **Trace ID source:**  
  1. If the request has `X-Trace-Id`, that value is used.  
  2. Else if it has `X-Request-Id`, that is used.  
  3. Else a new UUID is generated.

- **MDC:** The chosen value is put in SLF4J MDC under the key `traceId`. Log4j2’s ThreadContext backs SLF4J MDC, so the EcsLayout `KeyValuePair key="trace.id" value="$${ctx:traceId}"` sees it and adds `trace.id` to the JSON.

- **Response header:** When a trace ID is set, the filter adds `X-Trace-Id` to the response so callers can correlate.

- **Cleanup:** In a `finally` block the filter removes `traceId` from MDC so it does not leak to other requests on the same thread.

- **When `trace.id` is empty:** For logs outside a request (e.g. startup), there is no MDC. The layout still emits `trace.id`; it will be an empty string or omitted depending on the ECS layout behaviour. Phase 1 treats “when available” as “when a request has been assigned a trace ID.”

## 5. Log Path and Local Runs

- **Contract path:** `/apps/logs/application.log` (Phase 0).

- **Override:** The file appender uses `${sys:LOG_PATH:-/apps/logs/application.log}`. For local runs where `/app` does not exist or is not writable, set the JVM system property: `-DLOG_PATH=./logs/application.log` (or any writable path). For example, with `spring-boot:run` use  
  `-Dspring-boot.run.jvmArguments="-DLOG_PATH=./logs/application.log"`.

- **Creating the directory:** Log4j2 creates parent directories for the log file when possible. If the path is under a non‑writable root (e.g. `/app` on a host where you are not root), creation fails and the app will not start until `LOG_PATH` points to a writable location.

## 6. Environment (env)

- **Source:** The `env` (and ECS `service.environment`) value comes from the process environment variable `ENV`.  
- **Default:** If `ENV` is not set, the default is `dev`.  
- **Setting it:** In the environment or in the process that starts the JVM (e.g. `ENV=prod` or `export ENV=staging`). No application.properties entry is used for this in Phase 1.
