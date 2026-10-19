package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.EntregaResult;
import com.rentaherramientas.application.dtos.FiltroDevoluciones;
import com.rentaherramientas.application.dtos.FiltroIncidencias;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.util.Optional;

/**
 * Lado de lectura (CQRS ligero) de entregas, devoluciones y daños: cada resultado junta datos de varias
 * tablas (reserva, cliente, proveedor, herramienta, usuario) en una consulta optimizada.
 *
 * <p>Es un puerto de salida de la capa de aplicación: aquí solo está el contrato; la consulta SQL/JPA
 * vive en infraestructura (ConsultaDevolucionesAdapter).</p>
 */
public interface ConsultaDevolucionesPort {

    Optional<EntregaResult> entregaDeReserva(Long reservaId);

    Optional<DevolucionResult> obtenerDevolucion(Long id);

    Optional<DevolucionResult> devolucionDeReserva(Long reservaId);

    /** Las más recientes primero. */
    Pagina<DevolucionResult> listarDevoluciones(FiltroDevoluciones filtro, SolicitudPagina solicitud);

    Optional<ReporteDanoResult> obtenerReporte(Long id);

    /** Los más recientes primero. */
    Pagina<ReporteDanoResult> listarReportes(FiltroIncidencias filtro, SolicitudPagina solicitud);
}
