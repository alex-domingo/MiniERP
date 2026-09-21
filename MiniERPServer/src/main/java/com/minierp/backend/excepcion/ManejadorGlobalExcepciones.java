package com.minierp.backend.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones del sistema a respuestas HTTP uniformes en
 * formato ProblemDetail (RFC 9457).
 *
 * Extiende ResponseEntityExceptionHandler, que ya sabe responder con el
 * codigo correcto a todas las excepciones estandar de Spring MVC:
 *
 *   JSON mal formado ................ 400
 *   parametro de tipo invalido ...... 400  (ej. /api/productos/abc)
 *   parametro obligatorio ausente ... 400
 *   ruta inexistente ................ 404
 *   metodo HTTP no soportado ........ 405
 *   tipo de contenido no soportado .. 415
 *
 * Sin esta herencia, todas esas caerian en el manejador generico de
 * Exception y el cliente recibiria un 500 por un simple error suyo.
 *
 * handleExceptionInternal se sobrescribe para que TODAS las respuestas,
 * propias y del framework, lleven los mismos campos adicionales.
 *
 * Nunca se filtra el detalle interno de una excepcion no prevista: se
 * registra completa en el log del servidor y al cliente se le devuelve
 * un mensaje generico.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private static final String BASE_TIPO = "https://minierp.gt/errores/";

    // -----------------------------------------------------------------
    //  Excepciones propias del negocio
    // -----------------------------------------------------------------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex,
                                             HttpServletRequest peticion) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                ex.getMessage(), "recurso-no-encontrado", peticion.getRequestURI());
    }

    @ExceptionHandler(ExistenciaInsuficienteException.class)
    public ProblemDetail manejarExistencias(ExistenciaInsuficienteException ex,
                                            HttpServletRequest peticion) {
        ProblemDetail problema = construir(HttpStatus.CONFLICT, "Existencias insuficientes",
                ex.getMessage(), "existencias-insuficientes", peticion.getRequestURI());
        problema.setProperty("codigoProducto", ex.getCodigoProducto());
        problema.setProperty("solicitado", ex.getSolicitado());
        problema.setProperty("disponible", ex.getDisponible());
        return problema;
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(ReglaNegocioException ex,
                                             HttpServletRequest peticion) {
        return construir(HttpStatus.CONFLICT, "Regla de negocio violada",
                ex.getMessage(), "regla-negocio", peticion.getRequestURI());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail manejarSolicitudInvalida(SolicitudInvalidaException ex,
                                                  HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "Solicitud invalida",
                ex.getMessage(), "solicitud-invalida", peticion.getRequestURI());
    }

    /**
     * Violacion de una restriccion de la base: UNIQUE, CHECK o llave
     * foranea. Llegar aqui significa que una regla se escapo de la capa
     * de servicio y la ataja el motor. Se registra como advertencia
     * porque casi siempre senala un hueco en la validacion previa, o
     * una carrera entre dos peticiones simultaneas.
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
                "integridad", peticion.getRequestURI());
    }

    /**
     * Credenciales invalidas o cuenta desactivada. Sin este manejador,
     * el generico lo convertiria en 500.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail manejarAutenticacion(AuthenticationException ex,
                                              HttpServletRequest peticion) {
        return construir(HttpStatus.UNAUTHORIZED, "Autenticacion fallida",
                ex.getMessage(), "credenciales-invalidas", peticion.getRequestURI());
    }

    /**
     * Permiso denegado que se produce DENTRO de un controlador (por
     * ejemplo, con seguridad a nivel de metodo). Las denegaciones por
     * ruta las atiende la cadena de filtros antes de llegar aqui.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarAccesoDenegado(AccessDeniedException ex,
                                               HttpServletRequest peticion) {
        return construir(HttpStatus.FORBIDDEN, "Acceso denegado",
                "Su rol no tiene permiso para realizar esta operacion.",
                "acceso-denegado", peticion.getRequestURI());
    }

    /** Ultimo recurso: lo que no encaja en ningun manejador anterior. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarNoPrevista(Exception ex,
                                                           HttpServletRequest peticion) {
        log.error("Error no previsto en {}", peticion.getRequestURI(), ex);
        ProblemDetail problema = construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Ocurrio un error inesperado. Consulte el registro del servidor.",
                "interno", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

    // -----------------------------------------------------------------
    //  Excepciones estandar de Spring MVC
    // -----------------------------------------------------------------

    /**
     * Errores de validacion de los DTO de entrada. Se sobrescribe el
     * metodo heredado en lugar de declarar un @ExceptionHandler propio:
     * hacer ambas cosas provoca "Ambiguous @ExceptionHandler method" y
     * la aplicacion no arranca.
     *
     * Devuelve el detalle campo por campo para que el formulario de
     * Angular pueda marcar exactamente cual esta mal.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = construir(HttpStatus.BAD_REQUEST, "Datos invalidos",
                "Uno o mas campos no cumplen las validaciones requeridas",
                "validacion", ruta(request));
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().headers(headers).body(problema);
    }

    /**
     * Punto por el que pasan todas las respuestas de error del
     * framework. Aqui se les agregan los mismos campos que a las
     * propias, para que el cliente reciba siempre una forma unica.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        ResponseEntity<Object> respuesta =
                super.handleExceptionInternal(ex, body, headers, statusCode, request);

        if (respuesta != null && respuesta.getBody() instanceof ProblemDetail problema) {
            if (problema.getType() == null
                    || "about:blank".equals(problema.getType().toString())) {
                problema.setType(URI.create(BASE_TIPO + "solicitud-" + statusCode.value()));
            }
            problema.setProperty("marcaTiempo", LocalDateTime.now().toString());
            problema.setProperty("ruta", ruta(request));
        }
        return respuesta;
    }

    // -----------------------------------------------------------------

    private ProblemDetail construir(HttpStatus estado, String titulo, String detalle,
                                    String tipo, String ruta) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create(BASE_TIPO + tipo));
        problema.setProperty("marcaTiempo", LocalDateTime.now().toString());
        problema.setProperty("ruta", ruta);
        return problema;
    }

    private String ruta(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return null;
    }
}
