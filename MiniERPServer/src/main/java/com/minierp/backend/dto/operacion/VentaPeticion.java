package com.minierp.backend.dto.operacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VentaPeticion(
        @NotNull(message = "El cliente es obligatorio")
        Long idCliente,

        @NotEmpty(message = "La venta debe tener al menos una linea")
        @Size(max = 100, message = "Una venta admite hasta 100 lineas")
        List<@Valid @NotNull LineaVentaPeticion> lineas
) {
}
