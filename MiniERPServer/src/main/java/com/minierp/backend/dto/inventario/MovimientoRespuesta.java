package com.minierp.backend.dto.inventario;

import com.minierp.backend.entidad.MovimientoInventario;
import com.minierp.backend.entidad.enums.OrigenMovimiento;
import com.minierp.backend.entidad.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Una fila del kardex: que cambio, por que, con que documento y quien. */
public record MovimientoRespuesta(
        Long idMovimiento,
        Long idProducto,
        String codigoProducto,
        String nombreProducto,
        LocalDateTime fechaMovimiento,
        TipoMovimiento tipoMovimiento,
        OrigenMovimiento origen,
        String documento,
        String tercero,
        Integer cantidad,
        BigDecimal costoUnitario,
        BigDecimal valorMovimiento,
        Integer existenciaAnterior,
        Integer existenciaNueva,
        String usuario,
        String observaciones
) {
    public static MovimientoRespuesta de(MovimientoInventario m) {
        String documento = "AJUSTE";
        String tercero = null;
        if (m.getCompra() != null) {
            documento = m.getCompra().getNumeroDocumento();
            tercero = m.getCompra().getProveedor().getNombre();
        } else if (m.getVenta() != null) {
            documento = m.getVenta().getNumeroFactura();
            tercero = m.getVenta().getCliente().getNombre();
        }
        return new MovimientoRespuesta(m.getIdMovimiento(),
                m.getProducto().getIdProducto(), m.getProducto().getCodigo(), m.getProducto().getNombre(),
                m.getFechaMovimiento(), m.getTipoMovimiento(), m.getOrigen(), documento, tercero,
                m.getCantidad(), m.getCostoUnitario(),
                m.getCostoUnitario().multiply(BigDecimal.valueOf(m.getCantidad())).setScale(2, RoundingMode.HALF_UP),
                m.getExistenciaAnterior(), m.getExistenciaNueva(),
                m.getUsuario().getNombreUsuario(), m.getObservaciones());
    }
}
