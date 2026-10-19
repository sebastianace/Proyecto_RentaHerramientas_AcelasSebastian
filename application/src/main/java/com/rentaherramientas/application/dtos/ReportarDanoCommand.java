package com.rentaherramientas.application.dtos;

import com.rentaherramientas.domain.model.valueobject.SeveridadDano;

import java.math.BigDecimal;

/** Un daño encontrado en una de las herramientas devueltas. */
public record ReportarDanoCommand(Long herramientaId, String descripcion, SeveridadDano severidad,
                                  BigDecimal costoEstimado) {
}
