package com.minierp.backend.reporte;

import com.minierp.backend.dto.reporte.ReporteRespuesta;
import com.minierp.backend.dto.reporte.ReportesVentas.ClienteTop;
import com.minierp.backend.dto.reporte.ReportesVentas.Periodo;
import com.minierp.backend.dto.reporte.ReportesVentas.ProductoIngreso;
import com.minierp.backend.dto.reporte.ReportesVentas.ResumenVentas;
import com.minierp.backend.dto.reporte.ReportesVentas.Venta;
import com.minierp.backend.excepcion.SolicitudInvalidaException;
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
 * Reportes de ventas y clientes (area de Ventas).
 *
 * Los montos salen de lo que quedo guardado en cada venta (precio,
 * subtotal, tasa de IVA de ese dia), nunca del catalogo actual: una
 * factura de enero sigue valiendo lo que valio en enero.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ServicioReportesVentas {

    /** Tope de filas del resumen por periodo (p. ej. ~1.4 anios por dia). */
    static final int MAXIMO_PERIODOS = 500;

    private final NamedParameterJdbcTemplate jdbc;

    public ServicioReportesVentas(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -----------------------------------------------------------------
    //  8. Ventas realizadas dentro de un rango de fechas
    // -----------------------------------------------------------------
    private static final String CTE_VENTAS = """
            WITH seleccion AS (
                SELECT v.id_venta, v.numero_factura, v.fecha_venta, v.id_cliente,
                       cl.nombre AS cliente, cl.nit AS nit_cliente, u.nombre_usuario AS usuario,
                       (SELECT COUNT(*)        FROM detalle_venta d WHERE d.id_venta = v.id_venta) AS lineas,
                       (SELECT SUM(d.cantidad) FROM detalle_venta d WHERE d.id_venta = v.id_venta) AS unidades,
                       v.subtotal, v.iva, v.total
                FROM   venta   v
                JOIN   cliente cl ON cl.id_cliente = v.id_cliente
                JOIN   usuario u  ON u.id_usuario  = v.id_usuario
                WHERE  v.fecha_venta >= :inicio AND v.fecha_venta < :fin
                  AND  (CAST(:idCliente AS BIGINT) IS NULL OR v.id_cliente = :idCliente)
            )
            """;

    private static final String SQL_VENTAS = CTE_VENTAS + """
            SELECT id_venta, numero_factura, fecha_venta, cliente, nit_cliente, usuario,
                   lineas, unidades, subtotal, iva, total
            FROM   seleccion
            ORDER  BY fecha_venta, id_venta
            """;

    private static final String SQL_RESUMEN_VENTAS = CTE_VENTAS + """
            SELECT COUNT(*)                    AS numero_ventas,
                   COUNT(DISTINCT id_cliente)  AS clientes_distintos,
                   COALESCE(SUM(unidades), 0)  AS unidades,
                   COALESCE(SUM(subtotal), 0)  AS subtotal,
                   COALESCE(SUM(iva), 0)       AS iva,
                   COALESCE(SUM(total), 0)     AS total,
                   CASE WHEN COUNT(*) > 0 THEN ROUND(SUM(total) / COUNT(*), 2) ELSE 0 END
                                               AS ticket_promedio
            FROM   seleccion
            """;

    public ReporteRespuesta<ResumenVentas, Venta> porFechas(LocalDate desde, LocalDate hasta, Long idCliente) {
        Rango rango = Rango.obligatorio(desde, hasta);
        MapSqlParameterSource p = parametros(rango).addValue("idCliente", idCliente, Types.BIGINT);
        ResumenVentas resumen = jdbc.queryForObject(SQL_RESUMEN_VENTAS, p,
                DataClassRowMapper.newInstance(ResumenVentas.class));
        List<Venta> filas = jdbc.query(SQL_VENTAS, p, DataClassRowMapper.newInstance(Venta.class));
        var f = filtros("idCliente", idCliente);
        rango.agregarA(f);
        return respuesta("Ventas realizadas en el periodo", f, resumen, filas, null);
    }

    // -----------------------------------------------------------------
    //  9. Top de clientes por monto acumulado
    // -----------------------------------------------------------------
    //  Monto = lo que el cliente pago, IVA incluido.
    private static final String SQL_TOP_CLIENTES = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY SUM(v.total) DESC, cl.id_cliente) AS INTEGER) AS posicion,
                   cl.id_cliente, cl.nit, cl.nombre,
                   COUNT(*) AS numero_compras,
                   SUM((SELECT SUM(d.cantidad) FROM detalle_venta d WHERE d.id_venta = v.id_venta)) AS unidades,
                   SUM(v.total) AS monto_total,
                   ROUND(SUM(v.total) / COUNT(*), 2) AS ticket_promedio,
                   ROUND(100 * SUM(v.total) / NULLIF(SUM(SUM(v.total)) OVER (), 0), 2) AS participacion,
                   MAX(v.fecha_venta) AS ultima_compra
            FROM   venta   v
            JOIN   cliente cl ON cl.id_cliente = v.id_cliente
            WHERE  v.fecha_venta >= :inicio AND v.fecha_venta < :fin
            GROUP  BY cl.id_cliente, cl.nit, cl.nombre
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, ClienteTop> topClientes(LocalDate desde, LocalDate hasta, Integer limite) {
        Rango rango = Rango.de(desde, hasta);
        int n = limite(limite, 10);
        List<ClienteTop> filas = jdbc.query(SQL_TOP_CLIENTES, parametros(rango).addValue("limite", n),
                DataClassRowMapper.newInstance(ClienteTop.class));
        var f = filtros("limite", n);
        rango.agregarA(f);
        return respuesta("Top " + n + " clientes por monto de compras", f, null, filas, null);
    }

    // -----------------------------------------------------------------
    //  10. Top de productos por ingresos generados
    // -----------------------------------------------------------------
    private static final String SQL_TOP_PRODUCTOS = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY SUM(d.subtotal) DESC, p.id_producto) AS INTEGER) AS posicion,
                   p.id_producto, p.codigo, p.nombre, c.nombre AS categoria,
                   SUM(d.cantidad)            AS unidades,
                   COUNT(DISTINCT v.id_venta) AS numero_ventas,
                   SUM(d.subtotal)            AS ingresos,
                   ROUND(SUM(d.subtotal) / SUM(d.cantidad), 2) AS precio_promedio,
                   ROUND(100 * SUM(d.subtotal) / NULLIF(SUM(SUM(d.subtotal)) OVER (), 0), 2) AS participacion
            FROM   detalle_venta d
            JOIN   venta     v ON v.id_venta     = d.id_venta
            JOIN   producto  p ON p.id_producto  = d.id_producto
            JOIN   categoria c ON c.id_categoria = p.id_categoria
            WHERE  v.fecha_venta >= :inicio AND v.fecha_venta < :fin
              AND  (CAST(:idCategoria AS BIGINT) IS NULL OR p.id_categoria = :idCategoria)
            GROUP  BY p.id_producto, p.codigo, p.nombre, c.nombre
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, ProductoIngreso> topProductos(LocalDate desde, LocalDate hasta,
                                                                Long idCategoria, Integer limite) {
        Rango rango = Rango.de(desde, hasta);
        int n = limite(limite, 10);
        List<ProductoIngreso> filas = jdbc.query(SQL_TOP_PRODUCTOS, parametros(rango)
                        .addValue("idCategoria", idCategoria, Types.BIGINT)
                        .addValue("limite", n),
                DataClassRowMapper.newInstance(ProductoIngreso.class));
        var f = filtros("idCategoria", idCategoria, "limite", n);
        rango.agregarA(f);
        return respuesta("Top " + n + " productos por ingresos", f, null, filas, null);
    }

    // -----------------------------------------------------------------
    //  11. Resumen de ventas agrupado por periodo
    // -----------------------------------------------------------------
    //  generate_series produce TODOS los periodos del rango, tambien los
    //  que no tuvieron ventas: un mes en cero es informacion (y una
    //  grafica sin ese hueco mentiria sobre la tendencia).
    private static final String SQL_POR_PERIODO = """
            WITH periodos AS (
                SELECT generate_series(date_trunc(:unidad, CAST(:primerDia AS TIMESTAMP)),
                                       date_trunc(:unidad, CAST(:ultimoDia AS TIMESTAMP)),
                                       CAST(:intervalo AS INTERVAL)) AS inicio
            ),
            ventas AS (
                SELECT date_trunc(:unidad, v.fecha_venta) AS inicio, v.id_venta, v.id_cliente,
                       v.subtotal, v.iva, v.total,
                       (SELECT SUM(d.cantidad) FROM detalle_venta d WHERE d.id_venta = v.id_venta) AS unidades
                FROM   venta v
                WHERE  v.fecha_venta >= :inicio AND v.fecha_venta < :fin
            )
            SELECT CAST(pe.inicio AS DATE)          AS periodo,
                   CAST(NULL AS VARCHAR)            AS etiqueta,
                   COUNT(ve.id_venta)               AS numero_ventas,
                   COUNT(DISTINCT ve.id_cliente)    AS clientes_distintos,
                   COALESCE(SUM(ve.unidades), 0)    AS unidades,
                   COALESCE(SUM(ve.subtotal), 0)    AS subtotal,
                   COALESCE(SUM(ve.iva), 0)         AS iva,
                   COALESCE(SUM(ve.total), 0)       AS total,
                   CASE WHEN COUNT(ve.id_venta) > 0
                        THEN ROUND(SUM(ve.total) / COUNT(ve.id_venta), 2) ELSE 0 END AS ticket_promedio
            FROM   periodos pe
            LEFT   JOIN ventas ve ON ve.inicio = pe.inicio
            GROUP  BY pe.inicio
            ORDER  BY pe.inicio
            """;

    public ReporteRespuesta<ResumenVentas, Periodo> porPeriodo(LocalDate desde, LocalDate hasta,
                                                               AgrupacionPeriodo agrupacion) {
        AgrupacionPeriodo a = agrupacion == null ? AgrupacionPeriodo.MES : agrupacion;
        Rango.de(desde, hasta);   // valida el orden de las fechas recibidas

        // Sin fechas, el resumen abarca desde la primera venta hasta hoy.
        LocalDate primerDia = desde;
        if (primerDia == null) {
            primerDia = jdbc.queryForObject("SELECT CAST(MIN(fecha_venta) AS DATE) FROM venta",
                    new MapSqlParameterSource(), LocalDate.class);
        }
        LocalDate ultimoDia = hasta == null ? LocalDate.now() : hasta;
        if (primerDia == null || primerDia.isAfter(ultimoDia)) {
            primerDia = ultimoDia;
        }
        Rango rango = Rango.de(primerDia, ultimoDia);

        if (a.periodos(primerDia, ultimoDia) > MAXIMO_PERIODOS) {
            throw new SolicitudInvalidaException("El rango genera mas de " + MAXIMO_PERIODOS
                    + " periodos. Acorte las fechas o use una agrupacion mayor (SEMANA, MES...)");
        }

        MapSqlParameterSource p = parametros(rango)
                .addValue("unidad", a.unidadSql())
                .addValue("intervalo", a.intervaloSql())
                .addValue("primerDia", primerDia.atStartOfDay(), Types.TIMESTAMP)
                .addValue("ultimoDia", ultimoDia.atStartOfDay(), Types.TIMESTAMP)
                .addValue("idCliente", null, Types.BIGINT);

        List<Periodo> filas = jdbc.query(SQL_POR_PERIODO, p, DataClassRowMapper.newInstance(Periodo.class))
                .stream().map(fila -> fila.conEtiqueta(a.etiqueta(fila.periodo()))).toList();
        ResumenVentas resumen = jdbc.queryForObject(SQL_RESUMEN_VENTAS, p,
                DataClassRowMapper.newInstance(ResumenVentas.class));

        var f = filtros("agrupacion", a);
        rango.agregarA(f);
        String titulo = "Resumen de ventas por " + a.nombre();
        return respuesta(titulo, f, resumen, filas, null);
    }

    private static MapSqlParameterSource parametros(Rango rango) {
        return new MapSqlParameterSource()
                .addValue("inicio", rango.inicio(), Types.TIMESTAMP)
                .addValue("fin", rango.fin(), Types.TIMESTAMP);
    }
}
