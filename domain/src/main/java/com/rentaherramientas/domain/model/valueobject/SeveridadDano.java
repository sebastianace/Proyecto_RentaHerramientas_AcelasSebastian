package com.rentaherramientas.domain.model.valueobject;

/**
 * Gravedad de un daño. Un daño GRAVE deja la unidad inservible: se saca de servicio
 * (aumenta unidades_fuera_servicio) y deja de contar para la disponibilidad.
 */
public enum SeveridadDano {
    LEVE,
    MODERADO,
    GRAVE;

    public boolean sacaDeServicio() {
        return this == GRAVE;
    }
}
