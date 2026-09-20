package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Categoria> findByActivoTrueOrderByNombreAsc();

    Page<Categoria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
