package com.minierp.backend.dto.autenticacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales enviadas por el cliente Angular al iniciar sesion.
 */
public record PeticionLogin(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 40, message = "El nombre de usuario debe tener entre 4 y 40 caracteres")
        String usuario,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres")
        String contrasena
) {
}
