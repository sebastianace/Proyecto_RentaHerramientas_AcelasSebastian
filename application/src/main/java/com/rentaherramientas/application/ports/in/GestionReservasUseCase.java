package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.CotizacionResult;
import com.rentaherramientas.application.dtos.CrearReservaCommand;
import com.rentaherramientas.application.dtos.DisponibilidadResult;
import com.rentaherramientas.application.dtos.FiltroReservas;
import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.time.LocalDate;

public interface GestionReservasUseCase {

    // ---------- Consultas públicas ----------

    DisponibilidadResult consultarDisponibilidad(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin);

    CotizacionResult cotizar(CrearReservaCommand command);

    // ---------- Cliente ----------

    ReservaResult crear(CrearReservaCommand command, Solicitante solicitante);

    ReservaResult cancelar(Long id, Solicitante solicitante);

    // ---------- Proveedor ----------

    ReservaResult aprobar(Long id, Solicitante solicitante);

    ReservaResult rechazar(Long id, String motivo, Solicitante solicitante);

    // ---------- Todos (cada uno ve lo suyo) ----------

    ReservaResult obtener(Long id, Solicitante solicitante);

    Pagina<ReservaResult> listar(FiltroReservas filtro, SolicitudPagina solicitud, Solicitante solicitante);

    // ---------- Proceso automático ----------

    /** Marca como VENCIDAS las reservas aprobadas que no se pagaron a tiempo. Devuelve cuántas cambió. */
    int vencerReservasSinPago();
}
