-- Spring Session JDBC Tabellen (HA/Clustering)

CREATE TABLE IF NOT EXISTS spring_session (
    primary_id            CHAR(36) NOT NULL PRIMARY KEY,
    session_id            CHAR(36) NOT NULL,
    creation_time         BIGINT NOT NULL,
    last_access_time      BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time           BIGINT NOT NULL,
    principal_name        VARCHAR(300)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_spring_session_id ON spring_session(session_id);
CREATE INDEX IF NOT EXISTS idx_spring_session_expiry ON spring_session(expiry_time);

CREATE TABLE IF NOT EXISTS spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);
