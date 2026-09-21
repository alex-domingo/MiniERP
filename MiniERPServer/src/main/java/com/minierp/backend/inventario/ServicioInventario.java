package com.minierp.backend.inventario;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.config.PropiedadesNegocio;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.dto.inventario.AjustePeticion;
import com.minierp.backend.dto.inventario.MovimientoRespuesta;
import com.minierp.backend.dto.inventario.ValuacionProductoRespuesta;
import com.minierp.backend.entidad.*;
import com.minierp.backend.entidad.enums.*;
import com.minierp.backend.excepcion.ExistenciaInsuficienteException;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.excepcion.ReglaNegocioException;
import com.minierp.backend.excepcion.SolicitudInvalidaException;
import com.minierp.backend.repositorio.CapaInventarioRepositorio;
import com.minierp.backend.repositorio.ConsumoCapaRepositorio;
import com.minierp.backend.repositorio.MovimientoInventarioRepositorio;
import com.minierp.backend.repositorio.ProductoRepositorio;
import com.minierp.backend.seguridad.UsuarioActual;
import com.minierp.backend.util.Texto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Motor de inventario. Es el UNICO componente que modifica existencias.
 *
 * Todo cambio de stock pasa por aqui y deja, en la misma transaccion:
 *   - el movimiento en el kardex (como y por que cambio),
 *   - las capas de costo creadas o consumidas (a que costo),
 *   - el nuevo stock_actual del producto.
 *
 * CONCURRENCIA
 *
 * Cada operacion bloquea primero la fila del producto (SELECT ... FOR
 * UPDATE) y solo despues lee su stock y sus capas. Dos ventas del mismo
 * producto quedan asi en fila: la segunda espera a que la primera
 * confirme y entonces lee el stock ya descontado.
 *
 * Quien llama debe bloquear los productos en orden ascendente de id.
 * Si una venta bloqueara A y luego B, y otra B y luego A, cada una
 * esperaria a la otra para siempre (interbloqueo). Con un orden unico
 * ese ciclo no puede formarse.
 *
 * Los metodos de compra y venta exigen una transaccion ya abierta
 * (Propagation.MANDATORY): nunca deben ejecutarse sueltos, porque el
 * movimiento de inventario y el documento que lo origina deben
 * confirmarse o revertirse juntos.
 */
@Service
public class ServicioInventario {

    private final ProductoRepositorio productoRepositorio;
    private final CapaInventarioRepositorio capaRepositorio;
    private final ConsumoCapaRepositorio consumoRepositorio;
    private final MovimientoInventarioRepositorio movimientoRepositorio;
    private final PropiedadesNegocio negocio;
    private final UsuarioActual usuarioActual;

    public ServicioInventario(ProductoRepositorio productoRepositorio,
                              CapaInventarioRepositorio capaRepositorio,
                              ConsumoCapaRepositorio consumoRepositorio,
                              MovimientoInventarioRepositorio movimientoRepositorio,
                              PropiedadesNegocio negocio,
                              UsuarioActual usuarioActual) {
        this.productoRepositorio = productoRepositorio;
        this.capaRepositorio = capaRepositorio;
        this.consumoRepositorio = consumoRepositorio;
        this.movimientoRepositorio = movimientoRepositorio;
        this.negocio = negocio;
        this.usuarioActual = usuarioActual;
    }

    // =================================================================
    //  Operaciones internas, llamadas por compras y ventas
    // =================================================================

    /**
     * Bloquea el producto para escritura y lo devuelve con su stock
     * vigente. Debe ser el PRIMER acceso a ese producto dentro de la
     * transaccion: si ya estuviera cargado, Hibernate devolveria la copia
     * en memoria sin releer el stock tras obtener el bloqueo.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Producto bloquear(Long idProducto) {
        return productoRepositorio.bloquearParaActualizar(idProducto)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", idProducto));
    }

    /** Verifica existencias suficientes sobre un producto ya bloqueado. */
    public void exigirExistencia(Producto producto, int cantidad) {
        if (producto.getStockActual() < cantidad) {
            throw new ExistenciaInsuficienteException(producto.getCodigo(), producto.getNombre(),
                    cantidad, producto.getStockActual());
        }
    }

