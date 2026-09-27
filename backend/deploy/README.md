# Private backend deployment and monitoring

This is a single-server Docker Compose setup. It runs the Go API, Prometheus, Alertmanager and Grafana. The API and Grafana bind to the server's **loopback interface**; Prometheus, Alertmanager and `/metrics` are reachable only on the Compose network. Keep it private while the fixed OTP (`1234`) and mock payments exist. `MOCK_MODE` defaults to `false`.

## Start on a Linux VPS with Docker Compose

1. Copy the repository to the server, install Docker Engine and the Compose plugin, and point a terminal at `backend/deploy`.
2. Copy `.env.example` to `.env`. Replace `GRAFANA_ADMIN_PASSWORD` with a long unique password. Keep `.env` off Git.
3. Run `docker compose up -d --build` and `docker compose ps`.
4. Check `curl http://127.0.0.1:8080/healthz` on the server. In another terminal, use `ssh -L 3000:127.0.0.1:3000 -L 8080:127.0.0.1:8080 USER@SERVER` and open `http://127.0.0.1:3000`. Sign into Grafana as `admin` with the password from `.env`; open **Dashboards → Worship → Worship API**.

The API data is in the named `account_data` Docker volume. Prometheus keeps 15 days of metrics in `prometheus_data`; Grafana and Alertmanager have separate volumes. Back up `account_data` before updating or moving the server. Do not run two API containers against the same JSON data file. To upgrade, pull code and run `docker compose up -d --build`; do not run `docker compose down -v` because it deletes account and monitoring volumes.

To check the metrics from the server without publishing port 9091, run `docker compose exec api wget -qO- http://127.0.0.1:9091/metrics | head`. The dashboard shows request rate, P95 response time, errors by route/status, and whether Prometheus can reach the API. API logs contain a fixed route name, HTTP method/status and duration in milliseconds; they omit request bodies, tokens, phone numbers, raw paths and queries. View them with `docker compose logs --since=1h api`.

## Alerts

Prometheus loads `alerts.yml`: **WorshipAPIHTTP500** fires when it sees at least one exact HTTP 500 in five minutes, **WorshipAPIUnavailableErrors** catches 502/503/504 responses, and **WorshipAPIDown** fires when the API cannot be scraped for two minutes. They are visible in Prometheus/Alertmanager even before a delivery channel is configured. `alertmanager.yml` has a no-delivery receiver by default, so **no external notification is sent until you configure a contact**.

For email, copy `alertmanager.email.yml.example` to `alertmanager.local.yml`, fill the SMTP host, sender, authentication and recipient, and set `ALERTMANAGER_CONFIG=./alertmanager.local.yml` in `.env`. The local file is Git ignored; restrict its permissions with `chmod 600 alertmanager.local.yml`. Run `docker compose up -d --force-recreate alertmanager` and confirm the receiver in Alertmanager logs. To verify delivery, temporarily stop just the API with `docker compose stop api`, wait over two minutes, check the `WorshipAPIDown` notification, then `docker compose start api`. A 500 alert can be tested against a private test endpoint or through an integration test; do not induce failures for real users.

If you prefer Slack or PagerDuty, replace the email receiver with a supported Alertmanager receiver in a Git ignored local config. A destination address or webhook credential is still needed to send notifications.

## Release limits

This stack is for private staging. Public release needs a verified SMS login provider, real payment verification, abuse protection, TLS termination, a transactional database, a backup/restore drill, and a privacy review. Avoid publishing either API port 8080 or metrics port 9091 directly to the internet. When those dependencies are ready, place a TLS reverse proxy in front of the API and give Android the HTTPS URL.

The API container listens internally on `0.0.0.0:8080`; Docker publishes it only on `127.0.0.1:8080` of the host. This lets an SSH tunnel reach it while keeping public traffic out. The backend itself defaults to loopback when run without Docker. The metrics listener defaults to `127.0.0.1:9091` outside Docker and is separate from the API.
