package com.rentaherramientas.infrastructure;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: levanta la aplicación COMPLETA (controladores, Spring Security con JWT,
 * casos de uso, JPA y MySQL) contra la base renta_herramientas_test.
 *
 * <p>MockMvc se arma con la cadena de filtros de Spring Security (springSecurityFilterChain), así cada petición
 * pasa por el JwtValidationFilter igual que en producción. {@code @Transactional}: cada prueba termina con
 * rollback, por eso se pueden correr muchas veces sin ensuciar los datos.</p>
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
@Tag("integracion")
public abstract class IntegracionBase {

    protected static final String CLAVE = "Renta2026*";
    protected static final String ADMIN = "admin@rentaherramientas.com";
    protected static final String PROVEEDOR = "carlos.rueda@herroriente.com";
    protected static final String CLIENTE = "juanpablo.ardila@gmail.com";

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    Filter seguridad;

    protected MockMvc mvc;

    @BeforeEach
    void armarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguridad).build();
    }

    /** Inicia sesión y devuelve el JWT. */
    protected String token(String email) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
