package com.minierp.backend.auditoria;

import com.minierp.backend.servicio.ServicioBitacora;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;

/**
 * Escribe en la bitacora cada metodo anotado con @Auditable, sin que la
 * logica de negocio tenga que saber que la auditoria existe.
 *
 * ORDEN RESPECTO DE LA TRANSACCION
 *
 * Spring envuelve el servicio en dos capas: la transaccional y esta. Si
 * no se fija el orden, ambas tienen la misma precedencia y Spring las
 * aplica en un orden no garantizado. Por eso se declara @Order(0):
 * el administrador de transacciones usa la precedencia mas baja
 * posible, asi que este aspecto queda SIEMPRE por fuera de el.
 *
 * Consecuencia: @AfterReturning se ejecuta despues de que la
 * transaccion del negocio ya se confirmo. Si el commit falla, el metodo
 * lanza una excepcion, este consejo no corre y no se registra nada.
 * La bitacora nunca afirma que ocurrio algo que no se guardo.
 */
@Aspect
@Component
@Order(0)
public class AspectoAuditoria {

    private static final Logger log = LoggerFactory.getLogger(AspectoAuditoria.class);

    private final ServicioBitacora servicioBitacora;

    public AspectoAuditoria(ServicioBitacora servicioBitacora) {
        this.servicioBitacora = servicioBitacora;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "resultado")
    public void auditar(JoinPoint punto, Auditable auditable, Object resultado) {
        try {
            String descripcion = auditable.descripcion().isBlank()
                    ? auditable.accion() + " sobre " + auditable.entidad()
                    : auditable.descripcion();

            Long id = extraerId(resultado);
            if (id == null) {
                id = primerIdentificador(punto.getArgs());
            }

            // "Registro de un nuevo producto" dice poco en un reporte;
            // "Registro de un nuevo producto: SIL-006 - Silla Ergonomica"
            // dice exactamente que paso.
            String etiqueta = extraerEtiqueta(resultado);
            if (etiqueta != null) {
                descripcion = descripcion + ": " + etiqueta;
            }

            servicioBitacora.registrar(auditable.modulo(), auditable.accion(),
                    auditable.entidad(), id, descripcion);

        } catch (RuntimeException ex) {
            // Auditar nunca debe tumbar una operacion que ya se confirmo.
            log.error("Fallo el aspecto de auditoria en {}: {}",
                    punto.getSignature().toShortString(), ex.getMessage(), ex);
        }
    }

    /**
     * Toma el identificador del DTO devuelto: el primer componente del
     * record cuyo nombre empiece con "id" y sea numerico. Por convencion
     * todos los DTO de respuesta declaran su propio id en primer lugar.
     */
    private Long extraerId(Object resultado) {
        if (resultado == null || !resultado.getClass().isRecord()) {
            return null;
        }
        try {
            for (RecordComponent componente : resultado.getClass().getRecordComponents()) {
                if (componente.getName().startsWith("id")
                        && Number.class.isAssignableFrom(envolver(componente.getType()))) {
                    Object valor = componente.getAccessor().invoke(resultado);
                    return valor == null ? null : ((Number) valor).longValue();
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            log.debug("No se pudo extraer el id del resultado: {}", ex.getMessage());
        }
        return null;
    }

    /** Componentes que identifican a un recurso ante una persona, en orden. */
    private static final String[] CAMPOS_ETIQUETA =
            {"codigo", "codigoProducto", "nit", "nombre", "nombreProducto"};

    /**
     * Arma una etiqueta legible con el codigo o NIT y el nombre del
     * recurso, si el DTO devuelto los tiene.
     */
    private String extraerEtiqueta(Object resultado) {
        if (resultado == null || !resultado.getClass().isRecord()) {
            return null;
        }
        java.util.List<String> partes = new java.util.ArrayList<>();
        try {
            for (String campo : CAMPOS_ETIQUETA) {
                for (RecordComponent componente : resultado.getClass().getRecordComponents()) {
                    if (componente.getName().equals(campo)) {
                        Object valor = componente.getAccessor().invoke(resultado);
                        if (valor != null && !partes.contains(valor.toString()) && partes.size() < 2) {
                            partes.add(valor.toString());
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            log.debug("No se pudo armar la etiqueta: {}", ex.getMessage());
        }
        return partes.isEmpty() ? null : String.join(" - ", partes);
    }

    /**
     * Respaldo para metodos que no devuelven un DTO: el primer argumento
     * numerico suele ser el id del recurso (desactivar(Long id), etc.).
     */
    private Long primerIdentificador(Object[] argumentos) {
        if (argumentos == null) {
            return null;
        }
        for (Object argumento : argumentos) {
            if (argumento instanceof Number numero) {
                return numero.longValue();
            }
        }
        return null;
    }

    private Class<?> envolver(Class<?> tipo) {
        if (tipo == long.class) {
            return Long.class;
        }
        if (tipo == int.class) {
            return Integer.class;
        }
        if (tipo == short.class) {
            return Short.class;
        }
        return tipo;
    }
}
