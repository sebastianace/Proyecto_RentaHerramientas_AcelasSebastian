package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ProveedorEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.IdentidadEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProveedorRepositoryAdapter implements ProveedorRepository {

    private final ProveedorJpaRepository jpa;
    private final RolResolver rolResolver;

    public ProveedorRepositoryAdapter(ProveedorJpaRepository jpa, RolResolver rolResolver) {
        this.jpa = jpa;
        this.rolResolver = rolResolver;
    }

    @Override
    public Proveedor guardar(Proveedor proveedor) {
        ProveedorEntity entidad = proveedor.getId() == null
                ? new ProveedorEntity()
                : jpa.findById(proveedor.getId())
                     .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", proveedor.getId()));
        UsuarioEntity usuario = entidad.getUsuario() == null ? new UsuarioEntity() : entidad.getUsuario();
        IdentidadEntityMapper.copiar(proveedor.getUsuario(), usuario, rolResolver::buscar);
        IdentidadEntityMapper.copiar(proveedor, entidad);
        entidad.setUsuario(usuario);
        return IdentidadEntityMapper.aDominio(jpa.save(entidad));
    }

    @Override
    public Optional<Proveedor> buscarPorId(Long id) {
        return jpa.findById(id).map(IdentidadEntityMapper::aDominio);
    }

    @Override
    public Optional<Proveedor> buscarPorUsuarioId(Long usuarioId) {
        return jpa.findByUsuario_Id(usuarioId).map(IdentidadEntityMapper::aDominio);
    }

    @Override
    public boolean existePorNit(String nit) {
        return jpa.existsByNit(nit);
    }

    @Override
    public Pagina<Proveedor> listar(SolicitudPagina solicitud) {
        return PaginaMapper.aPagina(jpa.findAll(PaginaMapper.aPageRequest(solicitud)), IdentidadEntityMapper::aDominio);
    }
}
