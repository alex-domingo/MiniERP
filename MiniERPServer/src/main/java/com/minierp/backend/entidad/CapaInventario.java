package com.minierp.backend.entidad;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Capa de costo para la valuacion de inventario. Tabla: capa_inventario
 *
 * NUCLEO DEL METODO UEPS. Cada linea de compra crea una capa con su
 * propio costo unitario. Las salidas consumen capas en orden UEPS
 * (ultima entrada disponible primero), decrementando
 * cantidadDisponible. Una capa agotada permanece en la tabla como
 * parte del historico, con disponible en cero.
 *
 * Sin estas capas, UEPS seria imposible: un simple contador de
 * existencias no sabe a que costo entro cada unidad.
 */
@Entity
@Table(name = "capa_inventario")
public class CapaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_capa")
    private Long idCapa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_compra", nullable = false, unique = true)
    private DetalleCompra detalleCompra;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDateTime fechaEntrada;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    public CapaInventario() {
    }

    /** Consume unidades de esta capa. Devuelve cuantas alcanzo a tomar. */
    public int consumir(int solicitadas) {
        int tomadas = Math.min(solicitadas, cantidadDisponible);
        cantidadDisponible -= tomadas;
        return tomadas;
    }

    @Transient
    public boolean estaAgotada() {
        return cantidadDisponible != null && cantidadDisponible == 0;
    }

    public Long getIdCapa() {
        return idCapa;
    }

    public void setIdCapa(Long idCapa) {
        this.idCapa = idCapa;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public DetalleCompra getDetalleCompra() {
        return detalleCompra;
    }

    public void setDetalleCompra(DetalleCompra detalleCompra) {
        this.detalleCompra = detalleCompra;
    }

    public LocalDateTime getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDateTime fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public Integer getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(Integer cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public Integer getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(Integer cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }
}
