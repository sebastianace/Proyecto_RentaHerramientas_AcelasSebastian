package com.rentaherramientas.domain.model.valueobject;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de una reserva.
 *
 * <pre>
 * PENDIENTE ──► APROBADA ──► PAGADA ──► ENTREGADA ──► DEVUELTA
 *     │             │
 *     ├► RECHAZADA  ├► VENCIDA (no se pagó a tiempo)
 *     └► CANCELADA  └► CANCELADA
 * </pre>
 */
public enum EstadoReserva {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    PAGADA,
    ENTREGADA,
    DEVUELTA,
    VENCIDA,
    CANCELADA;

    /** Estados que ocupan unidades del inventario: cuentan al calcular la disponibilidad. */
    public static final Set<EstadoReserva> OCUPAN_INVENTARIO = EnumSet.of(APROBADA, PAGADA, ENTREGADA);

    public boolean ocupaInventario() {
        return OCUPAN_INVENTARIO.contains(this);
    }
}
