package com.minierp.backend.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Filas de los reportes de compras y proveedores. */
public final class ReportesCompras {

    private ReportesCompras() {
    }

    public record Compra(
            Long idCompra,
            String numeroDocumento,
            LocalDateTime fechaCompra,
            String proveedor,
            String nitProveedor,
            String usuario,
            Long lineas,
            Long unidades,
            BigDecimal total
    ) {
    }

    public record ResumenCompras(
            Long numeroCompras,
            Long proveedoresDistintos,
            Long unidades,
            BigDecimal montoTotal,
            BigDecimal promedioPorCompra
    ) {
    }

    public record ProveedorTop(
            Integer posicion,
            Long idProveedor,
            String nit,
            String nombre,
            Long numeroCompras,
            Long productosDistintos,
            Long unidades,
            BigDecimal montoTotal,
            BigDecimal participacion,
            LocalDateTime ultimaCompra
    ) {
    }

    public record ProductoFrecuente(
            Integer posicion,
            Long idProducto,
            String codigo,
            String nombre,
            String categoria,
            Long numeroCompras,
            Long proveedoresDistintos,
            Long unidades,
            BigDecimal montoTotal,
            BigDecimal costoPromedio,
            LocalDateTime ultimaCompra
    ) {
    }
}
