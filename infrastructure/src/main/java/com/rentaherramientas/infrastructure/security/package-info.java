/**
 * Seguridad: Spring Security sin sesiones (stateless) con tokens JWT.
 * Aquí viven los adaptadores de los puertos de autenticación de la capa de aplicación
 * (AutenticadorPort, TokenProviderPort, PasswordEncoderPort), el filtro que valida el token
 * en cada petición y la configuración de CORS.
 */
package com.rentaherramientas.infrastructure.security;
