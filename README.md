[![CI/CD Pipeline](https://github.com/Valantech/suljhaoo-backend-service/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Valantech/suljhaoo-backend-service/actions/workflows/ci-cd.yml)

# suljhaoo-backend-spring

## Run locally (logging stack)

Use these steps to run the full logging pipeline (Backend, Filebeat, Logstash, Elasticsearch, Kibana, nginx) on your machine. No author-specific secrets are required; everything is driven by local `.env` and generated certificates.

**Before you start:**

- `.env` is local-only and must never be committed. It is in `.gitignore`. Passwords are intentionally not in Git; you create `.env` from `env.example` and set values locally.
- TLS certificates are generated locally by `tls/gen-certs.sh`. Generated keys and certs under `tls/` are gitignored and must not be committed.

**Prerequisites:** Docker and Docker Compose installed.

---

**1. Clone the repository (if you have not already).**

```bash
git clone https://github.com/singyuvi46/centralized-logging-platform.git
cd centralized-logging-platform
```

This gives you the codebase. Success: you are in the project root.

---

**2. Create local environment file from the template.**

```bash
cp env.example .env
```

The template defines required variables (Elasticsearch, Logstash, Kibana passwords) with placeholder values so the stack can start. For local runs the placeholders are sufficient; for any shared or production use, edit `.env` and set strong passwords. Success: the file `.env` exists in the project root; do not commit it.

---

**3. Generate TLS certificates.**

```bash
chmod +x tls/gen-certs.sh && ./tls/gen-certs.sh
```

The script creates a self-signed CA and server certificates for Logstash, nginx, Kibana, and Elasticsearch. Filebeat, Logstash, Kibana, and nginx require these files to start. Success: `tls/` contains `ca.crt`, `ca.key`, and the generated `.crt`/`.key` pairs; the script prints confirmation.

---

**4. Start the stack.**

```bash
docker compose up -d
```

This starts the backend, Filebeat, Logstash, Elasticsearch, es-setup (one-off), Kibana, and nginx. Success: all containers start; the backend may restart until a database is configured (DB is outside the logging stack).

---

**5. Verify.**

Check that containers are running:

```bash
docker compose ps
```

You should see `suljhaoo-elasticsearch`, `suljhaoo-logstash`, `suljhaoo-filebeat`, `suljhaoo-kibana`, `suljhaoo-nginx` (and optionally `suljhaoo-backend`) with status `Up` or `running`. Then open `https://localhost` in a browser and accept the self-signed certificate; you should get the Kibana login page. Log in with the username and password you set in `.env` for `KIBANA_USER_PASSWORD` (the `kibana_user` account).

Optional: confirm Elasticsearch is up from the host (replace `changeme-elastic` with your `ELASTIC_PASSWORD` from `.env`):

```bash
docker compose exec logstash curl -sk -u "elastic:changeme-elastic" "https://elasticsearch:9200/_cluster/health?pretty"
```

Success: JSON output with `"status" : "yellow"` or `"green"`.

---

**6. Tear down (optional).**

To stop all services and remove volumes:

```bash
docker compose down -v
```

---

Logging pipeline design, phases, and troubleshooting: see `docs/logging/`. Handoff summary for reviewers: see `HANDOFF-README.md`.

CI runs compile and package only; tests are skipped because they require local env. See HANDOFF-README.md, section "CI status note".
