package com.rentaherramientas.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Propiedades app.recuperacion.* del application.yml (recuperación de contraseña). */
@ConfigurationProperties(prefix = "app.recuperacion")
public record RecuperacionProperties(Duration vigencia, String urlRestablecer) {
}
