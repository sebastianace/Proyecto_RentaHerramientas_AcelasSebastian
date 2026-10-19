package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.application.dtos.FiltroPagos;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.application.ports.out.ConsultaPagosPort;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.FacturacionEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ConsultaPagosAdapter implements ConsultaPagosPort {

    private final PagoJpaRepository jpa;

    public ConsultaPagosAdapter(PagoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PagoResult> obtener(Long id) {
        return jpa.findConRelacionesById(id).map(FacturacionEntityMapper::aResult);
    }

    /** "hasta" se incluye completo: se busca hasta el inicio del día siguiente. */
    @Override
    public Pagina<PagoResult> listar(FiltroPagos filtro, SolicitudPagina solicitud) {
        LocalDateTime desde = filtro.desde() == null ? null : filtro.desde().atStartOfDay();
        LocalDateTime hasta = filtro.hasta() == null ? null : filtro.hasta().plusDays(1).atStartOfDay();
        PageRequest pagina = PageRequest.of(solicitud.pagina(), solicitud.tamanio(), Sort.by(Sort.Direction.DESC, "fechaPago"));
        return PaginaMapper.aPagina(jpa.buscar(filtro.estado(), filtro.clienteId(), filtro.proveedorId(), desde, hasta,
                pagina), FacturacionEntityMapper::aResult);
    }
}
