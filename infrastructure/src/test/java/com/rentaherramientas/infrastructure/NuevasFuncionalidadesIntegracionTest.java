package com.rentaherramientas.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Buscar clientes y cambiar la contraseña propia, de punta a punta. */
class NuevasFuncionalidadesIntegracionTest extends IntegracionBase {

    @Test
    @DisplayName("El ADMIN busca clientes por nombre, sin importar mayúsculas")
    void buscarPorNombre() throws Exception {
        mvc.perform(get("/api/v1/clientes?texto=ARDILA").header("Authorization", bearer(token(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.contenido[*].usuario.email", hasItem(CLIENTE)));
    }

    @Test
    @DisplayName("Busca por número de documento y por ciudad, y combina los dos filtros")
    void buscarPorDocumentoYCiudad() throws Exception {
        String admin = bearer(token(ADMIN));
        mvc.perform(get("/api/v1/clientes?texto=1098765432").header("Authorization", admin))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/v1/clientes?ciudad=Floridablanca").header("Authorization", admin))
                .andExpect(jsonPath("$.contenido[*].ciudad", everyItem(is("Floridablanca"))));
        mvc.perform(get("/api/v1/clientes?texto=ardila&ciudad=Floridablanca").header("Authorization", admin))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Un texto sin coincidencias devuelve una lista vacía, no un error")
    void sinResultados() throws Exception {
        mvc.perform(get("/api/v1/clientes?texto=zzzzzz").header("Authorization", bearer(token(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Un cliente no puede buscar clientes (403)")
    void clienteNoPuedeBuscar() throws Exception {
        mvc.perform(get("/api/v1/clientes?texto=ardila").header("Authorization", bearer(token(CLIENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cambiar la contraseña: con la actual correcta funciona y la clave vieja deja de servir")
    void cambiarPassword() throws Exception {
        String token = token(CLIENTE);
        mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + CLAVE + "\",\"passwordNueva\":\"NuevaClave2026\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CLIENTE + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CLIENTE + "\",\"password\":\"NuevaClave2026\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Con la contraseña actual incorrecta responde 422; con una nueva débil, 400; sin token, 401")
    void cambiarPasswordInvalido() throws Exception {
        String token = bearer(token(CLIENTE));
        mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"incorrecta1\",\"passwordNueva\":\"NuevaClave2026\"}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + CLAVE + "\",\"passwordNueva\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.passwordNueva").exists());
        mvc.perform(post("/api/v1/auth/cambiar-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + CLAVE + "\",\"passwordNueva\":\"NuevaClave2026\"}"))
                .andExpect(status().isUnauthorized());
    }
}
