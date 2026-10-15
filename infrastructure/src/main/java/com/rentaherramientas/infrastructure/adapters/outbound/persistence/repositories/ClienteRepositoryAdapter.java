package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.valueobject.TipoDocumento;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ClienteEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.IdentidadEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpa;
    private final RolResolver rolResolver;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpa, RolResolver rolResolver) {
        this.jpa = jpa;
        this.rolResolver = rolResolver;
    }

    /**
     * Si el cliente es nuevo (sin id) crea las entidades; si ya existe, carga la entidad y le copia
     * los cambios. Así nunca se pisan columnas que el dominio no maneja (como created_at).
     */
    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteEntity entidad = cliente.getId() == null
                ? new ClienteEntity()
                : jpa.findById(cliente.getId())
                     .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", cliente.getId()));
        UsuarioEntity usuario = entidad.getUsuario() == null ? new UsuarioEntity() : entidad.getUsuario();
        IdentidadEntityMapper.copiar(cliente.getUsuario(), usuario, rolResolver::buscar);
        IdentidadEntityMapper.copiar(cliente, entidad);
        entidad.setUsuario(usuario);
        return IdentidadEntityMapper.aDominio(jpa.save(entidad));
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return jpa.findById(id).map(IdentidadEntityMapper::aDominio);
    }

    @Override
    public Optional<Cliente> buscarPorUsuarioId(Long usuarioId) {
        return jpa.findByUsuario_Id(usuarioId).map(IdentidadEntityMapper::aDominio);
    }

    @Override
    public boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        return jpa.existsByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento);
    }

    @Override
    public Pagina<Cliente> listar(SolicitudPagina solicitud) {
        return PaginaMapper.aPagina(jpa.findAll(PaginaMapper.aPageRequest(solicitud)), IdentidadEntityMapper::aDominio);
    }
}
