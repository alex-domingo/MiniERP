package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Rol;
import com.minierp.backend.entidad.enums.NombreRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepositorio extends JpaRepository<Rol, Short> {

    Optional<Rol> findByNombre(NombreRol nombre);
}
