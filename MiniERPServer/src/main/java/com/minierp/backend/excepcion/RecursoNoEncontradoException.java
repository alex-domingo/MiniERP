package com.minierp.backend.excepcion;

/**
 * El recurso solicitado no existe. Se traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /** Ejemplo: de("Producto", 42) -> "Producto con identificador 42 no existe". */
    public static RecursoNoEncontradoException de(String entidad, Object identificador) {
        return new RecursoNoEncontradoException(
                entidad + " con identificador " + identificador + " no existe");
    }
}
