package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.application.dtos.FiltroCatalogo;
import com.rentaherramientas.application.dtos.HerramientaResult;
import com.rentaherramientas.application.ports.out.ConsultaHerramientasPort;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.outbound.persistence.mappers.CatalogoEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador del puerto de consulta del catálogo: arma las vistas directamente desde las entidades JPA. */
@Repository
public class ConsultaHerramientasAdapter implements ConsultaHerramientasPort {

    private final HerramientaJpaRepository jpa;

    public ConsultaHerramientasAdapter(HerramientaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<HerramientaResult> obtener(Long id) {
        return jpa.findConRelacionesById(id).map(CatalogoEntityMapper::aResult);
    }

    @Override
    public Pagina<HerramientaResult> buscarCatalogo(FiltroCatalogo filtro, SolicitudPagina solicitud) {
        return PaginaMapper.aPagina(jpa.buscarCatalogo(EstadoHerramienta.ACTIVA, filtro.categoriaId(), filtro.texto(),
                filtro.ciudad(), filtro.precioMaximo(), PaginaMapper.aPageRequest(solicitud)),
                CatalogoEntityMapper::aResult);
    }

    @Override
    public Pagina<HerramientaResult> listarInventario(Long proveedorId, EstadoHerramienta estado,
                                                      SolicitudPagina solicitud) {
        return PaginaMapper.aPagina(jpa.buscarInventario(proveedorId, estado, PaginaMapper.aPageRequest(solicitud)),
                CatalogoEntityMapper::aResult);
    }
}
