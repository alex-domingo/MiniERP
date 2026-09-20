package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.CapaInventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CapaInventarioRepositorio extends JpaRepository<CapaInventario, Long> {

    /**
     * Capas disponibles de un producto en ORDEN UEPS: la entrada mas
     * reciente primero. Para PEPS bastaria invertir el ORDER BY.
     *
     * El bloqueo pesimista es imprescindible: sin el, dos ventas
     * concurrentes del mismo producto leerian las mismas capas y las
     * consumirian dos veces, produciendo existencias negativas o
     * costos mal calculados. Con el, la segunda venta espera.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CapaInventario c "
            + "WHERE c.producto.idProducto = :idProducto AND c.cantidadDisponible > 0 "
            + "ORDER BY c.fechaEntrada DESC, c.idCapa DESC")
    List<CapaInventario> capasDisponiblesUeps(Long idProducto);

    /** Mismo conjunto en orden PEPS, por si cambiara el criterio. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CapaInventario c "
            + "WHERE c.producto.idProducto = :idProducto AND c.cantidadDisponible > 0 "
            + "ORDER BY c.fechaEntrada ASC, c.idCapa ASC")
    List<CapaInventario> capasDisponiblesPeps(Long idProducto);

    /** Unidades realmente disponibles en capas, para reconciliar. */
    @Query("SELECT COALESCE(SUM(c.cantidadDisponible), 0) FROM CapaInventario c "
            + "WHERE c.producto.idProducto = :idProducto")
    Integer unidadesDisponibles(Long idProducto);

    List<CapaInventario> findByProductoIdProductoOrderByFechaEntradaDesc(Long idProducto);
}
