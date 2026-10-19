package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.model.valueobject.TipoDocumento;

/**
 * Perfil de cliente: quien alquila herramientas.
 *
 * <p>Es la raíz del agregado y contiene a su {@link Usuario}: ambos nacen juntos al
 * registrarse y se guardan en una sola operación. El documento de identidad no se
 * puede modificar después del registro.</p>
 */
public class Cliente {

    private final Long id;
    private final Usuario usuario;
    private final TipoDocumento tipoDocumento;
    private final String numeroDocumento;
    private String direccion;
    private String ciudad;

    private Cliente(Long id, Usuario usuario, TipoDocumento tipoDocumento, String numeroDocumento,
                    String direccion, String ciudad) {
        if (usuario == null || !usuario.tieneRol(RolNombre.CLIENTE)) {
            throw new ReglaDeNegocioException("Un cliente debe tener un usuario con rol CLIENTE");
        }
        if (tipoDocumento == null) {
            throw new ReglaDeNegocioException("El campo 'tipoDocumento' es obligatorio");
        }
        this.id = id;
        this.usuario = usuario;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = ReglaDeNegocioException.requerido(numeroDocumento, "numeroDocumento");
        this.direccion = ReglaDeNegocioException.requerido(direccion, "direccion");
        this.ciudad = ReglaDeNegocioException.requerido(ciudad, "ciudad");
    }

    public static Cliente nuevo(Usuario usuario, TipoDocumento tipoDocumento, String numeroDocumento,
                                String direccion, String ciudad) {
        return new Cliente(null, usuario, tipoDocumento, numeroDocumento, direccion, ciudad);
    }

    public static Cliente reconstruir(Long id, Usuario usuario, TipoDocumento tipoDocumento, String numeroDocumento,
                                      String direccion, String ciudad) {
        return new Cliente(id, usuario, tipoDocumento, numeroDocumento, direccion, ciudad);
    }

    public void actualizar(String nombres, String apellidos, String telefono, String direccion, String ciudad) {
        usuario.actualizarDatosPersonales(nombres, apellidos, telefono);
        this.direccion = ReglaDeNegocioException.requerido(direccion, "direccion");
        this.ciudad = ReglaDeNegocioException.requerido(ciudad, "ciudad");
    }

    public void desactivar() {
        usuario.desactivar();
    }

    public void activar() {
        usuario.activar();
    }

    /** Indica si este cliente pertenece al usuario autenticado (para validar que solo vea sus datos). */
    public boolean perteneceA(Long usuarioId) {
        return usuario.getId() != null && usuario.getId().equals(usuarioId);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getDireccion() { return direccion; }
    public String getCiudad() { return ciudad; }
}
