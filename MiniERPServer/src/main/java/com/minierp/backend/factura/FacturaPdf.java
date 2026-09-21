package com.minierp.backend.factura;

/**
 * Factura generada. El id y el numero no son adorno: el aspecto de
 * auditoria los lee para dejar en la bitacora "Emision de factura PDF:
 * FAC-2026-00026 - Clinica Medica San Rafael".
 */
public record FacturaPdf(
        Long idVenta,
        String numeroFactura,
        String nombreCliente,
        byte[] contenido
) {

    public String nombreArchivo() {
        return numeroFactura + ".pdf";
    }
}
