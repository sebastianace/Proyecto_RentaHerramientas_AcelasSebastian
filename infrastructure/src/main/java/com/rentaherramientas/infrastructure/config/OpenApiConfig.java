package com.rentaherramientas.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI. Agrega el esquema "bearerAuth" para que en Swagger UI aparezca
 * el botón Authorize: se pega el token del login y Swagger lo envía en cada petición.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Plataforma de Renta de Herramientas")
                        .version("1.0.0")
                        .description("""
                                API REST para alquilar herramientas y equipos de construcción.

                                **Cómo autenticarse:**
                                1. Ejecuta `POST /api/v1/auth/login` (usuarios de prueba con clave `Renta2026*`).
                                2. Copia el valor de `token` de la respuesta.
                                3. Presiona **Authorize** y pega el token (sin escribir "Bearer").
                                """)
                        .contact(new Contact().name("Sebastián Acelas")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .name(ESQUEMA_SEGURIDAD)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
