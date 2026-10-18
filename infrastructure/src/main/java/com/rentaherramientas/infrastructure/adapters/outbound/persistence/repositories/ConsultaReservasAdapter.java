package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.application.dtos.FiltroReservas;
import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.application.ports.out.ConsultaReservasPort;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.ReservaEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConsultaReservasAdapter implements ConsultaReservasPort {

    private final ReservaJpaRepository jpa;

    public ConsultaReservasAdapter(ReservaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ReservaResult> obtener(Long id) {
        return jpa.findConRelacionesById(id).map(ReservaEntityMapper::aResult);
    }

    @Override
    public Pagina<ReservaResult> listar(FiltroReservas filtro, SolicitudPagina solicitud) {
        PageRequest pagina = PageRequest.of(solicitud.pagina(), solicitud.tamanio(), Sort.by(Sort.Direction.DESC, "id"));
        return PaginaMapper.aPagina(jpa.buscar(filtro.clienteId(), filtro.proveedorId(), filtro.estado(), pagina),
                ReservaEntityMapper::aResult);
    }
}
