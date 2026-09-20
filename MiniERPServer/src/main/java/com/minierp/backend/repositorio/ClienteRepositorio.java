package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNit(String nit);

    boolean existsByNit(String nit);

    Page<Cliente> findByActivoTrue(Pageable pageable);

    Page<Cliente> findByActivoTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
