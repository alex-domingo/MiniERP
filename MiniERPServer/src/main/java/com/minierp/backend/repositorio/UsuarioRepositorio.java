package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    /**
     * Trae el rol en la misma consulta: la autenticacion siempre lo
     * necesita y con FetchType.LAZY provocaria una segunda consulta.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> buscarPorNombreUsuarioConRol(String nombreUsuario);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    Page<Usuario> findByActivoTrue(Pageable pageable);
}
