package com.minierp.backend.entidad;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.math.BigDecimal;

/**
 * Linea de una venta. Tabla: detalle_venta
 *
 * precioUnitario se congela al momento de la venta.
 *
 * costoUnitario es el costo promedio ponderado que resulto de consumir
 * las capas de inventario bajo UEPS. Lo escribe el motor de inventario,
 * no el usuario. Guardarlo aqui permite calcular margen y costo de
 * ventas sin recorrer el kardex en cada reporte.
 *
 * subtotal es columna generada por PostgreSQL (ver DetalleCompra).
 */
@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Long idDetalleVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "subtotal", insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected DetalleVenta() {
    }

    public Long getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public void setIdDetalleVenta(Long idDetalleVenta) {
        this.idDetalleVenta = idDetalleVenta;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    /** Calculado por la base de datos; no tiene setter a proposito. */
    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
