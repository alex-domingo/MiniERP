package com.minierp.backend.dto.catalogo;

import com.minierp.backend.entidad.Cliente;

public record ClienteRespuesta(
        Long idCliente,
        String nit,
        String nombre,
        String telefono,
        String correo,
        String direccion,
        Boolean activo
) {
    public static ClienteRespuesta de(Cliente cliente) {
        return new ClienteRespuesta(
                cliente.getIdCliente(),
                cliente.getNit(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getDireccion(),
                cliente.getActivo());
    }
}
