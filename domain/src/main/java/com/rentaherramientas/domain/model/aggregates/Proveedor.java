package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.RolNombre;

/**
 * Perfil de proveedor: empresa que publica su inventario de herramientas.
 *
 * <p>Raíz del agregado que contiene a su {@link Usuario}. El NIT identifica a la
 * empresa y no se puede modificar después del registro.</p>
 */
public class Proveedor {

    private final Long id;
    private final Usuario usuario;
    private String razonSocial;
    private final String nit;
    private String direccion;
    private String ciudad;
    private String descripcion;

    private Proveedor(Long id, Usuario usuario, String razonSocial, String nit, String direccion,
                      String ciudad, String descripcion) {
        if (usuario == null || !usuario.tieneRol(RolNombre.PROVEEDOR)) {
            throw new ReglaDeNegocioException("Un proveedor debe tener un usuario con rol PROVEEDOR");
        }
        this.id = id;
        this.usuario = usuario;
        this.razonSocial = ReglaDeNegocioException.requerido(razonSocial, "razonSocial");
        this.nit = ReglaDeNegocioException.requerido(nit, "nit");
        this.direccion = ReglaDeNegocioException.requerido(direccion, "direccion");
        this.ciudad = ReglaDeNegocioException.requerido(ciudad, "ciudad");
        this.descripcion = ReglaDeNegocioException.opcional(descripcion);
    }

    public static Proveedor nuevo(Usuario usuario, String razonSocial, String nit, String direccion,
                                  String ciudad, String descripcion) {
        return new Proveedor(null, usuario, razonSocial, nit, direccion, ciudad, descripcion);
    }

    public static Proveedor reconstruir(Long id, Usuario usuario, String razonSocial, String nit, String direccion,
                                        String ciudad, String descripcion) {
        return new Proveedor(id, usuario, razonSocial, nit, direccion, ciudad, descripcion);
    }

    public void actualizar(String nombres, String apellidos, String telefono, String razonSocial,
                           String direccion, String ciudad, String descripcion) {
        usuario.actualizarDatosPersonales(nombres, apellidos, telefono);
        this.razonSocial = ReglaDeNegocioException.requerido(razonSocial, "razonSocial");
        this.direccion = ReglaDeNegocioException.requerido(direccion, "direccion");
        this.ciudad = ReglaDeNegocioException.requerido(ciudad, "ciudad");
        this.descripcion = ReglaDeNegocioException.opcional(descripcion);
    }

    public void desactivar() {
        usuario.desactivar();
    }

    public void activar() {
        usuario.activar();
    }

    public boolean perteneceA(Long usuarioId) {
        return usuario.getId() != null && usuario.getId().equals(usuarioId);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getRazonSocial() { return razonSocial; }
    public String getNit() { return nit; }
    public String getDireccion() { return direccion; }
    public String getCiudad() { return ciudad; }
    public String getDescripcion() { return descripcion; }
}
