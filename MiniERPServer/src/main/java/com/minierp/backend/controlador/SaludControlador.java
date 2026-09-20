package com.minierp.backend.controlador;

import com.minierp.backend.config.PropiedadesNegocio;
import com.minierp.backend.repositorio.ProductoRepositorio;
import com.minierp.backend.repositorio.UsuarioRepositorio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comprobacion de vida del servidor.
 *
 * Sirve para verificar la Fase 1 de extremo a extremo: si responde con
 * los conteos correctos, entonces el contexto de Spring arranco, las
 * entidades concuerdan con el esquema y los repositorios consultan la
 * base de verdad.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludControlador {

    private final ProductoRepositorio productoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PropiedadesNegocio propiedadesNegocio;

    public SaludControlador(ProductoRepositorio productoRepositorio,
                            UsuarioRepositorio usuarioRepositorio,
                            PropiedadesNegocio propiedadesNegocio) {
        this.productoRepositorio = productoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.propiedadesNegocio = propiedadesNegocio;
    }

    @GetMapping
    public Map<String, Object> estado() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "OPERATIVO");
        respuesta.put("aplicacion", "Mini ERP");
        respuesta.put("marcaTiempo", LocalDateTime.now());
        respuesta.put("metodoValuacion", propiedadesNegocio.getMetodoValuacion());
        respuesta.put("iva", propiedadesNegocio.getIva());
        respuesta.put("productosRegistrados", productoRepositorio.count());
        respuesta.put("usuariosRegistrados", usuarioRepositorio.count());
        return respuesta;
    }
}
