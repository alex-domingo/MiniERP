package com.minierp.backend.dto.operacion;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LineaCompraPeticion(
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100000, message = "La cantidad no puede exceder 100000")
        Integer cantidad,

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El costo admite hasta 10 enteros y 2 decimales")
        BigDecimal costoUnitario
) {
}
