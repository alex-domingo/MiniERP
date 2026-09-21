package com.minierp.backend.factura;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Casos del espanol que una conversion ingenua escribe mal. */
class NumeroALetrasTest {

    private static String q(String monto) {
        return NumeroALetras.quetzales(new BigDecimal(monto));
    }

    @Test
    void totalDeUnaFacturaReal() {
        assertEquals("TRES MIL OCHOCIENTOS SESENTA Y CINCO QUETZALES CON 12/100", q("3865.12"));
    }

    @Test
    void singularYCero() {
        assertEquals("UN QUETZAL CON 50/100", q("1.50"));
        assertEquals("CERO QUETZALES CON 99/100", q("0.99"));
    }

    @Test
    void apocopeDelanteDelSustantivo() {
        assertEquals("VEINTIÚN QUETZALES CON 00/100", q("21"));
        assertEquals("CUARENTA Y UN QUETZALES CON 00/100", q("41"));
        assertEquals("CIENTO UN QUETZALES CON 00/100", q("101"));
    }

    @Test
    void apocopeDelanteDeMil() {
        assertEquals("VEINTIÚN MIL QUETZALES CON 00/100", q("21000"));
        assertEquals("CIENTO UN MIL QUINIENTOS QUETZALES CON 00/100", q("101500"));
        assertEquals("MIL UN QUETZALES CON 00/100", q("1001"));
    }

    @Test
    void cienSoloCuandoVaSolo() {
        assertEquals("CIEN QUETZALES CON 00/100", q("100"));
        assertEquals("CIEN MIL QUETZALES CON 00/100", q("100000"));
        assertEquals("CIENTO VEINTE QUETZALES CON 00/100", q("120"));
    }

    @Test
    void millones() {
        assertEquals("UN MILLÓN DE QUETZALES CON 00/100", q("1000000"));
        assertEquals("UN MILLÓN DOSCIENTOS MIL QUETZALES CON 00/100", q("1200000"));
        assertEquals("VEINTIÚN MILLONES DE QUETZALES CON 00/100", q("21000000"));
        assertEquals("MIL QUINIENTOS MILLONES DE QUETZALES CON 00/100", q("1500000000"));
    }

    @Test
    void acentosDeLosVeintes() {
        assertEquals("VEINTIDÓS QUETZALES CON 00/100", q("22"));
        assertEquals("DIECISÉIS QUETZALES CON 00/100", q("16"));
    }

    @Test
    void redondeaCentavos() {
        assertEquals("DIEZ QUETZALES CON 01/100", q("10.005"));
    }

    @Test
    void rechazaNegativos() {
        assertThrows(IllegalArgumentException.class, () -> q("-1"));
    }
}
