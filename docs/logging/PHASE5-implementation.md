# Phase 5 – Implementation Details (Kibana & nginx)

## 1. TLS Assets (tls/gen-certs.sh)

`tls/gen-certs.sh` was extended to generate all TLS artifacts from a single dev CA:

- **CA:** `ca.crt`, `ca.key` – self-signed dev CA.
- **Logstash:** `logstash.crt`, `logstash.key` – unchanged from Phase 2 (Beats TLS).
- **nginx (public HTTPS):** `nginx.crt`, `nginx.key` – SANs: `DNS:localhost`, `IP:127.0.0.1`. Used for browser → nginx on port 443.
- **Kibana internal TLS:** `kibana.crt`, `kibana.key` – SAN `DNS:nginx`. Used by nginx on port 9200 for Kibana → Elasticsearch TLS.

Private keys and certs are gitignored (`.gitignore` updated to ignore all of the above under `tls/`), and must be re-generated locally with:

```sh
./tls/gen-certs.sh
```

---

## 2. Elasticsearch Security – Kibana Users and Roles

`scripts/elasticsearch-init-security.sh` now also bootstraps Kibana-related security:

- **kibana_system password**: Sets the password for the built-in `kibana_system` user from `KIBANA_SYSTEM_PASSWORD`.
- **Role `kibana_readonly`:**
  - `cluster`: `[]` (no cluster-level privileges).
  - `indices`: names `*-logs-*`, privileges `["read", "view_index_metadata"]`.
- **User `kibana_user`:**
  - Password from `KIBANA_USER_PASSWORD`.
  - Roles: `["kibana_readonly"]`.

`docker-compose.yml` passes these env vars into the `es-setup` one-off container:

- `KIBANA_SYSTEM_PASSWORD=${KIBANA_SYSTEM_PASSWORD:?Set KIBANA_SYSTEM_PASSWORD in .env}`
- `KIBANA_USER_PASSWORD=${KIBANA_USER_PASSWORD:?Set KIBANA_USER_PASSWORD in .env}`

---

## 3. Kibana Service (docker-compose.yml)

New service `kibana`:

- **Image:** `docker.elastic.co/kibana/kibana:8.15.0`
- **Ports:** none published (only `5601/tcp` internally).
- **Env:**
  - `SERVER_HOST=0.0.0.0`
  - `SERVER_PORT=5601`
  - `ELASTICSEARCH_HOSTS=https://nginx:9200` – Kibana connects to nginx on port 9200 using HTTPS.
  - `ELASTICSEARCH_USERNAME=kibana_system`
  - `ELASTICSEARCH_PASSWORD=${KIBANA_SYSTEM_PASSWORD}` – from `.env`.
  - `ELASTICSEARCH_SSL_CERTIFICATEAUTHORITIES=/usr/share/kibana/config/certs/ca.crt`
- **Volumes:**
  - `./tls/ca.crt:/usr/share/kibana/config/certs/ca.crt:ro`
- **Depends on:** `elasticsearch`, `es-setup`.
- **Network:** `suljhaoo-network`.

Kibana uses `kibana_system` only as its internal service account; end users authenticate as `kibana_user`.

---

## 4. nginx Reverse Proxy

New service `nginx`:

- **Image:** `nginx:1.27-alpine`
- **Ports:** `443:443` (only). Port 80 is not published to the host.
- **Volumes:**
  - `./nginx/nginx.conf:/etc/nginx/nginx.conf:ro`
  - `./tls/nginx.crt:/etc/nginx/certs/nginx.crt:ro`
  - `./tls/nginx.key:/etc/nginx/certs/nginx.key:ro`
  - `./tls/kibana.crt:/etc/nginx/certs/kibana.crt:ro`
  - `./tls/kibana.key:/etc/nginx/certs/kibana.key:ro`
- **Depends on:** `kibana`
- **Network:** `suljhaoo-network`

`nginx/nginx.conf` defines:

- **Public HTTPS server (port 443):**
  - `ssl_certificate /etc/nginx/certs/nginx.crt`
  - `ssl_certificate_key /etc/nginx/certs/nginx.key`
  - `location /` proxies to `suljhaoo-kibana:5601` (Kibana UI).
  - Adds `X-Forwarded-Proto: https` so Kibana knows it is behind HTTPS.

- **Internal TLS endpoint for Kibana → Elasticsearch (port 9200):**
  - `listen 9200 ssl;`
  - `server_name nginx;`
  - `ssl_certificate /etc/nginx/certs/kibana.crt`
  - `ssl_certificate_key /etc/nginx/certs/kibana.key`
  - Proxies all paths to `suljhaoo-elasticsearch:9200` over HTTP.

Kibana connects to `https://nginx:9200`, validates the cert against `ca.crt`, and nginx forwards the request to Elasticsearch.

---

## 5. Environment Variables (Phase 5)

`env.example` was updated with Phase 5 variables (no real values):

- `KIBANA_SYSTEM_PASSWORD` – password for `kibana_system` user.
- `KIBANA_USER_PASSWORD` – password for `kibana_user` (UI login).

Real values are set in local `.env` only and must not be committed.

---

## 6. Public Exposure and Privacy

- **Elasticsearch:** Still has **no host ports** published; only other containers on `suljhaoo-network` can reach it.
- **Kibana:** No ports published; reachable only via nginx.
- **nginx:** The **only** publicly exposed entrypoint is HTTPS on port 443.

Kibana UI is therefore reachable at `https://localhost/` (or the host where docker-compose runs), with TLS terminated by nginx, while Elasticsearch stays private.

