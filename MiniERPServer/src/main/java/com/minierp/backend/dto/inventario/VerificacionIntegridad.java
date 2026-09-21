package com.minierp.backend.dto.inventario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de las comprobaciones de consistencia del inventario. En un
 * sistema sano, todas dan cero fallos.
 */
public record VerificacionIntegridad(
        boolean consistente,
        LocalDateTime fecha,
        List<Comprobacion> comprobaciones
) {
    public record Comprobacion(String nombre, String descripcion, long fallos) {
    }
}
