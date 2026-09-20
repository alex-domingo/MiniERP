package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByNit(String nit);

    boolean existsByNit(String nit);

    Page<Proveedor> findByActivoTrue(Pageable pageable);

    Page<Proveedor> findByActivoTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
