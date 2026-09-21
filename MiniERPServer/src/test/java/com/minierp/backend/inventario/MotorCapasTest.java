package com.minierp.backend.inventario;

import com.minierp.backend.entidad.CapaInventario;
import com.minierp.backend.entidad.enums.MetodoValuacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del algoritmo de consumo de capas, el nucleo del metodo UEPS.
 *
 * No levantan Spring ni tocan la base: el motor es logica pura, asi que
 * se prueba en milisegundos y de forma aislada.
 *
 * El primer escenario reproduce capas REALES de la carga inicial (las
 * del producto SIL-005), para que la prueba verifique el mismo calculo
 * que se valido contra la base de datos.
 */
class MotorCapasTest {

    private static CapaInventario capa(long id, String fecha, String costo, int cantidad) {
        CapaInventario c = new CapaInventario();
        c.setIdCapa(id);
        c.setFechaEntrada(LocalDateTime.parse(fecha + "T10:00"));
        c.setCostoUnitario(new BigDecimal(costo));
        c.setCantidadInicial(cantidad);
        c.setCantidadDisponible(cantidad);
        return c;
    }

    /** Capas con saldo de SIL-005 en la carga inicial. */
    private static List<CapaInventario> capasSil005() {
        return List.of(
                capa(21, "2026-01-26", "361.88", 14),
                capa(31, "2026-03-01", "387.22", 11),
                capa(62, "2026-06-28", "377.10", 11),
                capa(64, "2026-07-15", "361.13", 8));
    }

    @Test
    @DisplayName("UEPS consume primero la capa que entro mas recientemente")
    void uepsTomaLaCapaMasReciente() {
        var resultado = MotorCapas.consumir(capasSil005(), 4, MetodoValuacion.UEPS);

        assertEquals(1, resultado.tomas().size());
        assertEquals(64L, resultado.tomas().get(0).capa().getIdCapa());
        assertEquals(0, new BigDecimal("1444.52").compareTo(resultado.costoTotal()));
    }

    @Test
    @DisplayName("Al agotar una capa continua con la siguiente mas reciente")
    void agotaUnaCapaYContinua() {
        List<CapaInventario> capas = capasSil005();

        var resultado = MotorCapas.consumir(capas, 10, MetodoValuacion.UEPS);

        assertEquals(2, resultado.tomas().size());
        assertEquals(64L, resultado.tomas().get(0).capa().getIdCapa());
        assertEquals(8, resultado.tomas().get(0).cantidad());
        assertEquals(62L, resultado.tomas().get(1).capa().getIdCapa());
        assertEquals(2, resultado.tomas().get(1).cantidad());
        assertEquals(0, capas.get(3).getCantidadDisponible(), "la capa 64 debe quedar agotada");
        assertEquals(14, capas.get(0).getCantidadDisponible(), "la capa mas antigua no se toca");
    }

    @Test
    @DisplayName("El costo de la salida es la suma exacta de las porciones tomadas")
    void costoEsLaSumaDeLasPorciones() {
        var resultado = MotorCapas.consumir(capasSil005(), 10, MetodoValuacion.UEPS);

        BigDecimal esperado = new BigDecimal("361.13").multiply(BigDecimal.valueOf(8))
                .add(new BigDecimal("377.10").multiply(BigDecimal.valueOf(2)));
        assertEquals(0, esperado.compareTo(resultado.costoTotal()));
        // (2889.04 + 754.20) / 10 = 364.324
        assertEquals(0, new BigDecimal("364.3240").compareTo(resultado.costoUnitarioPromedio()));
    }

    @Test
    @DisplayName("Se niega a consumir mas de lo que hay en las capas")
    void rechazaConsumirMasDeLoDisponible() {
        List<CapaInventario> capas = List.of(capa(1, "2026-01-01", "10.00", 3));

        assertThrows(IllegalStateException.class,
                () -> MotorCapas.consumir(capas, 5, MetodoValuacion.UEPS));
    }

    @Test
    @DisplayName("Rechaza cantidades no positivas")
    void rechazaCantidadNoPositiva() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorCapas.consumir(capasSil005(), 0, MetodoValuacion.UEPS));
    }

    @Test
    @DisplayName("PEPS, el caso par del enunciado, invierte el orden")
    void pepsTomaLaCapaMasAntigua() {
        var resultado = MotorCapas.consumir(capasSil005(), 3, MetodoValuacion.PEPS);

        assertEquals(21L, resultado.tomas().get(0).capa().getIdCapa());
    }

    @Test
    @DisplayName("Con la misma fecha de entrada, desempata por identificador")
    void desempatePorIdentificador() {
        List<CapaInventario> capas = List.of(
                capa(7, "2026-05-05", "10.00", 5),
                capa(9, "2026-05-05", "20.00", 5));

        var resultado = MotorCapas.consumir(capas, 1, MetodoValuacion.UEPS);

        assertEquals(9L, resultado.tomas().get(0).capa().getIdCapa());
    }

    @Test
    @DisplayName("Ignora las capas agotadas aunque sean las mas recientes")
    void ignoraCapasAgotadas() {
        CapaInventario agotada = capa(99, "2026-09-01", "999.00", 5);
        agotada.setCantidadDisponible(0);
        List<CapaInventario> capas = List.of(capa(1, "2026-01-01", "10.00", 5), agotada);

        var resultado = MotorCapas.consumir(capas, 2, MetodoValuacion.UEPS);

        assertEquals(1L, resultado.tomas().get(0).capa().getIdCapa());
        assertEquals(0, new BigDecimal("20.00").compareTo(resultado.costoTotal()));
    }
}
