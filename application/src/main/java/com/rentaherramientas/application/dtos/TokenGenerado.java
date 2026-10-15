package com.rentaherramientas.application.dtos;

import java.time.Instant;

public record TokenGenerado(String token, Instant expiraEn) {
}
