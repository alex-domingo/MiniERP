package com.minierp.backend.entidad;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Relacion N:M entre proveedor y producto, exigida por el enunciado:
 * un proveedor suministra varios productos y un mismo producto puede
 * adquirirse a varios proveedores.
 *
 * Lleva atributo propio (costo_referencia), por lo que se modela como
 * entidad con llave compuesta y no como @ManyToMany simple.
 *
 * @MapsId enlaza cada parte de la llave compuesta con su relacion, de
 * modo que la columna se escribe una sola vez y no hay duplicidad de
 * mapeo entre el EmbeddedId y las llaves foraneas.
 */
@Entity
@Table(name = "proveedor_producto")
public class ProveedorProducto {

    @EmbeddedId
    private ProveedorProductoId id;

    @MapsId("idProveedor")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @MapsId("idProducto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "costo_referencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoReferencia;

    @Column(name = "fecha_asociacion", nullable = false)
    private LocalDateTime fechaAsociacion;

    protected ProveedorProducto() {
    }

    @PrePersist
    private void alPersistir() {
        if (fechaAsociacion == null) {
            fechaAsociacion = LocalDateTime.now();
        }
    }

    public ProveedorProductoId getId() {
        return id;
    }

    public void setId(ProveedorProductoId id) {
        this.id = id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getCostoReferencia() {
        return costoReferencia;
    }

    public void setCostoReferencia(BigDecimal costoReferencia) {
        this.costoReferencia = costoReferencia;
    }

    public LocalDateTime getFechaAsociacion() {
        return fechaAsociacion;
    }

    public void setFechaAsociacion(LocalDateTime fechaAsociacion) {
        this.fechaAsociacion = fechaAsociacion;
    }
}
