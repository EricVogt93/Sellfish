-- Multi-Tenant: Organisationen & Team-Features

CREATE TABLE organization (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    plan       VARCHAR(20)  NOT NULL DEFAULT 'CORE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE org_member (
    id         UUID PRIMARY KEY,
    org_id     UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, user_id)
);
CREATE INDEX idx_org_member_user ON org_member(user_id);
CREATE INDEX idx_org_member_org  ON org_member(org_id);

-- Optional: org-scoped Daten für jede betroffene Tabelle.
-- org_id bleibt NULLable — so sind bestehende private Datensaetze
-- weiterhin als user-private erkennbar und nicht zwingend einer Org zugeordnet.

ALTER TABLE users
    ADD COLUMN current_org_id UUID REFERENCES organization(id) ON DELETE SET NULL;

ALTER TABLE user_profile
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_user_profile_org ON user_profile(org_id);

ALTER TABLE user_preferences
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_user_preferences_org ON user_preferences(org_id);

ALTER TABLE documents
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_documents_org ON documents(org_id);

ALTER TABLE cv_structured
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_cv_structured_org ON cv_structured(org_id);

ALTER TABLE jobs
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE SET NULL;
CREATE INDEX idx_jobs_org ON jobs(org_id);

ALTER TABLE job_matches
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_job_matches_org ON job_matches(org_id);

ALTER TABLE feedback_events
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_feedback_events_org ON feedback_events(org_id);

ALTER TABLE generated_documents
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_generated_docs_org ON generated_documents(org_id);

ALTER TABLE search_runs
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE SET NULL;
CREATE INDEX idx_search_runs_org ON search_runs(org_id);

ALTER TABLE user_ranking_model
    ADD COLUMN org_id UUID REFERENCES organization(id) ON DELETE CASCADE;
CREATE INDEX idx_user_ranking_model_org ON user_ranking_model(org_id);
