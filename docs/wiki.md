# Sellfish — Wiki

Comprehensive reference for every functional area of Sellfish. This document describes
what each module does, how the pieces fit together, and the key API surfaces.

> **Status: Beta.** This wiki reflects the current state of the codebase. Features may
> change between releases.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Authentication & Users](#authentication--users)
- [Profile & Preferences](#profile--preferences)
- [Documents](#documents)
- [AI / LLM Providers](#ai--llm-providers)
- [Job Sources & Ingestion](#job-sources--ingestion)
- [Matching & Ranking](#matching--ranking)
- [Self-Learning](#self-learning)
- [Generators](#generators)
- [Agent & MCP](#agent--mcp)
- [Admin](#admin)
- [Enterprise Features](#enterprise-features)
- [Multi-Tenant (Organizations)](#multi-tenant-organizations)
- [SSO / OIDC](#sso--oidc)
- [Audit Log](#audit-log)
- [Reports](#reports)
- [Storage](#storage)
- [Security & Crypto](#security--crypto)
- [Beta Auto-Setup](#beta-auto-setup)
- [Database Schema](#database-schema)
- [Frontend](#frontend)

---

## Architecture Overview

Sellfish is a **modular monolith**: a single deployable Spring Boot backend plus a
SvelteKit frontend and PostgreSQL. Background work (job search, embeddings, self-learning
retraining) runs asynchronously via schedulers.

The backend follows **hexagonal architecture** (Ports & Adapters):

- **Domain** — core logic (matching, learning, generation) is framework-agnostic
- **Application** — services orchestrating use cases
- **Adapters** — web controllers (inbound), databases/external APIs (outbound)

```
                    ┌─────────────┐     ┌──────────────┐
   Browser ───────▶ │   Caddy     │────▶│  SvelteKit   │
                    │  (auto-TLS) │     └──────────────┘
                    │             │     ┌──────────────┐
                    │             │────▶│  Spring Boot │
                    └─────────────┘     │   modular    │
                                        │   monolith   │
                                        └──────┬───────┘
                                               │
                          ┌────────────────────┼────────────────────┐
                          ▼                    ▼                    ▼
                   ┌──────────┐        ┌──────────────┐      ┌──────────────┐
                   │ Postgres │        │  LLM provider│      │  MinIO / FS  │
                   │ +pgvector│        │              │      │              │
                   └──────────┘        └──────────────┘      └──────────────┘
```

### Backend Modules (`de.sellfish.*`)

| Package | Responsibility |
|---------|----------------|
| `auth` | JWT issue/validate, login/registration, user details |
| `users` | User entity, `/api/me`, admin user management |
| `profile` | Profile, preferences, filters (hard filter vs. soft weights) |
| `docs` | Upload & parsing of CV/certificates/references/cover letters/project lists |
| `ai` | LLM provider abstraction, per-user provider selection, embeddings |
| `jobs` | Job sources, ingestion, dedup, pgvector embeddings, scheduler |
| `matching` | Hard filter → semantic (pgvector) → feature score → optional LLM rerank |
| `generate` | Generators: tailored CV, cover letter, motivation, application text |
| `learning` | Feedback collection, profile drift, weight retraining |
| `agent` | External agent API / MCP interface |
| `storage` | File storage abstraction (MinIO / filesystem) |
| `common` | Base entity, security config, error handling, properties |
| `admin` | Admin operations (users, sources, providers) |
| `enterprise` | Feature toggles, license validation |
| `tenant` | Multi-tenant organizations |
| `sso` | OIDC SSO providers |
| `audit` | Audit event logging |
| `reports` | Activity reports |
| `beta` | Auto-setup for demo/testing |

---

## Authentication & Users

**Module:** `auth`, `users`
**Controllers:** `AuthController`, `UserController`, `AccountController`

### Features
- **JWT-based auth** — login and registration return a JWT token; validated via
  `JwtAuthenticationFilter` on every request.
- **4 roles:**
  - `USER` — standard user, sees only their own data
  - `ADMIN` — manages users and platform config
  - `ORG_ADMIN` — admin within their organization (enterprise)
  - `SUPER_ADMIN` — full access across all orgs
- **Account self-service (GDPR):**
  - `POST /api/account/export` — download all user data as JSON
  - `DELETE /api/account` — delete account and all associated data
- **Session management** — Spring Session JDBC (HA-ready, works with multiple backend instances)

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Login, returns JWT |
| `POST` | `/api/auth/register` | Register new user |
| `GET` | `/api/me` | Current user info |
| `GET` | `/api/account/export` | Export all user data |
| `DELETE` | `/api/account` | Delete account |

---

## Profile & Preferences

**Module:** `profile`
**Controller:** `ProfileController`

### Features
- **User profile** — CV summary, skills, experience, education, languages
- **Preferences** — desired job title, location, salary range, contract type, remote preference
- **Hard filters** — strict exclusions applied as SQL predicates:
  - Location (country/region/city radius)
  - Salary minimum
  - Contract type (permanent, contract, freelance)
  - Excluded companies or keywords
- **Soft weights** — tunable preferences that flow into the feature score (see [Matching](#matching--ranking))
- **Country preferences** — select which countries to search in (maps to source filtering)

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/profile` | Get profile |
| `PUT` | `/api/profile` | Update profile |
| `GET` | `/api/preferences` | Get preferences |
| `PUT` | `/api/preferences` | Update preferences |

---

## Documents

**Module:** `docs`
**Controller:** `DocumentController`
**Services:** `DocumentService`, `TextExtractionService`, `CvParsingService`

### Features
- **Upload** CVs, certificates, references, cover letters, project lists
- **Text extraction** via Apache Tika (PDF, DOCX, ODT, TXT, …)
- **CV parsing** — LLM extracts structured data (skills, experience, education) into `cv_structured`
- **Versioning** — multiple versions of a document can be stored
- **Storage abstraction** — MinIO or local filesystem (see [Storage](#storage))

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/documents` | Upload document |
| `GET` | `/api/documents` | List documents |
| `GET` | `/api/documents/{id}` | Download document |
| `DELETE` | `/api/documents/{id}` | Delete document |

---

## AI / LLM Providers

**Module:** `ai`
**Controller:** `LlmConfigController`
**Services:** `LlmService`, `LlmConfigService`, `ProviderResolver`

### Supported Providers

| Provider | Type | Notes |
|----------|------|-------|
| **Ollama** | Self-hosted | Point `base_url` to your instance |
| **OpenAI / ChatGPT** | Cloud | OpenAI-compatible API |
| **NVIDIA NIM** | Cloud | OpenAI-compatible |
| **OpenRouter** | Cloud | OpenAI-compatible, access many models |
| **Anthropic / Claude** | Cloud | Native Anthropic API |
| **Google / Gemini** | Cloud | Native Google API |
| **OpenAI-compatible** | Any | Generic client for any OpenAI-compatible endpoint |

### How It Works
1. `ProviderResolver` selects the per-user config from `llm_provider_config` (by purpose:
   `CHAT` or `EMBEDDING`)
2. Resolves the API key — from Infisical (global) or AES-decrypted from DB (per-user)
3. Returns a `ResolvedModel` to `LlmService`
4. `LlmService` dispatches to the correct client (`OpenAiCompatibleClient`, `AnthropicClient`,
   `GoogleGeminiClient`, `OllamaClient`)

### Per-User Provider Config
Each user can configure their own providers (e.g., their own OpenAI key, or a shared Ollama
instance). Keys are AES-GCM-encrypted before storage.

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/llm/config` | List user's provider configs |
| `POST` | `/api/llm/config` | Add/update provider config |
| `DELETE` | `/api/llm/config/{id}` | Remove provider config |

See [`docs/provider-setup.md`](provider-setup.md) and [`docs/infisical-setup.md`](infisical-setup.md).

---

## Job Sources & Ingestion

**Module:** `jobs`
**Controller:** `JobSearchController`
**Services:** `JobIngestionService`, `JobEmbeddingService`, `JobSearchService`

### 33 Sources

See the [README source table](../README.md#job-sources) for the complete list with API key
requirements and country coverage.

### Source Types
- **Public APIs** (free) — Bundesagentur, Arbeitnow, RemoteOK, Remotive, ATS boards (Greenhouse,
  Lever, Recruitee, Ashby, SmartRecruiters, Workable), …
- **Keyed APIs** — Adzuna, CareerJet, Findwork, Jooble, Reed, TheMuse, USAJobs, ZipRecruiter
- **HTML scrapers** — EuropeRemotely, Honeypot, 4Scotty, IT-Talents, WeWorkRemotely, …
- **Generic scraper** — configure any URL with CSS selectors
- **LLM web search** — uses the configured LLM to search the web for jobs
- **Who is Hiring** — Hacker News monthly "Who is Hiring" thread parser

### Ingestion Pipeline
1. **Fetch** — each source fetches listings (API call or HTML scrape)
2. **Normalize** — map to common `Job` schema (title, company, location, description, URL, source)
3. **Dedupe** — fingerprint-based deduplication (title + company + location hash)
4. **Embed** — generate pgvector embeddings (`JobEmbeddingService`, configurable dimension)
5. **Store** — persist to `jobs` + `job_embedding` tables (HNSW cosine index)

### Search Runs
- `JobSearchService.runForUser(userId)` — triggers a search across all enabled sources
  for the user's preferred countries, stores a `SearchRun` record (status: running/completed/failed)
- Scheduled automatically; can also be triggered manually or via the Agent API

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/jobs/search` | Trigger a search run |
| `GET` | `/api/jobs/search/{id}` | Get search run status |
| `GET` | `/api/jobs` | List jobs (paginated) |

---

## Matching & Ranking

**Module:** `matching`
**Controller:** `MatchController`
**Services:** `MatchingService`, `FeatureScorer`, `JobValidationService`

### 4-Stage Pipeline

```
  All jobs (filtered by country)
         │
         ▼
  ┌──────────────┐
  │ 1. Hard Filter│  SQL: location, salary, contract type, exclusions
  └──────┬───────┘
         │  ~hundreds
         ▼
  ┌──────────────┐
  │ 2. Semantic   │  pgvector cosine similarity (profile embedding ↔ job embedding)
  │   (top 300)   │  HNSW index
  └──────┬───────┘
         │  300
         ▼
  ┌──────────────┐
  │ 3. Feature    │  8 weighted features → composite score
  │    Score      │  Weights from user_ranking_model
  └──────┬───────┘
         │  top 50
         ▼
  ┌──────────────┐
  │ 4. LLM Rerank │  Optional: LLM validates top-N with reasoning
  │   (optional)  │  Few-shot from user feedback
  └──────┬───────┘
         │
         ▼
    job_matches (stored, ranked)
```

### 8 Features

| Feature | Description |
|---------|-------------|
| `semantic` | pgvector cosine similarity (profile ↔ job) |
| `title` | Job title match with profile target title |
| `keyword` | Keyword overlap (skills, technologies) |
| `location` | Location fit (distance, remote preference) |
| `recency` | How recently the job was posted |
| `remote` | Remote-work fit |
| `skillOverlap` | Explicit skill overlap between profile and job |
| `aiRelevance` | LLM-generated relevance score (pre-computed for top candidates) |

Each feature produces a normalized `0..1` value. The `Weights` record holds per-user weights;
the composite score is `Σ(weight_i × feature_i)`.

### Match Status Lifecycle

| Status | Meaning |
|--------|---------|
| `NEW` | Freshly matched, user hasn't seen it |
| `SEEN` | User opened the detail view |
| `SAVED` | User bookmarked it |
| `DISMISSED` | User rejected it |
| `APPLIED` | User applied |
| `INTERVIEW` | Interview stage |
| `OFFER` | Received an offer |
| `REJECTED` | Application rejected |

User-decided statuses (`SAVED`, `DISMISSED`, `APPLIED`, …) are not overwritten by recomputation.

### Job Validation Service
- `JobValidationService` — optional LLM-based validation of the top 50 candidates
- Provides a relevance score + short reasoning, used as the `aiRelevance` feature
- Uses few-shot prompting from the user's past feedback

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/matches` | List matches (paginated, filterable by status) |
| `PATCH` | `/api/matches/{id}` | Update match status |
| `POST` | `/api/matches/{id}/feedback` | Submit feedback (drives self-learning) |

---

## Self-Learning

**Module:** `learning`
**Controller:** `LearningController`
**Service:** `SelfLearningService`, `WeightLearner`, `VectorMath`

### How It Works
1. **Feedback collection** — every status change (`SAVED`, `DISMISSED`, `APPLIED`, …) creates a
   `feedback_event` with the job features at decision time
2. **Profile drift (centroid shift)** — the profile embedding is gradually shifted toward the
   centroid of positively-rated jobs, so semantic search adapts to evolving preferences
3. **Weight retraining** — `WeightLearner` trains a lightweight, explainable model
   (logistic regression) per user from their feedback events
4. **Scheduler** — retraining runs periodically; can also be triggered manually

### Retraining Result
`SelfLearningService.retrain(userId)` returns a `RetrainResult` with:
- New weights (per feature)
- Number of feedback events used
- Training accuracy

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/learning/weights` | Current user weights |
| `POST` | `/api/learning/retrain` | Trigger retraining |

---

## Generators

**Module:** `generate`
**Controller:** `GenerationController`
**Service:** `GenerationService`, `InterviewPrepService`, `SalaryInsightsService`

### Document Types

| Type | Description |
|------|-------------|
| `TAILORED_CV` | CV rewritten/optimized for a specific job |
| `COVER_LETTER` | Cover letter tailored to the job |
| `MOTIVATION` | Motivation letter |
| `APPLICATION_TEXT` | Short application text (e.g., for email body or form field) |

### Additional Generators
- **Interview Prep** — generates likely interview questions + suggested answers based on the
  job description and the user's CV
- **Salary Insights** — estimates salary range for a job based on location, seniority, and market data

### How It Works
1. `GenerationService` builds a context from: user profile, CV, the matched job, and prior feedback
2. Sends a structured prompt to the user's configured LLM provider
3. Stores the result in `generated_documents` (versioned — every generation is kept)
4. Returns the generated content

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/generate` | Generate a document (type + match ID) |
| `GET` | `/api/generate/{id}` | Get a generated document |
| `GET` | `/api/generate/history` | List generated documents |
| `POST` | `/api/generate/interview-prep` | Interview prep |
| `POST` | `/api/generate/salary-insights` | Salary insights |

---

## Agent & MCP

**Module:** `agent`
**Controllers:** `AgentController`, `McpTools`

### Agent API (REST)
A JWT-authenticated REST API that lets external AI agents control the platform:

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/agent/search` | Trigger a job search |
| `GET` | `/api/agent/matches` | List matches |
| `POST` | `/api/generate` | Generate a document |

### MCP Server (Model Context Protocol)
The backend runs an MCP server (via Spring AI MCP SDK) that exposes tools callable by
external AI agents like Claude Desktop, Cursor, or Continue:

- `searchJobs` — trigger a search run and return top matches
- `listMatches` — list current matches (filterable by status)
- `generateDocument` — generate a CV, cover letter, motivation, or application text

The `McpTool` / `McpToolParam` annotations (`de.sellfish.agent.mcp`) mark tool methods.
These are stubs that will be replaced by Spring AI's native annotations once available in 1.0.4+.

---

## Admin

**Module:** `admin`
**Controller:** `AdminController`
**Service:** `AdminService`

### Features
- **User management** — list, create, update, delete users; assign roles
- **Source management** — enable/disable job sources, configure API keys per source
- **Provider management** — manage global LLM provider configs

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/admin/users` | List users |
| `POST` | `/api/admin/users` | Create user |
| `PUT` | `/api/admin/users/{id}` | Update user |
| `DELETE` | `/api/admin/users/{id}` | Delete user |
| `GET` | `/api/admin/sources` | List job sources |
| `PUT` | `/api/admin/sources/{id}` | Update source config |

---

## Enterprise Features

**Module:** `enterprise`
**Controller:** `LicenseController`
**Service:** `LicenseService`, `LicenseValidator`, `FeatureToggle`, `EnterpriseProperties`

### Feature Toggles
Enterprise features are gated behind toggles in `application.yml`:

```yaml
app:
  enterprise:
    enabled: ${ENTERPRISE_ENABLED:false}
    public-key: ${ENTERPRISE_PUBLIC_KEY:}
    features:
      sso: false
      multi-tenant: false
      reports: false
      audit-log: false
      ha: false
```

- When `enabled=true`, an RSA-signed license can additionally unlock features
- `LicenseValidator` verifies the license signature against the configured public key
- `FeatureToggle` checks both the config flags and the active license

### License Management

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/admin/license` | Upload a license |
| `GET` | `/api/admin/license` | Current license status |

---

## Multi-Tenant (Organizations)

**Module:** `tenant`
**Controller:** `OrgController`
**Service:** `OrgService`

### Features
- **Organizations** — group users into orgs; each org has `ORG_ADMIN` members
- **Org switching** — users can belong to multiple orgs and switch active context
- **Org-scoped data** — matches, jobs, and reports can be scoped to the active org
- **Member roles** — `ORG_ADMIN`, `USER` within an org

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/orgs` | Create organization |
| `GET` | `/api/orgs` | List user's orgs |
| `POST` | `/api/orgs/{id}/members` | Add member |
| `POST` | `/api/orgs/switch` | Switch active org |

---

## SSO / OIDC

**Module:** `sso`
**Controller:** `SsoController`
**Service:** `OidcService`

### Features
- **OIDC-based SSO** — connect external identity providers (Authentik, Keycloak, Google, …)
- **Multiple providers** — configured as a YAML list in `application.yml`
- **Login flow** — `GET /api/auth/sso/{provider}/login` redirects to the provider; callback
  completes login and returns a JWT

### Configuration
```yaml
app:
  sso:
    enabled: ${SSO_ENABLED:false}
    redirect-uri: ${SSO_REDIRECT_URI:http://localhost:5173/auth/callback}
    providers:
      - id: authentik
        label: Authentik
        issuer-uri: https://auth.example.com/application/o/sellfish/
        client-id: ...
        client-secret: ...
```

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/auth/sso/providers` | List configured SSO providers |
| `GET` | `/api/auth/sso/{provider}/login` | Start SSO login |
| `POST` | `/api/auth/sso/callback` | Complete SSO login |

---

## Audit Log

**Module:** `audit`
**Controller:** `AuditController`
**Service:** `AuditService`

### Features
- **Event logging** — records significant actions (login, data export, admin operations, …)
- **Queryable** — admin can filter by user, action type, date range
- **Actions** (`AuditAction` enum): `LOGIN`, `DATA_EXPORT`, `ACCOUNT_DELETE`, `ADMIN_USER_CREATE`, …

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/audit` | Query audit events (admin) |

---

## Reports

**Module:** `reports`
**Controller:** `ReportController`
**Service:** `ReportService`

### Features
- **Activity summary** — application stats per user or org
- **Daily buckets** — search runs, matches, applications over time
- **Member stats** — per-user productivity within an org

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/reports/summary` | Overall summary |
| `GET` | `/api/reports/daily` | Daily breakdown |

---

## Storage

**Module:** `storage`
**Port:** `StorageService`
**Adapters:** `MinioStorageService`, `FileSystemStorageService`

### Features
- **Abstraction** — `StorageService` port with two adapters
- **MinIO** — S3-compatible object storage (production recommended)
- **Filesystem** — local directory (development)
- **Switch via env** — `STORAGE_BACKEND=minio|filesystem`

### Stored Artifacts
- Uploaded documents (CVs, certificates)
- Generated documents (cover letters, tailored CVs)

---

## Security & Crypto

**Module:** `common.crypto`, `common.security`
**Service:** `CryptoService`

### Per-User Encryption
- Each user gets an AES-GCM key, derived from the master key and encrypted at rest
- `CRYPTO_MASTER_KEY` (Base64, 32 bytes) is the root secret
- Used to encrypt: LLM API keys, document contents (when stored in DB)

### JWT
- HS256-signed JWTs
- `JWT_SECRET` (Base64, 32 bytes) is the signing key
- `JwtAuthenticationFilter` validates on every request
- 401 (not 403) for unauthenticated access

### Infisical Integration
- Global secrets (e.g., shared LLM keys) loaded from Infisical via machine identity
- `INFISICAL_ENABLED=true` + client credentials in env
- See [`docs/infisical-setup.md`](infisical-setup.md)

### Current User
- `CurrentUser` utility (`common.security`) — extracts the authenticated user ID from the
  security context, used across all services

---

## Beta Auto-Setup

**Module:** `beta`
**Controller:** `AutoSetupController`
**Service:** `AutoSetupService`

### Features
- **One-click demo setup** — creates a user, fills a sample profile, triggers a search run
- **Testing / evaluation** — useful for trying the platform without manual data entry
- **Disabled in production** — only available when `ENTERPRISE_ENABLED=false` or via a beta flag

---

## Database Schema

Managed by Flyway migrations (`backend/src/main/resources/db/migration/`).

### Core Tables (V1)

| Table | Description |
|-------|-------------|
| `users` | User accounts (email, password hash, role) |
| `user_profile` | Profile data (CV summary, skills, experience) |
| `user_preferences` | Job preferences and hard filters |
| `documents` | Uploaded documents (metadata + storage ref) |
| `cv_structured` | LLM-parsed structured CV data |
| `projects` | Project entries from CV parsing |
| `profile_embedding` | User profile pgvector embedding |
| `job_sources` | Configured job sources (code, config JSON, enabled) |
| `jobs` | Ingested job listings |
| `job_embedding` | Job pgvector embeddings (HNSW cosine index) |
| `job_matches` | Match results (job ↔ user, score, status) |
| `feedback_events` | User feedback (status changes with features snapshot) |
| `generated_documents` | LLM-generated documents (versioned) |
| `llm_provider_config` | Per-user LLM provider configurations |
| `search_runs` | Search run records (status, timestamps) |
| `user_ranking_model` | Learned per-user feature weights |

### Later Migrations

| Migration | Description |
|-----------|-------------|
| `V2`, `V3` | Additional job sources |
| `V4` | Enterprise (license table) |
| `V5` | Multi-tenant (organizations, members) |
| `V6` | SSO (provider configs) |
| `V7` | Audit log table |
| `V8`, `V9` | More job sources (remote IT boards) |
| `V10` | Country preferences |
| `V11` | Spring Session (JDBC, HA) |

### Embeddings
- Stored as `vector(N)` where N = `EMBEDDING_DIM` (default 768)
- HNSW cosine index on `job_embedding` for fast similarity search
- Dimension is fixed at first DB start (changing requires a fresh DB)

---

## Frontend

**Tech:** SvelteKit 5, TypeScript, Node adapter

### Routes

| Route | Description |
|-------|-------------|
| `/` | Main dashboard (jobs + matches) |
| `/settings` | Profile, preferences, provider config, model selection |
| `/auth/callback` | SSO callback handler |
| `/reports` | Activity reports (enterprise) |

### Key Components (`frontend/src/lib/autoapply/`)

| Component | Description |
|-----------|-------------|
| `JobsView` | Job listings with filters |
| `JobDrawer` | Slide-out job detail panel |
| `JobRow` | Single job row in the list |
| `MatchScore` | Visual match score indicator |
| `FilterChips` | Active filter chips |
| `FilterSummary` | Summary of applied filters |
| `ProfileView` | Profile editor |
| `ApplicationsView` | Application tracking |
| `ApplyModal` | Application submission modal |
| `UsersView` | Admin user management |
| `OrgSwitcher` | Organization switcher (enterprise) |
| `CommandPalette` | Keyboard-driven command palette |
| `Login` | Login/registration form |
| `Toasts` | Notification toasts |
| `Btn`, `Icon`, `Avatar`, `Stars`, `Kbd`, `StageBadge`, `CompanyMark`, `CursorGlow` | UI primitives |

### Design System
Custom dark theme with neon accents and glassmorphism — defined in `styles.css`.

---

*This wiki is maintained alongside the codebase. If you find discrepancies, please open an issue.*
