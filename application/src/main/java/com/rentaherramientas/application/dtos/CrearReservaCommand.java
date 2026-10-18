package com.rentaherramientas.application.dtos;

import java.time.LocalDate;
import java.util.List;

public record CrearReservaCommand(LocalDate fechaInicio, LocalDate fechaFin, List<Item> herramientas, String notas) {

    public CrearReservaCommand {
        herramientas = herramientas == null ? List.of() : List.copyOf(herramientas);
    }

    public record Item(Long herramientaId, int cantidad) {
    }
}
