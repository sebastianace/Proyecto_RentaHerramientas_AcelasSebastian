package com.rentaherramientas.domain.model.valueobject;

/**
 * Ciclo de un reporte de daño gestionado por el ADMIN: ABIERTO → EN_REVISION → RESUELTO.
 * Solo se avanza un paso a la vez y nunca se retrocede.
 */
public enum EstadoIncidencia {
    ABIERTO,
    EN_REVISION,
    RESUELTO;

    public boolean puedePasarA(EstadoIncidencia siguiente) {
        return siguiente != null && siguiente.ordinal() == this.ordinal() + 1;
    }
}
