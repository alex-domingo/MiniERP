package com.minierp.backend.servicio;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.dto.catalogo.CostoReferenciaPeticion;
import com.minierp.backend.dto.catalogo.ProveedorPeticion;
import com.minierp.backend.dto.catalogo.ProveedorProductoPeticion;
import com.minierp.backend.dto.catalogo.ProveedorProductoRespuesta;
import com.minierp.backend.dto.catalogo.ProveedorRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.entidad.Producto;
import com.minierp.backend.entidad.Proveedor;
import com.minierp.backend.entidad.ProveedorProducto;
import com.minierp.backend.entidad.ProveedorProductoId;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.especificacion.Especificaciones;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.excepcion.ReglaNegocioException;
import com.minierp.backend.repositorio.ProductoRepositorio;
import com.minierp.backend.repositorio.ProveedorProductoRepositorio;
import com.minierp.backend.repositorio.ProveedorRepositorio;
import com.minierp.backend.util.Texto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de proveedores y de la relacion N:M proveedor-producto.
 * La mantiene el area de Compras.
 */
@Service
public class ServicioProveedor {

    private final ProveedorRepositorio proveedorRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final ProveedorProductoRepositorio asociacionRepositorio;

    public ServicioProveedor(ProveedorRepositorio proveedorRepositorio,
                             ProductoRepositorio productoRepositorio,
                             ProveedorProductoRepositorio asociacionRepositorio) {
        this.proveedorRepositorio = proveedorRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.asociacionRepositorio = asociacionRepositorio;
    }

    // -----------------------------------------------------------------
    //  Proveedores
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public RespuestaPaginada<ProveedorRespuesta> listar(String busqueda, Boolean activo,
                                                       Pageable paginacion) {
        Specification<Proveedor> filtro = Especificaciones.<Proveedor>activo(activo)
                .and(Especificaciones.texto(busqueda, "nit", "nombre", "contacto"));
        return RespuestaPaginada.de(
                proveedorRepositorio.findAll(filtro, paginacion), ProveedorRespuesta::de);
    }

    @Transactional(readOnly = true)
    public List<ProveedorRespuesta> listarActivos() {
        return proveedorRepositorio.findByActivoTrueOrderByNombreAsc()
                .stream().map(ProveedorRespuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorRespuesta obtener(Long id) {
        return ProveedorRespuesta.de(buscar(id));
    }

    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.CREAR,
               entidad = "proveedor", descripcion = "Registro de un nuevo proveedor")
    @Transactional
    public ProveedorRespuesta crear(ProveedorPeticion peticion) {
        String nit = Texto.normalizarNit(peticion.nit());
        if (proveedorRepositorio.existsByNit(nit)) {
            throw new ReglaNegocioException("Ya existe un proveedor con el NIT " + nit);
        }
        Proveedor proveedor = new Proveedor();
        proveedor.setNit(nit);
        aplicar(proveedor, peticion);
        proveedor.setActivo(true);
        return ProveedorRespuesta.de(proveedorRepositorio.save(proveedor));
    }

    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.ACTUALIZAR,
               entidad = "proveedor", descripcion = "Modificacion de un proveedor")
    @Transactional
    public ProveedorRespuesta actualizar(Long id, ProveedorPeticion peticion) {
        Proveedor proveedor = buscar(id);
        String nit = Texto.normalizarNit(peticion.nit());

        proveedorRepositorio.findByNit(nit).ifPresent(existente -> {
            if (!existente.getIdProveedor().equals(id)) {
                throw new ReglaNegocioException("Ya existe otro proveedor con el NIT " + nit);
            }
        });

        proveedor.setNit(nit);
        aplicar(proveedor, peticion);
        return ProveedorRespuesta.de(proveedorRepositorio.save(proveedor));
    }

