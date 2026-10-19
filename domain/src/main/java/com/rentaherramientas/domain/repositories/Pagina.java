package com.rentaherramientas.domain.repositories;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados con la información necesaria para paginar en el frontend.
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    /** Convierte el contenido conservando los datos de paginación (ej. de dominio a DTO). */
    public <R> Pagina<R> map(Function<T, R> conversor) {
        return new Pagina<>(contenido.stream().map(conversor).toList(), pagina, tamanio, totalElementos, totalPaginas);
    }
}
