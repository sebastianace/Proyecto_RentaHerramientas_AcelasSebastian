package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.model.aggregates.ReporteDano;
import com.rentaherramientas.domain.repositories.ReporteDanoRepository;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.ReporteDanoEntity;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.DevolucionesEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ReporteDanoRepositoryAdapter implements ReporteDanoRepository {

    private final ReporteDanoJpaRepository jpa;
    private final DevolucionJpaRepository devolucionJpa;
    private final HerramientaJpaRepository herramientaJpa;
    private final UsuarioJpaRepository usuarioJpa;

    public ReporteDanoRepositoryAdapter(ReporteDanoJpaRepository jpa, DevolucionJpaRepository devolucionJpa,
                                        HerramientaJpaRepository herramientaJpa, UsuarioJpaRepository usuarioJpa) {
        this.jpa = jpa;
        this.devolucionJpa = devolucionJpa;
        this.herramientaJpa = herramientaJpa;
        this.usuarioJpa = usuarioJpa;
    }

    @Override
    public ReporteDano guardar(ReporteDano reporte) {
        ReporteDanoEntity entidad;
        if (reporte.getId() == null) {
            entidad = new ReporteDanoEntity();
            entidad.setDevolucion(devolucionJpa.getReferenceById(reporte.getDevolucionId()));
            entidad.setHerramienta(herramientaJpa.getReferenceById(reporte.getHerramientaId()));
            entidad.setDescripcion(reporte.getDescripcion());
            entidad.setSeveridad(reporte.getSeveridad());
            entidad.setCostoEstimado(reporte.getCostoEstimado().monto());
            entidad.setReportadoPor(usuarioJpa.getReferenceById(reporte.getReportadoPor()));
        } else {
            entidad = jpa.findById(reporte.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Reporte de daño", reporte.getId()));
        }
        // Lo único que cambia después de creado es el estado de la incidencia
        entidad.setEstado(reporte.getEstado());
        return DevolucionesEntityMapper.aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Optional<ReporteDano> buscarPorId(Long id) {
        return jpa.findConRelacionesById(id).map(DevolucionesEntityMapper::aDominio);
    }
}
