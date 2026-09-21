package com.minierp.backend.dto.catalogo;

import com.minierp.backend.entidad.Categoria;

public record CategoriaRespuesta(
        Long idCategoria,
        String nombre,
        String descripcion,
        Boolean activo
) {
    public static CategoriaRespuesta de(Categoria categoria) {
        return new CategoriaRespuesta(
                categoria.getIdCategoria(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActivo());
    }
}
