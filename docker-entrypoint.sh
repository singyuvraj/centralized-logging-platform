#!/bin/sh
# Ensure /apps/logs is writable by appuser when a volume is mounted (Phase 2 sidecar).
# Runs as root; drops to appuser for the JVM.
chown -R appuser:appuser /apps/logs 2>/dev/null || true
exec su -s /bin/sh -c "exec java -jar /app/app.jar" appuser
