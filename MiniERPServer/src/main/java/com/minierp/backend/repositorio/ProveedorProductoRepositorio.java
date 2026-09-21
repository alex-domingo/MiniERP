package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.ProveedorProducto;
import com.minierp.backend.entidad.ProveedorProductoId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProveedorProductoRepositorio
        extends JpaRepository<ProveedorProducto, ProveedorProductoId> {

    /** Productos que suministra un proveedor, con sus datos en una sola consulta. */
    @EntityGraph(attributePaths = {"proveedor", "producto"})
    List<ProveedorProducto> findByProveedorIdProveedorOrderByProductoCodigoAsc(Long idProveedor);

    /** Proveedores que suministran un producto. */
    @EntityGraph(attributePaths = {"proveedor", "producto"})
    List<ProveedorProducto> findByProductoIdProductoOrderByCostoReferenciaAsc(Long idProducto);
}
