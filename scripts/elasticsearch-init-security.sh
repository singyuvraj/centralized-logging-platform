#!/bin/sh
# Phase 4–5: Create security primitives for the logging pipeline in Elasticsearch.
# - Phase 4: logstash_writer role/user for Logstash output.
# - Phase 5: kibana_system password, kibana_readonly role, kibana_user for Kibana UI.
# Requires ELASTIC_PASSWORD, LOGSTASH_WRITER_PASSWORD, KIBANA_SYSTEM_PASSWORD, KIBANA_USER_PASSWORD in environment.
# Run after Elasticsearch is up (e.g. from a one-off container on same network).
set -e

ES_URL="${ES_URL:-https://elasticsearch:9200}"
CA_CERT="${CA_CERT:-/scripts/ca.crt}"

echo "Waiting for Elasticsearch at $ES_URL..."
until curl -sf --cacert "$CA_CERT" -u "elastic:${ELASTIC_PASSWORD}" "${ES_URL}/_cluster/health" | grep -qE '"status":"(green|yellow)"'; do
  sleep 5
done
echo "Elasticsearch is up."

echo "Creating role logstash_writer..."
curl -sf --cacert "$CA_CERT" -X PUT -u "elastic:${ELASTIC_PASSWORD}" "${ES_URL}/_security/role/logstash_writer" \
  -H "Content-Type: application/json" -d '{
  "cluster": ["monitor", "manage_index_templates"],
  "indices": [ { "names": ["*-logs-*"], "privileges": ["write", "create_index"] } ]
}'
echo ""

echo "Creating user logstash_writer..."
curl -sf --cacert "$CA_CERT" -X PUT -u "elastic:${ELASTIC_PASSWORD}" "${ES_URL}/_security/user/logstash_writer" \
  -H "Content-Type: application/json" -d "{\"password\":\"${LOGSTASH_WRITER_PASSWORD}\",\"roles\":[\"logstash_writer\"]}"
echo ""

echo "Setting password for built-in kibana_system user..."
curl -sf --cacert "$CA_CERT" -X POST -u "elastic:${ELASTIC_PASSWORD}" "${ES_URL}/_security/user/kibana_system/_password" \
  -H "Content-Type: application/json" -d "{\"password\":\"${KIBANA_SYSTEM_PASSWORD}\"}"
echo ""

echo "Creating role kibana_readonly..."
curl -sf --cacert "$CA_CERT" -X PUT -u "elastic:${ELASTIC_PASSWORD}" "${ES_URL}/_security/role/kibana_readonly" \
  -H "Content-Type: application/json" -d '{
  "cluster": [],
  "indices": [ { "names": ["*-logs-*"], "privileges": ["read", "view_index_metadata"] } ]
}'
echo ""

echo "Creating user kibana_user..."
curl -sf --cacert "$CA_CERT" -X PUT -u "elastic:${ELASTIC_PASSWORD}" "${ES_URL}/_security/user/kibana_user" \
  -H "Content-Type: application/json" -d "{\"password\":\"${KIBANA_USER_PASSWORD}\",\"roles\":[\"kibana_readonly\"]}"
echo ""

echo "Elasticsearch security setup complete."
