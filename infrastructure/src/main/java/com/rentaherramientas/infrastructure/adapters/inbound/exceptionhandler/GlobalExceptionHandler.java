package com.rentaherramientas.infrastructure.adapters.inbound.exceptionhandler;

import com.rentaherramientas.application.exceptions.AccesoDenegadoException;
import com.rentaherramientas.application.exceptions.CredencialesInvalidasException;
import com.rentaherramientas.application.exceptions.UsuarioInactivoException;
import com.rentaherramientas.domain.exceptions.ConflictoException;
import com.rentaherramientas.domain.exceptions.RecursoNoEncontradoException;
import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de errores: traduce cada excepción a una respuesta HTTP con formato
 * ProblemDetail (estándar RFC 9457), por ejemplo:
 *
 * <pre>
 * { "type": "about:blank", "title": "Conflicto", "status": 409,
 *   "detail": "Ya existe un usuario registrado con el email x@y.com",
 *   "instance": "/api/v1/auth/registro", "timestamp": "2026-10-14T16:00:00Z" }
 * </pre>
 *
 * Al extender ResponseEntityExceptionHandler, Spring ya resuelve los errores propios de MVC
 * (JSON mal formado, método no permitido, parámetros inválidos...) con este mismo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Errores de negocio (dominio y aplicación) ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail manejarConflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ProblemDetail manejarReglaDeNegocio(ReglaDeNegocioException ex) {
        return problema(HttpStatusCode.valueOf(422), "Regla de negocio incumplida", ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail manejarCredenciales(CredencialesInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", ex.getMessage());
    }

    @ExceptionHandler({UsuarioInactivoException.class, AccesoDenegadoException.class})
    public ProblemDetail manejarAccesoDenegadoNegocio(RuntimeException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    // ---------- Errores de Spring Security ----------

    /** Sin token, token vencido o firma inválida en una ruta protegida. */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail manejarNoAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado",
                "Debes enviar un token válido en el header Authorization: Bearer <token>");
    }

    /** Token válido pero sin el rol que exige el endpoint (@PreAuthorize). */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarSinPermiso(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permisos para realizar esta operación");
    }

    // ---------- Errores de datos ----------

    /** Última barrera: una restricción UNIQUE o FOREIGN KEY de MySQL que se escapó de las validaciones. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de la base de datos (dato duplicado o relacionado)");
    }

    /** Errores de @Valid en el body: responde 400 con el detalle de cada campo inválido. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "La petición tiene campos con errores de validación");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    /** Cualquier error no previsto: se registra completo en el log y al cliente se le responde genérico. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarInesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde");
    }

    private ProblemDetail problema(HttpStatusCode status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }
}
