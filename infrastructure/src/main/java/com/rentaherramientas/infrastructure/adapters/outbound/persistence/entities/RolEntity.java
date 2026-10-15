package com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities;

import com.rentaherramientas.domain.model.valueobject.RolNombre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * @JdbcTypeCode(VARCHAR): desde Hibernate 6.2, en MySQL los enum se esperan como columna ENUM nativa.
     * Nuestra columna es VARCHAR + CHECK, así que se lo indicamos para que ddl-auto=validate no falle.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, length = 30)
    private RolNombre nombre;

    protected RolEntity() {
    }

    public Long getId() { return id; }
    public RolNombre getNombre() { return nombre; }
}
