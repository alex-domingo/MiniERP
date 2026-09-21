package com.minierp.backend.servicio;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.dto.operacion.CompraPeticion;
import com.minierp.backend.dto.operacion.CompraRespuesta;
import com.minierp.backend.dto.operacion.CompraResumen;
import com.minierp.backend.dto.operacion.LineaCompraPeticion;
import com.minierp.backend.entidad.*;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.especificacion.Especificaciones;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.excepcion.ReglaNegocioException;
import com.minierp.backend.excepcion.SolicitudInvalidaException;
import com.minierp.backend.inventario.ServicioInventario;
import com.minierp.backend.repositorio.*;
import com.minierp.backend.seguridad.UsuarioActual;
import com.minierp.backend.util.Texto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

/**
 * Registro y consulta de compras. Area de Compras.
 *
 * Una compra, sus lineas, las capas de costo que genera, los
 * movimientos del kardex y el nuevo stock se confirman juntos o no se
 * confirma nada: todo ocurre en una sola transaccion.
 */
@Service
public class ServicioCompra {

    private final CompraRepositorio compraRepositorio;
    private final DetalleCompraRepositorio detalleRepositorio;
    private final ProveedorRepositorio proveedorRepositorio;
    private final ProveedorProductoRepositorio asociacionRepositorio;
    private final ServicioInventario inventario;
    private final UsuarioActual usuarioActual;

    public ServicioCompra(CompraRepositorio compraRepositorio,
                          DetalleCompraRepositorio detalleRepositorio,
                          ProveedorRepositorio proveedorRepositorio,
                          ProveedorProductoRepositorio asociacionRepositorio,
                          ServicioInventario inventario,
                          UsuarioActual usuarioActual) {
        this.compraRepositorio = compraRepositorio;
        this.detalleRepositorio = detalleRepositorio;
        this.proveedorRepositorio = proveedorRepositorio;
        this.asociacionRepositorio = asociacionRepositorio;
        this.inventario = inventario;
        this.usuarioActual = usuarioActual;
    }

    @Auditable(modulo = ModuloBitacora.COMPRAS, accion = AccionBitacora.CREAR,
               entidad = "compra", descripcion = "Registro de una compra")
    @Transactional
    public CompraRespuesta registrar(CompraPeticion peticion) {
        Usuario usuario = usuarioActual.obtener();

        Proveedor proveedor = proveedorRepositorio.findById(peticion.idProveedor())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Proveedor", peticion.idProveedor()));
        if (Boolean.FALSE.equals(proveedor.getActivo())) {
            throw new ReglaNegocioException("El proveedor '" + proveedor.getNombre()
                    + "' esta desactivado y no admite compras nuevas");
        }

        List<LineaCompraPeticion> lineas = ordenarSinDuplicados(peticion.lineas());

        // 1. Bloquear y validar TODO antes de escribir nada, en orden de id.
        Map<Long, Producto> productos = new LinkedHashMap<>();
        Map<Long, ProveedorProducto> asociaciones = new HashMap<>();
        for (LineaCompraPeticion linea : lineas) {
            Producto producto = inventario.bloquear(linea.idProducto());
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new ReglaNegocioException("El producto '" + producto.getCodigo()
                        + "' esta desactivado y no se puede comprar");
            }
            ProveedorProducto asociacion = asociacionRepositorio
                    .findById(new ProveedorProductoId(proveedor.getIdProveedor(), producto.getIdProducto()))
                    .orElseThrow(() -> new ReglaNegocioException("El proveedor '" + proveedor.getNombre()
                            + "' no suministra el producto '" + producto.getCodigo()
                            + "'. Agreguelo primero al catalogo del proveedor."));
            productos.put(producto.getIdProducto(), producto);
            asociaciones.put(producto.getIdProducto(), asociacion);
        }

        // 2. Encabezado
        Compra compra = new Compra();
        compra.setNumeroDocumento(String.format("COM-%d-%04d",
                Year.now().getValue(), compraRepositorio.siguienteNumero()));
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setFechaCompra(LocalDateTime.now());
        compra.setObservaciones(Texto.limpiar(peticion.observaciones()));
        compra.setTotal(BigDecimal.ZERO);
        compraRepositorio.save(compra);

        // 3. Lineas: cada una crea su capa de costo y su entrada al kardex
        BigDecimal total = BigDecimal.ZERO;
        for (LineaCompraPeticion linea : lineas) {
            Producto producto = productos.get(linea.idProducto());

            DetalleCompra detalle = new DetalleCompra();
            detalle.setProducto(producto);
            detalle.setCantidad(linea.cantidad());
            detalle.setCostoUnitario(linea.costoUnitario().setScale(2, RoundingMode.HALF_UP));
            compra.agregarDetalle(detalle);
            detalleRepositorio.save(detalle);

            inventario.registrarEntradaCompra(producto, detalle, compra, usuario);
            total = total.add(detalle.getCostoUnitario().multiply(BigDecimal.valueOf(linea.cantidad())));

            // El ultimo costo pagado pasa a ser la referencia con ese proveedor.
            asociaciones.get(producto.getIdProducto()).setCostoReferencia(detalle.getCostoUnitario());
        }

        compra.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        return CompraRespuesta.de(compra);
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<CompraResumen> listar(Long idProveedor, LocalDate desde, LocalDate hasta,
                                                  Pageable paginacion) {
        validarRango(desde, hasta);
        Specification<Compra> filtro = Especificaciones.<Compra>igual(idProveedor, "proveedor", "idProveedor")
                .and(Especificaciones.entre("fechaCompra", inicio(desde), fin(hasta)));
        return RespuestaPaginada.de(compraRepositorio.findAll(filtro, paginacion), CompraResumen::de);
    }

    @Transactional(readOnly = true)
    public CompraRespuesta obtener(Long id) {
        return compraRepositorio.findConDetallesByIdCompra(id)
                .map(CompraRespuesta::de)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compra", id));
    }

    // -----------------------------------------------------------------

    /**
     * Ordena las lineas por id de producto (orden de bloqueo unico, que
     * evita interbloqueos) y rechaza un mismo producto repetido: dos
     * lineas del mismo producto en un documento son casi siempre un
     * error de captura, y la base las rechazaria de todos modos.
     */
    static <L> List<L> ordenarPorProducto(List<L> lineas, java.util.function.Function<L, Long> id) {
        Set<Long> vistos = new HashSet<>();
        for (L linea : lineas) {
            if (!vistos.add(id.apply(linea))) {
                throw new SolicitudInvalidaException("El producto " + id.apply(linea)
                        + " aparece en mas de una linea. Combine las cantidades en una sola.");
            }
        }
        List<L> ordenadas = new ArrayList<>(lineas);
        ordenadas.sort(Comparator.comparing(id));
        return ordenadas;
    }

    private List<LineaCompraPeticion> ordenarSinDuplicados(List<LineaCompraPeticion> lineas) {
        return ordenarPorProducto(lineas, LineaCompraPeticion::idProducto);
    }

    static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("La fecha inicial no puede ser posterior a la final");
        }
    }

    static LocalDateTime inicio(LocalDate desde) {
        return desde == null ? null : desde.atStartOfDay();
    }

    static LocalDateTime fin(LocalDate hasta) {
        return hasta == null ? null : hasta.plusDays(1).atStartOfDay();
    }
}
