package com.minierp.backend.servicio;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.dto.catalogo.CategoriaPeticion;
import com.minierp.backend.dto.catalogo.CategoriaRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.entidad.Categoria;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.especificacion.Especificaciones;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.excepcion.ReglaNegocioException;
import com.minierp.backend.repositorio.CategoriaRepositorio;
import com.minierp.backend.repositorio.ProductoRepositorio;
import com.minierp.backend.util.Texto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de categorias. Las mantiene el area de Inventario.
 */
@Service
public class ServicioCategoria {

    private final CategoriaRepositorio categoriaRepositorio;
    private final ProductoRepositorio productoRepositorio;

    public ServicioCategoria(CategoriaRepositorio categoriaRepositorio,
                             ProductoRepositorio productoRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
        this.productoRepositorio = productoRepositorio;
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<CategoriaRespuesta> listar(String busqueda, Boolean activo,
                                                       Pageable paginacion) {
        Specification<Categoria> filtro = Especificaciones.<Categoria>activo(activo)
                .and(Especificaciones.texto(busqueda, "nombre", "descripcion"));
        return RespuestaPaginada.de(
                categoriaRepositorio.findAll(filtro, paginacion), CategoriaRespuesta::de);
    }

    /** Listado sin paginar, para los selectores del cliente. */
    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listarActivas() {
        return categoriaRepositorio.findByActivoTrueOrderByNombreAsc()
                .stream().map(CategoriaRespuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaRespuesta obtener(Long id) {
        return CategoriaRespuesta.de(buscar(id));
    }

    @Auditable(modulo = ModuloBitacora.CATEGORIAS, accion = AccionBitacora.CREAR,
               entidad = "categoria", descripcion = "Registro de una nueva categoria")
    @Transactional
    public CategoriaRespuesta crear(CategoriaPeticion peticion) {
        String nombre = Texto.limpiar(peticion.nombre());
        if (categoriaRepositorio.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe una categoria con el nombre '" + nombre + "'");
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(Texto.limpiar(peticion.descripcion()));
        categoria.setActivo(true);
        return CategoriaRespuesta.de(categoriaRepositorio.save(categoria));
    }

    @Auditable(modulo = ModuloBitacora.CATEGORIAS, accion = AccionBitacora.ACTUALIZAR,
               entidad = "categoria", descripcion = "Modificacion de una categoria")
    @Transactional
    public CategoriaRespuesta actualizar(Long id, CategoriaPeticion peticion) {
        Categoria categoria = buscar(id);
        String nombre = Texto.limpiar(peticion.nombre());

        // Solo es duplicado si el nombre pertenece a OTRA categoria.
        categoriaRepositorio.findByNombreIgnoreCase(nombre).ifPresent(existente -> {
            if (!existente.getIdCategoria().equals(id)) {
                throw new ReglaNegocioException(
                        "Ya existe otra categoria con el nombre '" + nombre + "'");
            }
        });

        categoria.setNombre(nombre);
        categoria.setDescripcion(Texto.limpiar(peticion.descripcion()));
        return CategoriaRespuesta.de(categoriaRepositorio.save(categoria));
    }

    /**
     * Borrado logico. No elimina la fila porque los productos y, a traves
     * de ellos, el historial de compras y ventas la referencian.
     *
     * Se impide desactivar una categoria que aun tenga productos activos:
     * quedarian clasificados en una categoria que ya no existe para el
     * negocio, y los reportes por categoria dejarian de cuadrar.
     */
    @Auditable(modulo = ModuloBitacora.CATEGORIAS, accion = AccionBitacora.DESACTIVAR,
               entidad = "categoria", descripcion = "Desactivacion de una categoria")
    @Transactional
    public CategoriaRespuesta desactivar(Long id) {
        Categoria categoria = buscar(id);
        if (Boolean.FALSE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException("La categoria ya se encuentra desactivada");
        }
        long activos = productoRepositorio.countByCategoriaIdCategoriaAndActivoTrue(id);
        if (activos > 0) {
            throw new ReglaNegocioException(
                    "No se puede desactivar la categoria porque tiene " + activos
                            + " producto(s) activo(s). Desactive o reclasifique primero esos productos.");
        }
        categoria.setActivo(false);
        return CategoriaRespuesta.de(categoriaRepositorio.save(categoria));
    }

    @Auditable(modulo = ModuloBitacora.CATEGORIAS, accion = AccionBitacora.ACTIVAR,
               entidad = "categoria", descripcion = "Reactivacion de una categoria")
    @Transactional
    public CategoriaRespuesta activar(Long id) {
        Categoria categoria = buscar(id);
        if (Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException("La categoria ya se encuentra activa");
        }
        categoria.setActivo(true);
        return CategoriaRespuesta.de(categoriaRepositorio.save(categoria));
    }

    private Categoria buscar(Long id) {
        return categoriaRepositorio.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoria", id));
    }
}
