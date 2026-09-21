package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface ClienteRepositorio
        extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByNit(String nit);

    boolean existsByNit(String nit);

    List<Cliente> findByActivoTrueOrderByNombreAsc();
}
