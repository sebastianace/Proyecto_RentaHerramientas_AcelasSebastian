package com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers;

import com.rentaherramientas.application.dtos.HerramientaResult;
import com.rentaherramientas.domain.model.aggregates.Categoria;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.model.aggregates.ImagenHerramienta;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.CategoriaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.HerramientaEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.HerramientaImagenEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CatalogoEntityMapper {

    private CatalogoEntityMapper() {
    }

    // ---------- Categoría ----------

    public static Categoria aDominio(CategoriaEntity e) {
        return Categoria.reconstruir(e.getId(), e.getNombre(), e.getDescripcion(), e.isActiva());
    }

    public static void copiar(Categoria origen, CategoriaEntity destino) {
        destino.setNombre(origen.getNombre());
        destino.setDescripcion(origen.getDescripcion());
        destino.setActiva(origen.isActiva());
    }

    // ---------- Herramienta (lado de escritura) ----------

    public static Herramienta aDominio(HerramientaEntity e) {
        List<ImagenHerramienta> imagenes = e.getImagenes().stream()
                .map(i -> ImagenHerramienta.reconstruir(i.getId(), i.getUrl(), i.isPrincipal(), i.getOrden()))
                .toList();
        return Herramienta.reconstruir(e.getId(), e.getProveedor().getId(), e.getCategoria().getId(), e.getNombre(),
                e.getDescripcion(), e.getMarca(), e.getModelo(), Dinero.de(e.getPrecioPorDia()), e.getStockTotal(),
                e.getUnidadesFueraServicio(), e.getEstado(), imagenes);
    }

    /** Copia los datos simples. Proveedor y categoría los asigna el adaptador (necesita referencias de JPA). */
    public static void copiarDatos(Herramienta origen, HerramientaEntity destino) {
        destino.setNombre(origen.getNombre());
        destino.setDescripcion(origen.getDescripcion());
        destino.setMarca(origen.getMarca());
        destino.setModelo(origen.getModelo());
        destino.setPrecioPorDia(origen.getPrecioPorDia().monto());
        destino.setStockTotal(origen.getStockTotal());
        destino.setUnidadesFueraServicio(origen.getUnidadesFueraServicio());
        destino.setEstado(origen.getEstado());
    }

    /**
     * Sincroniza la lista de imágenes de la entidad con la del dominio:
     * actualiza las que ya existen, agrega las nuevas (sin id) y quita las que ya no están
     * (orphanRemoval las borra de la tabla).
     */
    public static void sincronizarImagenes(Herramienta origen, HerramientaEntity destino) {
        Map<Long, ImagenHerramienta> enDominio = origen.getImagenes().stream()
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(ImagenHerramienta::getId, Function.identity()));
        destino.getImagenes().removeIf(entidad -> !enDominio.containsKey(entidad.getId()));
        destino.getImagenes().forEach(entidad -> {
            ImagenHerramienta imagen = enDominio.get(entidad.getId());
            entidad.setPrincipal(imagen.isPrincipal());
            entidad.setOrden(imagen.getOrden());
        });
        origen.getImagenes().stream().filter(i -> i.getId() == null).forEach(nueva -> {
            HerramientaImagenEntity entidad = new HerramientaImagenEntity();
            entidad.setHerramienta(destino);
            entidad.setUrl(nueva.getUrl());
            entidad.setPrincipal(nueva.isPrincipal());
            entidad.setOrden(nueva.getOrden());
            destino.getImagenes().add(entidad);
        });
    }

    // ---------- Herramienta (lado de lectura) ----------

    public static HerramientaResult aResult(HerramientaEntity e) {
        List<HerramientaResult.ImagenResult> imagenes = e.getImagenes().stream()
                .map(i -> new HerramientaResult.ImagenResult(i.getId(), i.getUrl(), i.isPrincipal(), i.getOrden()))
                .toList();
        String principal = imagenes.stream().filter(HerramientaResult.ImagenResult::principal)
                .map(HerramientaResult.ImagenResult::url).findFirst().orElse(null);
        return new HerramientaResult(
                e.getId(), e.getNombre(), e.getDescripcion(), e.getMarca(), e.getModelo(), e.getPrecioPorDia(),
                e.getStockTotal(), e.getUnidadesFueraServicio(), e.getStockTotal() - e.getUnidadesFueraServicio(),
                e.getEstado(),
                new HerramientaResult.CategoriaResumen(e.getCategoria().getId(), e.getCategoria().getNombre()),
                new HerramientaResult.ProveedorResumen(e.getProveedor().getId(), e.getProveedor().getRazonSocial(),
                        e.getProveedor().getCiudad()),
                principal, imagenes);
    }
}
