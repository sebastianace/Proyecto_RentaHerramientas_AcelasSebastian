package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository {

    Categoria guardar(Categoria categoria);

    Optional<Categoria> buscarPorId(Long id);

    /** Para validar nombres repetidos sin distinguir mayúsculas. idExcluido sirve al editar (puede ser null). */
    boolean existePorNombre(String nombre, Long idExcluido);

    List<Categoria> listar(boolean incluirInactivas);
}
