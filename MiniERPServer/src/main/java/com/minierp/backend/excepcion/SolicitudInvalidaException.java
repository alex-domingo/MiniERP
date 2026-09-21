package com.minierp.backend.excepcion;

/**
 * La peticion trae un parametro que no tiene sentido: un campo de
 * ordenamiento que no existe, una pagina negativa, un rango de fechas
 * invertido. Se traduce a HTTP 400.
 *
 * Se distingue de ReglaNegocioException (409): aqui el error esta en
 * como se formulo la peticion, no en el estado del sistema.
 */
public class SolicitudInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
