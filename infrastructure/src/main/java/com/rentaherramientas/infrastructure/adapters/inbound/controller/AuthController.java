package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.AutenticacionResult;
import com.rentaherramientas.application.dtos.PerfilResult;
import com.rentaherramientas.application.ports.in.AutenticacionUseCase;
import com.rentaherramientas.application.ports.in.RecuperacionPasswordUseCase;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.CambiarPasswordRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.LoginRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RecuperarPasswordRequest;
import com.rentaherramientas.infrastructure.adapters.inbound.dtos.RestablecerPasswordRequest;
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

import java.util.Map;

/**
 * Adaptador de entrada REST para autenticación. Solo traduce HTTP a casos de uso:
 * no contiene lógica de negocio.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT, registro de clientes y perfil del usuario")
public class AuthController {

    private final AutenticacionUseCase autenticacion;
    private final RecuperacionPasswordUseCase recuperacion;

    public AuthController(AutenticacionUseCase autenticacion, RecuperacionPasswordUseCase recuperacion) {
        this.autenticacion = autenticacion;
        this.recuperacion = recuperacion;
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

    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(summary = "Olvidé mi contraseña: pedir el enlace de recuperación",
            description = "Si el correo pertenece a un usuario activo, se envía un enlace que vence en 30 minutos "
                    + "(en desarrollo el correo se muestra en la consola del servidor). La respuesta es la misma "
                    + "exista o no el correo, para no revelar qué cuentas están registradas")
    @ApiResponse(responseCode = "202", description = "Solicitud recibida")
    @ApiResponse(responseCode = "400", description = "Email con formato inválido")
    public Map<String, String> recuperar(@Valid @RequestBody RecuperarPasswordRequest request) {
        recuperacion.solicitarRecuperacion(request.email());
        return Map.of("mensaje", "Si el correo está registrado, te enviamos un enlace para crear una nueva contraseña");
    }

    @PostMapping("/restablecer")
    @SecurityRequirements
    @Operation(summary = "Crear la contraseña nueva con el token del enlace",
            description = "El token vence y solo se puede usar una vez")
    @ApiResponse(responseCode = "200", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "400", description = "La contraseña no cumple el formato")
    @ApiResponse(responseCode = "422", description = "El enlace no es válido, ya se usó o venció")
    public Map<String, String> restablecer(@Valid @RequestBody RestablecerPasswordRequest request) {
        recuperacion.restablecerPassword(request.token(), request.password());
        return Map.of("mensaje", "Tu contraseña se actualizó. Ya puedes iniciar sesión");
    }

    @PostMapping("/cambiar-password")
    @Operation(summary = "Cambiar mi contraseña",
            description = "Requiere estar autenticado y escribir la contraseña actual. La nueva debe ser distinta")
    @ApiResponse(responseCode = "200", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "400", description = "La contraseña nueva no cumple el formato")
    @ApiResponse(responseCode = "422", description = "La contraseña actual es incorrecta o la nueva es igual a la actual")
    public Map<String, String> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        autenticacion.cambiarPassword(usuario.id(), request.passwordActual(), request.passwordNueva());
        return Map.of("mensaje", "Tu contraseña se actualizó correctamente");
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Incluye el id de cliente o de proveedor según el rol")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario")
    @ApiResponse(responseCode = "401", description = "Token ausente, vencido o inválido")
    public PerfilResult perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return autenticacion.obtenerPerfil(usuario.id());
    }
}
