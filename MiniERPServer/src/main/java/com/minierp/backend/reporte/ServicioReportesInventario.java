package com.minierp.backend.reporte;

import com.minierp.backend.dto.reporte.ReporteRespuesta;
import com.minierp.backend.dto.reporte.ReportesInventario.MasMovimientos;
import com.minierp.backend.dto.reporte.ReportesInventario.MasVendido;
import com.minierp.backend.dto.reporte.ReportesInventario.MenorExistencia;
import com.minierp.backend.dto.reporte.ReportesInventario.Movimiento;
import com.minierp.backend.dto.reporte.ReportesInventario.ResumenHistorial;
import com.minierp.backend.entidad.enums.OrigenMovimiento;
import com.minierp.backend.entidad.enums.TipoMovimiento;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.reporte.SoporteReporte.Rango;
import org.springframework.dao.EmptyResultDataAccessException;
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
 * Reportes de productos e inventario (area de Inventario).
 *
 * Todas las consultas son SQL nativo sobre las tablas y vistas creadas
 * por Flyway. REPEATABLE_READ hace que el resumen y las filas de un
 * mismo reporte salgan de la misma foto de la base, aunque entre una
 * consulta y otra alguien registre una venta.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ServicioReportesInventario {

    private final NamedParameterJdbcTemplate jdbc;

    public ServicioReportesInventario(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -----------------------------------------------------------------
    //  1. Top de productos mas vendidos
    // -----------------------------------------------------------------
    private static final String SQL_MAS_VENDIDOS = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY SUM(dv.cantidad) DESC, SUM(dv.subtotal) DESC,
                                                    p.id_producto) AS INTEGER) AS posicion,
                   p.id_producto, p.codigo, p.nombre, c.nombre AS categoria,
                   SUM(dv.cantidad)           AS unidades_vendidas,
                   COUNT(DISTINCT v.id_venta) AS numero_ventas,
                   SUM(dv.subtotal)           AS ingresos,
                   p.stock_actual
            FROM   detalle_venta dv
            JOIN   venta     v ON v.id_venta     = dv.id_venta
            JOIN   producto  p ON p.id_producto  = dv.id_producto
            JOIN   categoria c ON c.id_categoria = p.id_categoria
            WHERE  v.fecha_venta >= :inicio AND v.fecha_venta < :fin
              AND  (CAST(:idCategoria AS BIGINT) IS NULL OR p.id_categoria = :idCategoria)
            GROUP  BY p.id_producto, p.codigo, p.nombre, c.nombre, p.stock_actual
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, MasVendido> masVendidos(LocalDate desde, LocalDate hasta,
                                                          Long idCategoria, Integer limite) {
        Rango rango = Rango.de(desde, hasta);
        int n = limite(limite, 10);
        List<MasVendido> filas = jdbc.query(SQL_MAS_VENDIDOS, parametros(rango)
                        .addValue("idCategoria", idCategoria, Types.BIGINT)
                        .addValue("limite", n),
                DataClassRowMapper.newInstance(MasVendido.class));
        var f = filtros("idCategoria", idCategoria, "limite", n);
        rango.agregarA(f);
        return respuesta("Top " + n + " productos mas vendidos", f, null, filas, null);
    }

    // -----------------------------------------------------------------
    //  2. Productos con menor existencia disponible
    // -----------------------------------------------------------------
    //  El nivel usa las mismas reglas que vw_alerta_stock, mas NORMAL
    //  para los que estan por encima del minimo: el reporte muestra los
    //  de menor existencia aunque ninguno este en alerta.
    private static final String SQL_MENOR_EXISTENCIA = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY p.stock_actual, p.stock_actual - p.stock_minimo,
                                                    p.id_producto) AS INTEGER) AS posicion,
                   p.id_producto, p.codigo, p.nombre, c.nombre AS categoria,
                   p.stock_actual, p.stock_minimo,
                   p.stock_actual - p.stock_minimo AS diferencia,
                   CASE WHEN p.stock_actual = 0              THEN 'SIN_EXISTENCIA'
                        WHEN p.stock_actual < p.stock_minimo THEN 'CRITICO'
                        WHEN p.stock_actual = p.stock_minimo THEN 'EN_MINIMO'
                        ELSE 'NORMAL'
                   END AS nivel,
                   COALESCE((SELECT SUM(ci.cantidad_disponible * ci.costo_unitario)
                             FROM capa_inventario ci
                             WHERE ci.id_producto = p.id_producto AND ci.cantidad_disponible > 0), 0)
                       AS valor_inventario,
                   (SELECT MAX(m.fecha_movimiento) FROM movimiento_inventario m
                    WHERE m.id_producto = p.id_producto AND m.tipo_movimiento = 'ENTRADA') AS ultima_entrada
            FROM   producto  p
            JOIN   categoria c ON c.id_categoria = p.id_categoria
            WHERE  p.activo = TRUE
              AND  (CAST(:idCategoria AS BIGINT) IS NULL OR p.id_categoria = :idCategoria)
              AND  (:soloAlertas = FALSE OR p.stock_actual <= p.stock_minimo)
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, MenorExistencia> menorExistencia(Long idCategoria, boolean soloAlertas,
                                                                   Integer limite) {
        int n = limite(limite, 10);
        List<MenorExistencia> filas = jdbc.query(SQL_MENOR_EXISTENCIA, new MapSqlParameterSource()
                        .addValue("idCategoria", idCategoria, Types.BIGINT)
                        .addValue("soloAlertas", soloAlertas)
                        .addValue("limite", n),
                DataClassRowMapper.newInstance(MenorExistencia.class));
        var f = filtros("idCategoria", idCategoria, "soloAlertas", soloAlertas, "limite", n);
        return respuesta("Productos con menor existencia disponible", f, null, filas, null);
    }

    // -----------------------------------------------------------------
    //  3. Productos con mayor cantidad de movimientos
    // -----------------------------------------------------------------
    private static final String SQL_MAS_MOVIMIENTOS = """
            SELECT CAST(ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC, SUM(m.cantidad) DESC,
                                                    p.id_producto) AS INTEGER) AS posicion,
                   p.id_producto, p.codigo, p.nombre, c.nombre AS categoria,
                   COUNT(*) AS total_movimientos,
                   COUNT(*) FILTER (WHERE m.tipo_movimiento = 'ENTRADA') AS entradas,
                   COUNT(*) FILTER (WHERE m.tipo_movimiento = 'SALIDA')  AS salidas,
                   COUNT(*) FILTER (WHERE m.origen IN ('AJUSTE_ENTRADA', 'AJUSTE_SALIDA')) AS ajustes,
                   COALESCE(SUM(m.cantidad) FILTER (WHERE m.tipo_movimiento = 'ENTRADA'), 0) AS unidades_entrada,
                   COALESCE(SUM(m.cantidad) FILTER (WHERE m.tipo_movimiento = 'SALIDA'), 0)  AS unidades_salida,
                   MAX(m.fecha_movimiento) AS ultimo_movimiento
            FROM   movimiento_inventario m
            JOIN   producto  p ON p.id_producto  = m.id_producto
            JOIN   categoria c ON c.id_categoria = p.id_categoria
            WHERE  m.fecha_movimiento >= :inicio AND m.fecha_movimiento < :fin
              AND  (CAST(:idCategoria AS BIGINT) IS NULL OR p.id_categoria = :idCategoria)
            GROUP  BY p.id_producto, p.codigo, p.nombre, c.nombre
            ORDER  BY posicion
            LIMIT  :limite
            """;

    public ReporteRespuesta<Void, MasMovimientos> masMovimientos(LocalDate desde, LocalDate hasta,
                                                                 Long idCategoria, Integer limite) {
        Rango rango = Rango.de(desde, hasta);
        int n = limite(limite, 10);
        List<MasMovimientos> filas = jdbc.query(SQL_MAS_MOVIMIENTOS, parametros(rango)
                        .addValue("idCategoria", idCategoria, Types.BIGINT)
                        .addValue("limite", n),
                DataClassRowMapper.newInstance(MasMovimientos.class));
        var f = filtros("idCategoria", idCategoria, "limite", n);
        rango.agregarA(f);
        return respuesta("Productos con mayor cantidad de movimientos", f, null, filas, null);
    }

    // -----------------------------------------------------------------
    //  4. Historial de movimientos de un producto
    // -----------------------------------------------------------------
    //  Orden cronologico (el kardex impreso se lee de arriba hacia abajo).
    //  La existencia inicial es la que dejo el ultimo movimiento anterior
    //  al periodo; la final, la que dejo el ultimo movimiento del periodo.
    //  Los filtros de tipo y origen afectan solo al detalle: el resumen
    //  describe siempre el periodo completo, porque una existencia final
    //  "solo de las compras" no tendria sentido.
    private static final String SQL_RESUMEN_HISTORIAL = """
            SELECT p.id_producto, p.codigo, p.nombre, c.nombre AS categoria, p.stock_actual,
                   COALESCE((SELECT m.existencia_nueva FROM movimiento_inventario m
                             WHERE m.id_producto = p.id_producto AND m.fecha_movimiento < :inicio
                             ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC LIMIT 1), 0)
                       AS existencia_inicial,
                   COALESCE(SUM(m.cantidad) FILTER (WHERE m.tipo_movimiento = 'ENTRADA'), 0) AS unidades_entrada,
                   COALESCE(SUM(m.cantidad) FILTER (WHERE m.tipo_movimiento = 'SALIDA'), 0)  AS unidades_salida,
                   COALESCE((SELECT m.existencia_nueva FROM movimiento_inventario m
                             WHERE m.id_producto = p.id_producto AND m.fecha_movimiento < :fin
                             ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC LIMIT 1), 0)
                       AS existencia_final,
                   COALESCE(SUM(m.cantidad * m.costo_unitario) FILTER (WHERE m.tipo_movimiento = 'ENTRADA'), 0)
                       AS valor_entradas,
                   COALESCE(SUM(m.cantidad * m.costo_unitario) FILTER (WHERE m.tipo_movimiento = 'SALIDA'), 0)
                       AS valor_salidas,
                   COUNT(m.id_movimiento) AS movimientos
            FROM   producto  p
            JOIN   categoria c ON c.id_categoria = p.id_categoria
            LEFT   JOIN movimiento_inventario m ON m.id_producto = p.id_producto
                                             AND m.fecha_movimiento >= :inicio
                                             AND m.fecha_movimiento <  :fin
            WHERE  p.id_producto = :idProducto
            GROUP  BY p.id_producto, p.codigo, p.nombre, c.nombre, p.stock_actual
            """;

    private static final String FILTRO_HISTORIAL = """
            FROM   vw_kardex k
            WHERE  k.id_producto = :idProducto
              AND  k.fecha_movimiento >= :inicio AND k.fecha_movimiento < :fin
              AND  (CAST(:tipo AS VARCHAR) IS NULL OR k.tipo_movimiento = :tipo)
              AND  (CAST(:origen AS VARCHAR) IS NULL OR k.origen = :origen)
            """;

    private static final String SQL_DETALLE_HISTORIAL = """
            SELECT k.id_movimiento, k.fecha_movimiento, k.tipo_movimiento, k.origen, k.documento,
                   k.tercero, k.cantidad, k.costo_unitario,
                   ROUND(k.valor_movimiento, 2) AS valor_movimiento,
                   k.existencia_anterior, k.existencia_nueva, k.usuario, k.observaciones
            """ + FILTRO_HISTORIAL + """
            ORDER  BY k.fecha_movimiento, k.id_movimiento
            LIMIT  :tamano OFFSET :desplazamiento
            """;

    public ReporteRespuesta<ResumenHistorial, Movimiento> historial(Long idProducto, LocalDate desde,
                                                                    LocalDate hasta, TipoMovimiento tipo,
                                                                    OrigenMovimiento origen,
                                                                    int pagina, int tamano) {
        Rango rango = Rango.de(desde, hasta);
        SoporteReporte.validarPagina(pagina, tamano);
        MapSqlParameterSource p = parametros(rango)
                .addValue("idProducto", idProducto)
                .addValue("tipo", tipo == null ? null : tipo.name(), Types.VARCHAR)
                .addValue("origen", origen == null ? null : origen.name(), Types.VARCHAR)
                .addValue("tamano", tamano)
                .addValue("desplazamiento", (long) pagina * tamano);

        ResumenHistorial resumen;
        try {
            resumen = jdbc.queryForObject(SQL_RESUMEN_HISTORIAL, p,
                    DataClassRowMapper.newInstance(ResumenHistorial.class));
        } catch (EmptyResultDataAccessException ex) {
            throw RecursoNoEncontradoException.de("Producto", idProducto);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FILTRO_HISTORIAL, p, Long.class);
        List<Movimiento> filas = jdbc.query(SQL_DETALLE_HISTORIAL, p,
                DataClassRowMapper.newInstance(Movimiento.class));

        var f = filtros("idProducto", idProducto, "tipo", tipo, "origen", origen);
        rango.agregarA(f);
        return respuesta("Historial de movimientos: " + resumen.codigo() + " - " + resumen.nombre(),
                f, resumen, filas, ReporteRespuesta.Pagina.de(pagina, tamano, total == null ? 0 : total));
    }

    private static MapSqlParameterSource parametros(Rango rango) {
        return new MapSqlParameterSource()
                .addValue("inicio", rango.inicio(), Types.TIMESTAMP)
                .addValue("fin", rango.fin(), Types.TIMESTAMP);
    }
}
