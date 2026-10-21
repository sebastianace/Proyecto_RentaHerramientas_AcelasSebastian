package com.rentaherramientas.infrastructure.adapters.inbound.dtos;

import com.rentaherramientas.application.dtos.CategoriaCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de una categoría")
public record CategoriaRequest(
        @Schema(example = "Cortadoras de Cerámica")
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = ValidacionesComunes.REGEX_CATEGORIA, message = ValidacionesComunes.MENSAJE_CATEGORIA)
        String nombre,

        @Schema(example = "Cortadoras manuales y eléctricas para baldosa y porcelanato")
        @Size(max = 300)
        String descripcion) {

    public CategoriaCommand aCommand() {
        return new CategoriaCommand(nombre, descripcion);
    }
}
