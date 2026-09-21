package com.minierp.backend.dto.catalogo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Cambio del costo de referencia de un producto con un proveedor. */
public record CostoReferenciaPeticion(

        @NotNull(message = "El costo de referencia es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo de referencia debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2,
                message = "El costo admite hasta 10 enteros y 2 decimales")
        BigDecimal costoReferencia
) {
}
