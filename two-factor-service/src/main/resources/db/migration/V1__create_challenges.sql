-- Um desafio = um código de uso único enviado para um e-mail. O código em si nunca é gravado,
-- só o HMAC dele (code_hash). Linhas vencidas há mais de um dia são apagadas pelo serviço.
CREATE TABLE challenges (
    id          UUID PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    purpose     VARCHAR(40)  NOT NULL,
    code_hash   VARCHAR(64)  NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_challenges_email_created_at ON challenges (email, created_at);
