package com.minierp.backend.seguridad;

import com.minierp.backend.repositorio.UsuarioRepositorio;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga el usuario desde la tabla "usuario" para que Spring Security
 * compare la contrasena contra su hash BCrypt.
 */
@Service
public class ServicioDetalleUsuario implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioDetalleUsuario(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    /**
     * El mensaje de error es deliberadamente generico: decir "el
     * usuario no existe" permitiria averiguar que cuentas son validas
     * probando nombres.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) {
        return usuarioRepositorio.buscarPorNombreUsuarioConRol(nombreUsuario)
                .map(DetalleUsuario::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
    }
}
