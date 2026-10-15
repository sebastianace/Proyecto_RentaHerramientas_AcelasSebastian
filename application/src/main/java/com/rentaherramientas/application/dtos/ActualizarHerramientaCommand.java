package com.rentaherramientas.application.dtos;

import java.math.BigDecimal;

public record ActualizarHerramientaCommand(
        Long categoriaId,
        String nombre,
        String descripcion,
        String marca,
        String modelo,
        BigDecimal precioPorDia,
        int stockTotal,
        int unidadesFueraServicio) {
}
