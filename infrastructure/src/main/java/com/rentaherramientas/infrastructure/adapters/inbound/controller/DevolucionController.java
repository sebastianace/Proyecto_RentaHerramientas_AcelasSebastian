package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.DevolucionResult;
import com.rentaherramientas.application.dtos.EntregaResult;
import com.rentaherramientas.application.dtos.FiltroDevoluciones;
import com.rentaherramientas.application.dtos.FiltroIncidencias;
import com.rentaherramientas.application.dtos.ReporteDanoResult;
import com.rentaherramientas.application.ports.in.GestionDevolucionesUseCase;
import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.CambiarEstadoIncidenciaRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.ConfirmarDevolucionRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RegistrarEntregaRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.ReportarDanoRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entregas, devoluciones, daños e incidencias. Como todos los controladores: recibe, valida el formato,
 * llama al caso de uso y responde. Las reglas (estados, propiedad del recurso) están en el caso de uso y el dominio.
 */
@RestController
@Tag(name = "Entregas y devoluciones", description = "Entrega del equipo, devolución, reportes de daño e incidencias")
public class DevolucionController {

    private final GestionDevolucionesUseCase gestion;

    public DevolucionController(GestionDevolucionesUseCase gestion) {
        this.gestion = gestion;
    }

    // ---------- Entrega ----------

    @PostMapping("/api/v1/reservas/{id}/entrega")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    @Operation(summary = "Registrar la entrega del equipo", description = "La reserva PAGADA pasa a ENTREGADA")
    @ApiResponse(responseCode = "201", description = "Entrega registrada")
    @ApiResponse(responseCode = "409", description = "La reserva ya tiene una entrega")
    @ApiResponse(responseCode = "422", description = "La reserva no está PAGADA o falta la dirección de domicilio")
    public EntregaResult registrarEntrega(@PathVariable Long id, @Valid @RequestBody RegistrarEntregaRequest request,
                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.registrarEntrega(id, request.aCommand(), usuario.aSolicitante());
    }

    @GetMapping("/api/v1/reservas/{id}/entrega")
    @Operation(summary = "Ver la entrega de una reserva", description = "Dueños de la reserva y ADMIN")
    public EntregaResult obtenerEntrega(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.obtenerEntrega(id, usuario.aSolicitante());
    }

    // ---------- Devolución ----------

    @PostMapping("/api/v1/reservas/{id}/devolucion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    @Operation(summary = "Confirmar la devolución del equipo",
            description = "La reserva ENTREGADA pasa a DEVUELTA. Se pueden reportar daños en la misma petición; "
                    + "un daño GRAVE saca de servicio una unidad de la herramienta")
    @ApiResponse(responseCode = "201", description = "Devolución confirmada")
    @ApiResponse(responseCode = "422", description = "La reserva no está ENTREGADA, o se reportan daños con equipo BUENO")
    public DevolucionResult confirmarDevolucion(@PathVariable Long id,
                                                @Valid @RequestBody ConfirmarDevolucionRequest request,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.confirmarDevolucion(id, request.aCommand(), usuario.aSolicitante());
    }

    @GetMapping("/api/v1/reservas/{id}/devolucion")
    @Operation(summary = "Ver la devolución de una reserva", description = "Dueños de la reserva y ADMIN")
    public DevolucionResult obtenerDevolucionDeReserva(@PathVariable Long id,
                                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.obtenerDevolucionDeReserva(id, usuario.aSolicitante());
    }

    @GetMapping("/api/v1/devoluciones")
    @Operation(summary = "Listar devoluciones",
            description = "CLIENTE: las suyas. PROVEEDOR: las de su inventario. ADMIN: todas (supervisión)")
    public Pagina<DevolucionResult> listarDevoluciones(
            @Parameter(description = "true = ya validadas por el ADMIN, false = pendientes")
            @RequestParam(required = false) Boolean validada,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.listarDevoluciones(new FiltroDevoluciones(validada, clienteId, proveedorId),
                new SolicitudPagina(pagina, tamanio), usuario.aSolicitante());
    }

    @GetMapping("/api/v1/devoluciones/{id}")
    @Operation(summary = "Detalle de una devolución con sus daños")
    public DevolucionResult obtenerDevolucion(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.obtenerDevolucion(id, usuario.aSolicitante());
    }

    @PatchMapping("/api/v1/devoluciones/{id}/validar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Validar una devolución", description = "El ADMIN revisa y da por buena la devolución")
    @ApiResponse(responseCode = "422", description = "La devolución ya estaba validada")
    public DevolucionResult validarDevolucion(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.validarDevolucion(id, usuario.aSolicitante());
    }

    // ---------- Daños e incidencias ----------

    @PostMapping("/api/v1/devoluciones/{id}/danos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROVEEDOR','ADMIN')")
    @Operation(summary = "Reportar un daño de una devolución",
            description = "Un daño GRAVE saca de servicio una unidad de la herramienta")
    @ApiResponse(responseCode = "422", description = "Equipo devuelto en buen estado o herramienta ajena a la reserva")
    public ReporteDanoResult reportarDano(@PathVariable Long id, @Valid @RequestBody ReportarDanoRequest request,
                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.reportarDano(id, request.aCommand(), usuario.aSolicitante());
    }

    @GetMapping("/api/v1/danos")
    @Operation(summary = "Listar reportes de daño (incidencias)",
            description = "CLIENTE: de sus alquileres. PROVEEDOR: de su inventario. ADMIN: todos")
    public Pagina<ReporteDanoResult> listarIncidencias(
            @RequestParam(required = false) EstadoIncidencia estado,
            @RequestParam(required = false) SeveridadDano severidad,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Solo ADMIN") @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.listarIncidencias(new FiltroIncidencias(estado, severidad, clienteId, proveedorId),
                new SolicitudPagina(pagina, tamanio), usuario.aSolicitante());
    }

    @PatchMapping("/api/v1/danos/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Avanzar el estado de una incidencia",
            description = "ABIERTO → EN_REVISION → RESUELTO. Resolver un daño GRAVE reintegra la unidad reparada")
    @ApiResponse(responseCode = "422", description = "Transición no permitida")
    public ReporteDanoResult cambiarEstadoIncidencia(@PathVariable Long id,
                                                     @Valid @RequestBody CambiarEstadoIncidenciaRequest request,
                                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestion.cambiarEstadoIncidencia(id, request.estado(), usuario.aSolicitante());
    }
}
