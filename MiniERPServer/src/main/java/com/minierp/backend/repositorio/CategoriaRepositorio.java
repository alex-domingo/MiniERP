package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepositorio
        extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Categoria> findByActivoTrueOrderByNombreAsc();
}
