package com.minierp.backend.servicio;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.dto.catalogo.ProductoPeticion;
import com.minierp.backend.dto.catalogo.ProductoRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.entidad.Categoria;
import com.minierp.backend.entidad.Producto;
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
 * Gestion del catalogo de productos. Lo mantiene el area de Inventario.
 *
 * Este servicio NUNCA modifica stockActual. Las existencias cambian
 * unicamente por compras, ventas y ajustes, a traves del motor de
 * inventario, para que todo cambio quede respaldado en el kardex.
 */
@Service
public class ServicioProducto {

    private final ProductoRepositorio productoRepositorio;
    private final CategoriaRepositorio categoriaRepositorio;

    public ServicioProducto(ProductoRepositorio productoRepositorio,
                            CategoriaRepositorio categoriaRepositorio) {
        this.productoRepositorio = productoRepositorio;
        this.categoriaRepositorio = categoriaRepositorio;
    }

    /**
     * Listado con filtros combinables: texto (en codigo o nombre),
     * categoria y estado. Cualquier combinacion es valida, incluida
     * ninguna.
     */
    @Transactional(readOnly = true)
    public RespuestaPaginada<ProductoRespuesta> listar(String busqueda, Long idCategoria,
                                                      Boolean activo, Pageable paginacion) {
        Specification<Producto> filtro = Especificaciones.<Producto>activo(activo)
                .and(Especificaciones.igual(idCategoria, "categoria", "idCategoria"))
                .and(Especificaciones.texto(busqueda, "codigo", "nombre"));
        return RespuestaPaginada.de(
                productoRepositorio.findAll(filtro, paginacion), ProductoRespuesta::de);
    }

    @Transactional(readOnly = true)
    public ProductoRespuesta obtener(Long id) {
        return ProductoRespuesta.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public ProductoRespuesta obtenerPorCodigo(String codigo) {
        return productoRepositorio.findByCodigoIgnoreCase(Texto.limpiar(codigo))
                .map(ProductoRespuesta::de)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", codigo));
    }

    /**
     * Productos que alcanzaron el nivel de existencias que requiere
     * atencion, segun el enunciado.
     */
    @Transactional(readOnly = true)
    public List<ProductoRespuesta> alertasDeExistencia() {
        return productoRepositorio.buscarConExistenciaBaja()
                .stream().map(ProductoRespuesta::de).toList();
    }

    @Auditable(modulo = ModuloBitacora.PRODUCTOS, accion = AccionBitacora.CREAR,
               entidad = "producto", descripcion = "Registro de un nuevo producto")
    @Transactional
    public ProductoRespuesta crear(ProductoPeticion peticion) {
        String codigo = Texto.mayusculas(peticion.codigo());
        if (productoRepositorio.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un producto con el codigo '" + codigo + "'");
        }

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        aplicar(producto, peticion);
        // Nace sin existencias: solo una compra puede darle inventario.
        producto.setStockActual(0);
        producto.setActivo(true);

        return ProductoRespuesta.de(productoRepositorio.save(producto));
    }

    @Auditable(modulo = ModuloBitacora.PRODUCTOS, accion = AccionBitacora.ACTUALIZAR,
               entidad = "producto", descripcion = "Modificacion de un producto")
    @Transactional
    public ProductoRespuesta actualizar(Long id, ProductoPeticion peticion) {
        Producto producto = buscar(id);
        String codigo = Texto.mayusculas(peticion.codigo());

        productoRepositorio.findByCodigoIgnoreCase(codigo).ifPresent(existente -> {
            if (!existente.getIdProducto().equals(id)) {
                throw new ReglaNegocioException(
                        "Ya existe otro producto con el codigo '" + codigo + "'");
            }
        });

        producto.setCodigo(codigo);
        aplicar(producto, peticion);
        return ProductoRespuesta.de(productoRepositorio.save(producto));
    }

    /**
     * Borrado logico: el producto deja de comercializarse pero permanece
     * en las compras y ventas donde ya participo, con el precio que tenia
     * en cada una.
     *
     * Se impide si aun quedan existencias: esas unidades quedarian en el
     * inventario sin poder venderse, y seguirian sumando a la valuacion.
     * Primero hay que retirarlas con un ajuste de salida.
     */
    @Auditable(modulo = ModuloBitacora.PRODUCTOS, accion = AccionBitacora.DESACTIVAR,
               entidad = "producto", descripcion = "Desactivacion de un producto")
    @Transactional
    public ProductoRespuesta desactivar(Long id) {
        Producto producto = buscar(id);
        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("El producto ya se encuentra desactivado");
        }
        if (producto.getStockActual() != null && producto.getStockActual() > 0) {
            throw new ReglaNegocioException(
                    "No se puede desactivar '" + producto.getNombre() + "' porque aun tiene "
                            + producto.getStockActual() + " unidades en existencia. "
                            + "Retirelas primero con un ajuste de salida.");
        }
        producto.setActivo(false);
        return ProductoRespuesta.de(productoRepositorio.save(producto));
    }

    @Auditable(modulo = ModuloBitacora.PRODUCTOS, accion = AccionBitacora.ACTIVAR,
               entidad = "producto", descripcion = "Reactivacion de un producto")
    @Transactional
    public ProductoRespuesta activar(Long id) {
        Producto producto = buscar(id);
        if (Boolean.TRUE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("El producto ya se encuentra activo");
        }
        if (Boolean.FALSE.equals(producto.getCategoria().getActivo())) {
            throw new ReglaNegocioException("No se puede reactivar el producto: su categoria '"
                    + producto.getCategoria().getNombre() + "' esta desactivada");
        }
        producto.setActivo(true);
        return ProductoRespuesta.de(productoRepositorio.save(producto));
    }

    private void aplicar(Producto producto, ProductoPeticion peticion) {
        Categoria categoria = categoriaRepositorio.findById(peticion.idCategoria())
                .orElseThrow(() -> RecursoNoEncontradoException.de(
                        "Categoria", peticion.idCategoria()));
        if (Boolean.FALSE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException(
                    "La categoria '" + categoria.getNombre() + "' esta desactivada");
        }

        String unidad = Texto.mayusculas(peticion.unidadMedida());
        producto.setNombre(Texto.limpiar(peticion.nombre()));
        producto.setDescripcion(Texto.limpiar(peticion.descripcion()));
        producto.setCategoria(categoria);
        producto.setUnidadMedida(unidad == null ? "UNIDAD" : unidad);
        producto.setPrecioVenta(peticion.precioVenta());
        producto.setStockMinimo(peticion.stockMinimo());
    }

    private Producto buscar(Long id) {
        return productoRepositorio.findByIdProducto(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", id));
    }
}
