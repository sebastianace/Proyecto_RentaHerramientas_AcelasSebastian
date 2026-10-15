package com.rentaherramientas.application.mappers;

import com.rentaherramientas.application.dtos.CategoriaResult;
import com.rentaherramientas.domain.model.aggregates.Categoria;

public final class CatalogoMapper {

    private CatalogoMapper() {
    }

    public static CategoriaResult aResult(Categoria c) {
        return new CategoriaResult(c.getId(), c.getNombre(), c.getDescripcion(), c.isActiva());
    }
}
