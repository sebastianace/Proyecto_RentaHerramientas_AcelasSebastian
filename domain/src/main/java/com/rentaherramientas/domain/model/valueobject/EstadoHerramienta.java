package com.rentaherramientas.domain.model.valueobject;

/**
 * Estado de una herramienta en el inventario.
 * <ul>
 *   <li>ACTIVA: visible en el catálogo y se puede reservar.</li>
 *   <li>EN_MANTENIMIENTO: temporalmente fuera del catálogo.</li>
 *   <li>INACTIVA: retirada (borrado lógico); conserva su historial de alquileres.</li>
 * </ul>
 */
public enum EstadoHerramienta {
    ACTIVA,
    EN_MANTENIMIENTO,
    INACTIVA
}
