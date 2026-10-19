package com.rentaherramientas.application.usecases;

import com.rentaherramientas.application.config.PoliticaReservas;
import com.rentaherramientas.application.dtos.CotizacionResult;
import com.rentaherramientas.application.dtos.CrearReservaCommand;
import com.rentaherramientas.application.dtos.DisponibilidadResult;
import com.rentaherramientas.application.dtos.FiltroReservas;
import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.ports.in.GestionReservasUseCase;
import com.rentaherramientas.application.ports.out.ConsultaReservasPort;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.aggregates.DetalleReserva;
import com.rentaherramientas.domain.model.aggregates.Herramienta;
import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.domain.repositories.ClienteRepository;
import com.rentaherramientas.domain.repositories.HerramientaRepository;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.ProveedorRepository;
import com.rentaherramientas.domain.repositories.ReservaRepository;
import com.rentaherramientas.domain.repositories.SolicitudPagina;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Casos de uso de reservas y control de disponibilidad.
 *
 * <p><b>Disponibilidad:</b> unidades operativas de la herramienta − unidades comprometidas en reservas
 * APROBADAS, PAGADAS o ENTREGADAS que se cruzan con las fechas pedidas. Una reserva PENDIENTE
 * todavía no aparta unidades: se valida al crearla (para avisar pronto al cliente) y otra vez,
 * con las filas bloqueadas, al aprobarla (que es cuando de verdad aparta las unidades).</p>
 *
 * <p>Se recibe un {@link Clock} en vez de usar LocalDate.now() directamente: así las pruebas
 * pueden fijar "hoy" en una fecha conocida.</p>
 */
public class GestionReservasService implements GestionReservasUseCase {

    private final ReservaRepository reservaRepository;
    private final HerramientaRepository herramientaRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final ConsultaReservasPort consultaReservas;
    private final PoliticaReservas politica;
    private final Clock reloj;

    public GestionReservasService(ReservaRepository reservaRepository, HerramientaRepository herramientaRepository,
                                  ClienteRepository clienteRepository, ProveedorRepository proveedorRepository,
                                  ConsultaReservasPort consultaReservas, PoliticaReservas politica, Clock reloj) {
        this.reservaRepository = reservaRepository;
        this.herramientaRepository = herramientaRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.consultaReservas = consultaReservas;
        this.politica = politica;
        this.reloj = reloj;
    }

    // ---------- Consultas públicas ----------

    @Override
    public DisponibilidadResult consultarDisponibilidad(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        Herramienta herramienta = herramientaRepository.buscarPorId(herramientaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Herramienta", herramientaId));
        RangoFechas rango = new RangoFechas(fechaInicio, fechaFin);
        int operativas = herramienta.admiteReservas() ? herramienta.unidadesOperativas() : 0;
        int comprometidas = reservaRepository.unidadesComprometidas(herramientaId, rango, null);
        int disponibles = Math.max(0, operativas - comprometidas);
        return new DisponibilidadResult(herramientaId, fechaInicio, fechaFin, operativas, comprometidas, disponibles,
                disponibles > 0);
    }

    @Override
    public CotizacionResult cotizar(CrearReservaCommand cmd) {
        Map<Long, Herramienta> herramientas = cargarHerramientas(cmd);
        Reserva borrador = armarReserva("COTIZACION", 0L, cmd, herramientas);
        List<CotizacionResult.Linea> lineas = borrador.getDetalles().stream().map(d -> new CotizacionResult.Linea(
                d.getHerramientaId(), herramientas.get(d.getHerramientaId()).getNombre(), d.getCantidad(),
                d.getPrecioPorDia().monto(), d.getSubtotal().monto(),
                unidadesDisponibles(herramientas.get(d.getHerramientaId()), borrador.getRango(), null))).toList();
        return new CotizacionResult(borrador.getRango().inicio(), borrador.getRango().fin(), borrador.getRango().dias(),
                lineas, borrador.getSubtotal().monto(), borrador.getIva().monto(), borrador.getTotal().monto());
    }

    // ---------- Cliente ----------

    @Override
    public ReservaResult crear(CrearReservaCommand cmd, Solicitante solicitante) {
        Long clienteId = clienteRepository.buscarPorUsuarioId(solicitante.usuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("Solo los clientes pueden crear reservas"))
                .getId();
        Map<Long, Herramienta> herramientas = cargarHerramientas(cmd);
        String codigo = reservaRepository.generarCodigo(LocalDate.now(reloj).getYear());
        Reserva reserva = armarReserva(codigo, clienteId, cmd, herramientas);
        validarDisponibilidad(reserva, herramientas, "crear");
        return vista(reservaRepository.guardar(reserva));
    }

    @Override
    public ReservaResult cancelar(Long id, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        if (!solicitante.esAdmin() && !reserva.perteneceAlCliente(clienteIdDe(solicitante))) {
            throw new AccesoDenegadoException("Solo puedes cancelar tus propias reservas");
        }
        reserva.cancelar();
        return vista(reservaRepository.guardar(reserva));
    }

    // ---------- Proveedor ----------

    @Override
    public ReservaResult aprobar(Long id, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        validarEsProveedorDe(reserva, solicitante);
        // Bloqueo pesimista: otra aprobación de las mismas herramientas espera a que esta transacción termine
        List<Long> ids = reserva.getDetalles().stream().map(DetalleReserva::getHerramientaId).toList();
        Map<Long, Herramienta> herramientas = herramientaRepository.bloquearParaReserva(ids).stream()
                .collect(Collectors.toMap(Herramienta::getId, Function.identity()));
        for (DetalleReserva detalle : reserva.getDetalles()) {
            Herramienta herramienta = herramientas.get(detalle.getHerramientaId());
            if (!herramienta.admiteReservas()) {
                throw new ReglaDeNegocioException("La herramienta '" + herramienta.getNombre()
                        + "' ya no está disponible para alquiler");
            }
        }
        validarDisponibilidad(reserva, herramientas, "aprobar");
        reserva.aprobar(LocalDateTime.now(reloj), politica.plazoPago());
        return vista(reservaRepository.guardar(reserva));
    }

    @Override
    public ReservaResult rechazar(Long id, String motivo, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        validarEsProveedorDe(reserva, solicitante);
        reserva.rechazar(motivo);
        return vista(reservaRepository.guardar(reserva));
    }

    // ---------- Consultas con control de acceso ----------

    @Override
    public ReservaResult obtener(Long id, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        validarPuedeVer(reserva, solicitante);
        return vista(reserva);
    }

    @Override
    public Pagina<ReservaResult> listar(FiltroReservas filtro, SolicitudPagina solicitud, Solicitante solicitante) {
        FiltroReservas efectivo;
        if (solicitante.esAdmin()) {
            efectivo = filtro;
        } else if (solicitante.roles().contains(RolNombre.PROVEEDOR)) {
            efectivo = new FiltroReservas(filtro.clienteId(), proveedorIdDe(solicitante), filtro.estado());
        } else {
            efectivo = new FiltroReservas(clienteIdDe(solicitante), null, filtro.estado());
        }
        return consultaReservas.listar(efectivo, solicitud);
    }

    // ---------- Proceso automático ----------

    @Override
    public int vencerReservasSinPago() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int vencidas = 0;
        for (Reserva reserva : reservaRepository.buscarAprobadasConPagoVencido(ahora)) {
            if (reserva.vencerSiCorresponde(ahora)) {
                reservaRepository.guardar(reserva);
                vencidas++;
            }
        }
        return vencidas;
    }

