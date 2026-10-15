package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ActualizarHerramientaCommand;
import com.rentaherramientas.application.dtos.CrearHerramientaCommand;
import com.rentaherramientas.application.dtos.FiltroCatalogo;
import com.rentaherramientas.application.dtos.HerramientaResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.dtos.SubirImagenCommand;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.in.GestionHerramientasUseCase;
import com.rentaherramientas.application.ports.out.AlmacenamientoArchivosPort;
import com.rentaherramientas.application.ports.out.ConsultaHerramientasPort;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Categoria;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.model.aggregates.Proveedor;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.repositories.CategoriaRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.util.Map;

/**
 * Casos de uso del catálogo de herramientas.
 *
 * <p>Regla de acceso: un PROVEEDOR solo gestiona SUS herramientas; el ADMIN gestiona todas.
 * El catálogo público solo muestra herramientas ACTIVAS.</p>
 */
public class GestionHerramientasService implements GestionHerramientasUseCase {

    /** Tipos de imagen permitidos y su extensión. La extensión sale del tipo, nunca del nombre que manda el usuario. */
    private static final Map<String, String> TIPOS_IMAGEN = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");
    private static final long TAMANIO_MAXIMO_IMAGEN = 5L * 1024 * 1024;

    private final HerramientaRepository herramientaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ConsultaHerramientasPort consultaHerramientas;
    private final AlmacenamientoArchivosPort almacenamiento;

    public GestionHerramientasService(HerramientaRepository herramientaRepository,
                                      CategoriaRepository categoriaRepository,
                                      ProveedorRepository proveedorRepository,
                                      ConsultaHerramientasPort consultaHerramientas,
                                      AlmacenamientoArchivosPort almacenamiento) {
        this.herramientaRepository = herramientaRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.consultaHerramientas = consultaHerramientas;
        this.almacenamiento = almacenamiento;
    }

    // ---------- Catálogo público ----------

    @Override
    public Pagina<HerramientaResult> listarCatalogo(FiltroCatalogo filtro, SolicitudPagina solicitud) {
        return consultaHerramientas.buscarCatalogo(filtro, solicitud);
    }

