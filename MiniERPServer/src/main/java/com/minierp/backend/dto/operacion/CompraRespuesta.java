package com.minierp.backend.dto.operacion;

import com.minierp.backend.entidad.Compra;
import com.minierp.backend.entidad.DetalleCompra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraRespuesta(
        Long idCompra,
        String numeroDocumento,
        Long idProveedor,
        String nitProveedor,
        String nombreProveedor,
        String usuario,
        LocalDateTime fechaCompra,
        BigDecimal total,
        String observaciones,
        List<Linea> lineas
) {
    public record Linea(Long idDetalleCompra, Long idProducto, String codigoProducto,
                        String nombreProducto, Integer cantidad, BigDecimal costoUnitario,
                        BigDecimal subtotal) {
        static Linea de(DetalleCompra d) {
            return new Linea(d.getIdDetalleCompra(), d.getProducto().getIdProducto(),
                    d.getProducto().getCodigo(), d.getProducto().getNombre(),
                    d.getCantidad(), d.getCostoUnitario(), d.getSubtotal());
        }
    }

    public static CompraRespuesta de(Compra c) {
        return new CompraRespuesta(c.getIdCompra(), c.getNumeroDocumento(),
                c.getProveedor().getIdProveedor(), c.getProveedor().getNit(),
                c.getProveedor().getNombre(), c.getUsuario().getNombreUsuario(),
                c.getFechaCompra(), c.getTotal(), c.getObservaciones(),
                c.getDetalles().stream().map(Linea::de).toList());
    }
}
