package com.minierp.backend.dto.reporte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Envoltorio comun de todos los reportes.
 *
 * Ademas de los datos, devuelve el encabezado que cualquier reporte
 * impreso necesita: que es, cuando se genero, quien lo pidio y con que
 * filtros. Asi el cliente no tiene que reconstruirlo y lo que muestra
 * coincide siempre con lo que el servidor calculo.
 *
 * @param <R> tipo del resumen (totales del periodo); puede ser null
 * @param <F> tipo de cada fila
 */
public record ReporteRespuesta<R, F>(
        String titulo,
        LocalDateTime generado,
        String generadoPor,
        Map<String, Object> filtros,
        R resumen,
        List<F> filas,
        Pagina pagina
) {

    /** Solo presente en los reportes paginados (historial y bitacora). */
    public record Pagina(int numero, int tamano, long totalElementos, int totalPaginas) {

        public static Pagina de(int numero, int tamano, long total) {
            return new Pagina(numero, tamano, total, (int) ((total + tamano - 1) / tamano));
        }
    }
}
