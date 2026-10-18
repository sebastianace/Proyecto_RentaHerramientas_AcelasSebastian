package com.rentaherramientas.infrastructure.config;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Documenta los errores en Swagger con el formato REAL que devuelve la API: ProblemDetail (RFC 9457).
 *
 * <p>Sin esto, springdoc mostraba en las respuestas 401/403/404/409/422 el esquema del 200, lo cual es falso.
 * En vez de repetir anotaciones en cada controlador, este personalizador recorre TODAS las operaciones y:</p>
 * <ol>
 *   <li>registra el esquema {@code ProblemDetail} una sola vez;</li>
 *   <li>hace que toda respuesta 4xx/5xx use ese esquema, con un ejemplo realista por código;</li>
 *   <li>agrega 401 y 403 a las rutas protegidas y 400 a las que reciben cuerpo;</li>
 *   <li>quita el candado de las rutas públicas (login, registro, catálogo, cotización).</li>
 * </ol>
 */
@Configuration
public class OpenApiErroresConfig {

    private static final String REF = "#/components/schemas/ProblemDetail";

    private static final Map<String, String[]> EJEMPLOS = Map.of(
            "400", new String[]{"Datos inválidos", "La petición tiene campos con errores de validación"},
            "401", new String[]{"No autenticado", "Debes enviar un token válido en el header Authorization: Bearer <token>"},
            "403", new String[]{"Acceso denegado", "No tienes permisos para realizar esta operación"},
            "404", new String[]{"Recurso no encontrado", "Reserva con id 99 no existe"},
            "409", new String[]{"Conflicto", "No hay unidades suficientes de la herramienta en esas fechas"},
            "422", new String[]{"Regla de negocio incumplida", "No se puede pagar la reserva RES-2026-0036 porque está en estado PENDIENTE"},
            "500", new String[]{"Error interno", "Ocurrió un error inesperado. Intenta de nuevo más tarde"});

    @Bean
    public OpenApiCustomizer erroresComoProblemDetail() {
        return openApi -> {
            openApi.getComponents().addSchemas("ProblemDetail", esquemaProblemDetail());
            openApi.getPaths().forEach((ruta, item) -> item.readOperationsMap().forEach((metodo, operacion) -> {
                ApiResponses respuestas = operacion.getResponses();
                boolean publica = esPublica(ruta, metodo);
                if (publica) {
                    operacion.setSecurity(List.of()); // sin candado en Swagger UI
                } else {
                    respuestas.putIfAbsent("401", new ApiResponse().description("Falta el token o no es válido"));
                    respuestas.putIfAbsent("403", new ApiResponse().description("El rol no tiene permiso para esta operación"));
                }
                if (operacion.getRequestBody() != null) {
                    respuestas.putIfAbsent("400", new ApiResponse().description("Algún campo no pasó la validación"));
                }
                respuestas.forEach((codigo, respuesta) -> {
                    if (codigo.startsWith("4") || codigo.startsWith("5")) {
                        respuesta.setContent(contenidoProblema(codigo, ruta));
                    }
                });
            }));
        };
    }

    /** Rutas que SecurityConfig deja públicas. */
    private static boolean esPublica(String ruta, PathItem.HttpMethod metodo) {
        if (ruta.equals("/api/v1/auth/login") || ruta.equals("/api/v1/auth/registro")
                || ruta.equals("/api/v1/reservas/cotizacion")) {
            return true;
        }
        return metodo == PathItem.HttpMethod.GET && !ruta.equals("/api/v1/herramientas/inventario")
                && (ruta.startsWith("/api/v1/herramientas") || ruta.startsWith("/api/v1/categorias"));
    }

    private static Content contenidoProblema(String codigo, String ruta) {
        String[] ejemplo = EJEMPLOS.getOrDefault(codigo, new String[]{"Error", "Descripción del error"});
        Map<String, Object> valor = new LinkedHashMap<>();
        valor.put("type", "about:blank");
        valor.put("title", ejemplo[0]);
        valor.put("status", Integer.parseInt(codigo));
        valor.put("detail", ejemplo[1]);
        valor.put("instance", ruta);
        valor.put("timestamp", "2026-10-18T15:00:00Z");
        if ("400".equals(codigo)) {
            valor.put("errores", Map.of("email", "El email no tiene un formato válido"));
        }
        MediaType tipo = new MediaType().schema(new Schema<>().$ref(REF)).example(valor);
        return new Content().addMediaType("application/problem+json", tipo).addMediaType("application/json", tipo);
    }

    private static Schema<?> esquemaProblemDetail() {
        return new ObjectSchema()
                .description("Error en formato RFC 9457 (Problem Details), igual para todos los errores de la API")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().description("Resumen corto del tipo de error").example("Conflicto"))
                .addProperty("status", new IntegerSchema().description("Código HTTP").example(409))
                .addProperty("detail", new StringSchema().description("Explicación legible para el usuario"))
                .addProperty("instance", new StringSchema().description("Ruta que produjo el error"))
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("errores", new MapSchema().additionalProperties(new StringSchema())
                        .description("Solo en 400: mensaje de error por cada campo inválido"));
    }
}
