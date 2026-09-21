package com.minierp.backend.controlador;

import com.minierp.backend.dto.catalogo.ProductoPeticion;
import com.minierp.backend.dto.catalogo.ProductoRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.servicio.ServicioProducto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;

/**
 * Catalogo de productos.
 *   Lectura: cualquier usuario autenticado (Compras y Ventas lo
 *            necesitan para registrar sus operaciones).
 *   Escritura: area de Inventario.
 *
 * Ningun endpoint de este controlador modifica existencias.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoControlador {

    private static final Set<String> ORDENABLES = Set.of(
            "idProducto", "codigo", "nombre", "precioVenta", "stockActual", "stockMinimo");

    private final ServicioProducto servicio;

    public ProductoControlador(ServicioProducto servicio) {
        this.servicio = servicio;
    }

    /**
     * Ejemplos de filtros combinables:
     *   /api/productos?busqueda=silla
     *   /api/productos?idCategoria=2&activo=true
     *   /api/productos?busqueda=mesa&idCategoria=3&orden=precioVenta,desc
     */
    @GetMapping
    public RespuestaPaginada<ProductoRespuesta> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(busqueda, idCategoria, activo,
                Paginacion.crear(pagina, tamano, orden, ORDENABLES, "codigo", "idProducto"));
    }

    /** Productos con existencias en el nivel que requiere atencion. */
    @GetMapping("/alertas")
    public List<ProductoRespuesta> alertas() {
        return servicio.alertasDeExistencia();
    }

    @GetMapping("/codigo/{codigo}")
    public ProductoRespuesta obtenerPorCodigo(@PathVariable String codigo) {
        return servicio.obtenerPorCodigo(codigo);
    }

    @GetMapping("/{id}")
    public ProductoRespuesta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProductoRespuesta> crear(@Valid @RequestBody ProductoPeticion peticion) {
        ProductoRespuesta creado = servicio.crear(peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idProducto()).toUri()).body(creado);
    }

    @PutMapping("/{id}")
    public ProductoRespuesta actualizar(@PathVariable Long id,
                                        @Valid @RequestBody ProductoPeticion peticion) {
        return servicio.actualizar(id, peticion);
    }

    /** Borrado logico: el producto deja de comercializarse. */
    @DeleteMapping("/{id}")
    public ProductoRespuesta desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ProductoRespuesta activar(@PathVariable Long id) {
        return servicio.activar(id);
    }
}
