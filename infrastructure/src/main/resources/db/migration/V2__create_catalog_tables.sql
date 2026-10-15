-- =====================================================================
-- V2: Contexto de CATÁLOGO
-- Categorías, herramientas del inventario de cada proveedor e imágenes.
-- =====================================================================

CREATE TABLE categorias (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(80)  NOT NULL,
    descripcion VARCHAR(300) NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE herramientas (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    proveedor_id            BIGINT        NOT NULL,
    categoria_id            BIGINT        NOT NULL,
    nombre                  VARCHAR(120)  NOT NULL,
    descripcion             TEXT          NULL,
    marca                   VARCHAR(60)   NULL,
    modelo                  VARCHAR(60)   NULL,
    precio_por_dia          DECIMAL(12,2) NOT NULL COMMENT 'En pesos colombianos (COP). DECIMAL, nunca DOUBLE: el dinero no admite errores de redondeo',
    stock_total             INT           NOT NULL COMMENT 'Unidades físicas que tiene el proveedor',
    unidades_fuera_servicio INT           NOT NULL DEFAULT 0 COMMENT 'Unidades dañadas o en mantenimiento; no se pueden alquilar',
    estado                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_herramientas PRIMARY KEY (id),
    CONSTRAINT fk_herramientas_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_herramientas_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_herramientas_precio CHECK (precio_por_dia > 0),
    CONSTRAINT ck_herramientas_stock CHECK (stock_total >= 0),
    CONSTRAINT ck_herramientas_fuera_servicio CHECK (unidades_fuera_servicio >= 0 AND unidades_fuera_servicio <= stock_total),
    CONSTRAINT ck_herramientas_estado CHECK (estado IN ('ACTIVA', 'EN_MANTENIMIENTO', 'INACTIVA'))
) ENGINE = InnoDB;

CREATE TABLE herramienta_imagenes (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    herramienta_id BIGINT       NOT NULL,
    url            VARCHAR(500) NOT NULL,
    principal      BOOLEAN      NOT NULL DEFAULT FALSE,
    orden          INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_herramienta_imagenes PRIMARY KEY (id),
    -- Las imágenes no existen sin su herramienta: si se borra la herramienta, se borran sus imágenes
    CONSTRAINT fk_herramienta_imagenes_herramienta FOREIGN KEY (herramienta_id) REFERENCES herramientas (id) ON DELETE CASCADE
) ENGINE = InnoDB;
