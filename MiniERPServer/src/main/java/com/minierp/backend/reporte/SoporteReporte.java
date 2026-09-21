package com.minierp.backend.reporte;

import com.minierp.backend.dto.reporte.ReporteRespuesta;
import com.minierp.backend.excepcion.SolicitudInvalidaException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Piezas comunes a todos los reportes: rango de fechas, limite de filas,
 * eco de filtros y el envoltorio de la respuesta.
 */
final class SoporteReporte {

    /** Limites efectivos cuando el usuario no acota el rango. */
    private static final LocalDateTime SIN_INICIO = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime SIN_FIN = LocalDateTime.of(9999, 1, 1, 0, 0);

    static final int LIMITE_MAXIMO = 100;

    private SoporteReporte() {
    }

    /**
     * Rango de fechas de calendario, ambos extremos incluidos. En SQL se
     * traduce a [inicio, fin): fecha >= desde 00:00 y fecha < (hasta + 1) 00:00,
     * para que una venta del 31 a las 23:59 entre en "hasta el 31".
     */
    record Rango(LocalDate desde, LocalDate hasta) {

        static Rango de(LocalDate desde, LocalDate hasta) {
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                throw new SolicitudInvalidaException("La fecha inicial no puede ser posterior a la final");
            }
            return new Rango(desde, hasta);
        }

        /** Para los reportes que el enunciado define "dentro de un rango de fechas". */
        static Rango obligatorio(LocalDate desde, LocalDate hasta) {
            if (desde == null || hasta == null) {
                throw new SolicitudInvalidaException(
                        "Este reporte requiere las fechas 'desde' y 'hasta' (formato AAAA-MM-DD)");
            }
            return de(desde, hasta);
        }

        LocalDateTime inicio() {
            return desde == null ? SIN_INICIO : desde.atStartOfDay();
        }

        LocalDateTime fin() {
            return hasta == null ? SIN_FIN : hasta.plusDays(1).atStartOfDay();
        }

        void agregarA(Map<String, Object> filtros) {
            if (desde != null) {
                filtros.put("desde", desde);
            }
            if (hasta != null) {
                filtros.put("hasta", hasta);
            }
        }
    }

    static int limite(Integer solicitado, int porDefecto) {
        int valor = solicitado == null ? porDefecto : solicitado;
        if (valor < 1 || valor > LIMITE_MAXIMO) {
            throw new SolicitudInvalidaException("El limite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        return valor;
    }

    static void validarPagina(int pagina, int tamano) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException("La pagina no puede ser negativa");
        }
        if (tamano < 1 || tamano > LIMITE_MAXIMO) {
            throw new SolicitudInvalidaException("El tamano de pagina debe estar entre 1 y " + LIMITE_MAXIMO);
        }
    }

    /** Mapa ordenado que ignora los filtros no enviados. */
    static Map<String, Object> filtros(Object... claveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            if (claveValor[i + 1] != null) {
                mapa.put((String) claveValor[i], claveValor[i + 1]);
            }
        }
        return mapa;
    }

    static <R, F> ReporteRespuesta<R, F> respuesta(String titulo, Map<String, Object> filtros,
                                                   R resumen, List<F> filas,
                                                   ReporteRespuesta.Pagina pagina) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String usuario = autenticacion == null ? null : autenticacion.getName();
        return new ReporteRespuesta<>(titulo, LocalDateTime.now(), usuario, filtros,
                resumen, filas, pagina);
    }
}
