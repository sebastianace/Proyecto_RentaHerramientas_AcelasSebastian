package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.util.Optional;

public interface ConsultaFacturasPort {

    Optional<FacturaResult> obtener(Long id);

    /** clienteId y proveedorId son opcionales (null = sin filtro). Más recientes primero. */
    Pagina<FacturaResult> listar(Long clienteId, Long proveedorId, SolicitudPagina solicitud);
}
