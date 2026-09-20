package com.minierp.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de la empresa que encabezan la factura PDF.
 * Prefijo: "minierp.empresa".
 */
@ConfigurationProperties(prefix = "minierp.empresa")
public class PropiedadesEmpresa {

    private String nombre = "Mini ERP";
    private String nit = "";
    private String direccion = "";
    private String telefono = "";

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
