package com.minierp.backend.reporte;

import com.minierp.backend.dto.reporte.ReporteRespuesta;
import com.minierp.backend.dto.reporte.ReportesCompras.Compra;
import com.minierp.backend.dto.reporte.ReportesCompras.ProductoFrecuente;
import com.minierp.backend.dto.reporte.ReportesCompras.ProveedorTop;
import com.minierp.backend.dto.reporte.ReportesCompras.ResumenCompras;
import com.minierp.backend.reporte.SoporteReporte.Rango;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import static com.minierp.backend.reporte.SoporteReporte.filtros;
import static com.minierp.backend.reporte.SoporteReporte.limite;
import static com.minierp.backend.reporte.SoporteReporte.respuesta;

/**
 * Reportes de compras y proveedores (area de Compras).
 *
 * Los proveedores y productos desactivados siguen apareciendo: el
 * historial de compras se conserva aunque el elemento ya no se use.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ServicioReportesCompras {

    private final NamedParameterJdbcTemplate jdbc;

    public ServicioReportesCompras(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -----------------------------------------------------------------
    //  5. Compras realizadas dentro de un rango de fechas
    // -----------------------------------------------------------------
    private static final String CTE_COMPRAS = """
            WITH seleccion AS (
                SELECT co.id_compra, co.numero_documento, co.fecha_compra, co.id_proveedor,
                       pr.nombre AS proveedor, pr.nit AS nit_proveedor, u.nombre_usuario AS usuario,
                       (SELECT COUNT(*)        FROM detalle_compra d WHERE d.id_compra = co.id_compra) AS lineas,
                       (SELECT SUM(d.cantidad) FROM detalle_compra d WHERE d.id_compra = co.id_compra) AS unidades,
                       co.total
                FROM   compra    co
                JOIN   proveedor pr ON pr.id_proveedor = co.id_proveedor
                JOIN   usuario   u  ON u.id_usuario    = co.id_usuario
                WHERE  co.fecha_compra >= :inicio AND co.fecha_compra < :fin
                  AND  (CAST(:idProveedor AS BIGINT) IS NULL OR co.id_proveedor = :idProveedor)
            )
            """;

    private static final String SQL_COMPRAS = CTE_COMPRAS + """
            SELECT id_compra, numero_documento, fecha_compra, proveedor, nit_proveedor, usuario,
                   lineas, unidades, total
            FROM   seleccion
            ORDER  BY fecha_compra, id_compra
            """;

    private static final String SQL_RESUMEN_COMPRAS = CTE_COMPRAS + """
            SELECT COUNT(*)                     AS numero_compras,
                   COUNT(DISTINCT id_proveedor) AS proveedores_distintos,
                   COALESCE(SUM(unidades), 0)   AS unidades,
                   COALESCE(SUM(total), 0)      AS monto_total,
                   CASE WHEN COUNT(*) > 0 THEN ROUND(SUM(total) / COUNT(*), 2) ELSE 0 END
                                                AS promedio_por_compra
            FROM   seleccion
            """;

    public ReporteRespuesta<ResumenCompras, Compra> porFechas(LocalDate desde, LocalDate hasta,
                                                              Long idProveedor) {
        Rango rango = Rango.obligatorio(desde, hasta);
        MapSqlParameterSource p = parametros(rango).addValue("idProveedor", idProveedor, Types.BIGINT);
        ResumenCompras resumen = jdbc.queryForObject(SQL_RESUMEN_COMPRAS, p,
                DataClassRowMapper.newInstance(ResumenCompras.class));
        List<Compra> filas = jdbc.query(SQL_COMPRAS, p, DataClassRowMapper.newInstance(Compra.class));
        var f = filtros("idProveedor", idProveedor);
        rango.agregarA(f);
        return respuesta("Compras realizadas en el periodo", f, resumen, filas, null);
    }

    // -----------------------------------------------------------------
    //  6. Top de proveedores por monto acumulado de compras
    // -----------------------------------------------------------------
    //  La participacion se calcula contra el total de TODAS las compras
    //  del periodo (la ventana se evalua antes del LIMIT): "el primer
    //  proveedor concentra el 23% de lo comprado".
    private static final String SQL_TOP_PROVEEDORES = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY SUM(d.subtotal) DESC, pr.id_proveedor) AS INTEGER)
                       AS posicion,
                   pr.id_proveedor, pr.nit, pr.nombre,
                   COUNT(DISTINCT co.id_compra)   AS numero_compras,
                   COUNT(DISTINCT d.id_producto)  AS productos_distintos,
                   SUM(d.cantidad)                AS unidades,
                   SUM(d.subtotal)                AS monto_total,
                   ROUND(100 * SUM(d.subtotal) / NULLIF(SUM(SUM(d.subtotal)) OVER (), 0), 2)
                                                  AS participacion,
                   MAX(co.fecha_compra)           AS ultima_compra
            FROM   compra         co
            JOIN   detalle_compra d  ON d.id_compra     = co.id_compra
            JOIN   proveedor      pr ON pr.id_proveedor = co.id_proveedor
            WHERE  co.fecha_compra >= :inicio AND co.fecha_compra < :fin
            GROUP  BY pr.id_proveedor, pr.nit, pr.nombre
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, ProveedorTop> topProveedores(LocalDate desde, LocalDate hasta,
                                                               Integer limite) {
        Rango rango = Rango.de(desde, hasta);
        int n = limite(limite, 5);
        List<ProveedorTop> filas = jdbc.query(SQL_TOP_PROVEEDORES,
                parametros(rango).addValue("limite", n), DataClassRowMapper.newInstance(ProveedorTop.class));
        var f = filtros("limite", n);
        rango.agregarA(f);
        return respuesta("Top " + n + " proveedores por monto de compras", f, null, filas, null);
    }

    // -----------------------------------------------------------------
    //  7. Productos adquiridos con mayor frecuencia
    // -----------------------------------------------------------------
    //  Frecuencia = en cuantas compras distintas aparece el producto.
    //  Desempata por unidades: dos productos comprados 3 veces cada uno
    //  no pesan igual si uno llego de 5 en 5 y el otro de 40 en 40.
    private static final String SQL_PRODUCTOS_FRECUENTES = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY COUNT(DISTINCT co.id_compra) DESC, SUM(d.cantidad) DESC,
                                                    p.id_producto) AS INTEGER) AS posicion,
                   p.id_producto, p.codigo, p.nombre, c.nombre AS categoria,
                   COUNT(DISTINCT co.id_compra)    AS numero_compras,
                   COUNT(DISTINCT co.id_proveedor) AS proveedores_distintos,
                   SUM(d.cantidad)                 AS unidades,
                   SUM(d.subtotal)                 AS monto_total,
                   ROUND(SUM(d.subtotal) / SUM(d.cantidad), 4) AS costo_promedio,
                   MAX(co.fecha_compra)            AS ultima_compra
            FROM   detalle_compra d
            JOIN   compra    co ON co.id_compra    = d.id_compra
            JOIN   producto  p  ON p.id_producto   = d.id_producto
            JOIN   categoria c  ON c.id_categoria  = p.id_categoria
            WHERE  co.fecha_compra >= :inicio AND co.fecha_compra < :fin
              AND  (CAST(:idProveedor AS BIGINT) IS NULL OR co.id_proveedor = :idProveedor)
              AND  (CAST(:idCategoria AS BIGINT) IS NULL OR p.id_categoria = :idCategoria)
            GROUP  BY p.id_producto, p.codigo, p.nombre, c.nombre
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, ProductoFrecuente> productosFrecuentes(LocalDate desde, LocalDate hasta,
                                                                         Long idProveedor, Long idCategoria,
                                                                         Integer limite) {
        Rango rango = Rango.de(desde, hasta);
        int n = limite(limite, 10);
        List<ProductoFrecuente> filas = jdbc.query(SQL_PRODUCTOS_FRECUENTES, parametros(rango)
                        .addValue("idProveedor", idProveedor, Types.BIGINT)
                        .addValue("idCategoria", idCategoria, Types.BIGINT)
                        .addValue("limite", n),
                DataClassRowMapper.newInstance(ProductoFrecuente.class));
        var f = filtros("idProveedor", idProveedor, "idCategoria", idCategoria, "limite", n);
        rango.agregarA(f);
        return respuesta("Productos adquiridos con mayor frecuencia", f, null, filas, null);
    }

    private static MapSqlParameterSource parametros(Rango rango) {
        return new MapSqlParameterSource()
                .addValue("inicio", rango.inicio(), Types.TIMESTAMP)
                .addValue("fin", rango.fin(), Types.TIMESTAMP);
    }
}
