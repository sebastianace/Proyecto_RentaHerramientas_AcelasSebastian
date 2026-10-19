package com.rentaherramientas.application.ports.out;

import com.rentaherramientas.application.dtos.FacturaResult;

/** Convierte una factura en un documento PDF. */
public interface GeneradorFacturaPdfPort {

    byte[] generar(FacturaResult factura);
}
