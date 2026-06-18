# Deployment & CI/CD

## Pipelines (GitHub Actions)

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | Push (all branches), PR | Backend `mvn verify` (unit **and** Testcontainers integration tests), frontend `check` + `build`, jar artifact |
| `cd.yml` | Push to `main`, tags `v*`, manual | Builds Docker images for backend & frontend and pushes to **GHCR** (`ghcr.io/<owner>/sellfish-{backend,frontend}`) |
| `deploy.yml` | Manual (`workflow_dispatch`) | Optional SSH deploy to the target server (`docker compose pull && up -d`) |

Integration tests run in CI against a real PostgreSQL + pgvector container (Testcontainers uses
the runner's Docker daemon) — the stack is verified end-to-end on every push.

### Image Tags
`cd.yml` assigns: `latest` (only `main`), `sha-<short>`, and for Git tags `v1.2.3` plus the
SemVer version.

## Starting Production

Prerequisite: Docker + Docker Compose on the server. The public domain is **not** stored in
the repo — each installation sets it in `.env` (open-source friendly).

```bash
git clone <repo> /opt/sellfish
cd /opt/sellfish/infra
cp .env.example .env
# In .env set:
#   SITE_ADDRESS=<your-domain>           e.g. jobs.example.com  -> auto-TLS via Let's Encrypt
#   PUBLIC_ORIGIN=https://<your-domain>
#   JWT_SECRET / CRYPTO_MASTER_KEY        (openssl rand -base64 32)
#   DB_PASSWORD / MINIO_SECRET_KEY        (strong values)
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Requirements for auto-TLS: the domain's DNS A/AAAA record points to the server, and ports
80 **and** 443 are reachable. Caddy fetches and renews certificates automatically.

After that (single origin via Caddy):
- Frontend: `https://<your-domain>/`
- API: `https://<your-domain>/api/...`
- Swagger UI: `https://<your-domain>/swagger-ui.html`
- Health: `https://<your-domain>/actuator/health`

Without a domain (local / behind your own proxy): leave `SITE_ADDRESS=:80` → everything under
`http://<host>/`.

Components: `caddy` (reverse proxy + TLS) → `frontend` (SvelteKit/Node) + `backend` (Spring Boot),
`postgres` (pgvector), `minio` (object storage). Data in volumes `pgdata`/`miniodata`/`caddydata`.

### Secrets (mandatory in production)
`openssl rand -base64 32` for `JWT_SECRET` and `CRYPTO_MASTER_KEY`; strong `DB_PASSWORD` /
`MINIO_SECRET_KEY`. LLM provider keys ideally via Infisical (`INFISICAL_*`) rather than plaintext.

## Automatic Deploy (optional)

Start `deploy.yml` manually (Actions → Deploy → Run). Required repository secrets:
`SSH_HOST`, `SSH_USER`, `SSH_KEY` (private key), optionally `SSH_PORT`, `DEPLOY_PATH`,
`GHCR_TOKEN` (PAT with `read:packages`, if images are private). The workflow pulls the new
images on the server and restarts the production compose stack.

## TLS
Built in: Caddy terminates TLS automatically (Let's Encrypt) as soon as `SITE_ADDRESS` is a
domain. No certbot, no manual certificate management.
