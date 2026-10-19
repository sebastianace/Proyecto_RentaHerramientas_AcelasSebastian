package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.FiltroReservas;
import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.util.Optional;

/** Lado de lectura de las reservas (con datos de cliente, proveedor y herramientas). */
public interface ConsultaReservasPort {

    Optional<ReservaResult> obtener(Long id);

    /** Ordenadas de la más reciente a la más antigua. */
    Pagina<ReservaResult> listar(FiltroReservas filtro, SolicitudPagina solicitud);
}
