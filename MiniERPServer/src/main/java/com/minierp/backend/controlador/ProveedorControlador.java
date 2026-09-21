package com.minierp.backend.controlador;

import com.minierp.backend.dto.catalogo.CostoReferenciaPeticion;
import com.minierp.backend.dto.catalogo.ProveedorPeticion;
import com.minierp.backend.dto.catalogo.ProveedorProductoPeticion;
import com.minierp.backend.dto.catalogo.ProveedorProductoRespuesta;
import com.minierp.backend.dto.catalogo.ProveedorRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.servicio.ServicioProveedor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;

/**
 * Proveedores y su catalogo de productos (relacion N:M).
 *   Lectura: Administracion, Compras e Inventario.
 *   Escritura: area de Compras.
 *
 * Los costos de referencia viven aqui y no en /api/productos a
 * proposito: son informacion de compras, y el area de Ventas, que si
 * lee productos, no debe ver los costos ni, por lo tanto, los margenes.
 */
@RestController
@RequestMapping("/api/proveedores")
public class ProveedorControlador {

    private static final Set<String> ORDENABLES = Set.of("idProveedor", "nit", "nombre");

    private final ServicioProveedor servicio;

    public ProveedorControlador(ServicioProveedor servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public RespuestaPaginada<ProveedorRespuesta> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(busqueda, activo,
                Paginacion.crear(pagina, tamano, orden, ORDENABLES, "nombre", "idProveedor"));
    }

    @GetMapping("/activos")
    public List<ProveedorRespuesta> listarActivos() {
        return servicio.listarActivos();
    }

    /** Proveedores que suministran un producto, del mas barato al mas caro. */
    @GetMapping("/por-producto/{idProducto}")
    public List<ProveedorProductoRespuesta> porProducto(@PathVariable Long idProducto) {
        return servicio.proveedoresDe(idProducto);
    }

    @GetMapping("/{id}")
    public ProveedorRespuesta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProveedorRespuesta> crear(@Valid @RequestBody ProveedorPeticion peticion) {
        ProveedorRespuesta creado = servicio.crear(peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idProveedor()).toUri()).body(creado);
    }

    @PutMapping("/{id}")
    public ProveedorRespuesta actualizar(@PathVariable Long id,
                                         @Valid @RequestBody ProveedorPeticion peticion) {
        return servicio.actualizar(id, peticion);
    }

    @DeleteMapping("/{id}")
    public ProveedorRespuesta desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ProveedorRespuesta activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    // --- Catalogo del proveedor --------------------------------------

    @GetMapping("/{id}/productos")
    public List<ProveedorProductoRespuesta> productos(@PathVariable Long id) {
        return servicio.productosDe(id);
    }

    @PostMapping("/{id}/productos")
    public ResponseEntity<ProveedorProductoRespuesta> asociar(
            @PathVariable Long id, @Valid @RequestBody ProveedorProductoPeticion peticion) {
        ProveedorProductoRespuesta asociacion = servicio.asociar(id, peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idProducto}").buildAndExpand(asociacion.idProducto()).toUri())
                .body(asociacion);
    }

    @PutMapping("/{id}/productos/{idProducto}")
    public ProveedorProductoRespuesta actualizarCosto(
            @PathVariable Long id, @PathVariable Long idProducto,
            @Valid @RequestBody CostoReferenciaPeticion peticion) {
        return servicio.actualizarCosto(id, idProducto, peticion);
    }

    @DeleteMapping("/{id}/productos/{idProducto}")
    public ProveedorProductoRespuesta desasociar(@PathVariable Long id,
                                                 @PathVariable Long idProducto) {
        return servicio.desasociar(id, idProducto);
    }
}
