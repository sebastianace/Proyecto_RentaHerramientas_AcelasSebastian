package com.rentaherramientas.application.config;

import java.time.Duration;

/**
 * Configuración de la recuperación de contraseña (viene de application.yml, pero es un objeto Java puro).
 *
 * @param vigencia        cuánto dura el enlace antes de vencer
 * @param urlRestablecer  página del frontend que recibe el token (?token=...)
 */
public record PoliticaRecuperacion(Duration vigencia, String urlRestablecer) {

    public PoliticaRecuperacion {
        if (vigencia == null || vigencia.isNegative() || vigencia.isZero()) {
            throw new IllegalArgumentException("La vigencia del enlace de recuperación debe ser positiva");
        }
        if (urlRestablecer == null || urlRestablecer.isBlank()) {
            throw new IllegalArgumentException("Falta la URL de la página para restablecer la contraseña");
        }
    }
}
