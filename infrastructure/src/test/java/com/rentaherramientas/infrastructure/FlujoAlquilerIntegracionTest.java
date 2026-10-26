package com.rentaherramientas.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de un alquiler a través de la API real: reserva → aprobación → pago rechazado y aprobado →
 * factura en PDF → entrega → devolución con daño GRAVE → validación del ADMIN → reportes.
 * Todo ocurre en una transacción que se revierte al final.
 */
class FlujoAlquilerIntegracionTest extends IntegracionBase {

    private ResultActions enviar(String token, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
                                 String json) throws Exception {
        if (json != null) peticion.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(peticion.header("Authorization", bearer(token)));
    }

    private static String cuerpo(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Reserva, aprobación, pago, factura, entrega, devolución con daño y validación")
    void flujoCompleto() throws Exception {
        String cliente = token(CLIENTE);
        String proveedor = token(PROVEEDOR);
        String admin = token(ADMIN);
        LocalDate inicio = LocalDate.now().plusDays(120);

        // 1. El cliente solicita la reserva → PENDIENTE
        ResultActions creada = enviar(cliente, post("/api/v1/reservas"), """
                {"fechaInicio":"%s","fechaFin":"%s","herramientas":[{"herramientaId":1,"cantidad":1}]}"""
                .formatted(inicio, inicio.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(124950.0));
        int id = JsonPath.read(cuerpo(creada), "$.id");

        // 2. Pagar antes de la aprobación no se permite (422) y el cliente no puede aprobar (403)
        enviar(cliente, post("/api/v1/reservas/" + id + "/pagos"), "{\"metodo\":\"PSE\",\"titular\":\"JUAN\"}")
                .andExpect(status().isUnprocessableContent());
        enviar(cliente, patch("/api/v1/reservas/" + id + "/aprobar"), null).andExpect(status().isForbidden());

        // 3. El proveedor aprueba → APROBADA con plazo de pago
        enviar(proveedor, patch("/api/v1/reservas/" + id + "/aprobar"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.fechaLimitePago").exists());

        // 4. Tarjeta terminada en 0000 → rechazada; 4242 → aprobada con factura
        enviar(cliente, post("/api/v1/reservas/" + id + "/pagos"),
                "{\"metodo\":\"TARJETA_CREDITO\",\"numeroTarjeta\":\"4000000000000000\",\"titular\":\"JUAN\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aprobado").value(false))
                .andExpect(jsonPath("$.pago.reserva.estado").value("APROBADA"));
        ResultActions pagado = enviar(cliente, post("/api/v1/reservas/" + id + "/pagos"),
                "{\"metodo\":\"TARJETA_CREDITO\",\"numeroTarjeta\":\"4242424242424242\",\"titular\":\"JUAN\"}")
                .andExpect(jsonPath("$.aprobado").value(true))
                .andExpect(jsonPath("$.pago.reserva.estado").value("PAGADA"))
                .andExpect(jsonPath("$.pago.factura.numero").exists());
        int facturaId = JsonPath.read(cuerpo(pagado), "$.pago.factura.id");

        // 5. La factura se descarga como PDF real
        byte[] pdf = enviar(cliente, get("/api/v1/facturas/" + facturaId + "/pdf"), null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        // 6. Entrega: a domicilio sin dirección → 422; con dirección → ENTREGADA; repetir → 409
        enviar(proveedor, post("/api/v1/reservas/" + id + "/entrega"), "{\"tipo\":\"DOMICILIO\"}")
                .andExpect(status().isUnprocessableContent());
        enviar(proveedor, post("/api/v1/reservas/" + id + "/entrega"), "{\"tipo\":\"DOMICILIO\",\"direccion\":\"Cra 27 # 36-14\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserva.estado").value("ENTREGADA"));
        enviar(proveedor, post("/api/v1/reservas/" + id + "/entrega"), "{\"tipo\":\"RECOGIDA_EN_SEDE\"}")
                .andExpect(status().isConflict());

        // 7. Devolución con daño GRAVE → DEVUELTA y una unidad menos operativa
        int fueraAntes = JsonPath.read(cuerpo(mvc.perform(get("/api/v1/herramientas/1"))), "$.unidadesFueraServicio");
        ResultActions devuelta = enviar(proveedor, post("/api/v1/reservas/" + id + "/devolucion"), """
                {"estadoEquipo":"CON_DANOS","danos":[{"herramientaId":1,"descripcion":"Carcasa partida","severidad":"GRAVE","costoEstimado":180000}]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserva.estado").value("DEVUELTA"))
                .andExpect(jsonPath("$.danos[0].severidad").value("GRAVE"))
                // Devuelta antes de la fecha_fin: sin retraso y sin multa
                .andExpect(jsonPath("$.diasRetraso").value(0))
                .andExpect(jsonPath("$.multa").value(0.0));
        mvc.perform(get("/api/v1/herramientas/1")).andExpect(jsonPath("$.unidadesFueraServicio").value(fueraAntes + 1));
        int devolucionId = JsonPath.read(cuerpo(devuelta), "$.id");

        // 8. Solo el ADMIN valida la devolución, y una sola vez
        enviar(proveedor, patch("/api/v1/devoluciones/" + devolucionId + "/validar"), null).andExpect(status().isForbidden());
        enviar(admin, patch("/api/v1/devoluciones/" + devolucionId + "/validar"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validada").value(true));
        enviar(admin, patch("/api/v1/devoluciones/" + devolucionId + "/validar"), null)
                .andExpect(status().isUnprocessableContent());

        // 9. Reportes: el ADMIN ve la plataforma; el proveedor, solo lo suyo
        enviar(admin, get("/api/v1/reportes/resumen"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosNetos").isNumber());
        enviar(proveedor, get("/api/v1/reportes/herramientas-mas-alquiladas"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].proveedor").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("Herramientas del Oriente S.A.S."))));
    }
}
