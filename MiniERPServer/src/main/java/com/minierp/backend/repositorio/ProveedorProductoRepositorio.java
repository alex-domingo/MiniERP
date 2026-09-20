package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.ProveedorProducto;
import com.minierp.backend.entidad.ProveedorProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProveedorProductoRepositorio
        extends JpaRepository<ProveedorProducto, ProveedorProductoId> {

    List<ProveedorProducto> findByProveedorIdProveedor(Long idProveedor);

    List<ProveedorProducto> findByProductoIdProducto(Long idProducto);

    void deleteByProveedorIdProveedorAndProductoIdProducto(Long idProveedor, Long idProducto);
}
