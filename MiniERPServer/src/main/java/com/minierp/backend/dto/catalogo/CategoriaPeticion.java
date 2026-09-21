package com.minierp.backend.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaPeticion(

        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 60, message = "El nombre no puede exceder 60 caracteres")
        String nombre,

        @Size(max = 200, message = "La descripcion no puede exceder 200 caracteres")
        String descripcion
) {
}
