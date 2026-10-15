package com.rentaherramientas.application.ports.in;

import com.rentaherramientas.application.dtos.CategoriaCommand;
import com.rentaherramientas.application.dtos.CategoriaResult;

import java.util.List;

public interface GestionCategoriasUseCase {

    CategoriaResult crear(CategoriaCommand command);

    CategoriaResult actualizar(Long id, CategoriaCommand command);

    CategoriaResult obtener(Long id);

    List<CategoriaResult> listar(boolean incluirInactivas);

    void desactivar(Long id);

    void activar(Long id);
}
