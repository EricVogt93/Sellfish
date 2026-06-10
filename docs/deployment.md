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

Voraussetzung: Docker + Docker Compose auf dem Server. Die öffentliche Domain wird **nicht**
im Repo hinterlegt, sondern pro Installation in der `.env` gesetzt (Open-Source-tauglich).

```bash
git clone <repo> /opt/bewerbungsatze
cd /opt/bewerbungsatze/infra
cp .env.example .env
# In .env setzen:
#   SITE_ADDRESS=<deine-domain>           z. B. jobs.example.com  -> Auto-TLS via Let's Encrypt
#   PUBLIC_ORIGIN=https://<deine-domain>
#   JWT_SECRET / CRYPTO_MASTER_KEY        (openssl rand -base64 32)
#   DB_PASSWORD / MINIO_SECRET_KEY        (starke Werte)
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Voraussetzungen für Auto-TLS: DNS-A/AAAA-Record der Domain zeigt auf den Server,
Ports 80 **und** 443 sind erreichbar. Caddy holt und erneuert die Zertifikate automatisch.

Danach (Single-Origin über Caddy):
- Frontend: `https://<deine-domain>/`
- API: `https://<deine-domain>/api/...`
- Swagger UI: `https://<deine-domain>/swagger-ui.html`
- Health: `https://<deine-domain>/actuator/health`

Ohne Domain (lokal/hinter eigenem Proxy): `SITE_ADDRESS=:80` lassen → alles unter `http://<host>/`.

Komponenten: `caddy` (Reverse Proxy + TLS) → `frontend` (SvelteKit/Node) + `backend` (Spring Boot),
`postgres` (pgvector), `minio` (Objekt-Storage). Daten in Volumes `pgdata`/`miniodata`/`caddydata`.

### Secrets (Pflicht in Prod)
`openssl rand -base64 32` für `JWT_SECRET` und `CRYPTO_MASTER_KEY`; starke `DB_PASSWORD`/`MINIO_SECRET_KEY`.
LLM-Provider-Keys idealerweise über Infisical (`INFISICAL_*`) statt im Klartext.

## Automatischer Deploy (optional)

`deploy.yml` manuell starten (Actions → Deploy → Run). Erforderliche Repository-Secrets:
`SSH_HOST`, `SSH_USER`, `SSH_KEY` (privater Key), optional `SSH_PORT`, `DEPLOY_PATH`, `GHCR_TOKEN`
(PAT mit `read:packages`, falls die Images privat sind). Der Workflow zieht auf dem Server die
neuen Images und startet die Prod-Compose neu.

## TLS
Bereits eingebaut: Caddy terminiert TLS automatisch (Let's Encrypt), sobald `SITE_ADDRESS`
eine Domain ist. Kein certbot, keine manuelle Zertifikatspflege.
