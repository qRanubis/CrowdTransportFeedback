CREATE TABLE app_user (
 id UUID PRIMARY KEY,
 email VARCHAR(320) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(16) NOT NULL CHECK (role IN ('USER','ADMIN')),
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_user_email UNIQUE (email),
 CONSTRAINT ck_email_lower CHECK (email = lower(email))
);

CREATE TABLE refresh_session (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES app_user(id),
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 last_used_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 replaced_by UUID REFERENCES refresh_session(id)
);
CREATE INDEX idx_refresh_user ON refresh_session(user_id);

CREATE TABLE feedback (
 feedback_id UUID PRIMARY KEY,
 created_by_user_id UUID NOT NULL REFERENCES app_user(id),
 transport_type VARCHAR(24) NOT NULL CHECK (transport_type IN ('BUS','NIGHT_BUS','TRAM','TROLLEYBUS','METRO')),
 line VARCHAR(32) NOT NULL CHECK (btrim(line) <> ''),
 score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
 punctuality_score SMALLINT NOT NULL CHECK (punctuality_score BETWEEN 1 AND 5),
 cleanliness_score SMALLINT NOT NULL CHECK (cleanliness_score BETWEEN 1 AND 5),
 crowding_score SMALLINT NOT NULL CHECK (crowding_score BETWEEN 1 AND 5),
 comment VARCHAR(2000),
 latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
 longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
 created_at BIGINT NOT NULL CHECK (created_at >= 0)
);
CREATE INDEX idx_feedback_created ON feedback(created_at DESC);