    // ---------- Apoyo ----------

    private Map<Long, Herramienta> cargarHerramientas(CrearReservaCommand cmd) {
        if (cmd.herramientas().isEmpty()) {
            throw new ReglaDeNegocioException("La reserva debe incluir al menos una herramienta");
        }
        List<Long> ids = cmd.herramientas().stream().map(CrearReservaCommand.Item::herramientaId).distinct().toList();
        Map<Long, Herramienta> herramientas = herramientaRepository.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Herramienta::getId, Function.identity()));
        ids.stream().filter(id -> !herramientas.containsKey(id)).findFirst().ifPresent(id -> {
            throw new RecursoNoEncontradoException("Herramienta", id);
        });
        return herramientas;
    }

    private Reserva armarReserva(String codigo, Long clienteId, CrearReservaCommand cmd,
                                 Map<Long, Herramienta> herramientas) {
        List<Reserva.LineaSolicitada> lineas = cmd.herramientas().stream()
                .map(item -> new Reserva.LineaSolicitada(herramientas.get(item.herramientaId()), item.cantidad()))
                .toList();
        return Reserva.solicitar(codigo, clienteId, new RangoFechas(cmd.fechaInicio(), cmd.fechaFin()), lineas,
                cmd.notas(), LocalDate.now(reloj), politica.diasMaximosAlquiler());
    }

    private void validarDisponibilidad(Reserva reserva, Map<Long, Herramienta> herramientas, String accion) {
        for (DetalleReserva detalle : reserva.getDetalles()) {
            Herramienta herramienta = herramientas.get(detalle.getHerramientaId());
            int disponibles = unidadesDisponibles(herramienta, reserva.getRango(), reserva.getId());
            if (detalle.getCantidad() > disponibles) {
                throw new ConflictoException("No se puede " + accion + " la reserva: de '" + herramienta.getNombre()
                        + "' hay " + disponibles + " unidad(es) disponible(s) entre " + reserva.getRango().inicio()
                        + " y " + reserva.getRango().fin() + " y se piden " + detalle.getCantidad());
            }
        }
    }

    private int unidadesDisponibles(Herramienta herramienta, RangoFechas rango, Long reservaExcluida) {
        int comprometidas = reservaRepository.unidadesComprometidas(herramienta.getId(), rango, reservaExcluida);
        return Math.max(0, herramienta.unidadesOperativas() - comprometidas);
    }

    private Reserva buscar(Long id) {
        return reservaRepository.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    private ReservaResult vista(Reserva reserva) {
        return consultaReservas.obtener(reserva.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reserva.getId()));
    }

    private void validarEsProveedorDe(Reserva reserva, Solicitante solicitante) {
        if (!solicitante.esAdmin() && !reserva.perteneceAlProveedor(proveedorIdDe(solicitante))) {
            throw new AccesoDenegadoException("Solo el proveedor de la reserva puede aprobarla o rechazarla");
        }
    }

    private void validarPuedeVer(Reserva reserva, Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return;
        }
        boolean esSuya = solicitante.roles().contains(RolNombre.PROVEEDOR)
                ? reserva.perteneceAlProveedor(proveedorIdDe(solicitante))
                : reserva.perteneceAlCliente(clienteIdDe(solicitante));
        if (!esSuya) {
            throw new AccesoDenegadoException("No tienes acceso a esta reserva");
        }
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
