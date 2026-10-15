package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Categoria;
import com.rentaherramientas.domain.repositories.CategoriaRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.CategoriaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.CatalogoEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoriaRepositoryAdapter implements CategoriaRepository {

    private final CategoriaJpaRepository jpa;

    public CategoriaRepositoryAdapter(CategoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Categoria guardar(Categoria categoria) {
        CategoriaEntity entidad = categoria.getId() == null
                ? new CategoriaEntity()
                : jpa.findById(categoria.getId())
                     .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", categoria.getId()));
        CatalogoEntityMapper.copiar(categoria, entidad);
        return CatalogoEntityMapper.aDominio(jpa.save(entidad));
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return jpa.findById(id).map(CatalogoEntityMapper::aDominio);
    }

    @Override
    public boolean existePorNombre(String nombre, Long idExcluido) {
        return idExcluido == null
                ? jpa.existsByNombreIgnoreCase(nombre)
                : jpa.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido);
    }

    @Override
    public List<Categoria> listar(boolean incluirInactivas) {
        List<CategoriaEntity> entidades = incluirInactivas
                ? jpa.findAllByOrderByNombreAsc()
                : jpa.findByActivaTrueOrderByNombreAsc();
        return entidades.stream().map(CatalogoEntityMapper::aDominio).toList();
    }
}
