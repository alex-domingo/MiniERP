package com.minierp.backend.dto.operacion;

import com.minierp.backend.entidad.DetalleVenta;
import com.minierp.backend.entidad.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venta con sus lineas. No expone el costo UEPS de cada linea: el area
 * de Ventas no debe ver costos ni margenes. Ese dato vive en el kardex,
 * restringido a Administracion, Compras e Inventario.
 */
public record VentaRespuesta(
        Long idVenta,
        String numeroFactura,
        Long idCliente,
        String nitCliente,
        String nombreCliente,
        String usuario,
        LocalDateTime fechaVenta,
        BigDecimal subtotal,
        BigDecimal porcentajeIva,
        BigDecimal iva,
        BigDecimal total,
        List<Linea> lineas
) {
    public record Linea(Long idDetalleVenta, Long idProducto, String codigoProducto,
                        String nombreProducto, Integer cantidad, BigDecimal precioUnitario,
                        BigDecimal subtotal) {
        static Linea de(DetalleVenta d) {
            return new Linea(d.getIdDetalleVenta(), d.getProducto().getIdProducto(),
                    d.getProducto().getCodigo(), d.getProducto().getNombre(),
                    d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
        }
    }

    public static VentaRespuesta de(Venta v) {
        return new VentaRespuesta(v.getIdVenta(), v.getNumeroFactura(),
                v.getCliente().getIdCliente(), v.getCliente().getNit(), v.getCliente().getNombre(),
                v.getUsuario().getNombreUsuario(), v.getFechaVenta(), v.getSubtotal(),
                v.getPorcentajeIva(), v.getIva(), v.getTotal(),
                v.getDetalles().stream().map(Linea::de).toList());
    }
}
