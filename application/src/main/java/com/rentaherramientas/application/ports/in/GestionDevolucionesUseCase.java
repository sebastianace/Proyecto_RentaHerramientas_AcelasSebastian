package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.ConfirmarDevolucionCommand;
import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.EntregaResult;
import com.rentaherramientas.application.dtos.FiltroDevoluciones;
import com.rentaherramientas.application.dtos.FiltroIncidencias;
import com.rentaherramientas.application.dtos.RegistrarEntregaCommand;
import com.rentaherramientas.application.dtos.ReportarDanoCommand;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

/** Entregas, devoluciones, reportes de daño e incidencias (segunda mitad del ciclo de un alquiler). */
public interface GestionDevolucionesUseCase {

    /** PROVEEDOR dueño o ADMIN: la reserva PAGADA pasa a ENTREGADA. */
    EntregaResult registrarEntrega(Long reservaId, RegistrarEntregaCommand command, Solicitante solicitante);

    EntregaResult obtenerEntrega(Long reservaId, Solicitante solicitante);

    /** PROVEEDOR dueño o ADMIN: la reserva ENTREGADA pasa a DEVUELTA; opcionalmente reporta daños. */
    DevolucionResult confirmarDevolucion(Long reservaId, ConfirmarDevolucionCommand command, Solicitante solicitante);

    DevolucionResult obtenerDevolucion(Long id, Solicitante solicitante);

    DevolucionResult obtenerDevolucionDeReserva(Long reservaId, Solicitante solicitante);

    Pagina<DevolucionResult> listarDevoluciones(FiltroDevoluciones filtro, SolicitudPagina solicitud,
                                                Solicitante solicitante);

    /** Solo ADMIN: revisa y valida la devolución. */
    DevolucionResult validarDevolucion(Long id, Solicitante solicitante);

    /** PROVEEDOR dueño o ADMIN. Un daño GRAVE saca de servicio una unidad de la herramienta. */
    ReporteDanoResult reportarDano(Long devolucionId, ReportarDanoCommand command, Solicitante solicitante);

    Pagina<ReporteDanoResult> listarIncidencias(FiltroIncidencias filtro, SolicitudPagina solicitud,
                                                Solicitante solicitante);

    /** Solo ADMIN: ABIERTO → EN_REVISION → RESUELTO. Resolver un daño GRAVE reintegra la unidad reparada. */
    ReporteDanoResult cambiarEstadoIncidencia(Long id, EstadoIncidencia estado, Solicitante solicitante);
}
