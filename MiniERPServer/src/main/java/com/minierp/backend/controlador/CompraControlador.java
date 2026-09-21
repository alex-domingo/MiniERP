package com.minierp.backend.controlador;

import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.dto.operacion.CompraPeticion;
import com.minierp.backend.dto.operacion.CompraRespuesta;
import com.minierp.backend.dto.operacion.CompraResumen;
import com.minierp.backend.servicio.ServicioCompra;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Compras.
 *   Lectura: Administracion, Compras e Inventario.
 *   Registro: area de Compras.
 *
 * No hay PUT ni DELETE: una compra registrada no se modifica ni se
 * elimina. Sus capas de costo pudieron haber sido consumidas ya por
 * ventas posteriores, y el historial debe conservarse.
 */
@RestController
@RequestMapping("/api/compras")
public class CompraControlador {

    private static final Set<String> ORDENABLES = Set.of("idCompra", "fechaCompra", "total", "numeroDocumento");

    private final ServicioCompra servicio;

    public CompraControlador(ServicioCompra servicio) {
        this.servicio = servicio;
    }

    /** Ej.: /api/compras?desde=2026-03-01&hasta=2026-03-31&idProveedor=2 */
    @GetMapping
    public RespuestaPaginada<CompraResumen> listar(
            @RequestParam(required = false) Long idProveedor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(idProveedor, desde, hasta, Paginacion.crear(pagina, tamano, orden,
                ORDENABLES, "fechaCompra", Sort.Direction.DESC, "idCompra"));
    }

    @GetMapping("/{id}")
    public CompraRespuesta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CompraRespuesta> registrar(@Valid @RequestBody CompraPeticion peticion) {
        CompraRespuesta compra = servicio.registrar(peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(compra.idCompra()).toUri()).body(compra);
    }
}
