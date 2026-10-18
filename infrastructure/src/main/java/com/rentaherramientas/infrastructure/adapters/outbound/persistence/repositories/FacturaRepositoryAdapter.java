package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.Factura;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.repositories.FacturaRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.FacturaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class FacturaRepositoryAdapter implements FacturaRepository {

    private final FacturaJpaRepository jpa;
    private final PagoJpaRepository pagoJpa;

    public FacturaRepositoryAdapter(FacturaJpaRepository jpa, PagoJpaRepository pagoJpa) {
        this.jpa = jpa;
        this.pagoJpa = pagoJpa;
    }

    @Override
    public Factura guardar(Factura factura) {
        FacturaEntity entidad = factura.getId() == null
                ? new FacturaEntity()
                : jpa.findById(factura.getId())
                     .orElseThrow(() -> new RecursoNoEncontradoException("Factura", factura.getId()));
        entidad.setNumero(factura.getNumero());
        entidad.setPago(pagoJpa.getReferenceById(factura.getPagoId()));
        entidad.setFechaEmision(factura.getFechaEmision());
        entidad.setSubtotal(factura.getSubtotal().monto());
        entidad.setIva(factura.getIva().monto());
        entidad.setTotal(factura.getTotal().monto());
        entidad.setEstado(factura.getEstado());
        return aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Optional<Factura> buscarPorId(Long id) {
        return jpa.findById(id).map(FacturaRepositoryAdapter::aDominio);
    }

    @Override
    public String generarNumero(int anio) {
        return String.format("FAC-%d-%06d", anio, jpa.buscarMaximoId() + 1);
    }

    private static Factura aDominio(FacturaEntity e) {
        return Factura.reconstruir(e.getId(), e.getNumero(), e.getPago().getId(), e.getFechaEmision(),
                Dinero.de(e.getSubtotal()), Dinero.de(e.getIva()), Dinero.de(e.getTotal()), e.getEstado());
    }
}
