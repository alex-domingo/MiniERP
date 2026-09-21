package com.minierp.backend.inventario;

import com.minierp.backend.entidad.CapaInventario;
import com.minierp.backend.entidad.enums.MetodoValuacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Algoritmo de consumo de capas de costo. Es el corazon del metodo de
 * valuacion del enunciado: (impar -> UEPS, par -> PEPS).
 *
 * Se mantiene como logica pura, sin Spring ni base de datos, para poder
 * probarlo de forma aislada. El servicio de inventario se encarga de lo
 * demas: bloquear, persistir y escribir el kardex.
 *
 *   UEPS : se consume primero la capa que ENTRO MAS RECIENTEMENTE.
 *   PEPS : se consume primero la capa que ENTRO PRIMERO.
 *
 * Si una capa no alcanza, se agota y se continua con la siguiente en el
 * mismo orden. El costo de la salida es el promedio ponderado de las
 * porciones tomadas de cada capa.
 */
public final class MotorCapas {

    private MotorCapas() {
    }

    /** Porcion tomada de una capa. */
    public record Toma(CapaInventario capa, int cantidad, BigDecimal costoUnitario) {
        public BigDecimal costo() {
            return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    /** Resultado de consumir una cantidad. */
    public record Resultado(List<Toma> tomas, BigDecimal costoTotal, BigDecimal costoUnitarioPromedio) {
    }

    /**
     * Ordena las capas segun el metodo. El desempate por id es
     * necesario: dos capas pueden tener la misma fecha de entrada (las
     * lineas de una misma compra), y sin desempate el orden no seria
     * determinista.
     */
    public static List<CapaInventario> ordenar(List<CapaInventario> capas, MetodoValuacion metodo) {
        Comparator<CapaInventario> antiguedad = Comparator
                .comparing(CapaInventario::getFechaEntrada)
                .thenComparing(CapaInventario::getIdCapa,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        List<CapaInventario> ordenadas = new ArrayList<>(capas);
        ordenadas.sort(metodo == MetodoValuacion.UEPS ? antiguedad.reversed() : antiguedad);
        return ordenadas;
    }

    /**
     * Consume la cantidad pedida de las capas, en el orden del metodo,
     * y descuenta cantidadDisponible en cada capa tocada.
     *
     * @throws IllegalArgumentException si la cantidad no es positiva
     * @throws IllegalStateException    si las capas no alcanzan. Nunca
     *         deberia ocurrir: el servicio valida existencias antes de
     *         llamar aqui. Si ocurre, el inventario esta inconsistente
     *         (stock_actual no coincide con las capas) y lo correcto es
     *         detenerse, no vender a un costo inventado.
     */
    public static Resultado consumir(List<CapaInventario> capas, int cantidad, MetodoValuacion metodo) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a consumir debe ser positiva");
        }

        List<Toma> tomas = new ArrayList<>();
        BigDecimal costoTotal = BigDecimal.ZERO;
        int pendiente = cantidad;

        for (CapaInventario capa : ordenar(capas, metodo)) {
            if (pendiente == 0) {
                break;
            }
            if (capa.getCantidadDisponible() <= 0) {
                continue;
            }
            int tomadas = capa.consumir(pendiente);
            pendiente -= tomadas;
            Toma toma = new Toma(capa, tomadas, capa.getCostoUnitario());
            tomas.add(toma);
            costoTotal = costoTotal.add(toma.costo());
        }

        if (pendiente > 0) {
            throw new IllegalStateException("Inventario inconsistente: las capas de costo no cubren "
                    + cantidad + " unidades (faltan " + pendiente + ")");
        }

        BigDecimal promedio = costoTotal.divide(BigDecimal.valueOf(cantidad), 4, RoundingMode.HALF_UP);
        return new Resultado(List.copyOf(tomas), costoTotal, promedio);
    }
}
