package com.minierp.backend.controlador;

import com.minierp.backend.dto.autenticacion.PerfilUsuario;
import com.minierp.backend.dto.autenticacion.PeticionLogin;
import com.minierp.backend.dto.autenticacion.RespuestaLogin;
import com.minierp.backend.servicio.ServicioAutenticacion;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Endpoints de autenticacion.
 *
 *   POST /api/auth/login   publico
 *   GET  /api/auth/perfil  requiere token
 *   POST /api/auth/logout  requiere token
 *
 * El cierre de sesion no invalida el token en el servidor: con
 * autenticacion sin estado no hay donde marcarlo. Lo que hace es dejar
 * constancia en la bitacora; es el cliente quien descarta el token.
 * Para invalidacion inmediata habria que llevar una lista de tokens
 * revocados, que agrega estado y no lo pide el enunciado.
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionControlador {

    private final ServicioAutenticacion servicioAutenticacion;

    public AutenticacionControlador(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaLogin> iniciarSesion(
            @Valid @RequestBody PeticionLogin peticion) {
        return ResponseEntity.ok(servicioAutenticacion.iniciarSesion(peticion));
    }

    @GetMapping("/perfil")
    public ResponseEntity<PerfilUsuario> perfil(Principal principal) {
        return ResponseEntity.ok(servicioAutenticacion.perfil(principal.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(Principal principal) {
        servicioAutenticacion.cerrarSesion(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
