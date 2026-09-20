package com.minierp.backend.entidad;

import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de auditoria. Tabla: bitacora
 *
 * Respalda el "Reporte de logs" del enunciado: principales
 * interacciones de los usuarios dentro de los modulos del sistema.
 *
 * nombreUsuario se guarda como copia textual ademas de la llave
 * foranea: si un usuario se desactiva o cambia de nombre, la bitacora
 * historica conserva quien realizo la accion en su momento. La FK es
 * opcional para poder registrar intentos de acceso con usuarios que no
 * existen.
 */
@Entity
@Table(name = "bitacora")
public class Bitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Long idBitacora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "nombre_usuario", nullable = false, length = 40)
    private String nombreUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false, length = 30)
    private ModuloBitacora modulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 30)
    private AccionBitacora accion;

    @Column(name = "entidad_afectada", length = 60)
    private String entidadAfectada;

    @Column(name = "id_entidad")
    private Long idEntidad;

    @Column(name = "descripcion", nullable = false, length = 400)
    private String descripcion;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso = Boolean.TRUE;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected Bitacora() {
    }

    @PrePersist
    private void alPersistir() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
        if (exitoso == null) {
            exitoso = Boolean.TRUE;
        }
    }

    public Long getIdBitacora() {
        return idBitacora;
    }

    public void setIdBitacora(Long idBitacora) {
        this.idBitacora = idBitacora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public ModuloBitacora getModulo() {
        return modulo;
    }

    public void setModulo(ModuloBitacora modulo) {
        this.modulo = modulo;
    }

    public AccionBitacora getAccion() {
        return accion;
    }

    public void setAccion(AccionBitacora accion) {
        this.accion = accion;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public void setEntidadAfectada(String entidadAfectada) {
        this.entidadAfectada = entidadAfectada;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public void setDireccionIp(String direccionIp) {
        this.direccionIp = direccionIp;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
