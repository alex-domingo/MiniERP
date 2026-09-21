package com.minierp.backend.entidad;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.math.BigDecimal;

/**
 * Registro de que capa de costo alimento que linea de venta y en que
 * cantidad. Tabla: consumo_capa
 *
 * Da trazabilidad completa: permite auditar el costo de ventas capa por
 * capa y reconstruir el calculo UEPS de cualquier venta historica, aun
 * meses despues.
 *
 * costoTotal es columna generada por PostgreSQL.
 *
 * Origen (V3): cada consumo corresponde a UNA linea de venta o a UN
 * ajuste de salida. La base lo exige con ck_consumo_origen.
 */
@Entity
@Table(name = "consumo_capa")
public class ConsumoCapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consumo")
    private Long idConsumo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_capa", nullable = false)
    private CapaInventario capa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_detalle_venta")
    private DetalleVenta detalleVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_movimiento_ajuste")
    private MovimientoInventario movimientoAjuste;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoUnitario;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "costo_total", insertable = false, updatable = false, precision = 16, scale = 4)
    private BigDecimal costoTotal;

    public ConsumoCapa() {
    }

    public Long getIdConsumo() {
        return idConsumo;
    }

    public void setIdConsumo(Long idConsumo) {
        this.idConsumo = idConsumo;
    }

    public CapaInventario getCapa() {
        return capa;
    }

    public void setCapa(CapaInventario capa) {
        this.capa = capa;
    }

    public DetalleVenta getDetalleVenta() {
        return detalleVenta;
    }

    public void setDetalleVenta(DetalleVenta detalleVenta) {
        this.detalleVenta = detalleVenta;
    }

    public MovimientoInventario getMovimientoAjuste() {
        return movimientoAjuste;
    }

    public void setMovimientoAjuste(MovimientoInventario movimientoAjuste) {
        this.movimientoAjuste = movimientoAjuste;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    /** Calculado por la base de datos; no tiene setter a proposito. */
    public BigDecimal getCostoTotal() {
        return costoTotal;
    }
}
