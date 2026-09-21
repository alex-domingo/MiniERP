package com.minierp.backend.dto.operacion;

import com.minierp.backend.entidad.Compra;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del listado de compras: sin lineas, para no cargar el detalle de cada una. */
public record CompraResumen(
        Long idCompra, String numeroDocumento, Long idProveedor, String nombreProveedor,
        String usuario, LocalDateTime fechaCompra, BigDecimal total
) {
    public static CompraResumen de(Compra c) {
        return new CompraResumen(c.getIdCompra(), c.getNumeroDocumento(),
                c.getProveedor().getIdProveedor(), c.getProveedor().getNombre(),
                c.getUsuario().getNombreUsuario(), c.getFechaCompra(), c.getTotal());
    }
}
