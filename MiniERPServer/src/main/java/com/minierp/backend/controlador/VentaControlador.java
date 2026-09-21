package com.minierp.backend.controlador;

import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.factura.FacturaPdf;
import com.minierp.backend.factura.ServicioFactura;
import com.minierp.backend.dto.operacion.VentaPeticion;
import com.minierp.backend.dto.operacion.VentaRespuesta;
import com.minierp.backend.dto.operacion.VentaResumen;
import com.minierp.backend.servicio.ServicioVenta;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Ventas.
 *   Lectura: Administracion, Ventas e Inventario.
 *   Registro: area de Ventas.
 *   Factura PDF: Administracion y Ventas.
 *
 * Igual que las compras, una venta no se modifica ni se elimina.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentaControlador {

    private static final Set<String> ORDENABLES = Set.of("idVenta", "fechaVenta", "total", "numeroFactura");

    private final ServicioVenta servicio;
    private final ServicioFactura servicioFactura;

    public VentaControlador(ServicioVenta servicio, ServicioFactura servicioFactura) {
        this.servicio = servicio;
        this.servicioFactura = servicioFactura;
    }

    /** Ej.: /api/ventas?idCliente=7 muestra todas las operaciones con ese cliente. */
    @GetMapping
    public RespuestaPaginada<VentaResumen> listar(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(idCliente, desde, hasta, Paginacion.crear(pagina, tamano, orden,
                ORDENABLES, "fechaVenta", Sort.Direction.DESC, "idVenta"));
    }

    @GetMapping("/{id}")
    public VentaRespuesta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<VentaRespuesta> registrar(@Valid @RequestBody VentaPeticion peticion) {
        VentaRespuesta venta = servicio.registrar(peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(venta.idVenta()).toUri()).body(venta);
    }

    /**
     * Factura de la venta en PDF. El cliente la pide justo despues de
     * registrar la venta (POST devuelve el idVenta) y puede volver a
     * pedirla cuando quiera: siempre sale identica, porque se arma con
     * lo que quedo guardado.
     *
     * Por defecto se muestra en el navegador; con ?descargar=true se
     * baja como archivo FAC-aaaa-nnnnn.pdf.
     */
    @GetMapping("/{id}/factura")
    public ResponseEntity<byte[]> factura(@PathVariable Long id,
                                          @RequestParam(defaultValue = "false") boolean descargar) {
        FacturaPdf factura = servicioFactura.generar(id);
        ContentDisposition disposicion = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(factura.nombreArchivo())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(factura.contenido());
    }
}
