-- Bewerbungsatze – initiales Schema
-- Erfordert die pgvector-Extension (Image: pgvector/pgvector).

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- Nutzer & Auth
-- =====================================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    locale        VARCHAR(10)  NOT NULL DEFAULT 'de-DE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- =====================================================================
-- Profil, Wünsche & Filter
-- =====================================================================
CREATE TABLE user_profile (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    headline               VARCHAR(255),
    summary                TEXT,
    location               VARCHAR(255),
    willingness_to_relocate BOOLEAN NOT NULL DEFAULT false,
    salary_min            INTEGER,
    remote_pref           VARCHAR(20) NOT NULL DEFAULT 'ANY', -- ONSITE | HYBRID | REMOTE | ANY
    availability          VARCHAR(100),
    meta                  JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_preferences (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    desired_titles     TEXT[] NOT NULL DEFAULT '{}',
    industries         TEXT[] NOT NULL DEFAULT '{}',
    company_size       VARCHAR(50),
    contract_types     TEXT[] NOT NULL DEFAULT '{}',
    excluded_companies TEXT[] NOT NULL DEFAULT '{}',
    keywords           TEXT[] NOT NULL DEFAULT '{}',
    hard_filters       JSONB NOT NULL DEFAULT '{}'::jsonb,
    soft_weights       JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================================
-- Dokumente & strukturierter Lebenslauf
-- =====================================================================
CREATE TABLE documents (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(30) NOT NULL, -- CV | CERTIFICATE | REFERENCE | COVER_LETTER | PROJECT_LIST | OTHER
    storage_key   VARCHAR(512) NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    mime          VARCHAR(127),
    size_bytes    BIGINT,
    parsed_text   TEXT,
    parsed_struct JSONB,
    is_primary    BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_user ON documents(user_id);
CREATE INDEX idx_documents_user_type ON documents(user_id, type);

CREATE TABLE cv_structured (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id    UUID REFERENCES documents(id) ON DELETE SET NULL,
    experience     JSONB NOT NULL DEFAULT '[]'::jsonb,
    education      JSONB NOT NULL DEFAULT '[]'::jsonb,
    skills         JSONB NOT NULL DEFAULT '[]'::jsonb,
    languages      JSONB NOT NULL DEFAULT '[]'::jsonb,
    certifications JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cv_structured_user ON cv_structured(user_id);

CREATE TABLE projects (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    role        VARCHAR(255),
    period      VARCHAR(100),
    tech        TEXT[] NOT NULL DEFAULT '{}',
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_projects_user ON projects(user_id);

-- =====================================================================
-- Embeddings (pgvector). Dimension 768 als Default (Ollama nomic-embed-text);
-- bei abweichender Dimension via Folge-Migration anpassen.
-- =====================================================================
CREATE TABLE profile_embedding (
    user_id      UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    embedding    vector(768),
    model        VARCHAR(100),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================================
-- Job-Quellen & Stellen
-- =====================================================================
CREATE TABLE job_sources (
    id         UUID PRIMARY KEY,
    code       VARCHAR(30) NOT NULL UNIQUE, -- BA | ADZUNA | SCRAPER | LLM_WEB
    name       VARCHAR(100) NOT NULL,
    config     JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled    BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id           UUID PRIMARY KEY,
    source_code  VARCHAR(30) NOT NULL,
    external_ref VARCHAR(255),
    fingerprint  VARCHAR(64) NOT NULL UNIQUE,
    title        VARCHAR(500) NOT NULL,
    company      VARCHAR(500),
    location     VARCHAR(500),
    remote       VARCHAR(20),
    description  TEXT,
    url          VARCHAR(1000),
    salary_raw   VARCHAR(255),
    posted_at    TIMESTAMPTZ,
    raw          JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_jobs_source ON jobs(source_code);
CREATE INDEX idx_jobs_posted ON jobs(posted_at DESC);

CREATE TABLE job_embedding (
    job_id       UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
    embedding    vector(768),
    model        VARCHAR(100),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- ANN-Index für Ähnlichkeitssuche (Cosine).
CREATE INDEX idx_job_embedding_vec ON job_embedding USING hnsw (embedding vector_cosine_ops);

-- =====================================================================
-- Matches & Feedback
-- =====================================================================
CREATE TABLE job_matches (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    score           DOUBLE PRECISION NOT NULL DEFAULT 0,
    score_breakdown JSONB NOT NULL DEFAULT '{}'::jsonb,
    rank            INTEGER,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    -- NEW | SEEN | SAVED | DISMISSED | APPLIED | INTERVIEW | OFFER | REJECTED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_id)
);
CREATE INDEX idx_job_matches_user_score ON job_matches(user_id, score DESC);
CREATE INDEX idx_job_matches_user_status ON job_matches(user_id, status);

CREATE TABLE feedback_events (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id     UUID REFERENCES jobs(id) ON DELETE SET NULL,
    type       VARCHAR(30) NOT NULL, -- CLICK | SAVE | DISMISS | APPLY | OUTCOME ...
    payload    JSONB NOT NULL DEFAULT '{}'::jsonb,
    ts         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_feedback_user ON feedback_events(user_id, ts DESC);

-- =====================================================================
-- Generierte Dokumente
-- =====================================================================
CREATE TABLE generated_documents (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_match_id   UUID REFERENCES job_matches(id) ON DELETE SET NULL,
    type           VARCHAR(30) NOT NULL, -- TAILORED_CV | COVER_LETTER | MOTIVATION | APPLICATION_TEXT
    content        TEXT,
    model          VARCHAR(100),
    prompt_version VARCHAR(50),
    storage_key    VARCHAR(512),
    version        INTEGER NOT NULL DEFAULT 1,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_generated_docs_user ON generated_documents(user_id);

-- =====================================================================
-- LLM-Provider-Konfiguration (global = user_id NULL, sonst pro Nutzer)
-- =====================================================================
CREATE TABLE llm_provider_config (
    id           UUID PRIMARY KEY,
    user_id      UUID REFERENCES users(id) ON DELETE CASCADE,
    provider     VARCHAR(50) NOT NULL,  -- OLLAMA | OPENAI | ANTHROPIC | GOOGLE | NIM | OPENAI_COMPATIBLE
    model        VARCHAR(150) NOT NULL,
    base_url     VARCHAR(500),
    key_ref      VARCHAR(500),          -- Infisical-Pfad ODER verschlüsselter Key
    key_enc      TEXT,                  -- AES-GCM verschlüsselter Per-User-Key (optional)
    params       JSONB NOT NULL DEFAULT '{}'::jsonb,
    purpose      VARCHAR(20) NOT NULL DEFAULT 'CHAT', -- CHAT | EMBEDDING
    is_default   BOOLEAN NOT NULL DEFAULT false,
    enabled      BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_llm_cfg_user_purpose ON llm_provider_config(user_id, purpose);

-- =====================================================================
-- Suchläufe (Audit) & gelernte Ranking-Modelle
-- =====================================================================
CREATE TABLE search_runs (
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    sources     TEXT[] NOT NULL DEFAULT '{}',
    status      VARCHAR(20) NOT NULL DEFAULT 'RUNNING', -- RUNNING | DONE | FAILED
    stats       JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_search_runs_user ON search_runs(user_id, started_at DESC);

CREATE TABLE user_ranking_model (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weights    JSONB NOT NULL DEFAULT '{}'::jsonb,
    version    INTEGER NOT NULL DEFAULT 1,
    metrics    JSONB NOT NULL DEFAULT '{}'::jsonb,
    trained_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, version)
);
CREATE INDEX idx_ranking_model_user ON user_ranking_model(user_id, version DESC);

-- =====================================================================
-- Default Job-Quellen
-- =====================================================================
INSERT INTO job_sources (id, code, name, enabled) VALUES
    (gen_random_uuid(), 'BA',      'Bundesagentur für Arbeit', true),
    (gen_random_uuid(), 'ADZUNA',  'Adzuna',                   false),
    (gen_random_uuid(), 'SCRAPER', 'Generic Scraper',          false),
    (gen_random_uuid(), 'LLM_WEB', 'LLM Web Search',           false);
