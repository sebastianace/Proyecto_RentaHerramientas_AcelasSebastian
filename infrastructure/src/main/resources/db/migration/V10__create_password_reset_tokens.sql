-- =====================================================================
-- V10: tokens para recuperar la contraseña olvidada.
-- Se guarda solo el HASH SHA-256 del token (64 caracteres hex), nunca el token en sí.
-- Cada token vence (expira_en) y se puede usar una sola vez (usado).
-- =====================================================================

CREATE TABLE tokens_recuperacion (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT   NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    expira_en   DATETIME NOT NULL,
    usado       BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tokens_recuperacion PRIMARY KEY (id),
    CONSTRAINT uq_tokens_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_tokens_recuperacion_usuario ON tokens_recuperacion (usuario_id, usado);
