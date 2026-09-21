package com.minierp.backend.auditoria;

import com.minierp.backend.entidad.enums.AccionBitacora;
import com.minierp.backend.entidad.enums.ModuloBitacora;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de servicio cuyo resultado debe quedar en la
 * bitacora.
 *
 * La alternativa era llamar al servicio de bitacora dentro de cada
 * metodo. Con treinta y tantos metodos, tarde o temprano se olvida en
 * alguno, y ese hueco no lo nota nadie hasta que hace falta el
 * registro. Con una anotacion, olvidarla es visible al leer el codigo.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    ModuloBitacora modulo();

    AccionBitacora accion();

    /** Nombre de la tabla afectada, para el reporte de logs. */
    String entidad();

    /** Descripcion breve de la operacion. */
    String descripcion() default "";
}
