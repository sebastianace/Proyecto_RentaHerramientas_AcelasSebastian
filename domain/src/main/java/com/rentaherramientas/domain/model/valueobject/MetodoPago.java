package com.rentaherramientas.domain.model.valueobject;

/** Medios de pago en línea aceptados. */
public enum MetodoPago {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    PSE,
    NEQUI;

    public boolean esTarjeta() {
        return this == TARJETA_CREDITO || this == TARJETA_DEBITO;
    }
}
