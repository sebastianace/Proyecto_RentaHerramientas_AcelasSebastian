package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.HerramientaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.CatalogoEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class HerramientaRepositoryAdapter implements HerramientaRepository {

    private final HerramientaJpaRepository jpa;
    private final ProveedorJpaRepository proveedorJpa;
    private final CategoriaJpaRepository categoriaJpa;

    public HerramientaRepositoryAdapter(HerramientaJpaRepository jpa, ProveedorJpaRepository proveedorJpa,
                                        CategoriaJpaRepository categoriaJpa) {
        this.jpa = jpa;
        this.proveedorJpa = proveedorJpa;
        this.categoriaJpa = categoriaJpa;
    }

    @Override
    public Herramienta guardar(Herramienta herramienta) {
        HerramientaEntity entidad;
        if (herramienta.getId() == null) {
            entidad = new HerramientaEntity();
            // getReferenceById crea una referencia sin hacer SELECT: solo se necesita el id para la llave foránea
            entidad.setProveedor(proveedorJpa.getReferenceById(herramienta.getProveedorId()));
        } else {
            entidad = jpa.findById(herramienta.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Herramienta", herramienta.getId()));
        }
        entidad.setCategoria(categoriaJpa.getReferenceById(herramienta.getCategoriaId()));
        CatalogoEntityMapper.copiarDatos(herramienta, entidad);
        CatalogoEntityMapper.sincronizarImagenes(herramienta, entidad);
        // saveAndFlush: ejecuta los INSERT ya, para que las imágenes nuevas tengan id en la respuesta
        return CatalogoEntityMapper.aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Optional<Herramienta> buscarPorId(Long id) {
        return jpa.findById(id).map(CatalogoEntityMapper::aDominio);
    }
}
