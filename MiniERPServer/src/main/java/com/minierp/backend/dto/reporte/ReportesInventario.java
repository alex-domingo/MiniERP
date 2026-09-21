package com.minierp.backend.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Filas de los reportes de productos e inventario. */
public final class ReportesInventario {

    private ReportesInventario() {
    }

    /** Top de productos mas vendidos, por unidades. */
    public record MasVendido(
            Integer posicion,
            Long idProducto,
            String codigo,
            String nombre,
            String categoria,
            Long unidadesVendidas,
            Long numeroVentas,
            BigDecimal ingresos,
            Integer stockActual
    ) {
    }

    /** Productos con menor existencia disponible. */
    public record MenorExistencia(
            Integer posicion,
            Long idProducto,
            String codigo,
            String nombre,
            String categoria,
            Integer stockActual,
            Integer stockMinimo,
            Integer diferencia,
            String nivel,
            BigDecimal valorInventario,
            LocalDateTime ultimaEntrada
    ) {
    }

    /** Productos con mayor cantidad de movimientos. */
    public record MasMovimientos(
            Integer posicion,
            Long idProducto,
            String codigo,
            String nombre,
            String categoria,
            Long totalMovimientos,
            Long entradas,
            Long salidas,
            Long ajustes,
            Long unidadesEntrada,
            Long unidadesSalida,
            LocalDateTime ultimoMovimiento
    ) {
    }

    /** Un renglon del historial (kardex) de un producto. */
    public record Movimiento(
            Long idMovimiento,
            LocalDateTime fechaMovimiento,
            String tipoMovimiento,
            String origen,
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
    }

    /**
     * Cabecera del historial: el producto y como cambio su existencia en
     * el periodo. Se cumple siempre que
     * existenciaInicial + unidadesEntrada - unidadesSalida = existenciaFinal.
     */
    public record ResumenHistorial(
            Long idProducto,
            String codigo,
            String nombre,
            String categoria,
            Integer stockActual,
            Integer existenciaInicial,
            Long unidadesEntrada,
            Long unidadesSalida,
            Integer existenciaFinal,
            BigDecimal valorEntradas,
            BigDecimal valorSalidas,
            Long movimientos
    ) {
    }
}
