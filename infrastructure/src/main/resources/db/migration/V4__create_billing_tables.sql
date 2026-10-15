-- =====================================================================
-- V4: Contexto de FACTURACIÓN
-- Pagos en línea (pasarela simulada) y facturas.
-- Una reserva puede tener varios intentos de pago (ej. uno RECHAZADO y luego
-- uno APROBADO), pero solo un pago APROBADO genera factura (1:1).
-- =====================================================================

CREATE TABLE pagos (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    reserva_id             BIGINT        NOT NULL,
    monto                  DECIMAL(12,2) NOT NULL,
    metodo                 VARCHAR(20)   NOT NULL,
    estado                 VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    referencia_transaccion VARCHAR(60)   NOT NULL COMMENT 'Identificador devuelto por la pasarela de pago',
    fecha_pago             DATETIME      NOT NULL,
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pagos PRIMARY KEY (id),
    CONSTRAINT uq_pagos_referencia UNIQUE (referencia_transaccion),
    CONSTRAINT fk_pagos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id),
    CONSTRAINT ck_pagos_monto CHECK (monto > 0),
    CONSTRAINT ck_pagos_metodo CHECK (metodo IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'PSE', 'NEQUI')),
    CONSTRAINT ck_pagos_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO'))
) ENGINE = InnoDB;

CREATE TABLE facturas (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    numero        VARCHAR(20)   NOT NULL COMMENT 'Consecutivo, ej. FAC-2026-000001',
    pago_id       BIGINT        NOT NULL,
    fecha_emision DATETIME      NOT NULL,
    subtotal      DECIMAL(12,2) NOT NULL,
    iva           DECIMAL(12,2) NOT NULL,
    total         DECIMAL(12,2) NOT NULL,
    estado        VARCHAR(20)   NOT NULL DEFAULT 'EMITIDA',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_facturas PRIMARY KEY (id),
    CONSTRAINT uq_facturas_numero UNIQUE (numero),
    CONSTRAINT uq_facturas_pago UNIQUE (pago_id),
    CONSTRAINT fk_facturas_pago FOREIGN KEY (pago_id) REFERENCES pagos (id),
    CONSTRAINT ck_facturas_montos CHECK (total = subtotal + iva),
    CONSTRAINT ck_facturas_estado CHECK (estado IN ('EMITIDA', 'ANULADA'))
) ENGINE = InnoDB;
