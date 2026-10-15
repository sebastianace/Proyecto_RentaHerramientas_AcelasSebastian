package com.rentaherramientas.application.dtos;

import java.math.BigDecimal;

/** Filtros opcionales de búsqueda del catálogo público. Cualquier campo puede venir en null. */
public record FiltroCatalogo(Long categoriaId, String texto, String ciudad, BigDecimal precioMaximo) {

    public FiltroCatalogo {
        texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        ciudad = (ciudad == null || ciudad.isBlank()) ? null : ciudad.trim();
    }
}
