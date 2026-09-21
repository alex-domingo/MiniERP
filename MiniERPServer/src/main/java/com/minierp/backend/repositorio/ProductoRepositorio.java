package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio
        extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    /**
     * Listado con filtros combinables. Se sobrescribe unicamente para
     * agregar @EntityGraph: trae la categoria en la MISMA consulta.
     * Sin esto, cada producto de la pagina dispararia una consulta
     * adicional para su categoria (el clasico problema N+1): una pagina
     * de 30 productos serian 31 consultas en lugar de 2.
     */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Specification<Producto> especificacion, Pageable paginacion);

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByIdProducto(Long idProducto);

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    /** Cuantos productos activos cuelgan de una categoria. */
    long countByCategoriaIdCategoriaAndActivoTrue(Long idCategoria);

    /**
     * Productos cuyas existencias alcanzaron el nivel que requiere
     * atencion, segun el enunciado. Los mas criticos primero.
     */
    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria "
            + "WHERE p.activo = true AND p.stockActual <= p.stockMinimo "
            + "ORDER BY (p.stockActual - p.stockMinimo) ASC, p.codigo ASC")
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
