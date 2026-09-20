package com.minierp.backend.entidad;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento de adquisicion a un proveedor. Tabla: compra
 *
 * Conserva de quien se adquirio y cuando; el detalle conserva que se
 * adquirio y en que cantidades. Una compra no se elimina ni se anula:
 * sus lineas ya generaron capas de costo que pudieron ser consumidas
 * por ventas posteriores.
 */
@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Long idCompra;

    @Column(name = "numero_documento", nullable = false, length = 20, unique = true)
    private String numeroDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    /**
     * CascadeType.ALL + orphanRemoval: las lineas no existen fuera de
     * su compra. Se persisten junto con ella en una sola transaccion.
     */
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCompra> detalles = new ArrayList<>();

    protected Compra() {
    }

    @PrePersist
    private void alPersistir() {
        if (fechaCompra == null) {
            fechaCompra = LocalDateTime.now();
        }
    }

    /** Agrega una linea manteniendo los dos lados de la relacion en orden. */
    public void agregarDetalle(DetalleCompra detalle) {
        detalles.add(detalle);
        detalle.setCompra(this);
    }

    public Long getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Long idCompra) {
        this.idCompra = idCompra;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }
}
