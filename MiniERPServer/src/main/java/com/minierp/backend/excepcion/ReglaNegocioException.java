package com.minierp.backend.excepcion;

/**
 * Una operacion viola una regla del negocio: codigo duplicado, monto
 * invalido, cliente inactivo, etc. Se traduce a HTTP 409 (Conflict),
 * no a 400: la peticion esta bien formada, lo que no se puede es
 * ejecutarla en el estado actual del sistema.
 */
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
