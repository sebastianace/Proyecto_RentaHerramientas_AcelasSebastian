package com.rentaherramientas.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autenticación JWT, rutas públicas y protección por rol, de punta a punta. */
class SeguridadIntegracionTest extends IntegracionBase {

    @Test
    @DisplayName("Login correcto devuelve un JWT con los roles del usuario")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("Clave incorrecta: 401 en formato ProblemDetail")
    void claveIncorrecta() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\",\"password\":\"incorrecta1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("Ruta protegida sin token: 401")
    void sinToken() throws Exception {
        mvc.perform(get("/api/v1/reservas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token con el rol cambiado a ADMIN: la firma no coincide y responde 401")
    void tokenManipulado() throws Exception {
        String[] partes = token(CLIENTE).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"CLIENTE\"", "\"ADMIN\"");
        String falso = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];

        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(falso)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Si el ADMIN desactiva a un usuario, su token deja de funcionar de inmediato (401)")
    void usuarioDesactivadoPierdeAcceso() throws Exception {
        String cliente = token(CLIENTE);
        mvc.perform(get("/api/v1/reservas").header("Authorization", bearer(cliente))).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/clientes/1").header("Authorization", bearer(token(ADMIN))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/reservas").header("Authorization", bearer(cliente)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un CLIENTE no puede listar clientes ni ver reportes: 403")
    void clienteSinPermiso() throws Exception {
        String cliente = token(CLIENTE);
        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(cliente))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes/resumen").header("Authorization", bearer(cliente))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El catálogo, la disponibilidad y la cotización son públicos")
    void rutasPublicas() throws Exception {
        mvc.perform(get("/api/v1/herramientas")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/herramientas/1/disponibilidad?fechaInicio=2026-12-01&fechaFin=2026-12-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadesOperativas").isNumber());
        mvc.perform(post("/api/v1/reservas/cotizacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"2026-12-01\",\"fechaFin\":\"2026-12-03\",\"herramientas\":[{\"herramientaId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias").value(3))
                .andExpect(jsonPath("$.subtotal").value(210000.0));
    }

    @Test
    @DisplayName("Validación del cuerpo: 400 con el error de cada campo")
    void validacion() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }
}
