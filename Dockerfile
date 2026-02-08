# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Create app user
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001

WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Phase 2: /apps/logs for shared volume with Filebeat sidecar. Do not create /app/logs.
RUN chown -R appuser:appuser /app && mkdir -p /apps/logs && chown -R appuser:appuser /apps/logs

# Entrypoint runs as root so it can chown /apps/logs when a volume is mounted; then drops to appuser.
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

USER root

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]

