#!/usr/bin/env sh
# Generate self-signed CA and TLS certs for the logging stack.
# Phase 2: Logstash server cert for Beats TLS.
# Phase 5: nginx (public HTTPS) and internal Kibana→Elasticsearch TLS via nginx.
# Run from repo root or from tls/: ./tls/gen-certs.sh or ./gen-certs.sh
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

echo "Generating certificates in $DIR"

# CA (reuse existing under tls/ if present; create only on first run)
if [ ! -f ca.key ] || [ ! -f ca.crt ]; then
  openssl genrsa -out ca.key 2048
  openssl req -new -x509 -days 3650 -key ca.key -out ca.crt -subj "/CN=SULJHAOO Logging Dev CA"
  echo "Created new CA (ca.crt, ca.key)"
else
  echo "Reusing existing CA (ca.crt, ca.key)"
fi

# Logstash server key and cert (SAN for logstash hostname) — create only if missing (Phase 2)
if [ ! -f logstash.key ] || [ ! -f logstash.crt ]; then
  openssl genrsa -out logstash.key 2048
cat > logstash.cnf <<EOF
[req]
distinguished_name = dn
req_extensions = ext
[dn]
CN = logstash
[ext]
subjectAltName = DNS:logstash,DNS:localhost,IP:127.0.0.1
EOF
openssl req -new -key logstash.key -out logstash.csr -config logstash.cnf -subj "/CN=logstash" -batch
openssl x509 -req -in logstash.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out logstash.crt -days 3650 -extensions ext -extfile logstash.cnf
  echo "Created Logstash cert (logstash.crt, logstash.key)"
else
  echo "Reusing existing Logstash cert"
fi
rm -f logstash.csr logstash.cnf 2>/dev/null || true

# nginx public HTTPS cert (browser → nginx)
rm -f nginx.key nginx.crt 2>/dev/null || true
openssl genrsa -out nginx.key 2048
cat > nginx.cnf <<EOF
[req]
distinguished_name = dn
req_extensions = ext
[dn]
CN = localhost
[ext]
subjectAltName = DNS:localhost,IP:127.0.0.1
EOF
openssl req -new -key nginx.key -out nginx.csr -config nginx.cnf -subj "/CN=localhost" -batch
openssl x509 -req -in nginx.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out nginx.crt -days 3650 -extensions ext -extfile nginx.cnf

# Kibana→Elasticsearch TLS via nginx (Kibana talks to https://nginx:9200)
rm -f kibana.key kibana.crt 2>/dev/null || true
openssl genrsa -out kibana.key 2048
cat > kibana.cnf <<EOF
[req]
distinguished_name = dn
req_extensions = ext
[dn]
CN = nginx
[ext]
subjectAltName = DNS:nginx
EOF
openssl req -new -key kibana.key -out kibana.csr -config kibana.cnf -subj "/CN=nginx" -batch
openssl x509 -req -in kibana.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out kibana.crt -days 3650 -extensions ext -extfile kibana.cnf

# Elasticsearch HTTP SSL (HTTPS-only access; same CA, no CA regeneration)
if [ ! -f elasticsearch.key ] || [ ! -f elasticsearch.crt ]; then
  openssl genrsa -out elasticsearch.key 2048
  cat > elasticsearch.cnf <<EOF
[req]
distinguished_name = dn
req_extensions = ext
[dn]
CN = elasticsearch
[ext]
subjectAltName = DNS:elasticsearch
EOF
  openssl req -new -key elasticsearch.key -out elasticsearch.csr -config elasticsearch.cnf -subj "/CN=elasticsearch" -batch
  openssl x509 -req -in elasticsearch.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out elasticsearch.crt -days 3650 -extensions ext -extfile elasticsearch.cnf
  echo "Created Elasticsearch cert (elasticsearch.crt, elasticsearch.key)"
  rm -f elasticsearch.csr elasticsearch.cnf 2>/dev/null || true
else
  echo "Reusing existing Elasticsearch cert"
fi

rm -f logstash.csr logstash.cnf nginx.csr nginx.cnf kibana.csr kibana.cnf *.srl 2>/dev/null || true

echo "Generated CA (ca.crt), Logstash (logstash.crt), nginx (nginx.crt), Kibana internal (kibana.crt), and Elasticsearch (elasticsearch.crt) in $DIR"
