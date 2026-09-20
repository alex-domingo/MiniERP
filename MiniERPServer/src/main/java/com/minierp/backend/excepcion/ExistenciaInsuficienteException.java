package com.minierp.backend.excepcion;

/**
 * Se intento registrar una salida mayor a las existencias disponibles.
 *
 * Es la regla mas importante del enunciado: "No deberan permitirse
 * operaciones que provoquen existencias negativas o inconsistentes".
 * Tiene excepcion propia para que la respuesta pueda decirle al cliente
 * exactamente cuanto habia disponible.
 */
public class ExistenciaInsuficienteException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    private final String codigoProducto;
    private final int solicitado;
    private final int disponible;

    public ExistenciaInsuficienteException(String codigoProducto, String nombreProducto,
                                           int solicitado, int disponible) {
        super("Existencias insuficientes para el producto " + codigoProducto + " ("
                + nombreProducto + "): se solicitaron " + solicitado
                + " unidades y hay " + disponible + " disponibles");
        this.codigoProducto = codigoProducto;
        this.solicitado = solicitado;
        this.disponible = disponible;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public int getSolicitado() {
        return solicitado;
    }

    public int getDisponible() {
        return disponible;
    }
}
