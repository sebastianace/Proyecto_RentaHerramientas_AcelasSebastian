package com.rentaherramientas.infrastructure;

import com.rentaherramientas.application.ports.out.EnvioCorreoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reglas de formato de los datos personales y recuperación de contraseña, de punta a punta. */
class ValidacionYRecuperacionIntegracionTest extends IntegracionBase {

    /** El correo se reemplaza por un simulacro para leer el enlace que se "envía". */
    @MockitoBean
    EnvioCorreoPort correo;

    private static String registro(String nombres, String telefono, String documento, String ciudad) {
        return """
                {"email":"nuevo.cliente@gmail.com","password":"Clave2026","nombres":"%s","apellidos":"Pérez",
                 "telefono":"%s","tipoDocumento":"CC","numeroDocumento":"%s","direccion":"Calle 1 # 2-3","ciudad":"%s"}"""
                .formatted(nombres, telefono, documento, ciudad);
    }

    @Test
    @DisplayName("Nombre con números, teléfono de 9 dígitos, cédula con letras y ciudad con números: 400 en cada campo")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Laura2", "300123456", "10987abc", "Bucaramanga1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombres").exists())
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.numeroDocumentoValido").exists())
                .andExpect(jsonPath("$.errores.ciudad").exists());
    }

    @Test
    @DisplayName("Datos válidos con tildes y teléfono colombiano de 10 dígitos: 201")
    void registroValido() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("María José", "3001234567", "1112223334", "Bogotá D.C.")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Olvidé mi contraseña: el enlace permite crear una clave nueva una sola vez")
    void recuperarPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CLIENTE + "\"}"))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo).enviarRecuperacionPassword(eq(CLIENTE), anyString(), enlace.capture(), any(Duration.class));
        String token = enlace.getValue().substring(enlace.getValue().indexOf("token=") + 6);
        String cuerpo = "{\"token\":\"" + token + "\",\"password\":\"NuevaClave2026\"}";

        mvc.perform(post("/api/v1/auth/restablecer").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CLIENTE + "\",\"password\":\"NuevaClave2026\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/restablecer").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("Un correo que no existe recibe la misma respuesta (no se revela qué cuentas existen)")
    void correoInexistente() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@gmail.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").exists());
    }
}
