package com.minierp.backend.dto.comun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltura uniforme de los listados paginados.
 *
 * Se devuelve esto y no el Page de Spring Data porque su forma JSON
 * incluye detalles internos del framework y no es estable entre
 * versiones. El cliente Angular depende de este contrato, no del de
 * Spring.
 */
public record RespuestaPaginada<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
    /** Convierte un Page de entidades en respuesta de DTO. */
    public static <E, D> RespuestaPaginada<D> de(Page<E> pagina, Function<E, D> convertidor) {
        return new RespuestaPaginada<>(
                pagina.getContent().stream().map(convertidor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }
}
