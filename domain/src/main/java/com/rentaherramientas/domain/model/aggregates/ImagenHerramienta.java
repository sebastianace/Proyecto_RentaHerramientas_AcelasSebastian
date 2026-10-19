package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;

/**
 * Imagen de una herramienta. Es una entidad interna del agregado Herramienta:
 * no existe sola y solo se modifica a través de la herramienta.
 */
public class ImagenHerramienta {

    private final Long id;
    private final String url;
    private boolean principal;
    private final int orden;

    private ImagenHerramienta(Long id, String url, boolean principal, int orden) {
        this.id = id;
        this.url = ReglaDeNegocioException.requerido(url, "url");
        this.principal = principal;
        this.orden = orden;
    }

    static ImagenHerramienta nueva(String url, boolean principal, int orden) {
        return new ImagenHerramienta(null, url, principal, orden);
    }

    public static ImagenHerramienta reconstruir(Long id, String url, boolean principal, int orden) {
        return new ImagenHerramienta(id, url, principal, orden);
    }

    /** Solo la herramienta (mismo paquete) puede cambiar cuál imagen es la principal. */
    void setPrincipal(boolean principal) {
        this.principal = principal;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public boolean isPrincipal() { return principal; }
    public int getOrden() { return orden; }
}
