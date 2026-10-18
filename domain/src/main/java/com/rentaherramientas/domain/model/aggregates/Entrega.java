package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.TipoEntrega;

import java.time.LocalDateTime;

/**
 * Entrega del equipo al cliente (una por reserva). Es un registro histórico: una vez creado no cambia.
 *
 * <p>Vive en el dominio porque contiene una regla de negocio: si la entrega es a domicilio,
 * la dirección es obligatoria (la misma regla está como CHECK en la tabla, como segunda línea de defensa).</p>
 */
public class Entrega {

    private final Long id;
    private final Long reservaId;
    private final TipoEntrega tipo;
    private final String direccion;
    private final LocalDateTime fecha;
    private final Long registradaPor;
    private final String observaciones;

    private Entrega(Long id, Long reservaId, TipoEntrega tipo, String direccion, LocalDateTime fecha,
                    Long registradaPor, String observaciones) {
        if (reservaId == null || tipo == null || fecha == null || registradaPor == null) {
            throw new ReglaDeNegocioException("La entrega requiere reserva, tipo, fecha y usuario que la registra");
        }
        String direccionLimpia = ReglaDeNegocioException.opcional(direccion);
        if (tipo.requiereDireccion() && direccionLimpia == null) {
            throw new ReglaDeNegocioException("La dirección es obligatoria para las entregas a domicilio");
        }
        this.id = id;
        this.reservaId = reservaId;
        this.tipo = tipo;
        // En recogida en sede la dirección no aplica: se descarta para no guardar datos engañosos
        this.direccion = tipo.requiereDireccion() ? direccionLimpia : null;
        this.fecha = fecha;
        this.registradaPor = registradaPor;
        this.observaciones = ReglaDeNegocioException.opcional(observaciones);
    }

    /** Registra la entrega de una reserva. La reserva debe haber pasado ya a ENTREGADA (lo valida la Reserva). */
    public static Entrega registrar(Reserva reserva, TipoEntrega tipo, String direccion, String observaciones,
                                    Long usuarioId, LocalDateTime ahora) {
        return new Entrega(null, reserva.getId(), tipo, direccion, ahora, usuarioId, observaciones);
    }

    public static Entrega reconstruir(Long id, Long reservaId, TipoEntrega tipo, String direccion, LocalDateTime fecha,
                                      Long registradaPor, String observaciones) {
        return new Entrega(id, reservaId, tipo, direccion, fecha, registradaPor, observaciones);
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public TipoEntrega getTipo() { return tipo; }
    public String getDireccion() { return direccion; }
    public LocalDateTime getFecha() { return fecha; }
    public Long getRegistradaPor() { return registradaPor; }
    public String getObservaciones() { return observaciones; }
}
