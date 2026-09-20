package com.minierp.backend.entidad;

import com.minierp.backend.entidad.enums.OrigenMovimiento;
import com.minierp.backend.entidad.enums.TipoMovimiento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * KARDEX. Tabla: movimiento_inventario
 *
 * Responde a la exigencia del enunciado: "ademas de conocer la
 * existencia actual, debera ser posible determinar como y por que se
 * han producido cambios en las cantidades disponibles de un producto".
 *
 *   tipoMovimiento : que efecto tuvo (ENTRADA / SALIDA)
 *   origen         : por que ocurrio (COMPRA / VENTA / AJUSTE)
 *   compra / venta : el documento que lo respalda
 *
 * Las dos llaves foraneas son excluyentes y la base lo verifica con
 * ck_mov_documento. Ademas ck_mov_aritmetica impide registrar un
 * movimiento cuyo saldo resultante no corresponda al saldo anterior
 * mas o menos la cantidad: el kardex no puede contradecirse.
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 10)
    private TipoMovimiento tipoMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private OrigenMovimiento origen;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "existencia_anterior", nullable = false)
    private Integer existenciaAnterior;

    @Column(name = "existencia_nueva", nullable = false)
    private Integer existenciaNueva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    protected MovimientoInventario() {
    }

    @PrePersist
    private void alPersistir() {
        if (fechaMovimiento == null) {
            fechaMovimiento = LocalDateTime.now();
        }
    }

    public Long getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Long idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public OrigenMovimiento getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenMovimiento origen) {
        this.origen = origen;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public Integer getExistenciaAnterior() {
        return existenciaAnterior;
    }

    public void setExistenciaAnterior(Integer existenciaAnterior) {
        this.existenciaAnterior = existenciaAnterior;
    }

    public Integer getExistenciaNueva() {
        return existenciaNueva;
    }

    public void setExistenciaNueva(Integer existenciaNueva) {
        this.existenciaNueva = existenciaNueva;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
