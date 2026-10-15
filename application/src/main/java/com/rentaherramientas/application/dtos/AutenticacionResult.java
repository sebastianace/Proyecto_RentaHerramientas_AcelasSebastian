package com.rentaherramientas.application.dtos;

import java.time.Instant;

/**
 * Respuesta de un login o registro exitoso.
 *
 * @param token    JWT que el cliente debe enviar en el header Authorization
 * @param tipo     siempre "Bearer"
 * @param expiraEn momento exacto en que el token deja de ser válido
 */
public record AutenticacionResult(String token, String tipo, Instant expiraEn, UsuarioResult usuario) {
}
