package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;

import java.math.BigDecimal;
import java.util.List;

/** Vista de una herramienta para el catálogo y el inventario, con su categoría, proveedor e imágenes. */
public record HerramientaResult(
        Long id,
        String nombre,
        String descripcion,
        String marca,
        String modelo,
        BigDecimal precioPorDia,
        int stockTotal,
        int unidadesFueraServicio,
        int unidadesOperativas,
        EstadoHerramienta estado,
        CategoriaResumen categoria,
        ProveedorResumen proveedor,
        String imagenPrincipal,
        List<ImagenResult> imagenes) {

    public record CategoriaResumen(Long id, String nombre) {
    }

    public record ProveedorResumen(Long id, String razonSocial, String ciudad) {
    }

    public record ImagenResult(Long id, String url, boolean principal, int orden) {
    }
}
