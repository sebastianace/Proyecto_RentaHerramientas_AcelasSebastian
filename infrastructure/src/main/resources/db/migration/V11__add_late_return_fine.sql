-- =====================================================================
-- V11: multa por devolución tardía.
-- Si el equipo se devuelve después de la fecha_fin de la reserva, se registran los días de retraso y la multa
-- calculada (valor diario del alquiler x 150 % x días de retraso). Las devoluciones existentes quedan sin retraso (0).
-- No se editan las migraciones anteriores: un cambio de esquema siempre es una versión nueva.
-- =====================================================================

ALTER TABLE devoluciones
    ADD COLUMN dias_retraso INT           NOT NULL DEFAULT 0 COMMENT 'Días entre la fecha_fin de la reserva y la devolución real',
    ADD COLUMN multa        DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Recargo por devolver tarde; 0 si se devolvió a tiempo',
    ADD CONSTRAINT ck_devoluciones_retraso CHECK (dias_retraso >= 0 AND multa >= 0);
