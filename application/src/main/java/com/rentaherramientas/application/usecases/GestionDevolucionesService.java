package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.dtos.ConfirmarDevolucionCommand;
import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.EntregaResult;
import com.rentaherramientas.application.dtos.FiltroDevoluciones;
import com.rentaherramientas.application.dtos.FiltroIncidencias;
import com.rentaherramientas.application.dtos.RegistrarEntregaCommand;
import com.rentaherramientas.application.dtos.ReportarDanoCommand;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.application.dtos.ReservaReferencia;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.in.GestionDevolucionesUseCase;
import com.rentaherramientas.application.ports.out.ConsultaDevolucionesPort;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.Devolucion;
import com.rentaherramientas.domain.model.aggregates.Entrega;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.model.aggregates.ReporteDano;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.DevolucionRepository;
import com.rentaherramientas.domain.repositories.EntregaRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.ReporteDanoRepository;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Segunda mitad del alquiler: entrega, devolución, daños e incidencias.
 *
 * <p>Vive en la capa de aplicación porque ORQUESTA varios agregados (Reserva, Entrega, Devolución,
 * ReporteDano, Herramienta) dentro de una transacción y valida quién puede hacer qué; las reglas
 * de cada agregado (estados válidos, dirección obligatoria, orden de las incidencias) están en el dominio.</p>
 *
 * <p><b>Daño GRAVE:</b> saca de servicio una unidad de la herramienta (aumenta unidades_fuera_servicio),
 * así deja de contar para la disponibilidad. Al resolver la incidencia, la unidad reparada vuelve a servicio.
 * La fila de la herramienta se bloquea (SELECT ... FOR UPDATE) para no pisar una aprobación simultánea.</p>
 */
public class GestionDevolucionesService implements GestionDevolucionesUseCase {

    /** Cada reporte de daño grave corresponde a una unidad física dañada. */
    private static final int UNIDADES_POR_DANO_GRAVE = 1;

    private final ReservaRepository reservaRepository;
    private final EntregaRepository entregaRepository;
    private final DevolucionRepository devolucionRepository;
    private final ReporteDanoRepository reporteDanoRepository;
    private final HerramientaRepository herramientaRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final ConsultaDevolucionesPort consulta;
    private final Clock reloj;

    public GestionDevolucionesService(ReservaRepository reservaRepository, EntregaRepository entregaRepository,
                                      DevolucionRepository devolucionRepository,
                                      ReporteDanoRepository reporteDanoRepository,
                                      HerramientaRepository herramientaRepository, ClienteRepository clienteRepository,
                                      ProveedorRepository proveedorRepository, ConsultaDevolucionesPort consulta,
                                      Clock reloj) {
        this.reservaRepository = reservaRepository;
        this.entregaRepository = entregaRepository;
        this.devolucionRepository = devolucionRepository;
        this.reporteDanoRepository = reporteDanoRepository;
        this.herramientaRepository = herramientaRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.consulta = consulta;
        this.reloj = reloj;
    }

    // ---------- Entrega ----------

