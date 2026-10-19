package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.application.ports.out.ConsultaFacturasPort;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.FacturacionEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConsultaFacturasAdapter implements ConsultaFacturasPort {

    private final FacturaJpaRepository jpa;

    public ConsultaFacturasAdapter(FacturaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<FacturaResult> obtener(Long id) {
        return jpa.findConRelacionesById(id).map(FacturacionEntityMapper::aResult);
    }

    @Override
    public Pagina<FacturaResult> listar(Long clienteId, Long proveedorId, SolicitudPagina solicitud) {
        PageRequest pagina = PageRequest.of(solicitud.pagina(), solicitud.tamanio(), Sort.by(Sort.Direction.DESC, "id"));
        return PaginaMapper.aPagina(jpa.buscar(clienteId, proveedorId, pagina), FacturacionEntityMapper::aResult);
    }
}
