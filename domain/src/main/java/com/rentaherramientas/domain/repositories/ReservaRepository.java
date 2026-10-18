package com.rentaherramientas.domain.repositories;

import com.rentaherramientas.domain.model.aggregates.Reserva;
import com.rentaherramientas.domain.model.valueobject.RangoFechas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository {

    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorId(Long id);

    /**
     * Carga la reserva bloqueando su fila hasta el fin de la transacción (SELECT ... FOR UPDATE).
     * Evita que un doble clic en "Pagar" cobre dos veces la misma reserva.
     */
    Optional<Reserva> bloquearPorId(Long id);

    /**
     * Suma las unidades de una herramienta comprometidas en reservas que ocupan inventario
     * (APROBADA, PAGADA, ENTREGADA) y cuyas fechas se cruzan con el rango.
     *
     * @param reservaExcluida reserva que no se cuenta (la que se está evaluando); puede ser null
     */
    int unidadesComprometidas(Long herramientaId, RangoFechas rango, Long reservaExcluida);

    /** Reservas APROBADAS cuya fecha límite de pago ya pasó. */
    List<Reserva> buscarAprobadasConPagoVencido(LocalDateTime ahora);

    /** Siguiente código legible, ej. RES-2026-0036. */
    String generarCodigo(int anio);
}
