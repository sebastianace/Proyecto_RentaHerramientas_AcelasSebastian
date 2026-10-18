package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.CotizacionResult;
import com.rentaherramientas.application.dtos.FiltroReservas;
import com.rentaherramientas.application.dtos.ReservaResult;
import com.rentaherramientas.application.ports.in.GestionReservasUseCase;
import com.rentaherramientas.domain.model.valueobject.EstadoReserva;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.CrearReservaRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RechazarReservaRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "Reservas", description = "Solicitud, aprobación, rechazo y cancelación de alquileres. Cada rol ve solo sus reservas")
public class ReservaController {

    private final GestionReservasUseCase gestionReservas;

    public ReservaController(GestionReservasUseCase gestionReservas) {
        this.gestionReservas = gestionReservas;
    }

    @PostMapping("/cotizacion")
    @Operation(summary = "Cotizar una reserva", description = "Calcula días, subtotal, IVA y total sin guardar nada")
    @ApiResponse(responseCode = "422", description = "Fechas inválidas o herramientas de distintos proveedores")
    public CotizacionResult cotizar(@Valid @RequestBody CrearReservaRequest request) {
        return gestionReservas.cotizar(request.aCommand());
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Solicitar una reserva", description = "Valida disponibilidad y queda PENDIENTE hasta que el proveedor la apruebe")
    @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDIENTE")
    @ApiResponse(responseCode = "409", description = "No hay unidades suficientes para esas fechas")
    @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida (fechas, proveedor, herramienta inactiva)")
    public ResponseEntity<ReservaResult> crear(@Valid @RequestBody CrearReservaRequest request,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ReservaResult creada = gestionReservas.crear(request.aCommand(), usuario.aSolicitante());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.id()).toUri()).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar reservas (historial)",
            description = "CLIENTE: sus reservas. PROVEEDOR: las solicitudes de su inventario. ADMIN: todas, con filtros opcionales")
    public Pagina<ReservaResult> listar(
            @RequestParam(required = false) EstadoReserva estado,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionReservas.listar(new FiltroReservas(clienteId, proveedorId, estado),
                new SolicitudPagina(pagina, tamanio), usuario.aSolicitante());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una reserva")
    @ApiResponse(responseCode = "403", description = "La reserva no es tuya")
    @ApiResponse(responseCode = "404", description = "La reserva no existe")
    public ReservaResult obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionReservas.obtener(id, usuario.aSolicitante());
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('PROVEEDOR', 'ADMIN')")
    @Operation(summary = "Aprobar una reserva",
            description = "Revalida la disponibilidad con las filas bloqueadas, aparta las unidades y fija la fecha límite de pago")
    @ApiResponse(responseCode = "409", description = "Las unidades ya fueron comprometidas en otra reserva")
    public ReservaResult aprobar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionReservas.aprobar(id, usuario.aSolicitante());
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('PROVEEDOR', 'ADMIN')")
    @Operation(summary = "Rechazar una reserva")
    public ReservaResult rechazar(@PathVariable Long id, @Valid @RequestBody RechazarReservaRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionReservas.rechazar(id, request.motivo(), usuario.aSolicitante());
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Cancelar una reserva", description = "Solo antes de pagar (PENDIENTE o APROBADA)")
    public ReservaResult cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionReservas.cancelar(id, usuario.aSolicitante());
    }
}
