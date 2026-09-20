package com.minierp.backend.repositorio;

import com.minierp.backend.entidad.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface VentaRepositorio extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumeroFactura(String numeroFactura);

    boolean existsByNumeroFactura(String numeroFactura);

    Page<Venta> findByFechaVentaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Venta> findByClienteIdCliente(Long idCliente, Pageable pageable);

    /**
     * Carga la venta con todo lo que la factura necesita, en una sola
     * consulta: cliente, lineas y productos. Evita el N+1 al generar
     * el PDF.
     */
    @Query("SELECT v FROM Venta v "
            + "JOIN FETCH v.cliente "
            + "JOIN FETCH v.detalles d "
            + "JOIN FETCH d.producto "
            + "WHERE v.idVenta = :idVenta")
    Optional<Venta> buscarCompletaParaFactura(Long idVenta);

    @Query("SELECT MAX(v.numeroFactura) FROM Venta v")
    Optional<String> ultimoNumeroFactura();
}
