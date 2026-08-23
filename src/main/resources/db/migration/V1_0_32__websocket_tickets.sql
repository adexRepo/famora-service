CREATE TABLE famora.websocket_tickets
(
    id          uuid PRIMARY KEY,
    ticket_hash varchar(64)              NOT NULL,
    user_id     uuid                     NOT NULL REFERENCES famora.users (id),
    expires_at  timestamp with time zone NOT NULL,
    consumed_at timestamp with time zone,
    created_at  timestamp with time zone NOT NULL,
    updated_at  timestamp with time zone NOT NULL,
    CONSTRAINT uk_websocket_tickets_hash UNIQUE (ticket_hash)
);

CREATE INDEX idx_websocket_tickets_cleanup
    ON famora.websocket_tickets (expires_at, consumed_at);
