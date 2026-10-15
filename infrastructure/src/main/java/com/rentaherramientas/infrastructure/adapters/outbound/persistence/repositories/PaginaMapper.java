package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/** Convierte entre la paginación del dominio y la de Spring Data. */
final class PaginaMapper {

    private PaginaMapper() {
    }

    static PageRequest aPageRequest(SolicitudPagina solicitud) {
        return PageRequest.of(solicitud.pagina(), solicitud.tamanio(), Sort.by("id"));
    }

    static <E, D> Pagina<D> aPagina(Page<E> page, Function<E, D> conversor) {
        return new Pagina<>(page.getContent().stream().map(conversor).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
