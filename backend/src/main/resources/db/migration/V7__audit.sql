-- Audit-Log: Ereignisse für alle relevanten Aktionen

CREATE TABLE audit_events (
    id          UUID PRIMARY KEY,
    org_id      UUID REFERENCES organization(id) ON DELETE SET NULL,
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id   VARCHAR(255),
    details     JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip          VARCHAR(45),
    ts          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_user  ON audit_events(user_id, ts DESC);
CREATE INDEX idx_audit_org   ON audit_events(org_id, ts DESC);
CREATE INDEX idx_audit_action ON audit_events(action, ts DESC);
