package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.EntregaResult;
import com.rentaherramientas.application.dtos.FiltroDevoluciones;
import com.rentaherramientas.application.dtos.FiltroIncidencias;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.application.ports.out.ConsultaDevolucionesPort;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.DevolucionEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.DevolucionesEntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lado de lectura de entregas, devoluciones y daños. Cada consulta trae las relaciones con
 * {@code @EntityGraph}; los daños de una página de devoluciones se cargan con UNA consulta IN (sin N+1).
 */
@Repository
public class ConsultaDevolucionesAdapter implements ConsultaDevolucionesPort {

    private final EntregaJpaRepository entregaJpa;
    private final DevolucionJpaRepository devolucionJpa;
    private final ReporteDanoJpaRepository reporteJpa;

    public ConsultaDevolucionesAdapter(EntregaJpaRepository entregaJpa, DevolucionJpaRepository devolucionJpa,
                                       ReporteDanoJpaRepository reporteJpa) {
        this.entregaJpa = entregaJpa;
        this.devolucionJpa = devolucionJpa;
        this.reporteJpa = reporteJpa;
    }

    @Override
    public Optional<EntregaResult> entregaDeReserva(Long reservaId) {
        return entregaJpa.findByReservaId(reservaId).map(DevolucionesEntityMapper::aResult);
    }

    @Override
    public Optional<DevolucionResult> obtenerDevolucion(Long id) {
        return devolucionJpa.findConRelacionesById(id).map(d -> conDanos(List.of(d)).get(0));
    }

    @Override
    public Optional<DevolucionResult> devolucionDeReserva(Long reservaId) {
        return devolucionJpa.findByReservaId(reservaId).map(d -> conDanos(List.of(d)).get(0));
    }

    @Override
    public Pagina<DevolucionResult> listarDevoluciones(FiltroDevoluciones filtro, SolicitudPagina solicitud) {
        PageRequest pagina = PageRequest.of(solicitud.pagina(), solicitud.tamanio(), Sort.by(Sort.Direction.DESC, "id"));
        Page<DevolucionEntity> page = devolucionJpa.buscar(filtro.validada(), filtro.clienteId(), filtro.proveedorId(),
                pagina);
        return new Pagina<>(conDanos(page.getContent()), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public Optional<ReporteDanoResult> obtenerReporte(Long id) {
        return reporteJpa.findConRelacionesById(id).map(DevolucionesEntityMapper::aResult);
    }

    @Override
    public Pagina<ReporteDanoResult> listarReportes(FiltroIncidencias filtro, SolicitudPagina solicitud) {
        PageRequest pagina = PageRequest.of(solicitud.pagina(), solicitud.tamanio(), Sort.by(Sort.Direction.DESC, "id"));
        return PaginaMapper.aPagina(reporteJpa.buscar(filtro.estado(), filtro.severidad(), filtro.clienteId(),
                filtro.proveedorId(), pagina), DevolucionesEntityMapper::aResult);
    }

    /** Arma cada devolución con sus daños, trayendo los daños de todas en una sola consulta. */
    private List<DevolucionResult> conDanos(List<DevolucionEntity> devoluciones) {
        if (devoluciones.isEmpty()) {
            return List.of();
        }
        List<Long> ids = devoluciones.stream().map(DevolucionEntity::getId).toList();
        Map<Long, List<ReporteDanoResult>> danosPorDevolucion = reporteJpa.findByDevolucionIdInOrderByIdAsc(ids)
                .stream().map(DevolucionesEntityMapper::aResult)
                .collect(Collectors.groupingBy(ReporteDanoResult::devolucionId));
        return devoluciones.stream()
                .map(d -> DevolucionesEntityMapper.aResult(d, danosPorDevolucion.getOrDefault(d.getId(), List.of())))
                .toList();
    }
}
