package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleCompraRepositorio extends JpaRepository<DetalleCompra, Long> {

    List<DetalleCompra> findByCompraIdCompra(Long idCompra);

    List<DetalleCompra> findByProductoIdProducto(Long idProducto);
}
