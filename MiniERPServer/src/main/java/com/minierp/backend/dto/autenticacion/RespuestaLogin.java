package com.minierp.backend.dto.autenticacion;

import com.minierp.backend.entidad.enums.NombreRol;
import java.time.LocalDateTime;

/**
 * Respuesta de un inicio de sesion exitoso.
 *
 * Incluye el rol para que el cliente Angular arme el menu sin tener que
 * decodificar el token. La autorizacion real siempre la decide el
 * servidor: lo que viaja aqui es solo para pintar la interfaz.
 */
public record RespuestaLogin(
        String token,
        String tipo,
        Long idUsuario,
        String usuario,
        String nombreCompleto,
        NombreRol rol,
        LocalDateTime expiraEn
) {
    public static RespuestaLogin de(String token, Long idUsuario, String usuario,
                                    String nombreCompleto, NombreRol rol,
                                    LocalDateTime expiraEn) {
        return new RespuestaLogin(token, "Bearer", idUsuario, usuario,
                nombreCompleto, rol, expiraEn);
    }
}
