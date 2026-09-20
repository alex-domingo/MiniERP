package com.minierp.backend.servicio;

import com.minierp.backend.dto.autenticacion.PerfilUsuario;
import com.minierp.backend.dto.autenticacion.PeticionLogin;
import com.minierp.backend.dto.autenticacion.RespuestaLogin;
import com.minierp.backend.entidad.Usuario;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.repositorio.UsuarioRepositorio;
import com.minierp.backend.seguridad.DetalleUsuario;
import com.minierp.backend.seguridad.ServicioJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Inicio de sesion y consulta del perfil.
 */
@Service
public class ServicioAutenticacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacion.class);

    private final AuthenticationManager gestorAutenticacion;
    private final ServicioJwt servicioJwt;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ServicioBitacora servicioBitacora;

    public ServicioAutenticacion(AuthenticationManager gestorAutenticacion,
                                 ServicioJwt servicioJwt,
                                 UsuarioRepositorio usuarioRepositorio,
                                 ServicioBitacora servicioBitacora) {
        this.gestorAutenticacion = gestorAutenticacion;
        this.servicioJwt = servicioJwt;
        this.usuarioRepositorio = usuarioRepositorio;
        this.servicioBitacora = servicioBitacora;
    }

    /**
     * Valida credenciales y emite el token.
     *
     * Ante un fallo se registra el intento y se relanza SIEMPRE el
     * mismo mensaje, sin distinguir si el usuario no existe o si la
     * contrasena es incorrecta: diferenciarlos permitiria averiguar
     * que cuentas son validas probando nombres.
     */
    @Transactional
    public RespuestaLogin iniciarSesion(PeticionLogin peticion) {
        try {
            Authentication autenticacion = gestorAutenticacion.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            peticion.usuario(), peticion.contrasena()));

            DetalleUsuario detalle = (DetalleUsuario) autenticacion.getPrincipal();

            Usuario usuario = usuarioRepositorio.findById(detalle.getIdUsuario())
                    .orElseThrow(() -> RecursoNoEncontradoException.de(
                            "Usuario", detalle.getIdUsuario()));
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepositorio.save(usuario);

            String token = servicioJwt.generarToken(
                    detalle.getUsername(), detalle.getIdUsuario(), detalle.getRol().name());

            servicioBitacora.registrarAcceso(detalle.getUsername(), usuario,
                    AccionBitacora.LOGIN, true,
                    "Inicio de sesion exitoso del area " + detalle.getRol());

            log.info("Sesion iniciada: '{}' ({})", detalle.getUsername(), detalle.getRol());

            return RespuestaLogin.de(token, detalle.getIdUsuario(), detalle.getUsername(),
                    detalle.getNombreCompleto(), detalle.getRol(),
                    servicioJwt.calcularExpiracion());

        } catch (DisabledException ex) {
            registrarFallo(peticion.usuario(), "Cuenta desactivada");
            throw new BadCredentialsException("Usuario o contrasena incorrectos");

        } catch (AuthenticationException ex) {
            registrarFallo(peticion.usuario(), "Credenciales incorrectas");
            throw new BadCredentialsException("Usuario o contrasena incorrectos");
        }
    }

    @Transactional(readOnly = true)
    public PerfilUsuario perfil(String nombreUsuario) {
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuarioConRol(nombreUsuario)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", nombreUsuario));

        return new PerfilUsuario(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().getNombre(),
                usuario.getRol().getDescripcion(),
                usuario.getUltimoAcceso());
    }

    @Transactional
    public void cerrarSesion(String nombreUsuario) {
        Usuario usuario = usuarioRepositorio.findByNombreUsuario(nombreUsuario).orElse(null);
        servicioBitacora.registrarAcceso(nombreUsuario, usuario,
                AccionBitacora.LOGOUT, true, "Cierre de sesion");
    }

    private void registrarFallo(String nombreUsuario, String motivo) {
        Usuario usuario = usuarioRepositorio.findByNombreUsuario(nombreUsuario).orElse(null);
        servicioBitacora.registrarAcceso(nombreUsuario, usuario,
                AccionBitacora.LOGIN_FALLIDO, false,
                "Intento de acceso fallido: " + motivo);
        log.warn("Intento de acceso fallido para '{}': {}", nombreUsuario, motivo);
    }
}
