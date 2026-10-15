package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.AutenticacionResult;
import com.rentaherramientas.application.dtos.PerfilResult;
import com.rentaherramientas.application.ports.in.AutenticacionUseCase;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.LoginRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RegistroClienteRequest;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para autenticación. Solo traduce HTTP a casos de uso:
 * no contiene lógica de negocio.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT, registro de clientes y perfil del usuario")
public class AuthController {

    private final AutenticacionUseCase autenticacion;

    public AuthController(AutenticacionUseCase autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Devuelve un JWT que se envía en el header Authorization: Bearer <token>")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos")
    @ApiResponse(responseCode = "403", description = "Usuario inactivo")
    public AutenticacionResult login(@Valid @RequestBody LoginRequest request) {
        return autenticacion.iniciarSesion(request.aCommand());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registrarse como cliente", description = "Crea la cuenta con rol CLIENTE y devuelve el token (queda con sesión iniciada)")
    @ApiResponse(responseCode = "201", description = "Cliente registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "El email o el documento ya están registrados")
    public AutenticacionResult registrar(@Valid @RequestBody RegistroClienteRequest request) {
        return autenticacion.registrarCliente(request.aCommand());
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Incluye el id de cliente o de proveedor según el rol")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario")
    @ApiResponse(responseCode = "401", description = "Token ausente, vencido o inválido")
    public PerfilResult perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return autenticacion.obtenerPerfil(usuario.id());
    }
}
