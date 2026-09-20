package com.minierp.backend.config;

import com.minierp.backend.entidad.enums.MetodoValuacion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

/**
 * Reglas del negocio configurables desde application.properties,
 * bajo el prefijo "minierp.negocio".
 *
 * Tener el IVA y el metodo de valuacion aqui y no incrustados en el
 * codigo permite cambiarlos sin recompilar, y deja explicito en un solo
 * lugar cuales son los parametros del negocio.
 */
@ConfigurationProperties(prefix = "minierp.negocio")
public class PropiedadesNegocio {

    /** Tasa de IVA vigente. Guatemala: 0.12 */
    private BigDecimal iva = new BigDecimal("0.12");

    /** Metodo de valuacion de inventario. Este proyecto opera con UEPS. */
    private MetodoValuacion metodoValuacion = MetodoValuacion.UEPS;

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public MetodoValuacion getMetodoValuacion() {
        return metodoValuacion;
    }

    public void setMetodoValuacion(MetodoValuacion metodoValuacion) {
        this.metodoValuacion = metodoValuacion;
    }
}
