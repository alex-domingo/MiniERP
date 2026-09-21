package com.minierp.backend.controlador;

import com.minierp.backend.dto.catalogo.ClientePeticion;
import com.minierp.backend.dto.catalogo.ClienteRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.servicio.ServicioCliente;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;

/**
 * Clientes.
 *   Lectura: Administracion y Ventas.
 *   Escritura: area de Ventas.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteControlador {

    private static final Set<String> ORDENABLES = Set.of("idCliente", "nit", "nombre");

    private final ServicioCliente servicio;

    public ClienteControlador(ServicioCliente servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public RespuestaPaginada<ClienteRespuesta> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(busqueda, activo,
                Paginacion.crear(pagina, tamano, orden, ORDENABLES, "nombre", "idCliente"));
    }

    @GetMapping("/activos")
    public List<ClienteRespuesta> listarActivos() {
        return servicio.listarActivos();
    }

    @GetMapping("/{id}")
    public ClienteRespuesta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ClienteRespuesta> crear(@Valid @RequestBody ClientePeticion peticion) {
        ClienteRespuesta creado = servicio.crear(peticion);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idCliente()).toUri()).body(creado);
    }

    @PutMapping("/{id}")
    public ClienteRespuesta actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ClientePeticion peticion) {
        return servicio.actualizar(id, peticion);
    }

    @DeleteMapping("/{id}")
    public ClienteRespuesta desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ClienteRespuesta activar(@PathVariable Long id) {
        return servicio.activar(id);
    }
}
