package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.RegistrarEntregaCommand;
import com.rentaherramientas.domain.model.valueobject.TipoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Entrega del equipo al cliente. La dirección es obligatoria si el tipo es DOMICILIO")
public record RegistrarEntregaRequest(
        @Schema(example = "DOMICILIO")
        @NotNull(message = "El tipo de entrega es obligatorio")
        TipoEntrega tipo,

        @Schema(example = "Cra 27 # 36-14, obra Torre Norte")
        @Size(max = 200)
        String direccion,

        @Schema(example = "Se entregó con 2 brocas y estuche")
        @Size(max = 500)
        String observaciones) {

    public RegistrarEntregaCommand aCommand() {
        return new RegistrarEntregaCommand(tipo, direccion, observaciones);
    }
}
