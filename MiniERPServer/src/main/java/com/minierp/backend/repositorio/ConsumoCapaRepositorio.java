package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.ConsumoCapa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsumoCapaRepositorio extends JpaRepository<ConsumoCapa, Long> {

    List<ConsumoCapa> findByDetalleVentaIdDetalleVenta(Long idDetalleVenta);

    List<ConsumoCapa> findByCapaIdCapa(Long idCapa);
}
