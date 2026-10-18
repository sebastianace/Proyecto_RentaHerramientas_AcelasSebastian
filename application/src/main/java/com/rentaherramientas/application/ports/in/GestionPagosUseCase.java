package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.ArchivoGenerado;
import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.application.dtos.FiltroPagos;
import com.rentaherramientas.application.dtos.PagarReservaCommand;
import com.rentaherramientas.application.dtos.PagoRealizadoResult;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

public interface GestionPagosUseCase {

    /** Cobra la reserva; si se aprueba, la confirma (PAGADA) y emite la factura automáticamente. */
    PagoRealizadoResult pagar(Long reservaId, PagarReservaCommand command, Solicitante solicitante);

    Pagina<PagoResult> listarPagos(FiltroPagos filtro, SolicitudPagina solicitud, Solicitante solicitante);

    Pagina<FacturaResult> listarFacturas(SolicitudPagina solicitud, Solicitante solicitante);

    FacturaResult obtenerFactura(Long id, Solicitante solicitante);

    ArchivoGenerado descargarFacturaPdf(Long id, Solicitante solicitante);
}