    /**
     * Borrado logico. Un proveedor desactivado conserva su historial de
     * compras y sus asociaciones, pero ya no admite compras nuevas.
     */
    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.DESACTIVAR,
               entidad = "proveedor", descripcion = "Desactivacion de un proveedor")
    @Transactional
    public ProveedorRespuesta desactivar(Long id) {
        Proveedor proveedor = buscar(id);
        if (Boolean.FALSE.equals(proveedor.getActivo())) {
            throw new ReglaNegocioException("El proveedor ya se encuentra desactivado");
        }
        proveedor.setActivo(false);
        return ProveedorRespuesta.de(proveedorRepositorio.save(proveedor));
    }

    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.ACTIVAR,
               entidad = "proveedor", descripcion = "Reactivacion de un proveedor")
    @Transactional
    public ProveedorRespuesta activar(Long id) {
        Proveedor proveedor = buscar(id);
        if (Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new ReglaNegocioException("El proveedor ya se encuentra activo");
        }
        proveedor.setActivo(true);
        return ProveedorRespuesta.de(proveedorRepositorio.save(proveedor));
    }

    // -----------------------------------------------------------------
    //  Relacion N:M con productos
    // -----------------------------------------------------------------

    /** Productos que suministra un proveedor. */
    @Transactional(readOnly = true)
    public List<ProveedorProductoRespuesta> productosDe(Long idProveedor) {
        buscar(idProveedor);
        return asociacionRepositorio.findByProveedorIdProveedorOrderByProductoCodigoAsc(idProveedor)
                .stream().map(ProveedorProductoRespuesta::de).toList();
    }

    /** Proveedores que suministran un producto, del mas barato al mas caro. */
    @Transactional(readOnly = true)
    public List<ProveedorProductoRespuesta> proveedoresDe(Long idProducto) {
        if (!productoRepositorio.existsById(idProducto)) {
            throw RecursoNoEncontradoException.de("Producto", idProducto);
        }
        return asociacionRepositorio.findByProductoIdProductoOrderByCostoReferenciaAsc(idProducto)
                .stream().map(ProveedorProductoRespuesta::de).toList();
    }

    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.CREAR,
               entidad = "proveedor_producto",
               descripcion = "Nuevo producto en el catalogo del proveedor")
    @Transactional
    public ProveedorProductoRespuesta asociar(Long idProveedor, ProveedorProductoPeticion peticion) {
        Proveedor proveedor = buscar(idProveedor);
        Producto producto = productoRepositorio.findById(peticion.idProducto())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", peticion.idProducto()));

        if (Boolean.FALSE.equals(proveedor.getActivo())) {
            throw new ReglaNegocioException("El proveedor '" + proveedor.getNombre() + "' esta desactivado");
        }
        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new ReglaNegocioException("El producto '" + producto.getCodigo() + "' esta desactivado");
        }

        ProveedorProductoId id = new ProveedorProductoId(idProveedor, producto.getIdProducto());
        if (asociacionRepositorio.existsById(id)) {
            throw new ReglaNegocioException("El proveedor '" + proveedor.getNombre()
                    + "' ya suministra el producto '" + producto.getCodigo()
                    + "'. Para cambiar el costo, actualice la asociacion existente.");
        }

        ProveedorProducto asociacion = new ProveedorProducto();
        asociacion.setId(id);
        asociacion.setProveedor(proveedor);
        asociacion.setProducto(producto);
        asociacion.setCostoReferencia(peticion.costoReferencia());
        return ProveedorProductoRespuesta.de(asociacionRepositorio.save(asociacion));
    }

    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.ACTUALIZAR,
               entidad = "proveedor_producto",
               descripcion = "Cambio de costo de referencia con el proveedor")
    @Transactional
    public ProveedorProductoRespuesta actualizarCosto(Long idProveedor, Long idProducto,
                                                      CostoReferenciaPeticion peticion) {
        ProveedorProducto asociacion = buscarAsociacion(idProveedor, idProducto);
        asociacion.setCostoReferencia(peticion.costoReferencia());
        return ProveedorProductoRespuesta.de(asociacionRepositorio.save(asociacion));
    }

    /**
     * Retira un producto del catalogo del proveedor. Es seguro borrar la
     * fila: las compras historicas no dependen de esta relacion, sino de
     * sus propias llaves hacia proveedor y producto, que se conservan.
     *
     * Se registra como ACTUALIZAR porque, desde el negocio, es una
     * modificacion del catalogo del proveedor.
     */
    @Auditable(modulo = ModuloBitacora.PROVEEDORES, accion = AccionBitacora.ACTUALIZAR,
               entidad = "proveedor_producto",
               descripcion = "Retiro de un producto del catalogo del proveedor")
    @Transactional
    public ProveedorProductoRespuesta desasociar(Long idProveedor, Long idProducto) {
        ProveedorProducto asociacion = buscarAsociacion(idProveedor, idProducto);
        ProveedorProductoRespuesta respuesta = ProveedorProductoRespuesta.de(asociacion);
        asociacionRepositorio.delete(asociacion);
        return respuesta;
    }

    // -----------------------------------------------------------------

    private void aplicar(Proveedor proveedor, ProveedorPeticion peticion) {
        proveedor.setNombre(Texto.limpiar(peticion.nombre()));
        proveedor.setContacto(Texto.limpiar(peticion.contacto()));
        proveedor.setTelefono(Texto.limpiar(peticion.telefono()));
        proveedor.setCorreo(Texto.limpiar(peticion.correo()));
        proveedor.setDireccion(Texto.limpiar(peticion.direccion()));
    }

    private Proveedor buscar(Long id) {
        return proveedorRepositorio.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Proveedor", id));
    }

    private ProveedorProducto buscarAsociacion(Long idProveedor, Long idProducto) {
        return asociacionRepositorio.findById(new ProveedorProductoId(idProveedor, idProducto))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El proveedor " + idProveedor + " no suministra el producto " + idProducto));
    }
}
