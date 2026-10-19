package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.model.aggregates.Pago;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.repositories.PagoRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.PagoEntity;
import org.springframework.stereotype.Repository;

/** Los pagos no se modifican nunca: cada intento es un registro nuevo (historial inmutable). */
@Repository
public class PagoRepositoryAdapter implements PagoRepository {

    private final PagoJpaRepository jpa;
    private final ReservaJpaRepository reservaJpa;

    public PagoRepositoryAdapter(PagoJpaRepository jpa, ReservaJpaRepository reservaJpa) {
        this.jpa = jpa;
        this.reservaJpa = reservaJpa;
    }

    @Override
    public Pago guardar(Pago pago) {
        if (pago.getId() != null) {
            throw new IllegalStateException("Un pago registrado no se puede modificar");
        }
        PagoEntity entidad = new PagoEntity();
        entidad.setReserva(reservaJpa.getReferenceById(pago.getReservaId()));
        entidad.setMonto(pago.getMonto().monto());
        entidad.setMetodo(pago.getMetodo());
        entidad.setEstado(pago.getEstado());
        entidad.setReferenciaTransaccion(pago.getReferenciaTransaccion());
        entidad.setFechaPago(pago.getFechaPago());
        PagoEntity guardado = jpa.saveAndFlush(entidad);
        return Pago.reconstruir(guardado.getId(), pago.getReservaId(), Dinero.de(guardado.getMonto()),
                guardado.getMetodo(), guardado.getEstado(), guardado.getReferenciaTransaccion(), guardado.getFechaPago());
    }
}
