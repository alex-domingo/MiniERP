package com.minierp.backend.reporte;

import com.minierp.backend.dto.reporte.ReporteRespuesta;
import com.minierp.backend.dto.reporte.ReportesBitacora.Conteo;
import com.minierp.backend.dto.reporte.ReportesBitacora.Registro;
import com.minierp.backend.dto.reporte.ReportesBitacora.Resumen;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.reporte.SoporteReporte.Rango;
import com.minierp.backend.util.Texto;
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
import static com.minierp.backend.reporte.SoporteReporte.respuesta;

/**
 * Reporte de logs (area de Administracion): las interacciones de los
 * usuarios con los modulos del sistema, filtrables por fecha, usuario,
 * modulo, accion, resultado y texto libre.
 *
 * El resumen responde a "principales interacciones" (cuantas, en que
 * modulos, con que acciones, de que usuarios) y el detalle se pagina,
 * porque la bitacora es la tabla que mas crece.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ServicioReporteBitacora {

    private final NamedParameterJdbcTemplate jdbc;

    public ServicioReporteBitacora(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String FILTRO = """
            FROM   bitacora b
            WHERE  b.fecha_hora >= :inicio AND b.fecha_hora < :fin
              AND  (CAST(:usuario AS VARCHAR) IS NULL OR b.nombre_usuario ILIKE :usuario)
              AND  (CAST(:modulo  AS VARCHAR) IS NULL OR b.modulo = :modulo)
              AND  (CAST(:accion  AS VARCHAR) IS NULL OR b.accion = :accion)
              AND  (CAST(:exitoso AS BOOLEAN) IS NULL OR b.exitoso = :exitoso)
              AND  (CAST(:texto   AS VARCHAR) IS NULL OR b.descripcion ILIKE :texto)
            """;

    private static final String SQL_DETALLE = """
            SELECT b.id_bitacora, b.fecha_hora, b.nombre_usuario AS usuario, b.modulo, b.accion,
                   b.entidad_afectada, b.id_entidad, b.descripcion, b.direccion_ip, b.exitoso
            """ + FILTRO + """
            ORDER  BY b.fecha_hora DESC, b.id_bitacora DESC
            LIMIT  :tamano OFFSET :desplazamiento
            """;

    private static final String SQL_TOTALES = """
            SELECT COUNT(*)                                   AS total,
                   COUNT(*) FILTER (WHERE b.exitoso)          AS exitosos,
                   COUNT(*) FILTER (WHERE NOT b.exitoso)      AS fallidos,
                   COUNT(DISTINCT b.nombre_usuario)           AS usuarios_distintos
            """ + FILTRO;

    /** Conteo agrupado por una columna fija (nunca viene del usuario). */
    private static String sqlConteo(String columna) {
        return "SELECT " + columna + " AS clave, COUNT(*) AS cantidad " + FILTRO
                + " GROUP BY " + columna + " ORDER BY cantidad DESC, clave";
    }

    private record Totales(Long total, Long exitosos, Long fallidos, Long usuariosDistintos) {
    }

    public ReporteRespuesta<Resumen, Registro> consultar(LocalDate desde, LocalDate hasta, String usuario,
                                                         ModuloBitacora modulo, AccionBitacora accion,
                                                         Boolean exitoso, String texto,
                                                         int pagina, int tamano) {
        Rango rango = Rango.de(desde, hasta);
        SoporteReporte.validarPagina(pagina, tamano);

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("inicio", rango.inicio(), Types.TIMESTAMP)
                .addValue("fin", rango.fin(), Types.TIMESTAMP)
                .addValue("usuario", Texto.patronContiene(usuario), Types.VARCHAR)
                .addValue("modulo", modulo == null ? null : modulo.name(), Types.VARCHAR)
                .addValue("accion", accion == null ? null : accion.name(), Types.VARCHAR)
                .addValue("exitoso", exitoso, Types.BOOLEAN)
                .addValue("texto", Texto.patronContiene(texto), Types.VARCHAR)
                .addValue("tamano", tamano)
                .addValue("desplazamiento", (long) pagina * tamano);

        Totales t = jdbc.queryForObject(SQL_TOTALES, p, DataClassRowMapper.newInstance(Totales.class));
        var conteo = DataClassRowMapper.newInstance(Conteo.class);
        List<Conteo> porModulo = jdbc.query(sqlConteo("b.modulo"), p, conteo);
        List<Conteo> porAccion = jdbc.query(sqlConteo("b.accion"), p, conteo);
        List<Conteo> porUsuario = jdbc.query(sqlConteo("b.nombre_usuario"), p, conteo);
        Resumen resumen = new Resumen(t.total(), t.exitosos(), t.fallidos(), t.usuariosDistintos(),
                porModulo, porAccion, porUsuario);

        List<Registro> filas = jdbc.query(SQL_DETALLE, p, DataClassRowMapper.newInstance(Registro.class));

        var f = filtros("usuario", Texto.limpiar(usuario), "modulo", modulo, "accion", accion,
                "exitoso", exitoso, "texto", Texto.limpiar(texto));
        rango.agregarA(f);
        return respuesta("Bitácora de interacciones de los usuarios", f, resumen, filas,
                ReporteRespuesta.Pagina.de(pagina, tamano, t.total()));
    }
}
