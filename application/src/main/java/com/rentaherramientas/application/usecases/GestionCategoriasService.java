package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.CategoriaCommand;
import com.rentaherramientas.application.dtos.CategoriaResult;
import com.rentaherramientas.application.mappers.CatalogoMapper;
import com.rentaherramientas.application.ports.in.GestionCategoriasUseCase;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Categoria;
import com.rentaherramientas.domain.repositories.CategoriaRepository;

import java.util.List;

public class GestionCategoriasService implements GestionCategoriasUseCase {

    private final CategoriaRepository categoriaRepository;

    public GestionCategoriasService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public CategoriaResult crear(CategoriaCommand cmd) {
        validarNombreUnico(cmd.nombre(), null);
        return CatalogoMapper.aResult(categoriaRepository.guardar(Categoria.nueva(cmd.nombre(), cmd.descripcion())));
    }

    @Override
    public CategoriaResult actualizar(Long id, CategoriaCommand cmd) {
        Categoria categoria = buscar(id);
        validarNombreUnico(cmd.nombre(), id);
        categoria.actualizar(cmd.nombre(), cmd.descripcion());
        return CatalogoMapper.aResult(categoriaRepository.guardar(categoria));
    }

    @Override
    public CategoriaResult obtener(Long id) {
        return CatalogoMapper.aResult(buscar(id));
    }

    @Override
    public List<CategoriaResult> listar(boolean incluirInactivas) {
        return categoriaRepository.listar(incluirInactivas).stream().map(CatalogoMapper::aResult).toList();
    }

    @Override
    public void desactivar(Long id) {
        Categoria categoria = buscar(id);
        categoria.desactivar();
        categoriaRepository.guardar(categoria);
    }

    @Override
    public void activar(Long id) {
        Categoria categoria = buscar(id);
        categoria.activar();
        categoriaRepository.guardar(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        if (nombre != null && categoriaRepository.existePorNombre(nombre.trim(), idExcluido)) {
            throw new ConflictoException("Ya existe una categoría llamada '" + nombre.trim() + "'");
        }
    }
}
