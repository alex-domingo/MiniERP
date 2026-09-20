package com.minierp.backend.entidad.enums;

/**
 * Razon por la que se produjo un movimiento de inventario.
 * Coincide con la restriccion ck_mov_origen del kardex.
 *
 * La base exige coherencia entre el origen y el documento de respaldo
 * (ck_mov_documento): COMPRA obliga a id_compra, VENTA obliga a
 * id_venta, y los ajustes no llevan ninguno de los dos.
 */
public enum OrigenMovimiento {
    COMPRA,
    VENTA,
    AJUSTE_ENTRADA,
    AJUSTE_SALIDA
}
