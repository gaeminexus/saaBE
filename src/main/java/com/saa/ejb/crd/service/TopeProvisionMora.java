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
 * Así Σ tope repartido del préstamo ≤ tope del préstamo.</p>
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
     * Reparte el tope de cada préstamo entre sus cuotas.
     *
     * @param topePorPrestamo mora contabilizada pendiente por préstamo (el que no aparece vale 0)
     * @param cuotas          las cuotas candidatas (con su mora calculada pendiente, 0 si no tienen)
     * @return {@code idCuota → tope repartido} (toda cuota de la lista aparece; las sin tope valen 0)
     */
    public static Map<Long, Double> repartir(Map<Long, Double> topePorPrestamo, List<CuotaTope> cuotas) {
        Map<Long, Double> resultado = new HashMap<>();
        Map<Long, Double> restante = new HashMap<>();
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
        }
        return resultado;
    }
}
