-- Situação de cada serviço na última conferência.
CREATE TABLE target_states (
    name                 VARCHAR(60)  PRIMARY KEY,
    up                   BOOLEAN      NOT NULL DEFAULT TRUE,
    consecutive_failures INTEGER      NOT NULL DEFAULT 0,
    last_checked_at      TIMESTAMP WITH TIME ZONE,
    down_since           TIMESTAMP WITH TIME ZONE,
    last_error           VARCHAR(200),
    alerted_at           TIMESTAMP WITH TIME ZONE
);

-- Cópia da última lista de admins lida do admin-auth-service.
CREATE TABLE known_admins (
    email VARCHAR(254) PRIMARY KEY
);
