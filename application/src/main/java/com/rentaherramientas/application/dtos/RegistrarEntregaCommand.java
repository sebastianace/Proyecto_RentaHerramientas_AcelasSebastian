package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.TipoEntrega;

/** Datos de la entrega. La dirección solo es obligatoria si el tipo es DOMICILIO. */
public record RegistrarEntregaCommand(TipoEntrega tipo, String direccion, String observaciones) {
}