    /**
     * Entrada por compra: crea la capa de costo de la linea, registra el
     * movimiento y suma al stock. El producto debe venir bloqueado.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEntradaCompra(Producto producto, DetalleCompra detalle,
                                       Compra compra, Usuario usuario) {
        int anterior = producto.getStockActual();
        int nueva = anterior + detalle.getCantidad();

        CapaInventario capa = new CapaInventario();
        capa.setProducto(producto);
        capa.setDetalleCompra(detalle);
        capa.setFechaEntrada(compra.getFechaCompra());
        capa.setCostoUnitario(detalle.getCostoUnitario().setScale(4, RoundingMode.HALF_UP));
        capa.setCantidadInicial(detalle.getCantidad());
        capa.setCantidadDisponible(detalle.getCantidad());
        capaRepositorio.save(capa);

        MovimientoInventario movimiento = nuevoMovimiento(producto, TipoMovimiento.ENTRADA,
                OrigenMovimiento.COMPRA, detalle.getCantidad(), capa.getCostoUnitario(),
                anterior, nueva, usuario, compra.getFechaCompra(),
                "Ingreso por compra " + compra.getNumeroDocumento());
        movimiento.setCompra(compra);
        movimientoRepositorio.save(movimiento);

        producto.setStockActual(nueva);
    }

    /**
     * Salida por venta: consume capas segun el metodo de valuacion,
     * registra de que capa salio cada unidad, fija el costo de la linea
     * y descuenta el stock. El producto debe venir bloqueado.
     *
     * @return costo unitario promedio de la salida
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal registrarSalidaVenta(Producto producto, DetalleVenta detalle,
                                           Venta venta, Usuario usuario) {
        int cantidad = detalle.getCantidad();
        exigirExistencia(producto, cantidad);
        int anterior = producto.getStockActual();

        MotorCapas.Resultado resultado = consumirCapas(producto, cantidad);
        for (MotorCapas.Toma toma : resultado.tomas()) {
            ConsumoCapa consumo = new ConsumoCapa();
            consumo.setCapa(toma.capa());
            consumo.setDetalleVenta(detalle);
            consumo.setCantidad(toma.cantidad());
            consumo.setCostoUnitario(toma.costoUnitario());
            consumoRepositorio.save(consumo);
        }
        detalle.setCostoUnitario(resultado.costoUnitarioPromedio());

        MovimientoInventario movimiento = nuevoMovimiento(producto, TipoMovimiento.SALIDA,
                OrigenMovimiento.VENTA, cantidad, resultado.costoUnitarioPromedio(),
                anterior, anterior - cantidad, usuario, venta.getFechaVenta(),
                "Salida por venta " + venta.getNumeroFactura() + " (" + negocio.getMetodoValuacion() + ")");
        movimiento.setVenta(venta);
        movimientoRepositorio.save(movimiento);

        producto.setStockActual(anterior - cantidad);
        return resultado.costoUnitarioPromedio();
    }

    // =================================================================
    //  Ajustes de inventario
    // =================================================================

    /**
     * Ajuste manual tras un conteo fisico, una merma o para retirar las
     * existencias de un producto antes de descontinuarlo.
     *
     *   ENTRADA: crea una capa de costo nueva. Sin costo explicito, usa
     *            el de la capa mas reciente del producto.
     *   SALIDA:  consume capas en orden UEPS, exactamente como una venta.
     *
     * No hay documento de respaldo, asi que el motivo es obligatorio: es
     * lo que responde "por que" cambio la existencia.
     */
    @Auditable(modulo = ModuloBitacora.INVENTARIO, accion = AccionBitacora.CREAR,
               entidad = "movimiento_inventario", descripcion = "Ajuste de inventario")
    @Transactional
    public MovimientoRespuesta ajustar(AjustePeticion peticion) {
        Usuario usuario = usuarioActual.obtener();
        Producto producto = bloquear(peticion.idProducto());
        String motivo = Texto.limpiar(peticion.motivo());
        int cantidad = peticion.cantidad();
        int anterior = producto.getStockActual();
        LocalDateTime ahora = LocalDateTime.now();

        MovimientoInventario movimiento;

        if (peticion.tipo() == TipoMovimiento.ENTRADA) {
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new ReglaNegocioException("No se puede ingresar existencia a '"
                        + producto.getCodigo() + "' porque esta desactivado");
            }
            BigDecimal costo = peticion.costoUnitario() != null
                    ? peticion.costoUnitario().setScale(4, RoundingMode.HALF_UP)
                    : capaRepositorio.findFirstByProductoIdProductoOrderByFechaEntradaDescIdCapaDesc(
                                    producto.getIdProducto())
                            .map(CapaInventario::getCostoUnitario)
                            .orElseThrow(() -> new SolicitudInvalidaException(
                                    "El producto " + producto.getCodigo() + " nunca ha tenido entradas: "
                                            + "indique el costo unitario del ajuste"));

            movimiento = nuevoMovimiento(producto, TipoMovimiento.ENTRADA,
                    OrigenMovimiento.AJUSTE_ENTRADA, cantidad, costo,
                    anterior, anterior + cantidad, usuario, ahora, motivo);
            movimientoRepositorio.save(movimiento);

            CapaInventario capa = new CapaInventario();
            capa.setProducto(producto);
            capa.setMovimientoAjuste(movimiento);
            capa.setFechaEntrada(ahora);
            capa.setCostoUnitario(costo);
            capa.setCantidadInicial(cantidad);
            capa.setCantidadDisponible(cantidad);
            capaRepositorio.save(capa);

            producto.setStockActual(anterior + cantidad);

        } else {
            if (peticion.costoUnitario() != null) {
                throw new SolicitudInvalidaException("Un ajuste de salida no admite costo: "
                        + "lo determina el consumo " + negocio.getMetodoValuacion() + " de las capas");
            }
            exigirExistencia(producto, cantidad);
            MotorCapas.Resultado resultado = consumirCapas(producto, cantidad);

            movimiento = nuevoMovimiento(producto, TipoMovimiento.SALIDA,
                    OrigenMovimiento.AJUSTE_SALIDA, cantidad, resultado.costoUnitarioPromedio(),
                    anterior, anterior - cantidad, usuario, ahora, motivo);
            movimientoRepositorio.save(movimiento);

            for (MotorCapas.Toma toma : resultado.tomas()) {
                ConsumoCapa consumo = new ConsumoCapa();
                consumo.setCapa(toma.capa());
                consumo.setMovimientoAjuste(movimiento);
                consumo.setCantidad(toma.cantidad());
                consumo.setCostoUnitario(toma.costoUnitario());
                consumoRepositorio.save(consumo);
            }
            producto.setStockActual(anterior - cantidad);
        }

