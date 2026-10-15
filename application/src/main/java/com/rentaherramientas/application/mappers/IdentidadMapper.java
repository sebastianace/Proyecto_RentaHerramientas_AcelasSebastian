package com.rentaherramientas.application.mappers;

import com.rentaherramientas.application.dtos.ClienteResult;
import com.rentaherramientas.application.dtos.ProveedorResult;
import com.rentaherramientas.application.dtos.UsuarioResult;
import com.rentaherramientas.domain.model.aggregates.Cliente;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.aggregates.Usuario;

/** Convierte objetos de dominio en DTOs de salida. Mapeo manual: explícito y fácil de depurar. */
public final class IdentidadMapper {

    private IdentidadMapper() {
    }

    public static UsuarioResult aResult(Usuario u) {
        return new UsuarioResult(u.getId(), u.getEmail().valor(), u.getNombres(), u.getApellidos(),
                u.getTelefono(), u.isActivo(), u.getRoles());
    }

    public static ClienteResult aResult(Cliente c) {
        return new ClienteResult(c.getId(), aResult(c.getUsuario()), c.getTipoDocumento(),
                c.getNumeroDocumento(), c.getDireccion(), c.getCiudad());
    }

    public static ProveedorResult aResult(Proveedor p) {
        return new ProveedorResult(p.getId(), aResult(p.getUsuario()), p.getRazonSocial(), p.getNit(),
                p.getDireccion(), p.getCiudad(), p.getDescripcion());
    }
}
