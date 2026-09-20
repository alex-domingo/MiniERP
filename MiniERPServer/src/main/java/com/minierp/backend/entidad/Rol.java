package com.minierp.backend.entidad;

import com.minierp.backend.entidad.enums.NombreRol;
import jakarta.persistence.*;

/**
 * Area funcional del sistema. Determina los permisos de cada usuario.
 * Tabla: rol
 */
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Short idRol;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private NombreRol nombre;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    public Rol() {
    }

    public Short getIdRol() {
        return idRol;
    }

    public void setIdRol(Short idRol) {
        this.idRol = idRol;
    }

    public NombreRol getNombre() {
        return nombre;
    }

    public void setNombre(NombreRol nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
