package com.minierp.backend.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones del sistema a respuestas HTTP uniformes.
 *
 * Usa ProblemDetail (RFC 9457), que es el formato estandar de Spring 6
 * en adelante, de modo que los errores propios y los del framework
 * tengan la misma forma y el cliente Angular pueda manejarlos con un
 * solo interceptor.
 *
 * Nunca se filtra el detalle interno de una excepcion no prevista: se
 * registra completa en el log del servidor y al cliente se le devuelve
 * un mensaje generico. Un stack trace en la respuesta es informacion
 * util para quien quiera atacar el sistema.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private static final String BASE_TIPO = "https://minierp.gt/errores/";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex,
                                             HttpServletRequest peticion) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                ex.getMessage(), "recurso-no-encontrado", peticion);
    }

    @ExceptionHandler(ExistenciaInsuficienteException.class)
    public ProblemDetail manejarExistencias(ExistenciaInsuficienteException ex,
                                            HttpServletRequest peticion) {
        ProblemDetail problema = construir(HttpStatus.CONFLICT, "Existencias insuficientes",
                ex.getMessage(), "existencias-insuficientes", peticion);
        problema.setProperty("codigoProducto", ex.getCodigoProducto());
        problema.setProperty("solicitado", ex.getSolicitado());
        problema.setProperty("disponible", ex.getDisponible());
        return problema;
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(ReglaNegocioException ex,
                                             HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "Regla de negocio violada",
                ex.getMessage(), "regla-negocio", peticion);
    }

    /**
     * Errores de validacion de los DTO de entrada. Devuelve el detalle
     * campo por campo para que el formulario de Angular pueda marcar
     * exactamente cual esta mal.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex,
                                           HttpServletRequest peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = construir(HttpStatus.BAD_REQUEST, "Datos invalidos",
                "Uno o mas campos no cumplen las validaciones requeridas",
                "validacion", peticion);
        problema.setProperty("errores", errores);
        return problema;
    }

    /**
     * Violacion de una restriccion de la base: UNIQUE, CHECK o llave
     * foranea. Llegar aqui significa que una regla se escapo de la capa
     * de servicio y la ataja el motor. Se registra como advertencia
     * porque casi siempre senala un hueco en la validacion previa.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail manejarIntegridad(DataIntegrityViolationException ex,
                                           HttpServletRequest peticion) {
        log.warn("Restriccion de base de datos violada en {}: {}",
                peticion.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT, "Operacion rechazada por la base de datos",
                "La operacion viola una restriccion de integridad. "
                        + "Verifique que los datos no esten duplicados y que las "
                        + "cantidades y montos sean validos.",
                "integridad", peticion);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarNoPrevista(Exception ex,
                                                           HttpServletRequest peticion) {
        log.error("Error no previsto en {}", peticion.getRequestURI(), ex);
        ProblemDetail problema = construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Ocurrio un error inesperado. Consulte el registro del servidor.",
                "interno", peticion);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

    private ProblemDetail construir(HttpStatus estado, String titulo, String detalle,
                                    String tipo, HttpServletRequest peticion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create(BASE_TIPO + tipo));
        problema.setProperty("marcaTiempo", LocalDateTime.now().toString());
        problema.setProperty("ruta", peticion.getRequestURI());
        return problema;
    }
}
