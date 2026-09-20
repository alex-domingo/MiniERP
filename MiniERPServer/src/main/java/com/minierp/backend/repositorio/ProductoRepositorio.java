package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    Page<Producto> findByActivoTrue(Pageable pageable);

    Page<Producto> findByActivoTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Producto> findByActivoTrueAndCategoriaIdCategoria(Long idCategoria, Pageable pageable);

    /**
     * Productos cuyas existencias alcanzaron el nivel que requiere
     * atencion, segun el enunciado.
     */
    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stockActual <= p.stockMinimo "
            + "ORDER BY (p.stockActual - p.stockMinimo) ASC")
    List<Producto> buscarConExistenciaBaja();

    /**
     * Bloqueo pesimista sobre el producto antes de tocar sus
     * existencias. Sin esto, dos ventas simultaneas del mismo producto
     * podrian leer el mismo stock y descontarlo dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :idProducto")
    Optional<Producto> bloquearParaActualizar(Long idProducto);
}
