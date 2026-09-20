package com.minierp.backend.entidad;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.math.BigDecimal;

/**
 * Linea de una compra. Tabla: detalle_compra
 *
 * costoUnitario se congela al momento de la operacion: aunque el costo
 * de referencia del proveedor cambie despues, el historico permanece.
 *
 * subtotal es una COLUMNA GENERADA por PostgreSQL
 * (GENERATED ALWAYS AS (cantidad * costo_unitario) STORED). Por eso va
 * con insertable/updatable en false y @Generated: Hibernate no debe
 * intentar escribirla, solo releerla despues de cada INSERT o UPDATE.
 * Si se mapeara como columna normal, la base rechazaria la operacion.
 */
@Entity
@Table(name = "detalle_compra")
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_compra")
    private Long idDetalleCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "subtotal", insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    public DetalleCompra() {
    }

    public Long getIdDetalleCompra() {
        return idDetalleCompra;
    }

    public void setIdDetalleCompra(Long idDetalleCompra) {
        this.idDetalleCompra = idDetalleCompra;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
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
