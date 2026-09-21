package com.minierp.backend.controlador;

import com.minierp.backend.excepcion.SolicitudInvalidaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Construye la paginacion a partir de los parametros del cliente.
 *
 * El campo de ordenamiento se valida contra una lista blanca. Sin ella,
 * ordenar por un campo inexistente ("?orden=precio" en lugar de
 * "precioVenta") lanza PropertyReferenceException, que el cliente
 * recibiria como 500 aunque el error fue suyo.
 *
 * Siempre se agrega el identificador como criterio de desempate: si dos
 * filas tienen el mismo valor en el campo ordenado, sin desempate la
 * base puede devolverlas en distinto orden en cada consulta, y una fila
 * podria aparecer en dos paginas o en ninguna.
 */
public final class Paginacion {

    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    /**
     * @param orden       "campo" o "campo,asc" o "campo,desc"
     * @param permitidos  campos por los que se admite ordenar
     * @param campoId     identificador, usado como desempate
     */
    public static Pageable crear(int pagina, int tamano, String orden,
                                 Set<String> permitidos, String porDefecto, String campoId) {
        return crear(pagina, tamano, orden, permitidos, porDefecto, Sort.Direction.ASC, campoId);
    }

    /** Variante con direccion por defecto, para listados donde lo natural es "mas reciente primero". */
    public static Pageable crear(int pagina, int tamano, String orden, Set<String> permitidos,
                                 String porDefecto, Sort.Direction direccionPorDefecto, String campoId) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException("La pagina no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException(
                    "El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO);
        }

        String campo = porDefecto;
        Sort.Direction direccion = direccionPorDefecto;

        if (orden != null && !orden.isBlank()) {
            String[] partes = orden.split(",");
            campo = partes[0].trim();
            if (partes.length > 1) {
                String dir = partes[1].trim().toLowerCase();
                if (!dir.equals("asc") && !dir.equals("desc")) {
                    throw new SolicitudInvalidaException(
                            "La direccion de orden debe ser 'asc' o 'desc'");
                }
                direccion = dir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            }
            if (!permitidos.contains(campo)) {
                throw new SolicitudInvalidaException("No se puede ordenar por '" + campo
                        + "'. Campos permitidos: " + String.join(", ", permitidos));
            }
        }

        Sort sort = Sort.by(direccion, campo);
        if (!campo.equals(campoId)) {
            sort = sort.and(Sort.by(direccion, campoId));
        }
        return PageRequest.of(pagina, tamano, sort);
    }
}
