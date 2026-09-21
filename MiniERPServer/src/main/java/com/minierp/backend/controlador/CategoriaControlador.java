package com.minierp.backend.controlador;

import com.minierp.backend.dto.catalogo.CategoriaPeticion;
import com.minierp.backend.dto.catalogo.CategoriaRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.servicio.ServicioCategoria;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;

/**
 * Categorias de producto.
 *   Lectura: cualquier usuario autenticado.
 *   Escritura: area de Inventario.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaControlador {

    private static final Set<String> ORDENABLES = Set.of("idCategoria", "nombre");

    private final ServicioCategoria servicio;

    public CategoriaControlador(ServicioCategoria servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public RespuestaPaginada<CategoriaRespuesta> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(busqueda, activo,
                Paginacion.crear(pagina, tamano, orden, ORDENABLES, "nombre", "idCategoria"));
    }

    @GetMapping("/activas")
    public List<CategoriaRespuesta> listarActivas() {
        return servicio.listarActivas();
    }

    @GetMapping("/{id}")
    public CategoriaRespuesta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaRespuesta> crear(@Valid @RequestBody CategoriaPeticion peticion) {
        CategoriaRespuesta creada = servicio.crear(peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.idCategoria()).toUri()).body(creada);
    }

    @PutMapping("/{id}")
    public CategoriaRespuesta actualizar(@PathVariable Long id,
                                         @Valid @RequestBody CategoriaPeticion peticion) {
        return servicio.actualizar(id, peticion);
    }

    /** Borrado logico: la categoria se desactiva, no se elimina. */
    @DeleteMapping("/{id}")
    public CategoriaRespuesta desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public CategoriaRespuesta activar(@PathVariable Long id) {
        return servicio.activar(id);
    }
}
