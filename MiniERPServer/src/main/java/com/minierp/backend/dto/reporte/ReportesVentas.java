package com.minierp.backend.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Filas de los reportes de ventas y clientes.
 *
 * Ninguna expone costos: estos reportes los consulta el area de Ventas,
 * que no tiene acceso a la valuacion del inventario.
 */
public final class ReportesVentas {

    private ReportesVentas() {
    }

    public record Venta(
            Long idVenta,
            String numeroFactura,
            LocalDateTime fechaVenta,
            String cliente,
            String nitCliente,
            String usuario,
            Long lineas,
            Long unidades,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total
    ) {
    }

    public record ResumenVentas(
            Long numeroVentas,
            Long clientesDistintos,
            Long unidades,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            BigDecimal ticketPromedio
    ) {
    }

    public record ClienteTop(
            Integer posicion,
            Long idCliente,
            String nit,
            String nombre,
            Long numeroCompras,
            Long unidades,
            BigDecimal montoTotal,
            BigDecimal ticketPromedio,
            BigDecimal participacion,
            LocalDateTime ultimaCompra
    ) {
    }

    /** Ingresos = suma de subtotales de linea, sin IVA (el IVA no es ingreso). */
    public record ProductoIngreso(
            Integer posicion,
            Long idProducto,
            String codigo,
            String nombre,
            String categoria,
            Long unidades,
            Long numeroVentas,
            BigDecimal ingresos,
            BigDecimal precioPromedio,
            BigDecimal participacion
    ) {
    }

    public record Periodo(
            LocalDate periodo,
            String etiqueta,
            Long numeroVentas,
            Long clientesDistintos,
            Long unidades,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            BigDecimal ticketPromedio
    ) {

        public Periodo conEtiqueta(String nueva) {
            return new Periodo(periodo, nueva, numeroVentas, clientesDistintos, unidades,
                    subtotal, iva, total, ticketPromedio);
        }
    }
}
