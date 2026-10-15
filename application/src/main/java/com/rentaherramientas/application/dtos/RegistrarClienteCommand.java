package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.TipoDocumento;

public record RegistrarClienteCommand(
        String email,
        String password,
        String nombres,
        String apellidos,
        String telefono,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String direccion,
        String ciudad) {
}