    @Override
    public HerramientaResult obtener(Long id, Solicitante solicitante) {
        HerramientaResult herramienta = consultaHerramientas.obtener(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Herramienta", id));
        boolean visibleParaTodos = herramienta.estado() == EstadoHerramienta.ACTIVA;
        if (!visibleParaTodos && !puedeVerInventario(herramienta.proveedor().id(), solicitante)) {
            // Para el público, una herramienta retirada simplemente "no existe"
            throw new RecursoNoEncontradoException("Herramienta", id);
        }
        return herramienta;
    }

    // ---------- Inventario ----------

    @Override
    public Pagina<HerramientaResult> listarInventario(Long proveedorId, EstadoHerramienta estado,
                                                      SolicitudPagina solicitud, Solicitante solicitante) {
        Long proveedorFiltrado = solicitante.esAdmin() ? proveedorId : proveedorDe(solicitante).getId();
        return consultaHerramientas.listarInventario(proveedorFiltrado, estado, solicitud);
    }

    @Override
    public HerramientaResult crear(CrearHerramientaCommand cmd, Solicitante solicitante) {
        Long proveedorId = resolverProveedorParaCrear(cmd.proveedorId(), solicitante);
        validarCategoriaActiva(cmd.categoriaId());
        Herramienta herramienta = Herramienta.nueva(proveedorId, cmd.categoriaId(), cmd.nombre(), cmd.descripcion(),
                cmd.marca(), cmd.modelo(), Dinero.de(cmd.precioPorDia()), cmd.stockTotal());
        return vista(herramientaRepository.guardar(herramienta));
    }

    @Override
    public HerramientaResult actualizar(Long id, ActualizarHerramientaCommand cmd, Solicitante solicitante) {
        Herramienta herramienta = buscarParaModificar(id, solicitante);
        if (!cmd.categoriaId().equals(herramienta.getCategoriaId())) {
            validarCategoriaActiva(cmd.categoriaId());
        }
        herramienta.actualizar(cmd.categoriaId(), cmd.nombre(), cmd.descripcion(), cmd.marca(), cmd.modelo(),
                Dinero.de(cmd.precioPorDia()), cmd.stockTotal(), cmd.unidadesFueraServicio());
        return vista(herramientaRepository.guardar(herramienta));
    }

    @Override
    public HerramientaResult cambiarEstado(Long id, EstadoHerramienta estado, Solicitante solicitante) {
        Herramienta herramienta = buscarParaModificar(id, solicitante);
        herramienta.cambiarEstado(estado);
        return vista(herramientaRepository.guardar(herramienta));
    }

    @Override
    public void inactivar(Long id, Solicitante solicitante) {
        Herramienta herramienta = buscarParaModificar(id, solicitante);
        herramienta.inactivar();
        herramientaRepository.guardar(herramienta);
    }

    // ---------- Imágenes ----------

    @Override
    public HerramientaResult subirImagen(SubirImagenCommand cmd, Solicitante solicitante) {
        Herramienta herramienta = buscarParaModificar(cmd.herramientaId(), solicitante);
        String extension = TIPOS_IMAGEN.get(cmd.contentType());
        if (extension == null) {
            throw new ReglaDeNegocioException("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        if (cmd.tamanio() <= 0 || cmd.tamanio() > TAMANIO_MAXIMO_IMAGEN) {
            throw new ReglaDeNegocioException("La imagen debe pesar entre 1 byte y 5 MB");
        }
        if (herramienta.getImagenes().size() >= Herramienta.MAXIMO_IMAGENES) {
            throw new ReglaDeNegocioException("Una herramienta puede tener máximo " + Herramienta.MAXIMO_IMAGENES + " imágenes");
        }
        String url = almacenamiento.guardar("herramientas/" + herramienta.getId(), extension, cmd.contenido());
        herramienta.agregarImagen(url, cmd.principal());
        return vista(herramientaRepository.guardar(herramienta));
    }

    @Override
    public HerramientaResult eliminarImagen(Long herramientaId, Long imagenId, Solicitante solicitante) {
        Herramienta herramienta = buscarParaModificar(herramientaId, solicitante);
        String url = herramienta.quitarImagen(imagenId);
        HerramientaResult resultado = vista(herramientaRepository.guardar(herramienta));
        almacenamiento.eliminar(url);
        return resultado;
    }

    @Override
    public HerramientaResult marcarImagenPrincipal(Long herramientaId, Long imagenId, Solicitante solicitante) {
        Herramienta herramienta = buscarParaModificar(herramientaId, solicitante);
        herramienta.marcarImagenPrincipal(imagenId);
        return vista(herramientaRepository.guardar(herramienta));
    }

    // ---------- Apoyo ----------

    private HerramientaResult vista(Herramienta herramienta) {
        return consultaHerramientas.obtener(herramienta.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Herramienta", herramienta.getId()));
    }

    /** Carga la herramienta y verifica que quien la modifica sea su proveedor o un administrador. */
    private Herramienta buscarParaModificar(Long id, Solicitante solicitante) {
        Herramienta herramienta = herramientaRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Herramienta", id));
        if (!solicitante.esAdmin() && !herramienta.perteneceA(proveedorDe(solicitante).getId())) {
            throw new AccesoDenegadoException("Solo puedes modificar las herramientas de tu inventario");
        }
        return herramienta;
    }

    private Long resolverProveedorParaCrear(Long proveedorIdSolicitado, Solicitante solicitante) {
        if (!solicitante.esAdmin()) {
            return proveedorDe(solicitante).getId();
        }
        if (proveedorIdSolicitado == null) {
            throw new ReglaDeNegocioException("Como administrador debes indicar el proveedorId de la herramienta");
        }
        Proveedor proveedor = proveedorRepository.buscarPorId(proveedorIdSolicitado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", proveedorIdSolicitado));
        if (!proveedor.getUsuario().isActivo()) {
            throw new ReglaDeNegocioException("El proveedor está inactivo");
        }
        return proveedor.getId();
    }

    private Proveedor proveedorDe(Solicitante solicitante) {
        return proveedorRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("Tu usuario no tiene un perfil de proveedor"));
    }

    private boolean puedeVerInventario(Long proveedorId, Solicitante solicitante) {
        if (solicitante == null) {
            return false;
        }
        return solicitante.esAdmin() || proveedorRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .map(p -> p.getId().equals(proveedorId))
                .orElse(false);
    }

    private void validarCategoriaActiva(Long categoriaId) {
        if (categoriaId == null) {
            throw new ReglaDeNegocioException("La categoría es obligatoria");
        }
        Categoria categoria = categoriaRepository.buscarPorId(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", categoriaId));
        if (!categoria.isActiva()) {
            throw new ReglaDeNegocioException("La categoría '" + categoria.getNombre() + "' está inactiva");
        }
    }
}
