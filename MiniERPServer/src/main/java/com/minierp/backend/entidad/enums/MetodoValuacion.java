package com.minierp.backend.entidad.enums;

/**
 * Metodo de valuacion de inventario.
 *
 * Este proyecto opera bajo UEPS, segun el enunciado. El motor de
 * inventario consulta este valor para decidir el orden en que consume
 * las capas de costo: UEPS toma primero la capa mas reciente, PEPS la
 * mas antigua. Es el unico punto del sistema donde cambia el criterio.
 */
public enum MetodoValuacion {
    UEPS,
    PEPS
}
