package com.minierp.backend.factura;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un monto en quetzales a letras, como se escribe en una
 * factura guatemalteca:
 *
 *   3865.12  ->  TRES MIL OCHOCIENTOS SESENTA Y CINCO QUETZALES CON 12/100
 *   21.00    ->  VEINTIÚN QUETZALES CON 00/100
 *   1.50     ->  UN QUETZAL CON 50/100
 *
 * Cuida la apocope que exige el espanol: "uno" se vuelve "un" delante de
 * "mil", "millones" y del sustantivo de la moneda ("veintiún mil",
 * "un millón", "cuarenta y un quetzales"), y "cien" solo es "cien" cuando
 * va solo ("ciento uno").
 */
public final class NumeroALetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS",
            "SIETE", "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
            "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDÓS",
            "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO",
            "VEINTINUEVE"};

    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA",
            "SETENTA", "OCHENTA", "NOVENTA"};

    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
            "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    /** Suficiente para cualquier monto que admite NUMERIC(14,2). */
    private static final long MAXIMO = 999_999_999_999L;

    private NumeroALetras() {
    }

    public static String quetzales(BigDecimal monto) {
        if (monto == null || monto.signum() < 0) {
            throw new IllegalArgumentException("El monto debe ser cero o positivo");
        }
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = redondeado.longValue();
        if (entero > MAXIMO) {
            throw new IllegalArgumentException("Monto fuera de rango: " + monto);
        }
        int centavos = redondeado.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        String letras;
        if (entero == 0) {
            letras = "CERO QUETZALES";
        } else if (entero == 1) {
            letras = "UN QUETZAL";
        } else {
            // "un millón de quetzales", pero "un millón cien quetzales"
            String de = entero % 1_000_000 == 0 ? " DE" : "";
            letras = apocopar(convertir(entero)) + de + " QUETZALES";
        }
        return letras + String.format(" CON %02d/100", centavos);
    }

    /** Numero entero en letras (forma masculina completa: "uno", "veintiuno"). */
    static String convertir(long n) {
        if (n == 0) {
            return "CERO";
        }
        StringBuilder texto = new StringBuilder();

        long millardos = n / 1_000_000_000;    // miles de millones
        long millones = (n / 1_000_000) % 1_000;
        long miles = (n / 1_000) % 1_000;
        int resto = (int) (n % 1_000);

        if (millardos > 0) {
            texto.append(millardos == 1 ? "MIL" : apocopar(centenas((int) millardos)) + " MIL");
            if (millones == 0) {
                texto.append(" MILLONES");
            }
        }
        if (millones > 0) {
            separar(texto);
            if (millones == 1 && millardos == 0) {
                texto.append("UN MILLÓN");
            } else {
                texto.append(apocopar(centenas((int) millones))).append(" MILLONES");
            }
        }
        if (miles > 0) {
            separar(texto);
            texto.append(miles == 1 ? "MIL" : apocopar(centenas((int) miles)) + " MIL");
        }
        if (resto > 0) {
            separar(texto);
            texto.append(centenas(resto));
        }
        return texto.toString();
    }

    /** 1..999 */
    private static String centenas(int n) {
        if (n == 100) {
            return "CIEN";
        }
        int c = n / 100;
        int r = n % 100;
        String texto = CENTENAS[c];
        if (r > 0) {
            texto = (texto.isEmpty() ? "" : texto + " ") + decenas(r);
        }
        return texto;
    }

    /** 1..99 */
    private static String decenas(int n) {
        if (n < 30) {
            return UNIDADES[n];
        }
        int d = n / 10;
        int u = n % 10;
        return u == 0 ? DECENAS[d] : DECENAS[d] + " Y " + UNIDADES[u];
    }

    /** "uno" -> "un" y "veintiuno" -> "veintiún" delante de un sustantivo. */
    private static String apocopar(String texto) {
        if (texto.endsWith("VEINTIUNO")) {
            return texto.substring(0, texto.length() - "VEINTIUNO".length()) + "VEINTIÚN";
        }
        if (texto.endsWith("UNO")) {
            return texto.substring(0, texto.length() - 1);
        }
        return texto;
    }

    private static void separar(StringBuilder texto) {
        if (!texto.isEmpty()) {
            texto.append(' ');
        }
    }
}
