package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.ClienteResult;
import com.rentaherramientas.application.ports.in.GestionClientesUseCase;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.ActualizarClienteRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RegistroClienteRequest;
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
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "CRUD de clientes. El ADMIN gestiona a todos; cada CLIENTE solo su propio perfil")
public class ClienteController {

    private final GestionClientesUseCase gestionClientes;

    public ClienteController(GestionClientesUseCase gestionClientes) {
        this.gestionClientes = gestionClientes;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar clientes (paginado)")
    public Pagina<ClienteResult> listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return gestionClientes.listar(new SolicitudPagina(pagina, tamanio));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @Operation(summary = "Obtener un cliente", description = "Un CLIENTE solo puede consultar su propio perfil")
    @ApiResponse(responseCode = "404", description = "El cliente no existe")
    public ClienteResult obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionClientes.obtener(id, usuario.aSolicitante());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un cliente (administrador)")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @ApiResponse(responseCode = "409", description = "Email o documento ya registrados")
    public ResponseEntity<ClienteResult> crear(@Valid @RequestBody RegistroClienteRequest request) {
        ClienteResult creado = gestionClientes.crear(request.aCommand());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @Operation(summary = "Actualizar un cliente", description = "Un CLIENTE solo puede editar su propio perfil")
    public ClienteResult actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarClienteRequest request,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionClientes.actualizar(id, request.aCommand(), usuario.aSolicitante());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar un cliente", description = "Borrado lógico: conserva su historial pero ya no puede iniciar sesión")
    @ApiResponse(responseCode = "204", description = "Cliente desactivado")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        gestionClientes.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar un cliente")
    @ApiResponse(responseCode = "204", description = "Cliente activado")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        gestionClientes.activar(id);
        return ResponseEntity.noContent().build();
    }
}
