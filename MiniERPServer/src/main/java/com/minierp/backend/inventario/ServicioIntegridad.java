package com.minierp.backend.inventario;

import com.minierp.backend.config.PropiedadesNegocio;
import com.minierp.backend.dto.inventario.VerificacionIntegridad;
import com.minierp.backend.dto.inventario.VerificacionIntegridad.Comprobacion;
import com.minierp.backend.entidad.enums.MetodoValuacion;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprobaciones de consistencia del inventario, en SQL nativo.
 *
 * Cada comprobacion cuenta las filas que violan un invariante. En un
 * sistema sano todas devuelven cero. Son las mismas que se usaron para
 * validar la carga inicial, y sirven para demostrar en cualquier momento
 * que compras, ventas y ajustes dejaron el inventario cuadrado.
 */
@Service
public class ServicioIntegridad {

    private final EntityManager em;
    private final PropiedadesNegocio negocio;

    public ServicioIntegridad(EntityManager em, PropiedadesNegocio negocio) {
        this.em = em;
        this.negocio = negocio;
    }

    @Transactional(readOnly = true)
    public VerificacionIntegridad verificar() {
        List<Comprobacion> resultados = new ArrayList<>();

        agregar(resultados, "Existencia contra kardex",
                "Productos cuyo stock no coincide con la suma de entradas y salidas del kardex",
                "SELECT COUNT(*) FROM vw_existencia_calculada WHERE diferencia <> 0");

        agregar(resultados, "Existencia contra capas de costo",
                "Productos cuyo stock no coincide con las unidades disponibles en sus capas",
                """
                SELECT COUNT(*) FROM producto p
                WHERE p.stock_actual <> COALESCE(
                      (SELECT SUM(c.cantidad_disponible) FROM capa_inventario c
                       WHERE c.id_producto = p.id_producto), 0)
                """);

        agregar(resultados, "Saldo de capas contra consumos",
                "Capas cuyo saldo no es igual a su cantidad inicial menos lo consumido",
                """
                SELECT COUNT(*) FROM capa_inventario c
                WHERE c.cantidad_inicial - COALESCE(
                      (SELECT SUM(cc.cantidad) FROM consumo_capa cc WHERE cc.id_capa = c.id_capa), 0)
                      <> c.cantidad_disponible
                """);

        agregar(resultados, "Lineas de venta cubiertas por capas",
                "Lineas de venta cuyas unidades no salieron integramente de capas de costo",
                """
                SELECT COUNT(*) FROM detalle_venta dv
                WHERE dv.cantidad <> COALESCE(
                      (SELECT SUM(cc.cantidad) FROM consumo_capa cc
                       WHERE cc.id_detalle_venta = dv.id_detalle_venta), 0)
                """);

        agregar(resultados, "Costo de venta contra capas consumidas",
                "Lineas de venta cuyo costo unitario no es el promedio ponderado de sus consumos",
                """
                SELECT COUNT(*) FROM detalle_venta dv
                WHERE dv.costo_unitario <> ROUND(
                      (SELECT SUM(cc.costo_total) FROM consumo_capa cc
                       WHERE cc.id_detalle_venta = dv.id_detalle_venta) / dv.cantidad, 4)
                """);

        agregar(resultados, "Ajustes de salida cubiertos por capas",
                "Ajustes de salida cuyas unidades no salieron integramente de capas de costo",
                """
                SELECT COUNT(*) FROM movimiento_inventario m
                WHERE m.origen = 'AJUSTE_SALIDA'
                  AND m.cantidad <> COALESCE(
                      (SELECT SUM(cc.cantidad) FROM consumo_capa cc
                       WHERE cc.id_movimiento_ajuste = m.id_movimiento), 0)
                """);

        agregar(resultados, "Orden de consumo " + negocio.getMetodoValuacion(),
                negocio.getMetodoValuacion() == MetodoValuacion.UEPS
                        ? "Consumos que tomaron una capa habiendo otra MAS RECIENTE con saldo"
                        : "Consumos que tomaron una capa habiendo otra MAS ANTIGUA con saldo",
                sqlOrdenDeConsumo(negocio.getMetodoValuacion()));

        agregar(resultados, "Totales de compras",
                "Compras cuyo total no es la suma de sus lineas",
                """
                SELECT COUNT(*) FROM compra c
                WHERE c.total <> (SELECT COALESCE(SUM(d.subtotal), 0) FROM detalle_compra d
                                  WHERE d.id_compra = c.id_compra)
                """);

        agregar(resultados, "Totales de ventas",
                "Ventas cuyo subtotal no es la suma de sus lineas, o cuyo IVA no corresponde a su tasa",
                """
                SELECT COUNT(*) FROM venta v
                WHERE v.subtotal <> (SELECT COALESCE(SUM(d.subtotal), 0) FROM detalle_venta d
                                     WHERE d.id_venta = v.id_venta)
                   OR v.iva <> ROUND(v.subtotal * v.porcentaje_iva, 2)
                   OR v.total <> v.subtotal + v.iva
                """);

        agregar(resultados, "Existencias negativas",
                "Productos con existencia menor que cero",
                "SELECT COUNT(*) FROM producto WHERE stock_actual < 0");

        boolean consistente = resultados.stream().allMatch(r -> r.fallos() == 0);
        return new VerificacionIntegridad(consistente, LocalDateTime.now(), resultados);
    }

    /**
     * Un consumo viola el orden si, en el momento en que ocurrio, existia
     * otra capa del mismo producto con saldo que el metodo debio tomar
     * antes. El saldo de esa capa se reconstruye con los consumos
     * ANTERIORES en la secuencia real de procesamiento (id_consumo), no
     * por fecha: asi se cuenta bien el caso de una venta que agota una
     * capa y continua con la siguiente dentro de la misma operacion.
     */
    private String sqlOrdenDeConsumo(MetodoValuacion metodo) {
        String prioridad = metodo == MetodoValuacion.UEPS
                ? "(n.fecha_entrada, n.id_capa) > (x.fecha_capa, x.id_capa)"
                : "(n.fecha_entrada, n.id_capa) < (x.fecha_capa, x.id_capa)";
        return """
                WITH contexto AS (
                    SELECT cc.id_consumo, cap.id_producto, cap.id_capa,
                           cap.fecha_entrada AS fecha_capa,
                           COALESCE(v.fecha_venta, ma.fecha_movimiento) AS fecha_evento
                    FROM consumo_capa cc
                    JOIN capa_inventario cap ON cap.id_capa = cc.id_capa
                    LEFT JOIN detalle_venta dv ON dv.id_detalle_venta = cc.id_detalle_venta
                    LEFT JOIN venta v ON v.id_venta = dv.id_venta
                    LEFT JOIN movimiento_inventario ma ON ma.id_movimiento = cc.id_movimiento_ajuste
                )
                SELECT COUNT(DISTINCT x.id_consumo)
                FROM contexto x
                JOIN capa_inventario n
                  ON n.id_producto = x.id_producto
                 AND n.fecha_entrada <= x.fecha_evento
                 AND %s
                WHERE n.cantidad_inicial > COALESCE(
                      (SELECT SUM(c2.cantidad) FROM consumo_capa c2
                       WHERE c2.id_capa = n.id_capa AND c2.id_consumo < x.id_consumo), 0)
                """.formatted(prioridad);
    }

    private void agregar(List<Comprobacion> lista, String nombre, String descripcion, String sql) {
        Number fallos = (Number) em.createNativeQuery(sql).getSingleResult();
        lista.add(new Comprobacion(nombre, descripcion, fallos.longValue()));
    }
}
