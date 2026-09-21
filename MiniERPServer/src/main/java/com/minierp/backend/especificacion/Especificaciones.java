package com.minierp.backend.especificacion;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Criterios de filtrado reutilizables para los listados.
 *
 * Responde al objetivo 6 del enunciado: "consultas que permitan analizar
 * la informacion mediante distintos criterios de busqueda y filtrado".
 *
 * Cada criterio devuelve una Specification que no restringe nada cuando
 * su parametro viene vacio. Asi se combinan con and() sin importar que
 * filtros envio el cliente: busqueda + categoria + estado, cualquier
 * subconjunto, sin escribir un metodo de repositorio por combinacion.
 */
public final class Especificaciones {

    private static final char ESCAPE = '\\';

    private Especificaciones() {
    }

    /** Filtra por estado. null = todos. */
    public static <T> Specification<T> activo(Boolean activo) {
        if (activo == null) {
            return Specification.unrestricted();
        }
        return (raiz, consulta, cb) -> cb.equal(raiz.get("activo"), activo);
    }

    /** Filtra por igualdad sobre un atributo anidado, ej. ("categoria", "idCategoria"). */
    public static <T> Specification<T> igual(Object valor, String relacion, String atributo) {
        if (valor == null) {
            return Specification.unrestricted();
        }
        return (raiz, consulta, cb) -> cb.equal(raiz.get(relacion).get(atributo), valor);
    }

    /**
     * Busqueda de texto libre: coincide si el texto aparece en
     * CUALQUIERA de los campos indicados, sin distinguir mayusculas.
     *
     * Los comodines % y _ que escriba el usuario se escapan: sin eso,
     * buscar "10%" o "a_b" daria resultados que no corresponden a lo
     * que la persona tecleo.
     */
    public static <T> Specification<T> texto(String texto, String... campos) {
        if (texto == null || texto.isBlank()) {
            return Specification.unrestricted();
        }
        String patron = "%" + escapar(texto.trim().toLowerCase()) + "%";
        return (raiz, consulta, cb) -> {
            List<Predicate> alternativas = new ArrayList<>();
            for (String campo : campos) {
                alternativas.add(cb.like(cb.lower(raiz.get(campo)), patron, ESCAPE));
            }
            return cb.or(alternativas.toArray(Predicate[]::new));
        };
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
