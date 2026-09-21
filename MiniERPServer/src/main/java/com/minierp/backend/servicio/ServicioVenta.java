package com.minierp.backend.servicio;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.config.PropiedadesNegocio;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.dto.operacion.LineaVentaPeticion;
import com.minierp.backend.dto.operacion.VentaPeticion;
import com.minierp.backend.dto.operacion.VentaRespuesta;
import com.minierp.backend.dto.operacion.VentaResumen;
import com.minierp.backend.entidad.*;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.especificacion.Especificaciones;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.excepcion.ReglaNegocioException;
import com.minierp.backend.inventario.ServicioInventario;
import com.minierp.backend.repositorio.ClienteRepositorio;
import com.minierp.backend.repositorio.DetalleVentaRepositorio;
import com.minierp.backend.repositorio.VentaRepositorio;
import com.minierp.backend.seguridad.UsuarioActual;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro y consulta de ventas. Area de Ventas.
 *
 * El enunciado exige que "antes de completar una venta, el sistema
 * debera garantizar que la operacion pueda realizarse de acuerdo con las
 * existencias disponibles". Por eso primero se bloquean y validan TODOS
 * los productos, y solo si todos alcanzan se escribe algo. Si una linea
 * falla, no queda ni la venta, ni las otras lineas, ni un solo
 * movimiento.
 */
@Service
public class ServicioVenta {

    private final VentaRepositorio ventaRepositorio;
    private final DetalleVentaRepositorio detalleRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ServicioInventario inventario;
    private final UsuarioActual usuarioActual;
    private final PropiedadesNegocio negocio;

    public ServicioVenta(VentaRepositorio ventaRepositorio,
                         DetalleVentaRepositorio detalleRepositorio,
                         ClienteRepositorio clienteRepositorio,
                         ServicioInventario inventario,
                         UsuarioActual usuarioActual,
                         PropiedadesNegocio negocio) {
        this.ventaRepositorio = ventaRepositorio;
        this.detalleRepositorio = detalleRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.inventario = inventario;
        this.usuarioActual = usuarioActual;
        this.negocio = negocio;
    }

    @Auditable(modulo = ModuloBitacora.VENTAS, accion = AccionBitacora.CREAR,
               entidad = "venta", descripcion = "Registro de una venta")
    @Transactional
    public VentaRespuesta registrar(VentaPeticion peticion) {
        Usuario usuario = usuarioActual.obtener();

        Cliente cliente = clienteRepositorio.findById(peticion.idCliente())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", peticion.idCliente()));
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("El cliente '" + cliente.getNombre() + "' esta desactivado");
        }

        List<LineaVentaPeticion> lineas =
                ServicioCompra.ordenarPorProducto(peticion.lineas(), LineaVentaPeticion::idProducto);

        // 1. Bloquear en orden de id y validar existencias de TODAS las lineas.
        Map<Long, Producto> productos = new LinkedHashMap<>();
        for (LineaVentaPeticion linea : lineas) {
            Producto producto = inventario.bloquear(linea.idProducto());
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new ReglaNegocioException("El producto '" + producto.getCodigo()
                        + "' esta desactivado y no se puede vender");
            }
            inventario.exigirExistencia(producto, linea.cantidad());
            productos.put(producto.getIdProducto(), producto);
        }

        // 2. Encabezado. La tasa de IVA queda congelada en la venta.
        Venta venta = new Venta();
        venta.setNumeroFactura(String.format("FAC-%d-%05d",
                Year.now().getValue(), ventaRepositorio.siguienteNumero()));
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setPorcentajeIva(negocio.getIva().setScale(4, RoundingMode.HALF_UP));
        ventaRepositorio.save(venta);

        // 3. Lineas: precio del catalogo, costo por consumo UEPS.
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaVentaPeticion linea : lineas) {
            Producto producto = productos.get(linea.idProducto());

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            venta.agregarDetalle(detalle);
            detalleRepositorio.save(detalle);

            inventario.registrarSalidaVenta(producto, detalle, venta, usuario);
            subtotal = subtotal.add(producto.getPrecioVenta().multiply(BigDecimal.valueOf(linea.cantidad())));
        }

        // 4. Totales. La base exige total = subtotal + iva.
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotal.multiply(venta.getPorcentajeIva()).setScale(2, RoundingMode.HALF_UP);
        venta.setSubtotal(subtotal);
        venta.setIva(iva);
        venta.setTotal(subtotal.add(iva));

        return VentaRespuesta.de(venta);
    }

    /** Listado de ventas. Filtrar por cliente responde "las operaciones realizadas con cada uno". */
    @Transactional(readOnly = true)
    public RespuestaPaginada<VentaResumen> listar(Long idCliente, LocalDate desde, LocalDate hasta,
                                                 Pageable paginacion) {
        ServicioCompra.validarRango(desde, hasta);
        Specification<Venta> filtro = Especificaciones.<Venta>igual(idCliente, "cliente", "idCliente")
                .and(Especificaciones.entre("fechaVenta",
                        ServicioCompra.inicio(desde), ServicioCompra.fin(hasta)));
        return RespuestaPaginada.de(ventaRepositorio.findAll(filtro, paginacion), VentaResumen::de);
    }

    @Transactional(readOnly = true)
    public VentaRespuesta obtener(Long id) {
        return ventaRepositorio.findConDetallesByIdVenta(id)
                .map(VentaRespuesta::de)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Venta", id));
    }
}
