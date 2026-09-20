package com.minierp.backend.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Respuestas de los dos fallos de seguridad, con el mismo formato
 * ProblemDetail que usa el resto de la API para que el cliente Angular
 * los trate igual que cualquier otro error.
 *
 *   401 No autenticado : no hay token, o es invalido o expiro
 *   403 Sin permiso    : el token es valido pero el rol no alcanza
 *
 * El JSON se arma a mano a proposito: estos manejadores corren dentro
 * de la cadena de filtros, antes de que exista el contexto de MVC que
 * serializaria un objeto.
 */
@Component
public class ManejadorErroresSeguridad
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresSeguridad.class);

    /** 401: la peticion no viene autenticada. */
    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException ex) throws IOException {
        escribir(respuesta, HttpStatus.UNAUTHORIZED,
                "No autenticado",
                "Esta operacion requiere iniciar sesion. Envie un token valido "
                        + "en la cabecera Authorization: Bearer <token>.",
                "no-autenticado", peticion.getRequestURI());
    }

    /** 403: autenticado, pero el rol no tiene permiso sobre el recurso. */
    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException ex) throws IOException {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String usuario = autenticacion != null ? autenticacion.getName() : "desconocido";
        log.warn("Acceso denegado: usuario '{}' intento {} {}",
                usuario, peticion.getMethod(), peticion.getRequestURI());

        escribir(respuesta, HttpStatus.FORBIDDEN,
                "Acceso denegado",
                "Su rol no tiene permiso para realizar esta operacion.",
                "acceso-denegado", peticion.getRequestURI());
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado,
                          String titulo, String detalle, String tipo, String ruta)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("""
                {"type":"https://minierp.gt/errores/%s",\
                "title":"%s",\
                "status":%d,\
                "detail":"%s",\
                "marcaTiempo":"%s",\
                "ruta":"%s"}"""
                .formatted(tipo, titulo, estado.value(), detalle,
                        LocalDateTime.now(), ruta));
    }
}
