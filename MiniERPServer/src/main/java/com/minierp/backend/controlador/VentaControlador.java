package com.minierp.backend.controlador;

import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.dto.operacion.VentaPeticion;
import com.minierp.backend.dto.operacion.VentaRespuesta;
import com.minierp.backend.dto.operacion.VentaResumen;
import com.minierp.backend.servicio.ServicioVenta;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Ventas.
 *   Lectura: Administracion, Ventas e Inventario.
 *   Registro: area de Ventas.
 *
 * Igual que las compras, una venta no se modifica ni se elimina.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentaControlador {

    private static final Set<String> ORDENABLES = Set.of("idVenta", "fechaVenta", "total", "numeroFactura");

    private final ServicioVenta servicio;

    public VentaControlador(ServicioVenta servicio) {
        this.servicio = servicio;
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
}
