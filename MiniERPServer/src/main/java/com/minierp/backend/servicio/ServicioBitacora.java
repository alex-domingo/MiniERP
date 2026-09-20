package com.minierp.backend.servicio;

import com.minierp.backend.entidad.Bitacora;
import com.minierp.backend.entidad.Usuario;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.repositorio.BitacoraRepositorio;
import com.minierp.backend.repositorio.UsuarioRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Escribe la bitacora de auditoria que alimenta el "Reporte de logs"
 * del enunciado.
 *
 * Dos politicas transaccionales distintas, a proposito:
 *
 *   registrar()        participa en la transaccion del negocio. Si la
 *                      operacion se revierte, su registro tambien: la
 *                      bitacora no debe afirmar que ocurrio algo que
 *                      nunca se guardo.
 *
 *   registrarAcceso()  usa una transaccion propia (REQUIRES_NEW). Un
 *                      intento de acceso fallido debe quedar grabado
 *                      precisamente porque la operacion fracaso.
 */
@Service
public class ServicioBitacora {

    private static final Logger log = LoggerFactory.getLogger(ServicioBitacora.class);

    private static final int MAX_DESCRIPCION = 400;
    private static final int MAX_USUARIO = 40;

    private final BitacoraRepositorio bitacoraRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioBitacora(BitacoraRepositorio bitacoraRepositorio,
                            UsuarioRepositorio usuarioRepositorio) {
        this.bitacoraRepositorio = bitacoraRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    /** Registra una accion del usuario autenticado sobre un modulo. */
    @Transactional
    public void registrar(ModuloBitacora modulo, AccionBitacora accion,
                          String entidadAfectada, Long idEntidad, String descripcion) {
        String nombreUsuario = usuarioActual();
        Usuario usuario = usuarioRepositorio.findByNombreUsuario(nombreUsuario).orElse(null);
        guardar(usuario, nombreUsuario, modulo, accion, entidadAfectada,
                idEntidad, descripcion, true);
    }

    /** Registra un evento de autenticacion, exitoso o no. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcceso(String nombreUsuario, Usuario usuario,
                                AccionBitacora accion, boolean exitoso, String descripcion) {
        guardar(usuario, nombreUsuario, ModuloBitacora.AUTENTICACION, accion,
                "usuario", usuario != null ? usuario.getIdUsuario() : null,
                descripcion, exitoso);
    }

    private void guardar(Usuario usuario, String nombreUsuario, ModuloBitacora modulo,
                         AccionBitacora accion, String entidadAfectada, Long idEntidad,
                         String descripcion, boolean exitoso) {
        try {
            Bitacora registro = new Bitacora();
            registro.setUsuario(usuario);
            registro.setNombreUsuario(recortar(nombreUsuario, MAX_USUARIO));
            registro.setModulo(modulo);
            registro.setAccion(accion);
            registro.setEntidadAfectada(entidadAfectada);
            registro.setIdEntidad(idEntidad);
            registro.setDescripcion(recortar(descripcion, MAX_DESCRIPCION));
            registro.setDireccionIp(direccionIp());
            registro.setExitoso(exitoso);
            bitacoraRepositorio.save(registro);
        } catch (RuntimeException ex) {
            // Un fallo al auditar no debe tumbar la operacion del
            // negocio, pero si tiene que quedar visible en el log.
            log.error("No se pudo escribir en la bitacora: {}", ex.getMessage(), ex);
        }
    }

    private String usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return "anonimo";
        }
        return autenticacion.getName();
    }

    /**
     * Toma X-Forwarded-For cuando existe, porque detras de un proxy la
     * direccion remota seria siempre la del proxy.
     */
    private String direccionIp() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes atributos)) {
            return null;
        }
        HttpServletRequest peticion = atributos.getRequest();
        String reenviada = peticion.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return recortar(reenviada.split(",")[0].trim(), 45);
        }
        return recortar(peticion.getRemoteAddr(), 45);
    }

    /** Las columnas tienen longitud fija; recortar evita que el INSERT falle. */
    private String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
