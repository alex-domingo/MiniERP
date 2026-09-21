package com.minierp.backend.util;

/**
 * Normalizacion de texto de entrada, aplicada en los servicios antes de
 * guardar.
 *
 * Existe porque la validacion no basta: @Email, por ejemplo, considera
 * valida la cadena vacia "", y sin normalizar terminaria guardada en la
 * base como si fuera un correo.
 */
public final class Texto {

    private Texto() {
    }

    /** Recorta espacios y convierte la cadena vacia en null. */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    /** Recorta y pasa a mayusculas; null si queda vacio. */
    public static String mayusculas(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toUpperCase();
    }

    /**
     * Lleva un NIT a una forma canonica unica, para que la restriccion
     * UNIQUE funcione de verdad: "12345678", "1234567-8" y " 1234567-8 "
     * son el mismo NIT y deben chocar entre si.
     *
     * Forma canonica: digitos, guion y digito verificador ("1234567-8").
     * "CF" (consumidor final) se conserva tal cual.
     */
    public static String normalizarNit(String nit) {
        String limpio = mayusculas(nit);
        if (limpio == null) {
            return null;
        }
        String compacto = limpio.replace(" ", "").replace("-", "");
        if ("CF".equals(compacto) || compacto.length() < 2) {
            return compacto;
        }
        return compacto.substring(0, compacto.length() - 1)
                + "-" + compacto.charAt(compacto.length() - 1);
    }

    /** Patron comun para @Pattern de los DTO que reciben NIT. */
    public static final String PATRON_NIT = "^\\s*(?i:CF|[0-9]{1,15}\\s*-?\\s*[0-9K])\\s*$";
}
