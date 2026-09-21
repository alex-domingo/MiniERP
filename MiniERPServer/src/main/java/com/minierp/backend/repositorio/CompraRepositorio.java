package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CompraRepositorio
        extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    /** Listado con proveedor y usuario en la misma consulta. */
    @Override
    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<Compra> findAll(Specification<Compra> especificacion, Pageable paginacion);

    /** Una compra completa: encabezado, lineas y productos, en una consulta. */
    @EntityGraph(attributePaths = {"proveedor", "usuario", "detalles", "detalles.producto"})
    Optional<Compra> findConDetallesByIdCompra(Long idCompra);

    /**
     * Siguiente correlativo. Una secuencia nunca entrega el mismo numero
     * a dos transacciones, a diferencia de MAX()+1.
     */
    @Query(value = "SELECT nextval('seq_numero_compra')", nativeQuery = true)
    long siguienteNumero();
}
