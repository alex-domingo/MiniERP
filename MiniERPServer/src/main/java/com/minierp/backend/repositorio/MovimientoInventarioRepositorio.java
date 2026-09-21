package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

public interface MovimientoInventarioRepositorio
        extends JpaRepository<MovimientoInventario, Long> {

    /**
     * Kardex de un producto en un rango de fechas, del mas reciente al
     * mas antiguo. Trae en la misma consulta el documento de respaldo y
     * su tercero (proveedor o cliente), que es lo que responde "por que"
     * cambio la existencia.
     */
    @EntityGraph(attributePaths = {"producto", "usuario", "compra", "compra.proveedor",
                                   "venta", "venta.cliente"})
    @Query(value = "SELECT m FROM MovimientoInventario m "
            + "WHERE m.producto.idProducto = :idProducto "
            + "AND m.fechaMovimiento >= :desde AND m.fechaMovimiento < :hasta",
           countQuery = "SELECT COUNT(m) FROM MovimientoInventario m "
            + "WHERE m.producto.idProducto = :idProducto "
            + "AND m.fechaMovimiento >= :desde AND m.fechaMovimiento < :hasta")
    Page<MovimientoInventario> kardex(Long idProducto, LocalDateTime desde,
                                      LocalDateTime hasta, Pageable paginacion);

    @EntityGraph(attributePaths = {"producto", "usuario", "compra", "compra.proveedor",
                                   "venta", "venta.cliente"})
    @Query("SELECT m FROM MovimientoInventario m WHERE m.idMovimiento = :id")
    java.util.Optional<MovimientoInventario> buscarCompleto(Long id);
}
