CREATE TABLE famora.abuse_rate_limits (
    action varchar(40) NOT NULL,
    key_hash char(64) NOT NULL,
    window_started_at timestamptz NOT NULL,
    attempts integer NOT NULL,
    expires_at timestamptz NOT NULL,
    CONSTRAINT pk_abuse_rate_limits PRIMARY KEY (action, key_hash),
    CONSTRAINT chk_abuse_rate_limits_attempts_positive CHECK (attempts > 0)
);

CREATE INDEX idx_abuse_rate_limits_expires_at
    ON famora.abuse_rate_limits (expires_at);
