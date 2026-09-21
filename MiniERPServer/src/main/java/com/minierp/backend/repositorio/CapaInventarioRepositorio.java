package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.CapaInventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CapaInventarioRepositorio extends JpaRepository<CapaInventario, Long> {

    /**
     * Capas con saldo, en ORDEN UEPS (la entrada mas reciente primero),
     * bloqueadas para escritura.
     *
     * El bloqueo es imprescindible: sin el, dos ventas concurrentes del
     * mismo producto leerian las mismas capas y las consumirian dos
     * veces. Con el, la segunda venta espera a que la primera termine.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CapaInventario c "
            + "WHERE c.producto.idProducto = :idProducto AND c.cantidadDisponible > 0 "
            + "ORDER BY c.fechaEntrada DESC, c.idCapa DESC")
    List<CapaInventario> capasDisponiblesUeps(Long idProducto);

    /** Mismo conjunto en orden PEPS, para el caso par del enunciado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CapaInventario c "
            + "WHERE c.producto.idProducto = :idProducto AND c.cantidadDisponible > 0 "
            + "ORDER BY c.fechaEntrada ASC, c.idCapa ASC")
    List<CapaInventario> capasDisponiblesPeps(Long idProducto);

    /** Todas las capas de un producto, con la compra que origino cada una. */
    @EntityGraph(attributePaths = {"detalleCompra", "detalleCompra.compra"})
    List<CapaInventario> findByProductoIdProductoOrderByFechaEntradaDescIdCapaDesc(Long idProducto);

    /** Capa mas reciente del producto: su costo sirve de referencia para un ajuste de entrada. */
    Optional<CapaInventario> findFirstByProductoIdProductoOrderByFechaEntradaDescIdCapaDesc(Long idProducto);
}
