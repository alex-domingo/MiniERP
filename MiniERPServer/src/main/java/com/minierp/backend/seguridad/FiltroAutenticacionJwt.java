package com.minierp.backend.seguridad;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida el token en cada peticion y deja la autenticacion en el
 * contexto de seguridad.
 *
 * Se apoya solo en el contenido del token y no consulta la base en cada
 * peticion: esa es la ventaja de la autenticacion sin estado. La
 * contrapartida es que desactivar un usuario no surte efecto hasta que
 * su token expire; con ocho horas de vigencia es un compromiso
 * razonable para este sistema.
 *
 * Si el token falta o es invalido, el filtro NO rechaza la peticion:
 * deja el contexto vacio y sigue. Es la cadena de autorizacion la que
 * decide si esa ruta exigia autenticacion.
 */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt servicioJwt;

    public FiltroAutenticacionJwt(ServicioJwt servicioJwt) {
        this.servicioJwt = servicioJwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena)
            throws ServletException, IOException {

        String token = extraerToken(peticion);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = servicioJwt.validarYExtraer(token);
            if (claims != null) {
                String usuario = servicioJwt.extraerUsuario(claims);
                String rol = servicioJwt.extraerRol(claims);

                var autenticacion = new UsernamePasswordAuthenticationToken(
                        usuario, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                autenticacion.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(peticion));

                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }

        cadena.doFilter(peticion, respuesta);
    }

    private String extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(CABECERA);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            return cabecera.substring(PREFIJO.length()).trim();
        }
        return null;
    }
}