        return MovimientoRespuesta.de(movimiento);
    }

    // =================================================================
    //  Consultas
    // =================================================================

    /** Kardex de un producto. Sin fechas, devuelve todo el historial. */
    @Transactional(readOnly = true)
    public RespuestaPaginada<MovimientoRespuesta> kardex(Long idProducto, LocalDate desde,
                                                        LocalDate hasta, Pageable paginacion) {
        if (!productoRepositorio.existsById(idProducto)) {
            throw RecursoNoEncontradoException.de("Producto", idProducto);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("La fecha inicial no puede ser posterior a la final");
        }
        // Limites amplios en lugar de nulos: un parametro nulo en una
        // comparacion de fechas obliga a PostgreSQL a adivinar su tipo.
        LocalDateTime inicio = (desde != null ? desde : LocalDate.of(1900, 1, 1)).atStartOfDay();
        LocalDateTime fin = (hasta != null ? hasta : LocalDate.of(9999, 12, 30)).plusDays(1).atStartOfDay();
        return RespuestaPaginada.de(
                movimientoRepositorio.kardex(idProducto, inicio, fin, paginacion),
                MovimientoRespuesta::de);
    }

    /** Existencia de un producto desglosada por capa de costo. */
    @Transactional(readOnly = true)
    public ValuacionProductoRespuesta valuacion(Long idProducto) {
        Producto producto = productoRepositorio.findById(idProducto)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", idProducto));

        List<CapaInventario> capas = capaRepositorio
                .findByProductoIdProductoOrderByFechaEntradaDescIdCapaDesc(idProducto);
        List<CapaInventario> enOrden = MotorCapas.ordenar(capas, negocio.getMetodoValuacion());

        int unidades = 0;
        BigDecimal valor = BigDecimal.ZERO;
        List<ValuacionProductoRespuesta.Capa> filas = new java.util.ArrayList<>();
        for (CapaInventario c : enOrden) {
            BigDecimal valorCapa = c.getCostoUnitario()
                    .multiply(BigDecimal.valueOf(c.getCantidadDisponible()))
                    .setScale(2, RoundingMode.HALF_UP);
            unidades += c.getCantidadDisponible();
            valor = valor.add(valorCapa);
            String origen = c.getDetalleCompra() != null
                    ? "COMPRA " + c.getDetalleCompra().getCompra().getNumeroDocumento()
                    : "AJUSTE DE ENTRADA";
            filas.add(new ValuacionProductoRespuesta.Capa(c.getIdCapa(), c.getFechaEntrada(), origen,
                    c.getCostoUnitario(), c.getCantidadInicial(), c.getCantidadDisponible(), valorCapa));
        }
        BigDecimal promedio = unidades > 0
                ? valor.divide(BigDecimal.valueOf(unidades), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ValuacionProductoRespuesta(producto.getIdProducto(), producto.getCodigo(),
                producto.getNombre(), negocio.getMetodoValuacion(), producto.getStockActual(),
                unidades, valor, promedio, filas);
    }

    // =================================================================

    private MotorCapas.Resultado consumirCapas(Producto producto, int cantidad) {
        List<CapaInventario> capas = negocio.getMetodoValuacion() == MetodoValuacion.UEPS
                ? capaRepositorio.capasDisponiblesUeps(producto.getIdProducto())
                : capaRepositorio.capasDisponiblesPeps(producto.getIdProducto());
        return MotorCapas.consumir(capas, cantidad, negocio.getMetodoValuacion());
    }

    private MovimientoInventario nuevoMovimiento(Producto producto, TipoMovimiento tipo,
                                                 OrigenMovimiento origen, int cantidad,
                                                 BigDecimal costoUnitario, int anterior, int nueva,
                                                 Usuario usuario, LocalDateTime fecha,
                                                 String observaciones) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimiento(tipo);
        m.setOrigen(origen);
        m.setCantidad(cantidad);
        m.setCostoUnitario(costoUnitario);
        m.setExistenciaAnterior(anterior);
        m.setExistenciaNueva(nueva);
        m.setUsuario(usuario);
        m.setFechaMovimiento(fecha);
        m.setObservaciones(observaciones);
        return m;
    }
}
