package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lote 7b (D-186): el tope de la provisión de mora es POR PRÉSTAMO y se reparte entre sus cuotas. El paso ⑦ y la herramienta del
 * ajuste usan ESTA misma función, para que no puedan divergir.
 *
 * <p>Regla: el tope del préstamo (la mora contabilizada pendiente del préstamo, ver
 * {@code MovimientoInteresCuotaDaoService#selectMoraContabilizadaPendientePorPrestamo}) se reparte entre las cuotas del préstamo que tienen
 * mora calculada pendiente, de la más vieja a la más nueva (vencimiento y código), sin pasar de la mora calculada pendiente de cada una.
 * Lote 23: lo que sobra va a la cuota candidata más reciente (o a la de respaldo), así que Σ tope repartido del préstamo = tope del préstamo.</p>
 */
public final class TopeProvisionMora {

    private TopeProvisionMora() {
    }

    /** Una cuota candidata al reparto. */
    public static final class CuotaTope {
        private final Long idCuota;
        private final Long idPrestamo;
        private final LocalDate vencimiento;
        private final double moraCalculadaPendiente;

        public CuotaTope(Long idCuota, Long idPrestamo, LocalDate vencimiento, double moraCalculadaPendiente) {
            this.idCuota = idCuota;
            this.idPrestamo = idPrestamo;
            this.vencimiento = vencimiento;
            this.moraCalculadaPendiente = moraCalculadaPendiente;
        }

        public Long getIdCuota() { return idCuota; }
        public Long getIdPrestamo() { return idPrestamo; }
        public LocalDate getVencimiento() { return vencimiento; }
        public double getMoraCalculadaPendiente() { return moraCalculadaPendiente; }
    }

    /**
     * Reparte el tope de cada préstamo entre sus cuotas (sin cuota de respaldo ni reporte de lo que no se pueda colocar).
     *
     * @see #repartir(Map, List, Map, Map)
     */
    public static Map<Long, Double> repartir(Map<Long, Double> topePorPrestamo, List<CuotaTope> cuotas) {
        return repartir(topePorPrestamo, cuotas, null, null);
    }

    /**
     * Reparte el tope de cada préstamo entre sus cuotas y coloca SIEMPRE todo el tope (lote 23): primero de la más vieja a la más nueva sin pasar de la
     * mora calculada pendiente de cada una; lo que sobra del préstamo se suma a la cuota candidata MÁS RECIENTE (la última por vencimiento y código), aunque
     * pase de su mora calculada. Si el préstamo no tiene ninguna candidata, va a su cuota de respaldo. Si tampoco hay, queda en {@code sinRepartir}. El tope
     * del préstamo (D-186) no cambia: solo el reparto. Invariante: Σ repartido del préstamo = su tope, al centavo.
     *
     * @param topePorPrestamo          mora contabilizada pendiente por préstamo (el que no aparece vale 0)
     * @param cuotas                   las cuotas candidatas (con su mora calculada pendiente, 0 si no tienen)
     * @param cuotaRespaldoPorPrestamo {@code idPrestamo → idCuota} a la que va el tope de un préstamo sin candidatas (la cuota con saldo de provisión de mora
     *                                 más reciente); puede ser null
     * @param sinRepartir              salida opcional: {@code idPrestamo → sobrante} que no se pudo colocar en ninguna cuota; puede ser null
     * @return {@code idCuota → tope repartido} (toda cuota candidata aparece; las cuotas de respaldo aparecen solo si reciben tope)
     */
    public static Map<Long, Double> repartir(Map<Long, Double> topePorPrestamo, List<CuotaTope> cuotas,
            Map<Long, Long> cuotaRespaldoPorPrestamo, Map<Long, Double> sinRepartir) {
        Map<Long, Double> resultado = new HashMap<>();
        Map<Long, Double> restante = new HashMap<>();
        Map<Long, Long> ultimaPorPrestamo = new HashMap<>();
        List<CuotaTope> ordenadas = new ArrayList<>(cuotas);
        ordenadas.sort(Comparator
            .comparing(CuotaTope::getIdPrestamo, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CuotaTope::getVencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CuotaTope::getIdCuota, Comparator.nullsLast(Comparator.naturalOrder())));
        for (CuotaTope cuota : ordenadas) {
            Long prestamo = cuota.getIdPrestamo();
            double disponible = restante.computeIfAbsent(prestamo, k -> {
                Double tope = topePorPrestamo.get(k);
                return tope != null ? tope.doubleValue() : 0.0;
            });
            double asignado = Math.max(0.0, Math.min(cuota.getMoraCalculadaPendiente(), disponible));
            asignado = Math.round(asignado * 100.0) / 100.0;
            resultado.put(cuota.getIdCuota(), asignado);
            restante.put(prestamo, Math.max(0.0, Math.round((disponible - asignado) * 100.0) / 100.0));
            ultimaPorPrestamo.put(prestamo, cuota.getIdCuota());
        }
        // Lote 23: el sobrante de cada préstamo no se pierde.
        for (Map.Entry<Long, Double> entrada : topePorPrestamo.entrySet()) {
            Long prestamo = entrada.getKey();
            if (prestamo == null || entrada.getValue() == null || entrada.getValue().doubleValue() <= 0.004) {
                continue;
            }
            double sobrante = restante.containsKey(prestamo) ? restante.get(prestamo) : Math.round(entrada.getValue() * 100.0) / 100.0;
            if (sobrante <= 0.004) {
                continue;
            }
            Long destino = ultimaPorPrestamo.get(prestamo);
            if (destino == null && cuotaRespaldoPorPrestamo != null) {
                destino = cuotaRespaldoPorPrestamo.get(prestamo);
            }
            if (destino == null) {
                if (sinRepartir != null) {
                    sinRepartir.merge(prestamo, sobrante, Double::sum);
                }
                continue;
            }
            resultado.merge(destino, sobrante, (a, b) -> Math.round((a + b) * 100.0) / 100.0);
        }
        return resultado;
    }
}
