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

import java.math.BigDecimal;

@Entity
@Table(name = "reserva_detalles")
public class ReservaDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaEntity reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "herramienta_id", nullable = false)
    private HerramientaEntity herramienta;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_por_dia", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPorDia;

    @Column(nullable = false)
    private int dias;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public ReservaEntity getReserva() { return reserva; }
    public void setReserva(ReservaEntity reserva) { this.reserva = reserva; }
    public HerramientaEntity getHerramienta() { return herramienta; }
    public void setHerramienta(HerramientaEntity herramienta) { this.herramienta = herramienta; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioPorDia() { return precioPorDia; }
    public void setPrecioPorDia(BigDecimal precioPorDia) { this.precioPorDia = precioPorDia; }
    public int getDias() { return dias; }
    public void setDias(int dias) { this.dias = dias; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
