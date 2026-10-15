package com.rentaherramientas.infrastructure.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "herramienta_imagenes")
public class HerramientaImagenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "herramienta_id", nullable = false)
    private HerramientaEntity herramienta;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private boolean principal;

    @Column(nullable = false)
    private int orden;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public HerramientaEntity getHerramienta() { return herramienta; }
    public void setHerramienta(HerramientaEntity herramienta) { this.herramienta = herramienta; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
