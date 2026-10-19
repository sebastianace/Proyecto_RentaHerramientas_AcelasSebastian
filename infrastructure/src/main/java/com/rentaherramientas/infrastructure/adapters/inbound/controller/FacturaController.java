package com.rentaherramientas.infrastructure.adapters.inbound.controller;

import com.rentaherramientas.application.dtos.ArchivoGenerado;
import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.application.ports.in.GestionPagosUseCase;
import com.rentaherramientas.domain.repositories.Pagina;
import com.rentaherramientas.domain.repositories.SolicitudPagina;
import com.rentaherramientas.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturas")
@Tag(name = "Facturas", description = "Facturas generadas automáticamente al aprobarse un pago")
public class FacturaController {

    private final GestionPagosUseCase gestionPagos;

    public FacturaController(GestionPagosUseCase gestionPagos) {
        this.gestionPagos = gestionPagos;
    }

    @GetMapping
    @Operation(summary = "Listar facturas", description = "Cada rol ve las suyas; el ADMIN ve la facturación global")
    public Pagina<FacturaResult> listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionPagos.listarFacturas(new SolicitudPagina(pagina, tamanio), usuario.aSolicitante());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una factura")
    public FacturaResult obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return gestionPagos.obtenerFactura(id, usuario.aSolicitante());
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Descargar factura en PDF", description = "Incluye el comprobante del pago")
    @ApiResponse(responseCode = "200", description = "Archivo PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ArchivoGenerado archivo = gestionPagos.descargarFacturaPdf(id, usuario.aSolicitante());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(archivo.contenido());
    }
}
