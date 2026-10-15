package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.TipoDocumento;

public record ClienteResult(
        Long id,
        UsuarioResult usuario,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String direccion,
        String ciudad) {
}
