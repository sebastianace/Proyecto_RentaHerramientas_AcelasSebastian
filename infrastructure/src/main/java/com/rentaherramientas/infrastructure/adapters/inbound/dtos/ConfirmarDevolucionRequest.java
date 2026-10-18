package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.ConfirmarDevolucionCommand;
import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "El proveedor confirma que recibió el equipo; puede reportar daños en la misma petición")
public record ConfirmarDevolucionRequest(
        @Schema(example = "CON_DANOS")
        @NotNull(message = "El estado del equipo es obligatorio")
        EstadoEquipo estadoEquipo,

        @Schema(example = "Devuelto en la sede, revisado con el cliente presente")
        @Size(max = 500)
        String observaciones,

        @Size(max = 20, message = "Máximo 20 daños por devolución")
        List<@Valid ReportarDanoRequest> danos) {

    public ConfirmarDevolucionCommand aCommand() {
        return new ConfirmarDevolucionCommand(estadoEquipo, observaciones,
                danos == null ? List.of() : danos.stream().map(ReportarDanoRequest::aCommand).toList());
    }
}
