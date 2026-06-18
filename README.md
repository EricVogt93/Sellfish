<div align="center">

# Sellfish

**AI-powered, self-hostable job-search and application-assistant platform**

![Status: Beta](https://img.shields.io/badge/Status-Beta-orange?style=flat-square)
![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue?style=flat-square)
![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-green?style=flat-square)
![SvelteKit 5](https://img.shields.io/badge/SvelteKit-5-ff3e00?style=flat-square)
![PostgreSQL + pgvector](https://img.shields.io/badge/PostgreSQL-+pgvector-blue?style=flat-square)

[![Buy me a coffee](https://img.shields.io/badge/Buy%20me%20a%20coffee-☕-yellow?style=flat-square)](https://www.buymeacoffee.com/ericvogt)

</div>

---

> [!IMPORTANT]
> **Beta.** Sellfish is under active development. Features may change, database
> schemas may migrate, and there is no stability or support guarantee. Back up your data.
> See the [Roadmap](#roadmap) for what's done and what's in progress.

---

## What is Sellfish?

A self-hostable application that finds relevant job listings for each user — based on
their CV, preferences, hard filters, and **learned signals** — and helps them apply:
job-tailored CVs, cover letters, motivation letters, and application texts, generated
by the AI provider of your choice.

The AI integration is **provider-agnostic**: self-hosted Ollama, OpenAI/ChatGPT,
Anthropic/Claude, Google Gemini, NVIDIA NIM, OpenRouter — all through a single slim
multiprovider layer. Over time, a **self-learning process** improves match quality by
learning personal weights from user feedback (saved / dismissed / applied).

All user data (CVs, certificates, applications) is stored **per-user AES-GCM-encrypted**.
Global secrets can optionally be loaded from [Infisical](https://infisical.com).

## Features

| Area | Function |
|------|----------|
| **Auth** | JWT login/registration, 4 roles (`USER`, `ADMIN`, `ORG_ADMIN`, `SUPER_ADMIN`), account data export & deletion (GDPR) |
| **Profile** | CV, preferences, hard filters (location/salary/contract type/exclusions) vs. soft weights |
| **Documents** | Upload & parsing of CV/certificates/references/cover letters/project lists (Apache Tika + LLM) |
| **Job Sources** | 33 integrated sources (employment agencies, remote boards, ATS scrapers, LLM web search), dedup, pgvector embeddings |
| **Matching** | 4-stage pipeline: Hard filter → Semantic (pgvector) → Feature score (8 features) → optional LLM rerank |
| **Self-Learning** | Feedback labels, profile drift (centroid shift), per-user logistic regression |
| **Generators** | Tailored CV, cover letter, motivation letter, application text + interview prep & salary insights |
| **Agent / MCP** | External AI agents (Claude Desktop, Cursor, …) control the platform via JSON-RPC / MCP |
| **Admin** | User, source, and provider management |
| **Enterprise** | Feature toggles, RSA-signed licenses, multi-tenant (orgs), SSO/OIDC, audit log, reports |

> Detailed description of every function: **[docs/wiki.md](docs/wiki.md)**

## Job Sources

Sellfish aggregates listings from 33 sources across 18 countries plus worldwide-remote.
Sources marked **API key** require credentials configured per source (via the admin UI or
`job_sources` table). All others are free/public (public APIs or HTML scraping).

### Free / No API Key (25)

| Source | Code | Coverage |
|--------|------|----------|
| Bundesagentur für Arbeit | `BA` | DE (public key, no signup) |
| 4Scotty | `4SCOTTY` | DE |
| IT-Talents | `ITTALENTS` | DE, AT, CH |
| Honeypot | `HONEYPOT` | DE, NL, AT |
| Remotive | `REMOTIVE` | Worldwide Remote |
| RemoteOK | `REMOTEOK` | Worldwide Remote |
| Arbeitnow | `ARBEITNOW` | DE, GB, NL, PL, RO, CZ, HU, AT, CH, FR |
| Himalayas | `HIMALAYAS` | Worldwide Remote |
| Jobicy | `JOBICY` | Worldwide Remote |
| Working Nomads | `WORKINGNOMADS` | Worldwide Remote |
| Europe Remotely | `EURREMOTE` | 18 European countries |
| Remote.co | `REMOTECO` | Worldwide Remote |
| JustRemote | `JUSTRMOTE` | Worldwide Remote |
| Jobspresso | `JOBSPRESSO` | Worldwide Remote |
| NoDesk | `NODESK` | Worldwide Remote |
| WeWorkRemotely | `WWREMOTE` | Worldwide Remote |
| Who is Hiring (HN) | `WHOISHIRING` | Worldwide Remote |
| Greenhouse (ATS) | `GREENHOUSE` | US, DE, GB, CA, NL, FR, AT, CH |
| Lever (ATS) | `LEVER` | US, DE, GB, CA, NL, FR, AT, CH |
| Recruitee (ATS) | `RECRUITEE` | DE, NL, GB, FR, AT, CH |
| Ashby (ATS) | `ASHBY` | US, DE, GB, CA, NL |
| SmartRecruiters (ATS) | `SMARTRECRUITERS` | DE, US, GB, NL, FR, ES, IT, AT, CH |
| Workable (ATS) | `WORKABLE` | US, DE, GB, NL, FR, CA, AU, NZ |
| Generic Scraper | `SCRAPER` | Configurable (any URL) |
| LLM Web Search | `LLM_WEB` | Uses configured LLM to search |

### API Key Required (8)

| Source | Code | Config Keys | Coverage | Where to get a key |
|--------|------|-------------|----------|--------------------|
| Adzuna | `ADZUNA` | `app_id`, `app_key` | GB, US, DE, FR, NL, IT, ES, PL, AT, CH | [adzuna.com/developers](https://developer.adzuna.com) |
| CareerJet | `CAREERJET` | `api_key` | DE, GB, US, FR, NL, IT, ES, AT, CH, PL, CZ | [careerjet.com/partners](https://www.careerjet.com/partners/api/) |
| Findwork | `FINDWORK` | `api_key` | Worldwide Remote | [findwork.dev](https://findwork.dev/developers/) |
| Jooble | `JOOBLE` | `api_key` | DE, GB, US, FR, NL, IT, ES, PL, AT, CH | [jooble.org/api](https://jooble.org/about-api) |
| Reed | `REED` | `api_key` | GB | [reed.co.uk/developers](https://www.reed.co.uk/developers/jobseeker) |
| The Muse | `THEMUSE` | `api_key` | US, CA, GB, DE, FR, NL | [themuse.com/developers](https://www.themuse.com/developers/api/v2) |
| USA Jobs | `USAJOBS` | `api_key` | US | [developer.usajobs.gov](https://developer.usajobs.gov) |
| ZipRecruiter | `ZIPRECRUITER` | `api_key` | US | [ziprecruiter.com/developer](https://www.ziprecruiter.com/developer) |

> **Note:** The Bundesagentur (German federal employment agency) uses a hardcoded public
> API key (`jobboerse-jobsuche`) that is publicly documented — no signup needed.

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Architecture** | Hexagonal (Ports & Adapters) — [docs/hexagonal-architecture.md](docs/hexagonal-architecture.md) |
| **Backend** | Java 21, Spring Boot 3.4, Spring Security (JWT), Spring Session (JDBC, HA-ready) |
| **Database** | PostgreSQL + [pgvector](https://github.com/pgvector/pgvector) (HNSW cosine index), Flyway migrations |
| **Frontend** | SvelteKit 5 (TypeScript), Node adapter, Playwright E2E |
| **AI** | Custom multiprovider layer over `RestClient` (OpenAI-compatible, Anthropic, Gemini, Ollama) |
| **Secrets** | Infisical (machine identity) + AES-GCM-encrypted per-user keys |
| **Storage** | MinIO (S3-compatible) or local filesystem |
| **Reverse Proxy** | Caddy (auto-TLS via Let's Encrypt) |

## Architecture

```
                    ┌─────────────┐     ┌──────────────┐
   Browser ───────▶ │   Caddy     │────▶│  SvelteKit   │  (frontend, :3000)
                    │  (auto-TLS) │     └──────────────┘
                    │             │     ┌──────────────┐
                    │             │────▶│  Spring Boot │  (backend, :8080)
                    └─────────────┘     │   modular    │
                                        │   monolith   │
                                        └──────┬───────┘
                                               │
                          ┌────────────────────┼────────────────────┐
                          ▼                    ▼                    ▼
                   ┌──────────┐        ┌──────────────┐      ┌──────────────┐
                   │ Postgres │        │  LLM provider│      │  MinIO / FS  │
                   │ +pgvector│        │ (Ollama/OAI/ │      │ (documents)  │
                   │          │        │  Claude/Gem) │      └──────────────┘
                   └──────────┘        └──────────────┘
                                               │
                                        ┌──────────────┐
                                        │   Infisical  │  (optional secrets)
                                        └──────────────┘
```

## Project Structure

```
backend/    Spring Boot application (modular monolith, hexagonal)
frontend/   SvelteKit web app
infra/      docker-compose, .env.example, Caddyfile
docs/       Architecture, setup, and wiki documentation
.github/    CI/CD workflows + issue templates
```

## Getting Started (Local)

### Prerequisites
- JDK 21, Maven 3.9+
- Node 22+
- Docker (for Postgres + MinIO via docker-compose)

### 1. Infrastructure (Postgres + MinIO)
```bash
cp infra/.env.example infra/.env   # adjust values
docker compose -f infra/docker-compose.yml up -d postgres minio
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run
# Swagger UI:  http://localhost:8080/swagger-ui.html
# Health:      http://localhost:8080/actuator/health
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev   # http://localhost:5173 (proxies /api to :8080)
```

### Everything via Docker (development)
```bash
docker compose -f infra/docker-compose.yml up --build
```

## Production & CI/CD

- **CI** (`.github/workflows/ci.yml`): on every push — backend `mvn verify` including
  Testcontainers integration tests (real Postgres + pgvector) + frontend build + Playwright E2E.
- **CD** (`.github/workflows/cd.yml`): builds Docker images on `main`/tags and pushes to GHCR.
- **Production start** on a server (behind Caddy with auto-TLS; domain comes from `.env`,
  nothing is hardcoded in the repo):
  ```bash
  cd infra && cp .env.example .env   # SITE_ADDRESS=<your-domain> + set secrets!
  docker compose -f docker-compose.prod.yml pull
  docker compose -f docker-compose.prod.yml up -d
  ```

Details: [`docs/deployment.md`](docs/deployment.md).

## Configuration

All values come from environment variables (defaults are dev-only). See
[`infra/.env.example`](infra/.env.example) for the full list. Key ones:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_PASSWORD` | `sellfish` | Postgres password (production: use a real secret!) |
| `JWT_SECRET` | dev-only (Base64, 32 bytes) | JWT signing key (`openssl rand -base64 32`) |
| `CRYPTO_MASTER_KEY` | dev-only (Base64, 32 bytes) | AES-GCM master key for per-user keys |
| `EMBEDDING_DIM` | `768` | Vector dimension (settable only before first DB start) |
| `STORAGE_BACKEND` | `minio` | `minio` or `filesystem` |
| `ENTERPRISE_ENABLED` | `false` | Enterprise features + license validation |
| `SSO_ENABLED` | `false` | Enable OIDC SSO providers |
| `INFISICAL_ENABLED` | `false` | Load secrets from Infisical |
| `SITE_ADDRESS` | `:80` | Public domain for Caddy auto-TLS |

## Testing

- **Backend:** `cd backend && mvn verify` — uses Testcontainers (requires Docker) for a
  real Postgres with pgvector and tests the auth/profile flow end-to-end.
- **Frontend:** `cd frontend && npm run check` (type-check) + `npm run test:e2e` (Playwright).

## Roadmap

| Milestone | Scope | Status |
|-----------|-------|--------|
| **M0** | Scaffold: repo structure, docker-compose, Spring Boot & SvelteKit skeleton, Flyway schema, CI | ✅ |
| **M1** | Auth & multi-user (JWT), profile + preferences/filters | ✅ |
| **M2** | Document upload & parsing (Tika + LLM), LLM provider abstraction (multiprovider), Infisical, storage (FS/MinIO) | ✅ |
| **M3** | Job ingestion: 33 international API sources, dedup, pgvector embeddings, scheduler | ✅ |
| **M4** | Matching & ranking (hard filter → semantic → feature score → LLM rerank), matches API, feedback | ✅ |
| **M5** | Generators: tailored CV, cover letter, motivation, application text (LLM, versioned) | ✅ |
| **M6** | Self-learning: feedback labels, logistic regression (weights), profile drift, scheduler | ✅ |
| **M7** | Generic scraper, LLM web search, agent tool API, admin (users/sources/providers) | ✅ |
| **Enterprise** | Multi-tenant, SSO/OIDC, audit log, reports, license validation | 🔧 Beta |

Details: [`docs/architecture.md`](docs/architecture.md) and [`docs/wiki.md`](docs/wiki.md).

## Status & License

This project is **open source** (Apache License 2.0 — see [`LICENSE`](LICENSE) and
[`NOTICE`](NOTICE)). It is provided without support guarantee — use at your own risk.
Security reports: see [`SECURITY.md`](SECURITY.md).

Since Sellfish processes personal data (CVs, applications), GDPR-compliant operation
is the responsibility of each instance operator. The software provides building blocks
(per-user encryption, data export/deletion) but does not replace organizational measures.
