package com.minierp.backend.dto.catalogo;

import com.minierp.backend.entidad.Proveedor;

public record ProveedorRespuesta(
        Long idProveedor,
        String nit,
        String nombre,
        String contacto,
        String telefono,
        String correo,
        String direccion,
        Boolean activo
) {
    public static ProveedorRespuesta de(Proveedor proveedor) {
        return new ProveedorRespuesta(
                proveedor.getIdProveedor(),
                proveedor.getNit(),
                proveedor.getNombre(),
                proveedor.getContacto(),
                proveedor.getTelefono(),
                proveedor.getCorreo(),
                proveedor.getDireccion(),
                proveedor.getActivo());
    }
}
