-- =====================================================================
-- V3: Contexto de ALQUILERES
-- Reservas y su ciclo de vida: detalle, entrega, devolución y daños.
--
-- Ciclo de una reserva:
--   PENDIENTE -> APROBADA -> PAGADA -> ENTREGADA -> DEVUELTA
--   PENDIENTE -> RECHAZADA | CANCELADA
--   APROBADA  -> VENCIDA (no se pagó antes de fecha_limite_pago) | CANCELADA
--
-- Días cobrados = DATEDIFF(fecha_fin, fecha_inicio) + 1 (ambos días incluidos)
-- =====================================================================

CREATE TABLE reservas (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    codigo            VARCHAR(20)   NOT NULL COMMENT 'Código legible para el cliente, ej. RES-2026-0001',
    cliente_id        BIGINT        NOT NULL,
    proveedor_id      BIGINT        NOT NULL COMMENT 'Una reserva es de un solo proveedor: es quien la aprueba o rechaza',
    fecha_inicio      DATE          NOT NULL,
    fecha_fin         DATE          NOT NULL,
    estado            VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    subtotal          DECIMAL(12,2) NOT NULL,
    iva               DECIMAL(12,2) NOT NULL,
    total             DECIMAL(12,2) NOT NULL,
    fecha_limite_pago DATETIME      NULL COMMENT 'Se asigna al aprobar; si vence sin pago la reserva pasa a VENCIDA',
    motivo_rechazo    VARCHAR(300)  NULL,
    notas             VARCHAR(500)  NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_reservas PRIMARY KEY (id),
    CONSTRAINT uq_reservas_codigo UNIQUE (codigo),
    CONSTRAINT fk_reservas_cliente   FOREIGN KEY (cliente_id)   REFERENCES clientes (id),
    CONSTRAINT fk_reservas_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT ck_reservas_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_reservas_montos CHECK (subtotal >= 0 AND iva >= 0 AND total = subtotal + iva),
    CONSTRAINT ck_reservas_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'PAGADA',
                                                   'ENTREGADA', 'DEVUELTA', 'VENCIDA', 'CANCELADA'))
) ENGINE = InnoDB;

CREATE TABLE reserva_detalles (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    reserva_id     BIGINT        NOT NULL,
    herramienta_id BIGINT        NOT NULL,
    cantidad       INT           NOT NULL,
    precio_por_dia DECIMAL(12,2) NOT NULL COMMENT 'Precio congelado al reservar: si el proveedor cambia el precio, esta reserva no cambia',
    dias           INT           NOT NULL,
    subtotal       DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_reserva_detalles PRIMARY KEY (id),
    CONSTRAINT uq_reserva_detalles_herramienta UNIQUE (reserva_id, herramienta_id),
    CONSTRAINT fk_reserva_detalles_reserva     FOREIGN KEY (reserva_id)     REFERENCES reservas (id) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_detalles_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas (id),
    CONSTRAINT ck_reserva_detalles_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_detalles_dias CHECK (dias > 0),
    CONSTRAINT ck_reserva_detalles_subtotal CHECK (subtotal = cantidad * precio_por_dia * dias)
) ENGINE = InnoDB;

CREATE TABLE entregas (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    reserva_id        BIGINT       NOT NULL,
    tipo              VARCHAR(20)  NOT NULL,
    direccion_entrega VARCHAR(200) NULL COMMENT 'Obligatoria solo para entregas a domicilio',
    fecha_entrega     DATETIME     NOT NULL,
    registrada_por    BIGINT       NOT NULL COMMENT 'Usuario (proveedor o admin) que registró la entrega',
    observaciones     VARCHAR(500) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_entregas PRIMARY KEY (id),
    CONSTRAINT uq_entregas_reserva UNIQUE (reserva_id),
    CONSTRAINT fk_entregas_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id),
    CONSTRAINT fk_entregas_usuario FOREIGN KEY (registrada_por) REFERENCES usuarios (id),
    CONSTRAINT ck_entregas_tipo CHECK (tipo IN ('DOMICILIO', 'RECOGIDA_EN_SEDE')),
    CONSTRAINT ck_entregas_direccion CHECK (tipo = 'RECOGIDA_EN_SEDE' OR direccion_entrega IS NOT NULL)
) ENGINE = InnoDB;

CREATE TABLE devoluciones (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reserva_id       BIGINT       NOT NULL,
    fecha_devolucion DATETIME     NOT NULL,
    estado_equipo    VARCHAR(20)  NOT NULL,
    observaciones    VARCHAR(500) NULL,
    recibida_por     BIGINT       NOT NULL COMMENT 'Usuario proveedor que confirmó la devolución',
    validada         BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'El administrador revisa y valida la devolución',
    validada_por     BIGINT       NULL,
    fecha_validacion DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_devoluciones PRIMARY KEY (id),
    CONSTRAINT uq_devoluciones_reserva UNIQUE (reserva_id),
    CONSTRAINT fk_devoluciones_reserva   FOREIGN KEY (reserva_id)   REFERENCES reservas (id),
    CONSTRAINT fk_devoluciones_recibida  FOREIGN KEY (recibida_por) REFERENCES usuarios (id),
    CONSTRAINT fk_devoluciones_validada  FOREIGN KEY (validada_por) REFERENCES usuarios (id),
    CONSTRAINT ck_devoluciones_estado CHECK (estado_equipo IN ('BUENO', 'DESGASTE_NORMAL', 'CON_DANOS')),
    CONSTRAINT ck_devoluciones_validacion CHECK (validada = FALSE OR (validada_por IS NOT NULL AND fecha_validacion IS NOT NULL))
) ENGINE = InnoDB;

CREATE TABLE reportes_danos (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    devolucion_id  BIGINT        NOT NULL,
    herramienta_id BIGINT        NOT NULL,
    descripcion    VARCHAR(500)  NOT NULL,
    severidad      VARCHAR(20)   NOT NULL,
    costo_estimado DECIMAL(12,2) NOT NULL DEFAULT 0,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'ABIERTO',
    reportado_por  BIGINT        NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_reportes_danos PRIMARY KEY (id),
    CONSTRAINT fk_reportes_danos_devolucion  FOREIGN KEY (devolucion_id)  REFERENCES devoluciones (id),
    CONSTRAINT fk_reportes_danos_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas (id),
    CONSTRAINT fk_reportes_danos_usuario     FOREIGN KEY (reportado_por)  REFERENCES usuarios (id),
    CONSTRAINT ck_reportes_danos_severidad CHECK (severidad IN ('LEVE', 'MODERADO', 'GRAVE')),
    CONSTRAINT ck_reportes_danos_costo CHECK (costo_estimado >= 0),
    CONSTRAINT ck_reportes_danos_estado CHECK (estado IN ('ABIERTO', 'EN_REVISION', 'RESUELTO'))
) ENGINE = InnoDB;
