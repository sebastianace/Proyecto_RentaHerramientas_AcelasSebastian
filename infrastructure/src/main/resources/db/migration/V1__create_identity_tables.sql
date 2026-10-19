-- =====================================================================
-- V1: Contexto de IDENTIDAD
-- Usuarios, roles y perfiles (proveedor / cliente).
-- Un usuario inicia sesión; su perfil de proveedor o cliente guarda
-- los datos propios del negocio (1:1 con usuarios).
-- =====================================================================

CREATE TABLE roles (
    id     BIGINT      NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(30) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre),
    CONSTRAINT ck_roles_nombre CHECK (nombre IN ('ADMIN', 'PROVEEDOR', 'CLIENTE'))
) ENGINE = InnoDB;

CREATE TABLE usuarios (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL COMMENT 'Hash BCrypt, nunca la clave en texto plano',
    nombres       VARCHAR(80)  NOT NULL,
    apellidos     VARCHAR(80)  NOT NULL,
    telefono      VARCHAR(20)  NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'Borrado lógico: un usuario con historial no se elimina, se desactiva',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
) ENGINE = InnoDB;

-- Relación N:M entre usuarios y roles (mismo patrón del taller de Spring Security)
CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    CONSTRAINT pk_usuario_roles PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_roles_rol     FOREIGN KEY (rol_id)     REFERENCES roles (id)
) ENGINE = InnoDB;

CREATE TABLE proveedores (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id   BIGINT       NOT NULL,
    razon_social VARCHAR(150) NOT NULL,
    nit          VARCHAR(20)  NOT NULL,
    direccion    VARCHAR(200) NOT NULL,
    ciudad       VARCHAR(80)  NOT NULL,
    descripcion  VARCHAR(500) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_proveedores PRIMARY KEY (id),
    CONSTRAINT uq_proveedores_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_proveedores_nit UNIQUE (nit),
    CONSTRAINT fk_proveedores_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE TABLE clientes (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id       BIGINT       NOT NULL,
    tipo_documento   VARCHAR(10)  NOT NULL,
    numero_documento VARCHAR(20)  NOT NULL,
    direccion        VARCHAR(200) NOT NULL,
    ciudad           VARCHAR(80)  NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uq_clientes_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_clientes_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_clientes_tipo_documento CHECK (tipo_documento IN ('CC', 'CE', 'NIT', 'PASAPORTE'))
) ENGINE = InnoDB;
