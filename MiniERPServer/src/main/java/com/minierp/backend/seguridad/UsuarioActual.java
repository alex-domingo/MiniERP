package com.minierp.backend.seguridad;

import com.minierp.backend.entidad.Usuario;
import com.minierp.backend.repositorio.UsuarioRepositorio;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Entrega la entidad del usuario autenticado, para registrarlo como
 * responsable de compras, ventas y movimientos de inventario.
 *
 * El usuario se toma SIEMPRE del token, nunca del cuerpo de la
 * peticion: de lo contrario cualquiera podria registrar una operacion a
 * nombre de otro.
 */
@Component
public class UsuarioActual {

    private final UsuarioRepositorio usuarioRepositorio;

    public UsuarioActual(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    public Usuario obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
        }
        return usuarioRepositorio.findByNombreUsuario(autenticacion.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "El usuario del token ya no existe"));
    }
}
