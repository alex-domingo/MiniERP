package com.minierp.backend.factura;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.config.PropiedadesEmpresa;
import com.minierp.backend.entidad.Cliente;
import com.minierp.backend.entidad.DetalleVenta;
import com.minierp.backend.entidad.Venta;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.repositorio.VentaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Factura de venta en PDF (OpenPDF 2.x, paquetes com.lowagie).
 *
 * Se arma EXCLUSIVAMENTE con lo que quedo guardado en la venta: precios,
 * subtotales, tasa de IVA y totales historicos. Nada se recalcula con el
 * catalogo o la configuracion actuales, de modo que reimprimir una
 * factura de enero produce exactamente la factura de enero, aunque el
 * precio del producto o la tasa de IVA hayan cambiado despues.
 *
 * Solo los datos de la empresa emisora vienen de la configuracion.
 *
 * Sobre el @SuppressWarnings: OpenPDF 2.4.0 marca como obsoleto TODO el
 * paquete com.lowagie porque la serie 3.x lo renombro a org.openpdf. En
 * 2.x la API sigue completa y funcional; migrar a 3.x consiste en cambiar
 * la version en el pom y los imports de esta clase (ver pom.xml).
 */
@Service
@SuppressWarnings("deprecation")
public class ServicioFactura {

    private static final Color AZUL = new Color(31, 56, 100);
    private static final Color GRIS_CLARO = new Color(242, 244, 247);
    private static final Color GRIS_BORDE = new Color(200, 205, 212);
    private static final Color GRIS_TEXTO = new Color(95, 99, 104);

