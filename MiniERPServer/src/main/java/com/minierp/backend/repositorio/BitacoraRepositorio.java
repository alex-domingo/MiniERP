package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Bitacora;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface BitacoraRepositorio extends JpaRepository<Bitacora, Long> {

    Page<Bitacora> findByFechaHoraBetweenOrderByFechaHoraDesc(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Bitacora> findByModuloOrderByFechaHoraDesc(ModuloBitacora modulo, Pageable pageable);

    Page<Bitacora> findByAccionOrderByFechaHoraDesc(AccionBitacora accion, Pageable pageable);

    Page<Bitacora> findByUsuarioIdUsuarioOrderByFechaHoraDesc(Long idUsuario, Pageable pageable);
}
