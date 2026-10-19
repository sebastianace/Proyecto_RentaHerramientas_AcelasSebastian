package com.rentaherramientas.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Propiedades app.cors.*: orígenes del frontend que pueden llamar a la API. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
