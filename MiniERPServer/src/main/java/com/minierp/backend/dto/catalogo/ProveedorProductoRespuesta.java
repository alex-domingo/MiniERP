package com.minierp.backend.dto.catalogo;

import com.minierp.backend.entidad.ProveedorProducto;
import java.math.BigDecimal;

public record ProveedorProductoRespuesta(
        Long idProveedor,
        String nombreProveedor,
        Long idProducto,
        String codigoProducto,
        String nombreProducto,
        BigDecimal costoReferencia
) {
    public static ProveedorProductoRespuesta de(ProveedorProducto asociacion) {
        return new ProveedorProductoRespuesta(
                asociacion.getProveedor().getIdProveedor(),
                asociacion.getProveedor().getNombre(),
                asociacion.getProducto().getIdProducto(),
                asociacion.getProducto().getCodigo(),
                asociacion.getProducto().getNombre(),
                asociacion.getCostoReferencia());
    }
}
