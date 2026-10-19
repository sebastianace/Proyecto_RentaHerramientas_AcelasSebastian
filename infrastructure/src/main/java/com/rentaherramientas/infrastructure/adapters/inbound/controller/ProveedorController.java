package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.ProveedorResult;
import com.rentaherramientas.application.ports.in.GestionProveedoresUseCase;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.ActualizarProveedorRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RegistroProveedorRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/proveedores")
@Tag(name = "Proveedores", description = "CRUD de proveedores. Solo el ADMIN los registra; cada PROVEEDOR gestiona su propio perfil")
public class ProveedorController {

    private final GestionProveedoresUseCase gestionProveedores;

    public ProveedorController(GestionProveedoresUseCase gestionProveedores) {
        this.gestionProveedores = gestionProveedores;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar proveedores (paginado)")
    public Pagina<ProveedorResult> listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return gestionProveedores.listar(new SolicitudPagina(pagina, tamanio));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Obtener un proveedor", description = "Un PROVEEDOR solo puede consultar su propio perfil")
    @ApiResponse(responseCode = "404", description = "El proveedor no existe")
    public ProveedorResult obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionProveedores.obtener(id, usuario.aSolicitante());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un proveedor (administrador)")
    @ApiResponse(responseCode = "201", description = "Proveedor creado")
    @ApiResponse(responseCode = "409", description = "Email o NIT ya registrados")
    public ResponseEntity<ProveedorResult> crear(@Valid @RequestBody RegistroProveedorRequest request) {
        ProveedorResult creado = gestionProveedores.crear(request.aCommand());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    @Operation(summary = "Actualizar un proveedor", description = "Un PROVEEDOR solo puede editar su propio perfil")
    public ProveedorResult actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProveedorRequest request,
                                      @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionProveedores.actualizar(id, request.aCommand(), usuario.aSolicitante());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar un proveedor", description = "Borrado lógico: conserva su inventario e historial")
    @ApiResponse(responseCode = "204", description = "Proveedor desactivado")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        gestionProveedores.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar un proveedor")
    @ApiResponse(responseCode = "204", description = "Proveedor activado")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        gestionProveedores.activar(id);
        return ResponseEntity.noContent().build();
    }
}
