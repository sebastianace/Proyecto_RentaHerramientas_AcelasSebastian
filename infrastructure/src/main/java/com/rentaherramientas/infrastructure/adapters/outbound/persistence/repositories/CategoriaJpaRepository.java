package com.rentaherramientas.infrastructure.adapters.outbound.persistence.repositories;

import com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<CategoriaEntity> findByActivaTrueOrderByNombreAsc();

    List<CategoriaEntity> findAllByOrderByNombreAsc();
}
