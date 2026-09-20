package com.minierp.backend.dto.autenticacion;

import com.minierp.backend.entidad.enums.NombreRol;
import java.time.LocalDateTime;

/**
 * Datos del usuario autenticado. Nunca incluye la contrasena, ni
 * siquiera su hash.
 */
public record PerfilUsuario(
        Long idUsuario,
        String usuario,
        String nombreCompleto,
        String correo,
        NombreRol rol,
        String descripcionRol,
        LocalDateTime ultimoAcceso
) {
}
