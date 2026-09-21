package com.minierp.backend.servicio;

import com.minierp.backend.auditoria.Auditable;
import com.minierp.backend.dto.catalogo.ClientePeticion;
import com.minierp.backend.dto.catalogo.ClienteRespuesta;
import com.minierp.backend.dto.comun.RespuestaPaginada;
import com.minierp.backend.entidad.Cliente;
import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;
import com.minierp.backend.especificacion.Especificaciones;
import com.minierp.backend.excepcion.RecursoNoEncontradoException;
import com.minierp.backend.excepcion.ReglaNegocioException;
import com.minierp.backend.repositorio.ClienteRepositorio;
import com.minierp.backend.util.Texto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de clientes. La mantiene el area de Ventas.
 *
 * El enunciado pide conservar la informacion de los clientes "para
 * consultar posteriormente las operaciones realizadas con cada uno".
 * Por eso tambien aqui el borrado es logico.
 */
@Service
public class ServicioCliente {

    private final ClienteRepositorio clienteRepositorio;

    public ServicioCliente(ClienteRepositorio clienteRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
    }

    @Transactional(readOnly = true)
    public RespuestaPaginada<ClienteRespuesta> listar(String busqueda, Boolean activo,
                                                     Pageable paginacion) {
        Specification<Cliente> filtro = Especificaciones.<Cliente>activo(activo)
                .and(Especificaciones.texto(busqueda, "nit", "nombre", "correo"));
        return RespuestaPaginada.de(
                clienteRepositorio.findAll(filtro, paginacion), ClienteRespuesta::de);
    }

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listarActivos() {
        return clienteRepositorio.findByActivoTrueOrderByNombreAsc()
                .stream().map(ClienteRespuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteRespuesta obtener(Long id) {
        return ClienteRespuesta.de(buscar(id));
    }

    @Auditable(modulo = ModuloBitacora.CLIENTES, accion = AccionBitacora.CREAR,
               entidad = "cliente", descripcion = "Registro de un nuevo cliente")
    @Transactional
    public ClienteRespuesta crear(ClientePeticion peticion) {
        String nit = Texto.normalizarNit(peticion.nit());
        if (clienteRepositorio.existsByNit(nit)) {
            throw new ReglaNegocioException("Ya existe un cliente con el NIT " + nit);
        }
        Cliente cliente = new Cliente();
        cliente.setNit(nit);
        aplicar(cliente, peticion);
        cliente.setActivo(true);
        return ClienteRespuesta.de(clienteRepositorio.save(cliente));
    }

    @Auditable(modulo = ModuloBitacora.CLIENTES, accion = AccionBitacora.ACTUALIZAR,
               entidad = "cliente", descripcion = "Modificacion de un cliente")
    @Transactional
    public ClienteRespuesta actualizar(Long id, ClientePeticion peticion) {
        Cliente cliente = buscar(id);
        String nit = Texto.normalizarNit(peticion.nit());

        clienteRepositorio.findByNit(nit).ifPresent(existente -> {
            if (!existente.getIdCliente().equals(id)) {
                throw new ReglaNegocioException("Ya existe otro cliente con el NIT " + nit);
            }
        });

        cliente.setNit(nit);
        aplicar(cliente, peticion);
        return ClienteRespuesta.de(clienteRepositorio.save(cliente));
    }

    @Auditable(modulo = ModuloBitacora.CLIENTES, accion = AccionBitacora.DESACTIVAR,
               entidad = "cliente", descripcion = "Desactivacion de un cliente")
    @Transactional
    public ClienteRespuesta desactivar(Long id) {
        Cliente cliente = buscar(id);
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("El cliente ya se encuentra desactivado");
        }
        cliente.setActivo(false);
        return ClienteRespuesta.de(clienteRepositorio.save(cliente));
    }

    @Auditable(modulo = ModuloBitacora.CLIENTES, accion = AccionBitacora.ACTIVAR,
               entidad = "cliente", descripcion = "Reactivacion de un cliente")
    @Transactional
    public ClienteRespuesta activar(Long id) {
        Cliente cliente = buscar(id);
        if (Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("El cliente ya se encuentra activo");
        }
        cliente.setActivo(true);
        return ClienteRespuesta.de(clienteRepositorio.save(cliente));
    }

    private void aplicar(Cliente cliente, ClientePeticion peticion) {
        cliente.setNombre(Texto.limpiar(peticion.nombre()));
        cliente.setTelefono(Texto.limpiar(peticion.telefono()));
        cliente.setCorreo(Texto.limpiar(peticion.correo()));
        cliente.setDireccion(Texto.limpiar(peticion.direccion()));
    }

    private Cliente buscar(Long id) {
        return clienteRepositorio.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", id));
    }
}
