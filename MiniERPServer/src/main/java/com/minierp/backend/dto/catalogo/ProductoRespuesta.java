package com.minierp.backend.dto.catalogo;

import com.minierp.backend.entidad.Producto;
import java.math.BigDecimal;

public record ProductoRespuesta(
        Long idProducto,
        String codigo,
        String nombre,
        String descripcion,
        Long idCategoria,
        String nombreCategoria,
        String unidadMedida,
        BigDecimal precioVenta,
        Integer stockActual,
        Integer stockMinimo,
        boolean requiereAtencion,
        Boolean activo
) {
    public static ProductoRespuesta de(Producto producto) {
        return new ProductoRespuesta(
                producto.getIdProducto(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria().getIdCategoria(),
                producto.getCategoria().getNombre(),
                producto.getUnidadMedida(),
                producto.getPrecioVenta(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.requiereAtencion(),
                producto.getActivo());
    }
}
