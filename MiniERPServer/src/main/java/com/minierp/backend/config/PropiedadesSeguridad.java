package com.minierp.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de la autenticacion, bajo "minierp.seguridad.jwt".
 * El secreto nunca se escribe en el codigo: viene de una variable de
 * entorno con valor por defecto solo apto para desarrollo.
 */
@ConfigurationProperties(prefix = "minierp.seguridad.jwt")
public class PropiedadesSeguridad {

    /** Clave en Base64 para firmar los tokens. Minimo 32 bytes decodificados. */
    private String secreto;

    /** Vigencia del token en milisegundos. */
    private long expiracion = 28_800_000L;

    /** Emisor que se graba y se exige al validar. */
    private String emisor = "MiniERP";

    public String getSecreto() {
        return secreto;
    }

    public void setSecreto(String secreto) {
        this.secreto = secreto;
    }

    public long getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(long expiracion) {
        this.expiracion = expiracion;
    }

    public String getEmisor() {
        return emisor;
    }

    public void setEmisor(String emisor) {
        this.emisor = emisor;
    }
}
