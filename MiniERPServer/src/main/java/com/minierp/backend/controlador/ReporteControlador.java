package com.minierp.backend.controlador;

import com.minierp.backend.dto.reporte.ReporteRespuesta;
import com.minierp.backend.dto.reporte.ReportesBitacora;
import com.minierp.backend.dto.reporte.ReportesCompras;
import com.minierp.backend.dto.reporte.ReportesInventario;
import com.minierp.backend.dto.reporte.ReportesVentas;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.entidad.enums.OrigenMovimiento;
import com.minierp.backend.entidad.enums.TipoMovimiento;
import com.minierp.backend.reporte.AgrupacionPeriodo;
import com.minierp.backend.reporte.ServicioReporteBitacora;
import com.minierp.backend.reporte.ServicioReportesCompras;
import com.minierp.backend.reporte.ServicioReportesInventario;
import com.minierp.backend.reporte.ServicioReportesVentas;
import com.minierp.backend.servicio.ServicioBitacora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Modulo de reportes. Todas las fechas van en formato ISO (AAAA-MM-DD)
 * y ambos extremos del rango se incluyen.
 *
 * Permisos (ver ConfiguracionSeguridad):
 *   /inventario/**  Administracion e Inventario
 *   /compras/**     Administracion y Compras
 *   /ventas/**      Administracion y Ventas
 *   /bitacora       solo Administracion
 *
 * Cada consulta queda registrada en la bitacora (REPORTES / CONSULTAR)
 * con los filtros usados.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteControlador {

    private static final Logger log = LoggerFactory.getLogger(ReporteControlador.class);

    private final ServicioReportesInventario inventario;
    private final ServicioReportesCompras compras;
    private final ServicioReportesVentas ventas;
    private final ServicioReporteBitacora bitacora;
    private final ServicioBitacora servicioBitacora;

    public ReporteControlador(ServicioReportesInventario inventario, ServicioReportesCompras compras,
                              ServicioReportesVentas ventas, ServicioReporteBitacora bitacora,
                              ServicioBitacora servicioBitacora) {
        this.inventario = inventario;
        this.compras = compras;
        this.ventas = ventas;
        this.bitacora = bitacora;
        this.servicioBitacora = servicioBitacora;
    }

    // =================================================================
    //  Productos e inventario
    // =================================================================

    @GetMapping("/inventario/mas-vendidos")
    public ReporteRespuesta<Void, ReportesInventario.MasVendido> masVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Integer limite) {
        return registrar(inventario.masVendidos(desde, hasta, idCategoria, limite), null);
    }

    @GetMapping("/inventario/menor-existencia")
    public ReporteRespuesta<Void, ReportesInventario.MenorExistencia> menorExistencia(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(defaultValue = "false") boolean soloAlertas,
            @RequestParam(required = false) Integer limite) {
        return registrar(inventario.menorExistencia(idCategoria, soloAlertas, limite), null);
    }

    @GetMapping("/inventario/mas-movimientos")
    public ReporteRespuesta<Void, ReportesInventario.MasMovimientos> masMovimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Integer limite) {
        return registrar(inventario.masMovimientos(desde, hasta, idCategoria, limite), null);
    }

    @GetMapping("/inventario/historial/{idProducto}")
    public ReporteRespuesta<ReportesInventario.ResumenHistorial, ReportesInventario.Movimiento> historial(
            @PathVariable Long idProducto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) OrigenMovimiento origen,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return registrar(inventario.historial(idProducto, desde, hasta, tipo, origen, pagina, tamano), idProducto);
    }

    // =================================================================
    //  Compras y proveedores
    // =================================================================

    @GetMapping("/compras/por-fechas")
    public ReporteRespuesta<ReportesCompras.ResumenCompras, ReportesCompras.Compra> comprasPorFechas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idProveedor) {
        return registrar(compras.porFechas(desde, hasta, idProveedor), null);
    }

    @GetMapping("/compras/top-proveedores")
    public ReporteRespuesta<Void, ReportesCompras.ProveedorTop> topProveedores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite) {
        return registrar(compras.topProveedores(desde, hasta, limite), null);
    }

    @GetMapping("/compras/productos-frecuentes")
    public ReporteRespuesta<Void, ReportesCompras.ProductoFrecuente> productosFrecuentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idProveedor,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Integer limite) {
        return registrar(compras.productosFrecuentes(desde, hasta, idProveedor, idCategoria, limite), null);
    }

    // =================================================================
    //  Ventas y clientes
    // =================================================================

    @GetMapping("/ventas/por-fechas")
    public ReporteRespuesta<ReportesVentas.ResumenVentas, ReportesVentas.Venta> ventasPorFechas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idCliente) {
        return registrar(ventas.porFechas(desde, hasta, idCliente), null);
    }

    @GetMapping("/ventas/top-clientes")
    public ReporteRespuesta<Void, ReportesVentas.ClienteTop> topClientes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite) {
        return registrar(ventas.topClientes(desde, hasta, limite), null);
    }

    @GetMapping("/ventas/top-productos")
    public ReporteRespuesta<Void, ReportesVentas.ProductoIngreso> topProductos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Integer limite) {
        return registrar(ventas.topProductos(desde, hasta, idCategoria, limite), null);
    }

    @GetMapping("/ventas/por-periodo")
    public ReporteRespuesta<ReportesVentas.ResumenVentas, ReportesVentas.Periodo> ventasPorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AgrupacionPeriodo agrupacion) {
        return registrar(ventas.porPeriodo(desde, hasta, agrupacion), null);
    }

    // =================================================================
    //  Logs
    // =================================================================

    @GetMapping("/bitacora")
    public ReporteRespuesta<ReportesBitacora.Resumen, ReportesBitacora.Registro> bitacora(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) ModuloBitacora modulo,
            @RequestParam(required = false) AccionBitacora accion,
            @RequestParam(required = false) Boolean exitoso,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return registrar(bitacora.consultar(desde, hasta, usuario, modulo, accion, exitoso, texto,
                pagina, tamano), null);
    }

    // -----------------------------------------------------------------

    /**
     * Deja constancia de la consulta DESPUES de que el reporte se genero
     * (una consulta rechazada por filtros invalidos no es una consulta).
     * Se hace fuera de la transaccion de solo lectura del reporte, que no
     * admitiria el INSERT.
     */
    private <R, F> ReporteRespuesta<R, F> registrar(ReporteRespuesta<R, F> reporte, Long idEntidad) {
        try {
            String filtros = reporte.filtros().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
            String descripcion = "Consulta de reporte: " + reporte.titulo()
                    + (filtros.isEmpty() ? "" : " [" + filtros + "]");
            servicioBitacora.registrar(ModuloBitacora.REPORTES, AccionBitacora.CONSULTAR, "reporte",
                    idEntidad, descripcion.length() > 400 ? descripcion.substring(0, 397) + "..." : descripcion);
        } catch (RuntimeException ex) {
            log.error("No se pudo registrar la consulta del reporte en la bitacora: {}", ex.getMessage(), ex);
        }
        return reporte;
    }
}
