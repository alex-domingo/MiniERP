package com.minierp.backend.dto.catalogo;

import com.minierp.backend.util.Texto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorPeticion(

        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 20, message = "El NIT no puede exceder 20 caracteres")
        @Pattern(regexp = Texto.PATRON_NIT,
                 message = "El NIT debe tener el formato 1234567-8, o CF para consumidor final")
        String nit,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @Size(max = 120, message = "El contacto no puede exceder 120 caracteres")
        String contacto,

        @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
        String telefono,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 120, message = "El correo no puede exceder 120 caracteres")
        String correo,

        @Size(max = 200, message = "La direccion no puede exceder 200 caracteres")
        String direccion
) {
}
