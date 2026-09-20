package com.minierp.backend.entidad.enums;

/**
 * Acciones registrables en la bitacora.
 * Coincide con la restriccion ck_bitacora_acc.
 */
public enum AccionBitacora {
    LOGIN,
    LOGOUT,
    LOGIN_FALLIDO,
    CREAR,
    ACTUALIZAR,
    DESACTIVAR,
    ACTIVAR,
    CONSULTAR,
    EXPORTAR,
    ANULAR
}
