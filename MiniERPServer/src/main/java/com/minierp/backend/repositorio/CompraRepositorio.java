package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CompraRepositorio extends JpaRepository<Compra, Long> {

    Optional<Compra> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumento(String numeroDocumento);

    Page<Compra> findByFechaCompraBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Compra> findByProveedorIdProveedor(Long idProveedor, Pageable pageable);

    /** Ultimo correlativo emitido, para generar el siguiente documento. */
    @Query("SELECT MAX(c.numeroDocumento) FROM Compra c")
    Optional<String> ultimoNumeroDocumento();
}
