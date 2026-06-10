# Deployment & CI/CD

## Pipelines (GitHub Actions)

| Workflow | Trigger | Zweck |
|----------|---------|-------|
| `ci.yml`     | Push (alle Branches), PR        | Backend `mvn verify` (Unit- **und** Testcontainers-Integrationstests), Frontend `check` + `build`, Jar als Artefakt |
| `cd.yml`     | Push auf `main`, Tags `v*`, manuell | Baut Docker-Images für Backend & Frontend und pusht sie nach **GHCR** (`ghcr.io/<owner>/bewerbungsatze-{backend,frontend}`) |
| `deploy.yml` | manuell (`workflow_dispatch`)   | Optionaler SSH-Deploy auf den Zielserver (`docker compose pull && up -d`) |

Die Integrationstests laufen in CI gegen einen echten PostgreSQL+pgvector-Container
(Testcontainers nutzt den Docker-Daemon des Runners) — damit wird der Stack bei jedem
Push real verifiziert.

### Image-Tags
`cd.yml` vergibt: `latest` (nur `main`), `sha-<kurz>`, sowie bei Git-Tags `v1.2.3` und die SemVer-Version.

## Produktion starten

Voraussetzung: Docker + Docker Compose auf dem Server.

```bash
git clone <repo> /opt/bewerbungsatze
cd /opt/bewerbungsatze/infra
cp .env.example .env            # Secrets setzen! (JWT_SECRET, CRYPTO_MASTER_KEY, DB_PASSWORD, MINIO_SECRET_KEY)
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Danach läuft alles hinter nginx auf Port 80 (Single-Origin):
- Frontend: `http://<host>/`
- API: `http://<host>/api/...`
- Swagger UI: `http://<host>/swagger-ui.html`
- Health: `http://<host>/actuator/health`

Komponenten: `nginx` (Reverse Proxy) → `frontend` (SvelteKit/Node) + `backend` (Spring Boot),
`postgres` (pgvector), `minio` (Objekt-Storage). Daten liegen in den Volumes `pgdata`/`miniodata`.

### Secrets (Pflicht in Prod)
`openssl rand -base64 32` für `JWT_SECRET` und `CRYPTO_MASTER_KEY`; starke `DB_PASSWORD`/`MINIO_SECRET_KEY`.
LLM-Provider-Keys idealerweise über Infisical (`INFISICAL_*`) statt im Klartext.

## Automatischer Deploy (optional)

`deploy.yml` manuell starten (Actions → Deploy → Run). Erforderliche Repository-Secrets:
`SSH_HOST`, `SSH_USER`, `SSH_KEY` (privater Key), optional `SSH_PORT`, `DEPLOY_PATH`, `GHCR_TOKEN`
(PAT mit `read:packages`, falls die Images privat sind). Der Workflow zieht auf dem Server die
neuen Images und startet die Prod-Compose neu.

## TLS
Für HTTPS einen Reverse-Proxy mit automatischem Zertifikat (z. B. Caddy oder Traefik) vor
nginx setzen oder nginx um ein `certbot`-Setup erweitern und `PUBLIC_ORIGIN=https://<domain>`
sowie `HTTP_PORT` entsprechend anpassen.
