package com.minierp.backend.seguridad;

import com.minierp.backend.entidad.Usuario;
import com.minierp.backend.entidad.enums.NombreRol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre la entidad Usuario y el contrato de Spring Security.
 *
 * El rol se expone como autoridad con prefijo ROLE_, que es lo que
 * espera hasRole(). Si se omitiera el prefijo, hasRole("VENTAS") no
 * encontraria nunca la autoridad y todo daria 403.
 */
public class DetalleUsuario implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long idUsuario;
    private final String nombreUsuario;
    private final String contrasena;
    private final String nombreCompleto;
    private final NombreRol rol;
    private final boolean activo;

    public DetalleUsuario(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.nombreUsuario = usuario.getNombreUsuario();
        this.contrasena = usuario.getContrasena();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.rol = usuario.getRol().getNombre();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public NombreRol getRol() {
        return rol;
    }
}
