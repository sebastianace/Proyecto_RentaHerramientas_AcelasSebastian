package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.FiltroCatalogo;
import com.rentaherramientas.application.dtos.HerramientaResult;
import com.rentaherramientas.application.dtos.Solicitante;
import com.rentaherramientas.application.dtos.SubirImagenCommand;
import com.rentaherramientas.application.ports.in.GestionHerramientasUseCase;
import com.rentaherramientas.domain.model.valueobject.EstadoHerramienta;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.ActualizarHerramientaRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.CambiarEstadoHerramientaRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.CrearHerramientaRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/herramientas")
@Tag(name = "Herramientas", description = "Catálogo público e inventario de cada proveedor, con carga de imágenes")
public class HerramientaController {

    private final GestionHerramientasUseCase gestionHerramientas;

    public HerramientaController(GestionHerramientasUseCase gestionHerramientas) {
        this.gestionHerramientas = gestionHerramientas;
    }

    // ---------- Catálogo público ----------

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Buscar en el catálogo", description = "Público. Solo herramientas activas; todos los filtros son opcionales")
    public Pagina<HerramientaResult> catalogo(
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Texto a buscar en nombre o marca") @RequestParam(required = false) String texto,
            @Parameter(description = "Ciudad del proveedor") @RequestParam(required = false) String ciudad,
            @Parameter(description = "Precio máximo por día") @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int tamanio) {
        return gestionHerramientas.listarCatalogo(new FiltroCatalogo(categoriaId, texto, ciudad, precioMaximo),
                new SolicitudPagina(pagina, tamanio));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Detalle de una herramienta", description = "Público si está activa; si no, solo su proveedor o un ADMIN")
    @ApiResponse(responseCode = "404", description = "La herramienta no existe o no está disponible")
    public HerramientaResult obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionHerramientas.obtener(id, solicitante(usuario));
    }

    // ---------- Inventario ----------

    @GetMapping("/inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Inventario", description = "PROVEEDOR: sus herramientas en cualquier estado. ADMIN: todas, o las de un proveedor")
    public Pagina<HerramientaResult> inventario(
            @Parameter(description = "Solo ADMIN: filtrar por proveedor") @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) EstadoHerramienta estado,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionHerramientas.listarInventario(proveedorId, estado, new SolicitudPagina(pagina, tamanio),
                usuario.aSolicitante());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Agregar herramienta al inventario")
    @ApiResponse(responseCode = "201", description = "Herramienta creada")
    public ResponseEntity<HerramientaResult> crear(@Valid @RequestBody CrearHerramientaRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        HerramientaResult creada = gestionHerramientas.crear(request.aCommand(), usuario.aSolicitante());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.id()).toUri()).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Actualizar herramienta", description = "Incluye precio, stock y unidades fuera de servicio")
    public HerramientaResult actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarHerramientaRequest request,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionHerramientas.actualizar(id, request.aCommand(), usuario.aSolicitante());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Cambiar estado (ACTIVA, EN_MANTENIMIENTO, INACTIVA)")
    public HerramientaResult cambiarEstado(@PathVariable Long id,
                                           @Valid @RequestBody CambiarEstadoHerramientaRequest request,
                                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionHerramientas.cambiarEstado(id, request.estado(), usuario.aSolicitante());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Retirar herramienta", description = "Borrado lógico: pasa a INACTIVA y conserva su historial")
    @ApiResponse(responseCode = "204", description = "Herramienta retirada")
    public ResponseEntity<Void> inactivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        gestionHerramientas.inactivar(id, usuario.aSolicitante());
        return ResponseEntity.noContent().build();
    }

    // ---------- Imágenes ----------

    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Subir imagen", description = "JPG, PNG o WEBP de máximo 5 MB. Máximo 8 imágenes por herramienta")
    @ApiResponse(responseCode = "422", description = "Formato no permitido o límite de imágenes alcanzado")
    public HerramientaResult subirImagen(@PathVariable Long id,
                                         @RequestPart("archivo") MultipartFile archivo,
                                         @Parameter(description = "Marcarla como imagen principal")
                                         @RequestParam(defaultValue = "false") boolean principal,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) throws IOException {
        SubirImagenCommand command = new SubirImagenCommand(id, archivo.getContentType(), archivo.getSize(),
                archivo.getInputStream(), principal);
        return gestionHerramientas.subirImagen(command, usuario.aSolicitante());
    }

    @PatchMapping("/{id}/imagenes/{imagenId}/principal")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Marcar imagen como principal")
    public HerramientaResult marcarPrincipal(@PathVariable Long id, @PathVariable Long imagenId,
                                             @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionHerramientas.marcarImagenPrincipal(id, imagenId, usuario.aSolicitante());
    }

    @DeleteMapping("/{id}/imagenes/{imagenId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Eliminar imagen")
    public HerramientaResult eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionHerramientas.eliminarImagen(id, imagenId, usuario.aSolicitante());
    }

    private static Solicitante solicitante(UsuarioAutenticado usuario) {
        return usuario == null ? null : usuario.aSolicitante();
    }
}
