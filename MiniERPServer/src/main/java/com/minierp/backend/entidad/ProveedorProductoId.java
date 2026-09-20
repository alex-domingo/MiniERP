package com.minierp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Llave primaria compuesta de la relacion N:M proveedor-producto.
 */
@Embeddable
public class ProveedorProductoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "id_producto")
    private Long idProducto;

    protected ProveedorProductoId() {
    }

    public ProveedorProductoId(Long idProveedor, Long idProducto) {
        this.idProveedor = idProveedor;
        this.idProducto = idProducto;
    }

    public Long getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Long idProveedor) {
        this.idProveedor = idProveedor;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProveedorProductoId otro)) {
            return false;
        }
        return Objects.equals(idProveedor, otro.idProveedor)
                && Objects.equals(idProducto, otro.idProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProveedor, idProducto);
    }
}
