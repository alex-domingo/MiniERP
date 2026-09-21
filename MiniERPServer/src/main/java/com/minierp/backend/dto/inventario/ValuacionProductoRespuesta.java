package com.minierp.backend.dto.inventario;

import com.minierp.backend.entidad.enums.MetodoValuacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Existencia de un producto desglosada por capa de costo. Muestra, en
 * el orden en que el metodo las va a consumir, a que costo entro cada
 * unidad que queda en bodega.
 */
public record ValuacionProductoRespuesta(
        Long idProducto,
        String codigo,
        String nombre,
        MetodoValuacion metodo,
        Integer stockActual,
        Integer unidadesEnCapas,
        BigDecimal valorTotal,
        BigDecimal costoPromedio,
        List<Capa> capas
) {
    public record Capa(Long idCapa, LocalDateTime fechaEntrada, String origen, BigDecimal costoUnitario,
                       Integer cantidadInicial, Integer cantidadDisponible, BigDecimal valorDisponible) {
    }
}
