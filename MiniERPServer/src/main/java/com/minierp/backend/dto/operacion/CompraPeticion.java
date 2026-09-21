package com.minierp.backend.dto.operacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Registro de una compra. No lleva fecha ni usuario: la fecha la pone
 * el servidor (una compra con fecha retroactiva alteraria el orden de
 * las capas UEPS que ya consumieron ventas posteriores) y el usuario se
 * toma del token.
 */
public record CompraPeticion(
        @NotNull(message = "El proveedor es obligatorio")
        Long idProveedor,

        @Size(max = 300, message = "Las observaciones no pueden exceder 300 caracteres")
        String observaciones,

        @NotEmpty(message = "La compra debe tener al menos una linea")
        @Size(max = 100, message = "Una compra admite hasta 100 lineas")
        List<@Valid @NotNull LineaCompraPeticion> lineas
) {
}
