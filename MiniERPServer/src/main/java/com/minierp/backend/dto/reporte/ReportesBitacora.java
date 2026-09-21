package com.minierp.backend.dto.reporte;

import java.time.LocalDateTime;
import java.util.List;

/** Filas y resumen del reporte de logs. */
public final class ReportesBitacora {

    private ReportesBitacora() {
    }

    public record Registro(
            Long idBitacora,
            LocalDateTime fechaHora,
            String usuario,
            String modulo,
            String accion,
            String entidadAfectada,
            Long idEntidad,
            String descripcion,
            String direccionIp,
            Boolean exitoso
    ) {
    }

    public record Conteo(String clave, Long cantidad) {
    }

    /**
     * "Principales interacciones": cuantas hubo, cuantas fallaron y en
     * que modulos, con que acciones y por que usuarios se concentran.
     */
    public record Resumen(
            Long total,
            Long exitosos,
            Long fallidos,
            Long usuariosDistintos,
            List<Conteo> porModulo,
            List<Conteo> porAccion,
            List<Conteo> porUsuario
    ) {
    }
}
