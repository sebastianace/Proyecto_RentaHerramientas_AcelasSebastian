package com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers;

import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.aggregates.Usuario;
import com.rentaherramientas.domain.model.valueobject.Email;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ClienteEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ProveedorEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.RolEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.UsuarioEntity;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traduce entre entidades JPA (tablas) y objetos de dominio.
 * Va en infraestructura porque conoce ambos mundos; el dominio no sabe que existe JPA.
 */
public final class IdentidadEntityMapper {

    private IdentidadEntityMapper() {
    }

    // ---------- Entidad -> Dominio ----------

    public static Usuario aDominio(UsuarioEntity e) {
        Set<RolNombre> roles = e.getRoles().stream().map(RolEntity::getNombre).collect(Collectors.toSet());
        return Usuario.reconstruir(e.getId(), new Email(e.getEmail()), e.getPasswordHash(), e.getNombres(),
                e.getApellidos(), e.getTelefono(), e.isActivo(), roles);
    }

    public static Cliente aDominio(ClienteEntity e) {
        return Cliente.reconstruir(e.getId(), aDominio(e.getUsuario()), e.getTipoDocumento(),
                e.getNumeroDocumento(), e.getDireccion(), e.getCiudad());
    }

    public static Proveedor aDominio(ProveedorEntity e) {
        return Proveedor.reconstruir(e.getId(), aDominio(e.getUsuario()), e.getRazonSocial(), e.getNit(),
                e.getDireccion(), e.getCiudad(), e.getDescripcion());
    }

    // ---------- Dominio -> Entidad (copia sobre una entidad nueva o ya cargada) ----------

    /**
     * @param buscarRol función que devuelve la entidad de rol administrada por JPA
     *                  (los roles ya existen en la tabla roles; no se crean nuevos)
     */
    public static void copiar(Usuario origen, UsuarioEntity destino, Function<RolNombre, RolEntity> buscarRol) {
        destino.setEmail(origen.getEmail().valor());
        destino.setPasswordHash(origen.getPasswordHash());
        destino.setNombres(origen.getNombres());
        destino.setApellidos(origen.getApellidos());
        destino.setTelefono(origen.getTelefono());
        destino.setActivo(origen.isActivo());
        Set<RolNombre> actuales = destino.getRoles().stream().map(RolEntity::getNombre).collect(Collectors.toSet());
        if (!actuales.equals(origen.getRoles())) {
            destino.getRoles().clear();
            origen.getRoles().forEach(rol -> destino.getRoles().add(buscarRol.apply(rol)));
        }
    }

    public static void copiar(Cliente origen, ClienteEntity destino) {
        destino.setTipoDocumento(origen.getTipoDocumento());
        destino.setNumeroDocumento(origen.getNumeroDocumento());
        destino.setDireccion(origen.getDireccion());
        destino.setCiudad(origen.getCiudad());
    }

    public static void copiar(Proveedor origen, ProveedorEntity destino) {
        destino.setRazonSocial(origen.getRazonSocial());
        destino.setNit(origen.getNit());
        destino.setDireccion(origen.getDireccion());
        destino.setCiudad(origen.getCiudad());
        destino.setDescripcion(origen.getDescripcion());
    }
}
