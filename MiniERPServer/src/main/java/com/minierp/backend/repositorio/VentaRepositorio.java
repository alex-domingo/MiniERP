package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface VentaRepositorio
        extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "usuario"})
    Page<Venta> findAll(Specification<Venta> especificacion, Pageable paginacion);

    /** Una venta completa: cliente, lineas y productos. La usa tambien la factura PDF. */
    @EntityGraph(attributePaths = {"cliente", "usuario", "detalles", "detalles.producto"})
    Optional<Venta> findConDetallesByIdVenta(Long idVenta);

    @Query(value = "SELECT nextval('seq_numero_factura')", nativeQuery = true)
    long siguienteNumero();
}
