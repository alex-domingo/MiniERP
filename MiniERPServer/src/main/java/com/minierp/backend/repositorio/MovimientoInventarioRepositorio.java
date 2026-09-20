package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface MovimientoInventarioRepositorio
        extends JpaRepository<MovimientoInventario, Long> {

    /** Historial de movimientos de un producto: el kardex. */
    Page<MovimientoInventario> findByProductoIdProductoOrderByFechaMovimientoDesc(
            Long idProducto, Pageable pageable);

    Page<MovimientoInventario> findByFechaMovimientoBetween(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
