package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

/** Categoría del catálogo (Taladros, Andamios...). La gestiona el administrador. */
public class Categoria {

    private final Long id;
    private String nombre;
    private String descripcion;
    private boolean activa;

    private Categoria(Long id, String nombre, String descripcion, boolean activa) {
        this.id = id;
        this.nombre = ReglaDeNegocioException.requerido(nombre, "nombre");
        this.descripcion = ReglaDeNegocioException.opcional(descripcion);
        this.activa = activa;
    }

    public static Categoria nueva(String nombre, String descripcion) {
        return new Categoria(null, nombre, descripcion, true);
    }

    public static Categoria reconstruir(Long id, String nombre, String descripcion, boolean activa) {
        return new Categoria(id, nombre, descripcion, activa);
    }

    public void actualizar(String nombre, String descripcion) {
        this.nombre = ReglaDeNegocioException.requerido(nombre, "nombre");
        this.descripcion = ReglaDeNegocioException.opcional(descripcion);
    }

    public void desactivar() {
        this.activa = false;
    }

    public void activar() {
        this.activa = true;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isActiva() { return activa; }
}
