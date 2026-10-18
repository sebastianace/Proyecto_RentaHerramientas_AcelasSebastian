package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.FiltroPagos;
import com.rentaherramientas.application.dtos.PagoRealizadoResult;
import com.rentaherramientas.application.dtos.PagoResult;
import com.rentaherramientas.application.ports.in.GestionPagosUseCase;
import com.rentaherramientas.domain.model.valueobject.EstadoPago;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.PagarReservaRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Tag(name = "Pagos", description = "Pago en línea de reservas aprobadas e historial de pagos")
public class PagoController {

    private final GestionPagosUseCase gestionPagos;

    public PagoController(GestionPagosUseCase gestionPagos) {
        this.gestionPagos = gestionPagos;
    }

    @PostMapping("/api/v1/reservas/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Pagar una reserva aprobada",
            description = "Si el pago se aprueba, la reserva pasa a PAGADA y la factura se genera automáticamente. "
                    + "Si se rechaza, el intento queda registrado y la reserva sigue APROBADA para reintentar")
    @ApiResponse(responseCode = "201", description = "Intento de pago registrado (revisa el campo 'aprobado')")
    @ApiResponse(responseCode = "422", description = "La reserva no está APROBADA o su plazo de pago venció")
    public PagoRealizadoResult pagar(@PathVariable Long id, @Valid @RequestBody PagarReservaRequest request,
                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionPagos.pagar(id, request.aCommand(), usuario.aSolicitante());
    }

    @GetMapping("/api/v1/pagos")
    @Operation(summary = "Historial de pagos",
            description = "CLIENTE: sus pagos. PROVEEDOR: pagos de sus reservas. ADMIN: todos (monitoreo de pagos)")
    public Pagina<PagoResult> listar(
            @RequestParam(required = false) EstadoPago estado,
            @Parameter(description = "Desde (inclusive)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Hasta (inclusive)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionPagos.listarPagos(new FiltroPagos(estado, clienteId, proveedorId, desde, hasta),
                new SolicitudPagina(pagina, tamanio), usuario.aSolicitante());
    }
}
