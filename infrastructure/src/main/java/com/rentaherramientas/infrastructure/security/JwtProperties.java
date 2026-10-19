package com.rentaherramientas.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propiedades app.jwt.* del application.yml, enlazadas a un record inmutable.
 *
 * @param secret     clave HMAC en Base64 (mínimo 32 bytes = 256 bits)
 * @param expiracion vida del token, ej. 60m
 * @param emisor     valor del claim "iss"; se verifica al validar el token
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiracion, String emisor) {
}
