package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.FiltroPagos;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.util.Optional;

public interface ConsultaPagosPort {

    Optional<PagoResult> obtener(Long id);

    /** Ordenados del más reciente al más antiguo. */
    Pagina<PagoResult> listar(FiltroPagos filtro, SolicitudPagina solicitud);
}
