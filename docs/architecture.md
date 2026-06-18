# Architecture

Bewerbungsatze is a **modular monolith** (a single deployable Spring Boot backend) plus a
SvelteKit frontend and PostgreSQL. Background work (job search, embeddings, self-learning
retraining) runs asynchronously via schedulers.

## Backend Modules (`de.bewerbungsatze.*`)

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

## Data Model

Fully defined in `backend/src/main/resources/db/migration/V1__init.sql`. Core tables: `users`,
`user_profile`, `user_preferences`, `documents`, `cv_structured`, `projects`, `profile_embedding`,
`job_sources`, `jobs`, `job_embedding`, `job_matches`, `feedback_events`, `generated_documents`,
`llm_provider_config`, `search_runs`, `user_ranking_model`.

Embeddings are stored as `vector(768)` (pgvector); the job embeddings have an HNSW cosine index
for fast similarity search.

## LLM Provider Abstraction

A custom, dependency-light layer over Spring `RestClient` (no Spring AI dependency for the
client logic). A `ProviderResolver` selects the per-user configuration from `llm_provider_config`
(by purpose: `CHAT` or `EMBEDDING`), resolves the API key (from Infisical or AES-decrypted from
DB), and returns a `ResolvedModel`. The `LlmService` dispatches to the appropriate client
(`ChatProvider` / `EmbeddingProvider`):

- **Self-hosted:** `OllamaClient` (`base_url` points to your instance)
- **OpenAI/ChatGPT, NVIDIA NIM, OpenRouter, …:** `OpenAiCompatibleClient` with configurable base URL
- **Anthropic/Claude:** `AnthropicClient`
- **Google/Gemini:** `GoogleGeminiClient`

Each client is unit-tested in isolation against `MockRestServiceServer`.

Global keys come from **Infisical** (machine identity); per-user keys are AES-GCM-encrypted and
stored in the DB. See [`provider-setup.md`](provider-setup.md) and [`infisical-setup.md`](infisical-setup.md).

## Matching & Self-Learning

1. **Hard filter** (SQL): location / salary / contract type / exclusions.
2. **Semantic** (pgvector): cosine similarity profile ↔ job.
3. **Feature score**: 8 weighted features; weights from `user_ranking_model`.
4. **LLM rerank** (optional): top-N with reasoning, few-shot from user feedback.

The self-learning process collects `feedback_events`, shifts the profile embedding toward
positively-rated jobs (centroid shift), and periodically retrains a lightweight, explainable
model (logistic regression) per user.

## Security

- Stateless JWT (access/refresh), BCrypt password hashing.
- Strict user-scoping of all data.
- Secrets never in Git: global keys via Infisical, per-user keys AES-GCM-encrypted in DB.
- GDPR: per-user export/deletion; audit trail via `search_runs` and `feedback_events`.

For a full feature reference, see [`wiki.md`](wiki.md).
