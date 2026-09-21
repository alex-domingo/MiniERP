package com.minierp.backend.dto.operacion;

import com.minierp.backend.entidad.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResumen(
        Long idVenta, String numeroFactura, Long idCliente, String nombreCliente,
        String usuario, LocalDateTime fechaVenta, BigDecimal subtotal, BigDecimal iva, BigDecimal total
) {
    public static VentaResumen de(Venta v) {
        return new VentaResumen(v.getIdVenta(), v.getNumeroFactura(),
                v.getCliente().getIdCliente(), v.getCliente().getNombre(),
                v.getUsuario().getNombreUsuario(), v.getFechaVenta(),
                v.getSubtotal(), v.getIva(), v.getTotal());
    }
}
