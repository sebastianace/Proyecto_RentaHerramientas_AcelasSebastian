package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

/**
 * Qué página de resultados se pide. Se define en el dominio para que los puertos
 * de repositorio no dependan de las clases de paginación de Spring Data.
 *
 * @param pagina  número de página, empieza en 0
 * @param tamanio cantidad de elementos por página (1 a 100)
 */
public record SolicitudPagina(int pagina, int tamanio) {

    public static final int TAMANIO_MAXIMO = 100;

    public SolicitudPagina {
        if (pagina < 0) {
            throw new ReglaDeNegocioException("La página no puede ser negativa");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaDeNegocioException("El tamaño de página debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
    }
}
