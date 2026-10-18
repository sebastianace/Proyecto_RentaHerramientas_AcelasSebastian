package com.rentaherramientas.infrastructure.adapters.outbound.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rentaherramientas.application.dtos.FacturaResult;
import com.rentaherramientas.application.ports.out.GeneradorFacturaPdfPort;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el PDF de una factura con OpenPDF. Sirve también como comprobante de pago:
 * incluye método, referencia y fecha de la transacción.
 */
@Component
public class FacturaPdfAdapter implements GeneradorFacturaPdfPort {

    private static final String MARCA = "Renta de Herramientas";
    private static final Locale COLOMBIA = Locale.of("es", "CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color PRIMARIO = new Color(234, 88, 12);
    private static final Color GRIS_OSCURO = new Color(41, 37, 36);
    private static final Color GRIS_CLARO = new Color(245, 245, 244);

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARIO);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GRIS_OSCURO);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS_OSCURO);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GRIS_OSCURO);
    private static final Font ENCABEZADO_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARIO);

    @Override
    public byte[] generar(FacturaResult factura) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            agregarEncabezado(documento, factura);
            agregarPartes(documento, factura);
            agregarDetalle(documento, factura);
            agregarTotales(documento, factura);
            agregarPago(documento, factura);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura " + factura.numero(), e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private void agregarEncabezado(Document documento, FacturaResult f) throws Exception {
        PdfPTable tabla = new PdfPTable(new float[]{3, 2});
        tabla.setWidthPercentage(100);
        PdfPCell marca = celdaSinBorde(new Phrase(MARCA, TITULO));
        marca.setPaddingBottom(4);
        tabla.addCell(marca);
        PdfPCell numero = celdaSinBorde(new Phrase("FACTURA DE VENTA\n" + f.numero(), SUBTITULO));
        numero.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(numero);
        tabla.addCell(celdaSinBorde(new Phrase("Alquiler de herramientas y equipos de construcción", NORMAL)));
        PdfPCell fecha = celdaSinBorde(new Phrase("Fecha de emisión: " + f.fechaEmision().format(FECHA_HORA)
                + "\nEstado: " + f.estado(), NORMAL));
        fecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(fecha);
        documento.add(tabla);
        documento.add(espacio(12));
    }

    private void agregarPartes(Document documento, FacturaResult f) throws Exception {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        FacturaResult.Emisor e = f.emisor();
        tabla.addCell(bloque("EMISOR (PROVEEDOR)", e.razonSocial() + "\nNIT: " + e.nit() + "\n" + e.direccion()
                + ", " + e.ciudad() + "\n" + e.email() + (e.telefono() == null ? "" : " · " + e.telefono())));
        FacturaResult.Cliente c = f.cliente();
        tabla.addCell(bloque("CLIENTE", c.nombreCompleto() + "\n" + c.tipoDocumento() + ": " + c.numeroDocumento()
                + "\n" + c.direccion() + ", " + c.ciudad() + "\n" + c.email()));
        documento.add(tabla);
        Paragraph reserva = new Paragraph("Reserva " + f.reserva().codigo() + "  ·  Periodo de alquiler: "
                + f.reserva().fechaInicio().format(FECHA) + " al " + f.reserva().fechaFin().format(FECHA), NEGRITA);
        reserva.setSpacingBefore(10);
        reserva.setSpacingAfter(8);
        documento.add(reserva);
    }

    private void agregarDetalle(Document documento, FacturaResult f) throws Exception {
        PdfPTable tabla = new PdfPTable(new float[]{5, 1.2f, 1.2f, 2, 2.2f});
        tabla.setWidthPercentage(100);
        for (String titulo : new String[]{"Herramienta", "Cant.", "Días", "Precio/día", "Subtotal"}) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, ENCABEZADO_TABLA));
            celda.setBackgroundColor(GRIS_OSCURO);
            celda.setPadding(6);
            celda.setBorderColor(GRIS_OSCURO);
            tabla.addCell(celda);
        }
        boolean alterna = false;
        for (FacturaResult.Linea linea : f.lineas()) {
            Color fondo = alterna ? GRIS_CLARO : Color.WHITE;
            tabla.addCell(celdaDetalle(linea.herramienta(), fondo, Element.ALIGN_LEFT));
            tabla.addCell(celdaDetalle(String.valueOf(linea.cantidad()), fondo, Element.ALIGN_CENTER));
            tabla.addCell(celdaDetalle(String.valueOf(linea.dias()), fondo, Element.ALIGN_CENTER));
            tabla.addCell(celdaDetalle(pesos(linea.precioPorDia()), fondo, Element.ALIGN_RIGHT));
            tabla.addCell(celdaDetalle(pesos(linea.subtotal()), fondo, Element.ALIGN_RIGHT));
            alterna = !alterna;
        }
        documento.add(tabla);
    }

    private void agregarTotales(Document documento, FacturaResult f) throws Exception {
        PdfPTable tabla = new PdfPTable(new float[]{7.4f, 2, 2.2f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        filaTotal(tabla, "Subtotal", pesos(f.subtotal()), NORMAL);
        filaTotal(tabla, "IVA (19 %)", pesos(f.iva()), NORMAL);
        filaTotal(tabla, "TOTAL", pesos(f.total()), TOTAL);
        documento.add(tabla);
    }

    private void agregarPago(Document documento, FacturaResult f) throws Exception {
        documento.add(espacio(14));
        FacturaResult.Pago p = f.pago();
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.addCell(bloque("COMPROBANTE DE PAGO", "Método: " + p.metodo().name().replace('_', ' ')
                + "\nReferencia de la transacción: " + p.referenciaTransaccion()
                + "\nFecha del pago: " + p.fechaPago().format(FECHA_HORA)));
        documento.add(tabla);
        Paragraph nota = new Paragraph("Documento generado electrónicamente por la plataforma " + MARCA + ".", NORMAL);
        nota.setSpacingBefore(16);
        nota.setAlignment(Element.ALIGN_CENTER);
        documento.add(nota);
    }

    // ---------- Utilidades de formato ----------

    private static String pesos(BigDecimal valor) {
        return String.format(COLOMBIA, "$%,.0f", valor);
    }

    private static Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(alto);
        return p;
    }

    private static PdfPCell celdaSinBorde(Phrase frase) {
        PdfPCell celda = new PdfPCell(frase);
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    private static PdfPCell bloque(String titulo, String contenido) {
        Phrase frase = new Phrase();
        frase.add(new Phrase(titulo + "\n", SUBTITULO));
        frase.add(new Phrase(contenido, NORMAL));
        PdfPCell celda = new PdfPCell(frase);
        celda.setBackgroundColor(GRIS_CLARO);
        celda.setBorderColor(Color.WHITE);
        celda.setBorderWidth(4);
        celda.setPadding(8);
        return celda;
    }

    private static PdfPCell celdaDetalle(String texto, Color fondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NORMAL));
        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5);
        celda.setBorderColor(GRIS_CLARO);
        return celda;
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        tabla.addCell(celdaSinBorde(new Phrase("")));
        PdfPCell nombre = celdaSinBorde(new Phrase(etiqueta, fuente));
        nombre.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(nombre);
        PdfPCell monto = celdaSinBorde(new Phrase(valor, fuente));
        monto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(monto);
    }
}
