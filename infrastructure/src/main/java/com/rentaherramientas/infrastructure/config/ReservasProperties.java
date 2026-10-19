package com.rentaherramientas.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Propiedades app.reservas.* del application.yml. */
@ConfigurationProperties(prefix = "app.reservas")
public record ReservasProperties(Duration plazoPago, int diasMaximosAlquiler) {
}
