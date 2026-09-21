package com.minierp.backend.dto.inventario;

import com.minierp.backend.entidad.enums.TipoMovimiento;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Ajuste manual de existencias, tras un conteo fisico o una merma.
 *
 * costoUnitario solo aplica a las ENTRADAS y es opcional: si se omite,
 * se usa el costo de la capa mas reciente del producto. En las SALIDAS
 * el costo no se pide: lo determina el consumo UEPS de las capas.
 */
public record AjustePeticion(
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "El tipo de ajuste es obligatorio (ENTRADA o SALIDA)")
        TipoMovimiento tipo,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100000, message = "La cantidad no puede exceder 100000")
        Integer cantidad,

        @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor que cero")
        @Digits(integer = 10, fraction = 4, message = "El costo admite hasta 10 enteros y 4 decimales")
        BigDecimal costoUnitario,

        @NotBlank(message = "El motivo del ajuste es obligatorio")
        @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
        String motivo
) {
}
