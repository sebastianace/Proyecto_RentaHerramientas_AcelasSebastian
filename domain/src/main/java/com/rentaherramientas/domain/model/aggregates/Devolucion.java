package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.EstadoEquipo;

import java.time.LocalDateTime;

/**
 * Devolución del equipo (una por reserva). La confirma el PROVEEDOR al recibirlo y después
 * el ADMIN la valida (control de calidad): son dos responsables distintos, por eso hay dos pasos.
 */
public class Devolucion {

    private final Long id;
    private final Long reservaId;
    private final LocalDateTime fecha;
    private final EstadoEquipo estadoEquipo;
    private final String observaciones;
    private final Long recibidaPor;
    private boolean validada;
    private Long validadaPor;
    private LocalDateTime fechaValidacion;

    private Devolucion(Long id, Long reservaId, LocalDateTime fecha, EstadoEquipo estadoEquipo, String observaciones,
                       Long recibidaPor, boolean validada, Long validadaPor, LocalDateTime fechaValidacion) {
        if (reservaId == null || fecha == null || estadoEquipo == null || recibidaPor == null) {
            throw new ReglaDeNegocioException("La devolución requiere reserva, fecha, estado del equipo y quién la recibe");
        }
        this.id = id;
        this.reservaId = reservaId;
        this.fecha = fecha;
        this.estadoEquipo = estadoEquipo;
        this.observaciones = ReglaDeNegocioException.opcional(observaciones);
        this.recibidaPor = recibidaPor;
        this.validada = validada;
        this.validadaPor = validadaPor;
        this.fechaValidacion = fechaValidacion;
    }

    /** El proveedor confirma que recibió el equipo. La reserva ya debe estar DEVUELTA (lo valida la Reserva). */
    public static Devolucion confirmar(Reserva reserva, EstadoEquipo estadoEquipo, String observaciones,
                                       Long usuarioId, LocalDateTime ahora) {
        return new Devolucion(null, reserva.getId(), ahora, estadoEquipo, observaciones, usuarioId, false, null, null);
    }

    public static Devolucion reconstruir(Long id, Long reservaId, LocalDateTime fecha, EstadoEquipo estadoEquipo,
                                         String observaciones, Long recibidaPor, boolean validada, Long validadaPor,
                                         LocalDateTime fechaValidacion) {
        return new Devolucion(id, reservaId, fecha, estadoEquipo, observaciones, recibidaPor, validada, validadaPor,
                fechaValidacion);
    }

    /** El ADMIN revisa la devolución y la da por buena. Solo se valida una vez. */
    public void validar(Long adminUsuarioId, LocalDateTime ahora) {
        if (validada) {
            throw new ReglaDeNegocioException("La devolución ya fue validada el " + fechaValidacion);
        }
        this.validada = true;
        this.validadaPor = adminUsuarioId;
        this.fechaValidacion = ahora;
    }

    public boolean admiteReporteDeDano() {
        return estadoEquipo.admiteReporteDeDano();
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public LocalDateTime getFecha() { return fecha; }
    public EstadoEquipo getEstadoEquipo() { return estadoEquipo; }
    public String getObservaciones() { return observaciones; }
    public Long getRecibidaPor() { return recibidaPor; }
    public boolean isValidada() { return validada; }
    public Long getValidadaPor() { return validadaPor; }
    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
}
