package com.rentaherramientas.domain.model.valueobject;

/** Cómo recibe el cliente el equipo. Solo la entrega a domicilio exige dirección. */
public enum TipoEntrega {
    DOMICILIO,
    RECOGIDA_EN_SEDE;

    public boolean requiereDireccion() {
        return this == DOMICILIO;
    }
}
