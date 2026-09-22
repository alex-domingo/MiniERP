package com.minierp.backend.reporte;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;

/**
 * Agrupaciones admitidas por el resumen de ventas.
 *
 * Cada una se traduce a una unidad de date_trunc de PostgreSQL y a un
 * intervalo para generate_series; el usuario elige de esta lista cerrada
 * y nunca escribe SQL, de modo que no hay nada que inyectar.
 */
public enum AgrupacionPeriodo {

    DIA("día", "day", "1 day", ChronoUnit.DAYS),
    SEMANA("semana", "week", "1 week", ChronoUnit.WEEKS),
    MES("mes", "month", "1 month", ChronoUnit.MONTHS),
    TRIMESTRE("trimestre", "quarter", "3 months", ChronoUnit.MONTHS),
    ANIO("año", "year", "1 year", ChronoUnit.YEARS);

    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private final String nombre;
    private final String unidadSql;
    private final String intervaloSql;
    private final ChronoUnit unidad;

    AgrupacionPeriodo(String nombre, String unidadSql, String intervaloSql, ChronoUnit unidad) {
        this.nombre = nombre;
        this.unidadSql = unidadSql;
        this.intervaloSql = intervaloSql;
        this.unidad = unidad;
    }

    /** Nombre en espanol para titulos ("Resumen de ventas por año"). */
    String nombre() {
        return nombre;
    }

    String unidadSql() {
        return unidadSql;
    }

    String intervaloSql() {
        return intervaloSql;
    }

    /** Cuantos periodos genera el rango; sirve para frenar consultas desmedidas. */
    long periodos(LocalDate desde, LocalDate hasta) {
        long n = unidad.between(desde, hasta) + 1;
        return this == TRIMESTRE ? n / 3 + 1 : n;
    }

    /** Texto legible del periodo que empieza en la fecha dada. */
    String etiqueta(LocalDate inicio) {
        return switch (this) {
            case DIA -> inicio.toString();
            case SEMANA -> "Semana " + inicio.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                    + " (" + inicio + " al " + inicio.plusDays(6) + ")";
            case MES -> MESES[inicio.getMonthValue() - 1] + " " + inicio.getYear();
            case TRIMESTRE -> "T" + inicio.get(IsoFields.QUARTER_OF_YEAR) + " " + inicio.getYear();
            case ANIO -> String.valueOf(inicio.getYear());
        };
    }
}
