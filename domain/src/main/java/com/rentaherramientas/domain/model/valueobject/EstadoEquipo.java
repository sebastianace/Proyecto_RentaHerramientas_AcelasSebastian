package com.rentaherramientas.domain.model.valueobject;

/** Estado en el que el proveedor recibe el equipo al confirmar la devolución. */
public enum EstadoEquipo {
    BUENO,
    DESGASTE_NORMAL,
    CON_DANOS;

    /** Un equipo devuelto en buen estado no puede tener reportes de daño. */
    public boolean admiteReporteDeDano() {
        return this != BUENO;
    }
}
