package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.CategoriaResult;
import com.rentaherramientas.application.ports.in.GestionCategoriasUseCase;
import com.rentaherramientas.domain.model.valueobject.RolNombre;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.CategoriaRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Consulta pública; creación y edición solo para ADMIN")
public class CategoriaController {

    private final GestionCategoriasUseCase gestionCategorias;

    public CategoriaController(GestionCategoriasUseCase gestionCategorias) {
        this.gestionCategorias = gestionCategorias;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar categorías", description = "Público: solo activas. Un ADMIN puede pedir también las inactivas")
    public List<CategoriaResult> listar(
            @Parameter(description = "Solo tiene efecto para ADMIN")
            @RequestParam(defaultValue = "false") boolean incluirInactivas,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        boolean esAdmin = usuario != null && usuario.roles().contains(RolNombre.ADMIN);
        return gestionCategorias.listar(incluirInactivas && esAdmin);
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Obtener una categoría")
    @ApiResponse(responseCode = "404", description = "La categoría no existe")
    public CategoriaResult obtener(@PathVariable Long id) {
        return gestionCategorias.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una categoría")
    @ApiResponse(responseCode = "201", description = "Categoría creada")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    public ResponseEntity<CategoriaResult> crear(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResult creada = gestionCategorias.crear(request.aCommand());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.id()).toUri()).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una categoría")
    public CategoriaResult actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return gestionCategorias.actualizar(id, request.aCommand());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar una categoría", description = "Deja de aparecer en el catálogo; sus herramientas se conservan")
    @ApiResponse(responseCode = "204", description = "Categoría desactivada")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        gestionCategorias.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar una categoría")
    @ApiResponse(responseCode = "204", description = "Categoría activada")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        gestionCategorias.activar(id);
        return ResponseEntity.noContent().build();
    }
}
