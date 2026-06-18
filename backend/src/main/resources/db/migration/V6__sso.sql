-- SSO/OIDC: OIDC-Subject & Provider auf User

ALTER TABLE users
    ADD COLUMN oidc_subject  VARCHAR(255),
    ADD COLUMN oidc_provider VARCHAR(50);

CREATE UNIQUE INDEX idx_users_oidc ON users(oidc_subject, oidc_provider)
    WHERE oidc_subject IS NOT NULL;
