package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleVentaRepositorio extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaIdVenta(Long idVenta);

    List<DetalleVenta> findByProductoIdProducto(Long idProducto);
}
