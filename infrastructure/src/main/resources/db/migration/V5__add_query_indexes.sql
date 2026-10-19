-- =====================================================================
-- V5: Índices para las consultas más frecuentes
-- InnoDB ya crea un índice por cada PRIMARY KEY, UNIQUE y FOREIGN KEY.
-- Estos índices adicionales cubren los filtros de búsqueda y reportes.
-- =====================================================================

-- Exploración del catálogo: "herramientas activas de la categoría X"
CREATE INDEX idx_herramientas_categoria_estado ON herramientas (categoria_id, estado);

-- Inventario del proveedor filtrado por estado
CREATE INDEX idx_herramientas_proveedor_estado ON herramientas (proveedor_id, estado);

-- Búsqueda por nombre en el catálogo (LIKE 'texto%')
CREATE INDEX idx_herramientas_nombre ON herramientas (nombre);

-- Control de disponibilidad: reservas activas que se cruzan con un rango de fechas
CREATE INDEX idx_reservas_estado_fechas ON reservas (estado, fecha_inicio, fecha_fin);

-- Historial del cliente y bandeja de solicitudes del proveedor, ordenados por fecha
CREATE INDEX idx_reservas_cliente_creacion ON reservas (cliente_id, created_at);
CREATE INDEX idx_reservas_proveedor_estado ON reservas (proveedor_id, estado);

-- Job de vencimiento: reservas APROBADAS cuya fecha límite de pago ya pasó
CREATE INDEX idx_reservas_estado_limite_pago ON reservas (estado, fecha_limite_pago);

-- Reporte de ingresos: pagos aprobados en un rango de fechas
CREATE INDEX idx_pagos_estado_fecha ON pagos (estado, fecha_pago);

-- Gestión de incidencias del administrador
CREATE INDEX idx_reportes_danos_estado ON reportes_danos (estado, severidad);
