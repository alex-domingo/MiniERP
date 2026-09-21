package com.minierp.backend.controlador;

import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.dto.inventario.AjustePeticion;
import com.minierp.backend.dto.inventario.MovimientoRespuesta;
import com.minierp.backend.dto.inventario.ValuacionProductoRespuesta;
import com.minierp.backend.dto.inventario.VerificacionIntegridad;
import com.minierp.backend.inventario.ServicioIntegridad;
import com.minierp.backend.inventario.ServicioInventario;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Inventario: kardex, valuacion por capas, ajustes y verificacion.
 *   Lectura: Administracion, Compras e Inventario. Ventas no: el kardex
 *            expone costos, y Ventas consulta la existencia en /api/productos.
 *   Ajustes: area de Inventario.
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioControlador {

    private final ServicioInventario inventario;
    private final ServicioIntegridad integridad;

    public InventarioControlador(ServicioInventario inventario, ServicioIntegridad integridad) {
        this.inventario = inventario;
        this.integridad = integridad;
    }

    /** Historial de movimientos de un producto: como y por que cambio su existencia. */
    @GetMapping("/kardex/{idProducto}")
    public RespuestaPaginada<MovimientoRespuesta> kardex(
            @PathVariable Long idProducto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return inventario.kardex(idProducto, desde, hasta, Paginacion.crear(pagina, tamano, null,
                Set.of("fechaMovimiento"), "fechaMovimiento", Sort.Direction.DESC, "idMovimiento"));
    }

    /** Existencia de un producto desglosada por capa de costo, en el orden en que se consumira. */
    @GetMapping("/valuacion/{idProducto}")
    public ValuacionProductoRespuesta valuacion(@PathVariable Long idProducto) {
        return inventario.valuacion(idProducto);
    }

    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoRespuesta ajustar(@Valid @RequestBody AjustePeticion peticion) {
        return inventario.ajustar(peticion);
    }

    /** Comprobaciones de consistencia del inventario. Todas deben dar cero. */
    @GetMapping("/verificacion")
    public VerificacionIntegridad verificar() {
        return integridad.verificar();
    }
}
