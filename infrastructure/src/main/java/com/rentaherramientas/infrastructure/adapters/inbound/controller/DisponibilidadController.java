package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.DisponibilidadResult;
import com.rentaherramientas.application.ports.in.GestionReservasUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Tag(name = "Herramientas")
public class DisponibilidadController {

    private final GestionReservasUseCase gestionReservas;

    public DisponibilidadController(GestionReservasUseCase gestionReservas) {
        this.gestionReservas = gestionReservas;
    }

    @GetMapping("/api/v1/herramientas/{id}/disponibilidad")
    @SecurityRequirements
    @Operation(summary = "Consultar disponibilidad", description = "Público. Unidades libres de una herramienta entre dos fechas")
    public DisponibilidadResult disponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return gestionReservas.consultarDisponibilidad(id, fechaInicio, fechaFin);
    }
}
