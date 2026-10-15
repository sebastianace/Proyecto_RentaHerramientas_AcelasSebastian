package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Herramienta del inventario de un proveedor. Raíz del agregado que contiene sus imágenes.
 *
 * <p>Invariantes que protege:</p>
 * <ul>
 *   <li>El precio por día es mayor que cero.</li>
 *   <li>Las unidades fuera de servicio nunca superan el stock total.</li>
 *   <li>Máximo {@value #MAXIMO_IMAGENES} imágenes, y si hay imágenes exactamente una es la principal.</li>
 *   <li>El proveedor dueño no cambia nunca.</li>
 * </ul>
 */
public class Herramienta {

    public static final int MAXIMO_IMAGENES = 8;

    private final Long id;
    private final Long proveedorId;
    private Long categoriaId;
    private String nombre;
    private String descripcion;
    private String marca;
    private String modelo;
    private Dinero precioPorDia;
    private int stockTotal;
    private int unidadesFueraServicio;
    private EstadoHerramienta estado;
    private final List<ImagenHerramienta> imagenes;

    private Herramienta(Long id, Long proveedorId, Long categoriaId, String nombre, String descripcion, String marca,
                        String modelo, Dinero precioPorDia, int stockTotal, int unidadesFueraServicio,
                        EstadoHerramienta estado, List<ImagenHerramienta> imagenes) {
        if (proveedorId == null) {
            throw new ReglaDeNegocioException("La herramienta debe pertenecer a un proveedor");
        }
        this.id = id;
        this.proveedorId = proveedorId;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.imagenes = new ArrayList<>(imagenes);
        asignarDatos(categoriaId, nombre, descripcion, marca, modelo, precioPorDia, stockTotal, unidadesFueraServicio);
    }

    public static Herramienta nueva(Long proveedorId, Long categoriaId, String nombre, String descripcion, String marca,
                                    String modelo, Dinero precioPorDia, int stockTotal) {
        if (stockTotal < 1) {
            throw new ReglaDeNegocioException("Una herramienta nueva debe tener al menos una unidad en stock");
        }
        return new Herramienta(null, proveedorId, categoriaId, nombre, descripcion, marca, modelo, precioPorDia,
                stockTotal, 0, EstadoHerramienta.ACTIVA, List.of());
    }

    public static Herramienta reconstruir(Long id, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                                          String marca, String modelo, Dinero precioPorDia, int stockTotal,
                                          int unidadesFueraServicio, EstadoHerramienta estado,
                                          List<ImagenHerramienta> imagenes) {
        return new Herramienta(id, proveedorId, categoriaId, nombre, descripcion, marca, modelo, precioPorDia,
                stockTotal, unidadesFueraServicio, estado, imagenes);
    }

    public void actualizar(Long categoriaId, String nombre, String descripcion, String marca, String modelo,
                           Dinero precioPorDia, int stockTotal, int unidadesFueraServicio) {
        asignarDatos(categoriaId, nombre, descripcion, marca, modelo, precioPorDia, stockTotal, unidadesFueraServicio);
    }

    public void cambiarEstado(EstadoHerramienta nuevoEstado) {
        this.estado = Objects.requireNonNull(nuevoEstado, "estado");
    }

    /** Borrado lógico: deja de aparecer en el catálogo pero conserva su historial de reservas. */
    public void inactivar() {
        this.estado = EstadoHerramienta.INACTIVA;
    }

    // ---------- Imágenes ----------

    public void agregarImagen(String url, boolean principal) {
        if (imagenes.size() >= MAXIMO_IMAGENES) {
            throw new ReglaDeNegocioException("Una herramienta puede tener máximo " + MAXIMO_IMAGENES + " imágenes");
        }
        boolean seraPrincipal = principal || imagenes.isEmpty();
        if (seraPrincipal) {
            imagenes.forEach(imagen -> imagen.setPrincipal(false));
        }
        int orden = imagenes.stream().mapToInt(ImagenHerramienta::getOrden).max().orElse(-1) + 1;
        imagenes.add(ImagenHerramienta.nueva(url, seraPrincipal, orden));
    }

    /** Quita la imagen y devuelve su URL (para borrar el archivo). Si era la principal, la siguiente pasa a serlo. */
    public String quitarImagen(Long imagenId) {
        ImagenHerramienta imagen = buscarImagen(imagenId);
        imagenes.remove(imagen);
        if (imagen.isPrincipal() && !imagenes.isEmpty()) {
            imagenes.get(0).setPrincipal(true);
        }
        return imagen.getUrl();
    }

    public void marcarImagenPrincipal(Long imagenId) {
        ImagenHerramienta elegida = buscarImagen(imagenId);
        imagenes.forEach(imagen -> imagen.setPrincipal(imagen == elegida));
    }

    // ---------- Consultas de negocio ----------

    /** Unidades que físicamente se pueden alquilar (sin contar las dañadas o en mantenimiento). */
    public int unidadesOperativas() {
        return stockTotal - unidadesFueraServicio;
    }

    public boolean admiteReservas() {
        return estado == EstadoHerramienta.ACTIVA && unidadesOperativas() > 0;
    }

    public boolean perteneceA(Long proveedorId) {
        return this.proveedorId.equals(proveedorId);
    }

    private ImagenHerramienta buscarImagen(Long imagenId) {
        return imagenes.stream()
                .filter(imagen -> Objects.equals(imagen.getId(), imagenId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", imagenId));
    }

    private void asignarDatos(Long categoriaId, String nombre, String descripcion, String marca, String modelo,
                              Dinero precioPorDia, int stockTotal, int unidadesFueraServicio) {
        if (categoriaId == null) {
            throw new ReglaDeNegocioException("La categoría es obligatoria");
        }
        if (precioPorDia == null || !precioPorDia.esPositivo()) {
            throw new ReglaDeNegocioException("El precio por día debe ser mayor que cero");
        }
        if (stockTotal < 0) {
            throw new ReglaDeNegocioException("El stock no puede ser negativo");
        }
        if (unidadesFueraServicio < 0 || unidadesFueraServicio > stockTotal) {
            throw new ReglaDeNegocioException("Las unidades fuera de servicio deben estar entre 0 y el stock total ("
                    + stockTotal + ")");
        }
        this.categoriaId = categoriaId;
        this.nombre = ReglaDeNegocioException.requerido(nombre, "nombre");
        this.descripcion = ReglaDeNegocioException.opcional(descripcion);
        this.marca = ReglaDeNegocioException.opcional(marca);
        this.modelo = ReglaDeNegocioException.opcional(modelo);
        this.precioPorDia = precioPorDia;
        this.stockTotal = stockTotal;
        this.unidadesFueraServicio = unidadesFueraServicio;
    }

    public Long getId() { return id; }
    public Long getProveedorId() { return proveedorId; }
    public Long getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Dinero getPrecioPorDia() { return precioPorDia; }
    public int getStockTotal() { return stockTotal; }
    public int getUnidadesFueraServicio() { return unidadesFueraServicio; }
    public EstadoHerramienta getEstado() { return estado; }
    public List<ImagenHerramienta> getImagenes() { return List.copyOf(imagenes); }
}