    private static final Font TITULO_EMPRESA = new Font(Font.HELVETICA, 15, Font.BOLD, AZUL);
    private static final Font TITULO_FACTURA = new Font(Font.HELVETICA, 17, Font.BOLD, Color.WHITE);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 8, Font.BOLD, GRIS_TEXTO);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font BLANCA = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TOTAL = new Font(Font.HELVETICA, 11, Font.BOLD, AZUL);
    private static final Font PIE = new Font(Font.HELVETICA, 7, Font.NORMAL, GRIS_TEXTO);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaRepositorio ventaRepositorio;
    private final PropiedadesEmpresa empresa;

    public ServicioFactura(VentaRepositorio ventaRepositorio, PropiedadesEmpresa empresa) {
        this.ventaRepositorio = ventaRepositorio;
        this.empresa = empresa;
    }

    @Transactional(readOnly = true)
    @Auditable(modulo = ModuloBitacora.VENTAS, accion = AccionBitacora.EXPORTAR, entidad = "venta",
            descripcion = "Emision de factura PDF")
    public FacturaPdf generar(Long idVenta) {
        Venta venta = ventaRepositorio.findConDetallesByIdVenta(idVenta)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Venta", idVenta));
        return new FacturaPdf(venta.getIdVenta(), venta.getNumeroFactura(),
                venta.getCliente().getNombre(), dibujar(venta));
    }

    // -----------------------------------------------------------------

    private byte[] dibujar(Venta venta) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 42, 42, 40, 50);
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new PiePagina(venta.getNumeroFactura()));
            documento.addTitle("Factura " + venta.getNumeroFactura());
            documento.addAuthor(empresa.getNombre());
            documento.addCreator("Mini ERP");
            documento.open();

            documento.add(encabezado(venta));
            documento.add(espacio(10));
            documento.add(datosCliente(venta));
            documento.add(espacio(12));
            documento.add(detalle(venta));
            documento.add(totales(venta));
            documento.add(espacio(8));
            documento.add(enLetras(venta.getTotal()));

            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar la factura " + venta.getNumeroFactura(), ex);
        }
        return salida.toByteArray();
    }

    /** Empresa a la izquierda, recuadro con numero y fecha a la derecha. */
    private PdfPTable encabezado(Venta venta) {
        PdfPTable tabla = new PdfPTable(new float[]{62, 38});
        tabla.setWidthPercentage(100);

        PdfPCell emisor = sinBorde();
        emisor.addElement(new Paragraph(empresa.getNombre(), TITULO_EMPRESA));
        emisor.addElement(linea("NIT: ", empresa.getNit()));
        emisor.addElement(linea("Dirección: ", empresa.getDireccion()));
        emisor.addElement(linea("Teléfono: ", empresa.getTelefono()));
        tabla.addCell(emisor);

        PdfPTable recuadro = new PdfPTable(1);
        recuadro.setWidthPercentage(100);
        PdfPCell titulo = new PdfPCell(new Phrase("FACTURA", TITULO_FACTURA));
        titulo.setBackgroundColor(AZUL);
        titulo.setBorderColor(AZUL);
        titulo.setHorizontalAlignment(Element.ALIGN_CENTER);
        titulo.setPaddingTop(5);
        titulo.setPaddingBottom(7);
        recuadro.addCell(titulo);

        PdfPCell datos = new PdfPCell();
        datos.setBorderColor(AZUL);
        datos.setPadding(7);
        datos.addElement(parCentrado("No. ", venta.getNumeroFactura()));
        datos.addElement(parCentrado("Fecha: ", venta.getFechaVenta().format(FECHA)));
        recuadro.addCell(datos);

        PdfPCell contenedor = sinBorde();
        contenedor.addElement(recuadro);
        tabla.addCell(contenedor);
        return tabla;
    }

    private PdfPTable datosCliente(Venta venta) {
        Cliente cliente = venta.getCliente();
        PdfPTable tabla = new PdfPTable(new float[]{14, 46, 14, 26});
        tabla.setWidthPercentage(100);

        PdfPCell titulo = new PdfPCell(new Phrase("DATOS DEL CLIENTE", BLANCA));
        titulo.setColspan(4);
        titulo.setBackgroundColor(AZUL);
        titulo.setBorderColor(AZUL);
        titulo.setPadding(4);
        tabla.addCell(titulo);

        campo(tabla, "Nombre", cliente.getNombre());
        campo(tabla, "NIT", cliente.getNit());
        campo(tabla, "Dirección", valorO(cliente.getDireccion(), "Ciudad"));
        campo(tabla, "Teléfono", valorO(cliente.getTelefono(), "-"));
        campo(tabla, "Correo", valorO(cliente.getCorreo(), "-"));
        campo(tabla, "Atendió", venta.getUsuario().getNombreCompleto());
        return tabla;
    }

    private PdfPTable detalle(Venta venta) {
        PdfPTable tabla = new PdfPTable(new float[]{6, 13, 43, 10, 14, 14});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);   // si la factura pasa de una hoja, el encabezado se repite

        for (String titulo : new String[]{"No.", "Código", "Descripción", "Cantidad", "Precio unitario", "Subtotal"}) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, BLANCA));
            celda.setBackgroundColor(AZUL);
            celda.setBorderColor(AZUL);
            celda.setPadding(5);
            celda.setHorizontalAlignment(titulo.equals("Descripción") || titulo.equals("Código")
                    ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }

        List<DetalleVenta> lineas = venta.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleVenta::getIdDetalleVenta))
                .toList();
        int numero = 1;
        for (DetalleVenta d : lineas) {
            boolean par = numero % 2 == 0;
            tabla.addCell(celdaDetalle(String.valueOf(numero++), Element.ALIGN_CENTER, par));
            tabla.addCell(celdaDetalle(d.getProducto().getCodigo(), Element.ALIGN_LEFT, par));
            tabla.addCell(celdaDetalle(d.getProducto().getNombre(), Element.ALIGN_LEFT, par));
            tabla.addCell(celdaDetalle(String.valueOf(d.getCantidad()), Element.ALIGN_CENTER, par));
            tabla.addCell(celdaDetalle(moneda(d.getPrecioUnitario()), Element.ALIGN_RIGHT, par));
            tabla.addCell(celdaDetalle(moneda(d.getSubtotal()), Element.ALIGN_RIGHT, par));
        }
        return tabla;
    }

    private PdfPTable totales(Venta venta) {
        PdfPTable tabla = new PdfPTable(new float[]{72, 14, 14});
        tabla.setWidthPercentage(100);

        String tasa = venta.getPorcentajeIva().movePointRight(2)
                .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

        filaTotal(tabla, "Subtotal", moneda(venta.getSubtotal()), NEGRITA, false);
        filaTotal(tabla, "IVA (" + tasa + "%)", moneda(venta.getIva()), NEGRITA, false);
        filaTotal(tabla, "TOTAL", moneda(venta.getTotal()), TOTAL, true);
        return tabla;
    }

    private Paragraph enLetras(BigDecimal total) {
        Paragraph p = new Paragraph();
        p.add(new Chunk("Total en letras: ", ETIQUETA));
        p.add(new Chunk(NumeroALetras.quetzales(total), NEGRITA));
        return p;
    }

    // --- piezas pequenas ------------------------------------------------

    private void campo(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, ETIQUETA));
        e.setBackgroundColor(GRIS_CLARO);
        e.setBorderColor(GRIS_BORDE);
        e.setPadding(4);
        tabla.addCell(e);
        PdfPCell v = new PdfPCell(new Phrase(valor, NORMAL));
        v.setBorderColor(GRIS_BORDE);
        v.setPadding(4);
        tabla.addCell(v);
    }

    private PdfPCell celdaDetalle(String texto, int alineacion, boolean sombreada) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NORMAL));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setBorderColor(GRIS_BORDE);
        celda.setPadding(5);
        if (sombreada) {
            celda.setBackgroundColor(GRIS_CLARO);
        }
        return celda;
    }

    private void filaTotal(PdfPTable tabla, String etiqueta, String valor, Font fuente, boolean resaltada) {
        tabla.addCell(sinBorde());
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, fuente));
        PdfPCell v = new PdfPCell(new Phrase(valor, fuente));
        for (PdfPCell c : new PdfPCell[]{e, v}) {
            c.setBorderColor(GRIS_BORDE);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (resaltada) {
                c.setBackgroundColor(GRIS_CLARO);
            }
        }
        tabla.addCell(e);
        tabla.addCell(v);
    }

    private Paragraph linea(String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.setLeading(12);
        p.add(new Chunk(etiqueta, ETIQUETA));
        p.add(new Chunk(valorO(valor, "-"), NORMAL));
        return p;
    }

    private Paragraph parCentrado(String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(etiqueta, NEGRITA));
        p.add(new Chunk(valor, NORMAL));
        return p;
    }

    private static PdfPCell sinBorde() {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    private static Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private static String valorO(String valor, String alternativo) {
        return valor == null || valor.isBlank() ? alternativo : valor;
    }

    /** Q 1,234.50 : formato de Guatemala (coma de miles, punto decimal). */
    static String moneda(BigDecimal monto) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "Q " + formato.format(monto.setScale(2, RoundingMode.HALF_UP));
    }

    /** Pie de cada hoja: numero de factura, pagina y fecha de impresion. */
    private static final class PiePagina extends PdfPageEventHelper {

        private final String numeroFactura;
        private final String impresa = LocalDateTime.now().format(FECHA);

        PiePagina(String numeroFactura) {
            this.numeroFactura = numeroFactura;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            Rectangle hoja = documento.getPageSize();
            float y = documento.bottomMargin() - 22;
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Factura " + numeroFactura + "  -  impresa el " + impresa + " por Mini ERP", PIE),
                    documento.leftMargin(), y, 0);
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + escritor.getPageNumber(), PIE),
                    hoja.getWidth() - documento.rightMargin(), y, 0);
        }
    }
}