    @Override
    public EntregaResult registrarEntrega(Long reservaId, RegistrarEntregaCommand cmd, Solicitante solicitante) {
        // Bloqueo de la reserva: un doble clic en "Entregar" no crea dos entregas
        Reserva reserva = reservaRepository.bloquearPorId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        validarEsProveedorDe(reserva, solicitante);
        if (entregaRepository.existePorReserva(reservaId)) {
            throw new ConflictoException("La reserva " + reserva.getCodigo() + " ya tiene una entrega registrada");
        }
        // Primero se construye la entrega (valida la dirección) y solo después se cambia el estado de la reserva:
        // así una petición inválida no deja la reserva a medio modificar.
        Entrega entrega = Entrega.registrar(reserva, cmd.tipo(), cmd.direccion(), cmd.observaciones(),
                solicitante.usuarioId(), ahora());
        reserva.registrarEntrega();
        reservaRepository.guardar(reserva);
        entregaRepository.guardar(entrega);
        return consulta.entregaDeReserva(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega de la reserva", reservaId));
    }

    @Override
    public EntregaResult obtenerEntrega(Long reservaId, Solicitante solicitante) {
        EntregaResult entrega = consulta.entregaDeReserva(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega de la reserva", reservaId));
        validarPuedeVer(entrega.reserva(), solicitante);
        return entrega;
    }

    // ---------- Devolución ----------

    @Override
    public DevolucionResult confirmarDevolucion(Long reservaId, ConfirmarDevolucionCommand cmd,
                                                Solicitante solicitante) {
        Reserva reserva = reservaRepository.bloquearPorId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        validarEsProveedorDe(reserva, solicitante);
        // Se valida ANTES de modificar nada: una petición inválida no debe dejar la reserva a medio cambiar.
        if (!cmd.danos().isEmpty() && cmd.estadoEquipo() != null && !cmd.estadoEquipo().admiteReporteDeDano()) {
            throw new ReglaDeNegocioException("Si reportas daños, el estado del equipo no puede ser BUENO");
        }
        reserva.registrarDevolucion();
        reservaRepository.guardar(reserva);
        Devolucion devolucion = devolucionRepository.guardar(Devolucion.confirmar(reserva, cmd.estadoEquipo(),
                cmd.observaciones(), solicitante.usuarioId(), ahora()));
        for (ReportarDanoCommand dano : cmd.danos()) {
            registrarDano(devolucion, reserva, dano, solicitante);
        }
        return vistaDevolucion(devolucion.getId());
    }

    @Override
    public DevolucionResult obtenerDevolucion(Long id, Solicitante solicitante) {
        DevolucionResult devolucion = vistaDevolucion(id);
        validarPuedeVer(devolucion.reserva(), solicitante);
        return devolucion;
    }

    @Override
    public DevolucionResult obtenerDevolucionDeReserva(Long reservaId, Solicitante solicitante) {
        DevolucionResult devolucion = consulta.devolucionDeReserva(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Devolución de la reserva", reservaId));
        validarPuedeVer(devolucion.reserva(), solicitante);
        return devolucion;
    }

    @Override
    public Pagina<DevolucionResult> listarDevoluciones(FiltroDevoluciones filtro, SolicitudPagina solicitud,
                                                       Solicitante solicitante) {
        FiltroDevoluciones efectivo;
        if (solicitante.esAdmin()) {
            efectivo = filtro;
        } else if (esProveedor(solicitante)) {
            efectivo = new FiltroDevoluciones(filtro.validada(), null, proveedorIdDe(solicitante));
        } else {
            efectivo = new FiltroDevoluciones(filtro.validada(), clienteIdDe(solicitante), null);
        }
        return consulta.listarDevoluciones(efectivo, solicitud);
    }

    @Override
    public DevolucionResult validarDevolucion(Long id, Solicitante solicitante) {
        exigirAdmin(solicitante, "validar devoluciones");
        Devolucion devolucion = devolucionRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Devolución", id));
        devolucion.validar(solicitante.usuarioId(), ahora());
        devolucionRepository.guardar(devolucion);
        return vistaDevolucion(id);
    }

    // ---------- Daños e incidencias ----------

    @Override
    public ReporteDanoResult reportarDano(Long devolucionId, ReportarDanoCommand cmd, Solicitante solicitante) {
        Devolucion devolucion = devolucionRepository.buscarPorId(devolucionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Devolución", devolucionId));
        Reserva reserva = reservaRepository.buscarPorId(devolucion.getReservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", devolucion.getReservaId()));
        validarEsProveedorDe(reserva, solicitante);
        ReporteDano reporte = registrarDano(devolucion, reserva, cmd, solicitante);
        return vistaReporte(reporte.getId());
    }

    @Override
    public Pagina<ReporteDanoResult> listarIncidencias(FiltroIncidencias filtro, SolicitudPagina solicitud,
                                                       Solicitante solicitante) {
        FiltroIncidencias efectivo;
        if (solicitante.esAdmin()) {
            efectivo = filtro;
        } else if (esProveedor(solicitante)) {
            efectivo = new FiltroIncidencias(filtro.estado(), filtro.severidad(), null, proveedorIdDe(solicitante));
        } else {
            efectivo = new FiltroIncidencias(filtro.estado(), filtro.severidad(), clienteIdDe(solicitante), null);
        }
        return consulta.listarReportes(efectivo, solicitud);
    }

    @Override
    public ReporteDanoResult cambiarEstadoIncidencia(Long id, EstadoIncidencia estado, Solicitante solicitante) {
        exigirAdmin(solicitante, "gestionar incidencias");
        ReporteDano reporte = reporteDanoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte de daño", id));
        reporte.avanzarA(estado);
        reporteDanoRepository.guardar(reporte);
        if (reporte.estaResuelto() && reporte.sacaDeServicio()) {
            // La unidad reparada vuelve a estar disponible para alquilar
            Herramienta herramienta = bloquearHerramienta(reporte.getHerramientaId());
            if (herramienta.getUnidadesFueraServicio() >= UNIDADES_POR_DANO_GRAVE) {
                herramienta.reintegrarAServicio(UNIDADES_POR_DANO_GRAVE);
                herramientaRepository.guardar(herramienta);
            }
        }
        return vistaReporte(id);
    }

    // ---------- Apoyo ----------

    private ReporteDano registrarDano(Devolucion devolucion, Reserva reserva, ReportarDanoCommand cmd,
                                      Solicitante solicitante) {
        Dinero costo = cmd.costoEstimado() == null ? Dinero.CERO : Dinero.de(cmd.costoEstimado());
        ReporteDano reporte = reporteDanoRepository.guardar(ReporteDano.reportar(devolucion, reserva,
                cmd.herramientaId(), cmd.descripcion(), cmd.severidad(), costo, solicitante.usuarioId()));
        if (reporte.sacaDeServicio()) {
            Herramienta herramienta = bloquearHerramienta(reporte.getHerramientaId());
            // Si ya no quedan unidades operativas, no hay nada más que sacar (el reporte igual queda registrado)
            if (herramienta.unidadesOperativas() >= UNIDADES_POR_DANO_GRAVE) {
                herramienta.sacarDeServicio(UNIDADES_POR_DANO_GRAVE);
                herramientaRepository.guardar(herramienta);
            }
        }
        return reporte;
    }

    private Herramienta bloquearHerramienta(Long herramientaId) {
        return herramientaRepository.bloquearParaReserva(List.of(herramientaId)).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Herramienta", herramientaId));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private DevolucionResult vistaDevolucion(Long id) {
        return consulta.obtenerDevolucion(id).orElseThrow(() -> new RecursoNoEncontradoException("Devolución", id));
    }

    private ReporteDanoResult vistaReporte(Long id) {
        return consulta.obtenerReporte(id).orElseThrow(() -> new RecursoNoEncontradoException("Reporte de daño", id));
    }

    private void exigirAdmin(Solicitante solicitante, String accion) {
        if (!solicitante.esAdmin()) {
            throw new AccesoDenegadoException("Solo un administrador puede " + accion);
        }
    }

    private void validarEsProveedorDe(Reserva reserva, Solicitante solicitante) {
        if (!solicitante.esAdmin() && !reserva.perteneceAlProveedor(proveedorIdDe(solicitante))) {
            throw new AccesoDenegadoException("Solo el proveedor de la reserva puede registrar su entrega, "
                    + "devolución o daños");
        }
    }

    private void validarPuedeVer(ReservaReferencia reserva, Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return;
        }
        boolean esSuya = esProveedor(solicitante)
                ? reserva.proveedorId().equals(proveedorIdDe(solicitante))
                : reserva.clienteId().equals(clienteIdDe(solicitante));
        if (!esSuya) {
            throw new AccesoDenegadoException("No tienes acceso a esta reserva");
        }
    }

    private boolean esProveedor(Solicitante solicitante) {
        return solicitante.roles().contains(RolNombre.PROVEEDOR);
    }

    private Long clienteIdDe(Solicitante solicitante) {
        return clienteRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("Tu usuario no tiene un perfil de cliente"))
                .getId();
    }

    private Long proveedorIdDe(Solicitante solicitante) {
        return proveedorRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("Tu usuario no tiene un perfil de proveedor"))
                .getId();
    }
}
