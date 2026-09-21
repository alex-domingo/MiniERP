package com.minierp.backend.dto.operacion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Linea de venta. No lleva precio a proposito: el precio sale del
 * catalogo en el servidor. Si el cliente pudiera enviarlo, cualquiera
 * podria venderse a si mismo a Q0.01.
 */
public record LineaVentaPeticion(
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100000, message = "La cantidad no puede exceder 100000")
        Integer cantidad
) {
}
