package com.minierp.backend.dto.catalogo;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Datos con los que se crea o modifica un producto.
 *
 * NO incluye stockActual, y es deliberado: las existencias solo cambian
 * por compras, ventas o ajustes registrados en el kardex. Permitir
 * fijarlas desde el CRUD abriria la puerta a existencias sin respaldo
 * en movimientos, que es exactamente lo que el enunciado prohibe.
 */
public record ProductoPeticion(

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo no puede exceder 20 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$",
                 message = "El codigo solo admite letras, numeros y guiones")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @Size(max = 300, message = "La descripcion no puede exceder 300 caracteres")
        String descripcion,

        @NotNull(message = "La categoria es obligatoria")
        Long idCategoria,

        @Size(max = 20, message = "La unidad de medida no puede exceder 20 caracteres")
        String unidadMedida,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2,
                message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal precioVenta,

        @NotNull(message = "El stock minimo es obligatorio")
        @Min(value = 0, message = "El stock minimo no puede ser negativo")
        Integer stockMinimo
) {
}
