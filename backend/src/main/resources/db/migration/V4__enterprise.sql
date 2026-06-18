-- Lizenz- und Enterprise-Feature-Toggle-Tabelle

CREATE TABLE license (
    id             UUID PRIMARY KEY,
    license_key    TEXT NOT NULL,
    issued_to      VARCHAR(255),
    valid_until    TIMESTAMPTZ,
    features       JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
