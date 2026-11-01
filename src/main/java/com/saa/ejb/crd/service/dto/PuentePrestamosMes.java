package com.saa.ejb.crd.service.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * D-032 (29-oct-2026, decisión del líder) — la cuenta por cobrar de cuotas del período
 * ({@code 1.4.05.10}) vista préstamo por préstamo en un mes M.
 *
 * <ul>
 * <li>{@code abierto(i)} = lo que la apertura ③ abrió del préstamo i para M: cuotas con vencimiento
 *     hasta fin de M pendientes al corte del mes anterior (capital, interés, mora bruta a ese corte,
 *     desgravamen, seguro) — {@code CierreCarteraDaoService#selectAperturaPorPrestamo}.</li>
 * <li>{@code cobrado(i)} = lo que los cobros de M acreditaron en la cuenta por el préstamo i, antes
 *     del complemento — {@code CierreCarteraDaoService#selectCobradoPuentePorPrestamo}.</li>
 * <li>{@code noAtribuible} = lo que los repartos Petro de M acreditaron y no es pago de un préstamo
 *     (excedentes D-010: descontado a préstamos que se aplicó a aportes).</li>
 * <li>{@code complementoRegistrado} = débito de los complementos de apertura ya grabados en M.</li>
 * </ul>
 *
 * <p>Reglas: complemento objetivo del mes = Σ max(cobrado − abierto, 0) + noAtribuible (lo cobrado que
 * la apertura no abrió: cuotas que vencen después de M cobradas adelantadas, préstamos nuevos de M,
 * mora devengada en M y pagada); neteo ⑥ = Σ max(abierto − cobrado, 0) (lo abierto que no se cobró, sin
 * la mora devengada en M, que el ⑩ lleva a 1.4.02). Con el complemento registrado igual al objetivo,
 * saldo de la cuenta antes del ⑥ = Σ abierto + objetivo − Σ cobrado − noAtribuible = neteo.</p>
 */
public class PuentePrestamosMes {

    private final Map<Long, Double> abierto = new HashMap<>();
    private final Map<Long, Double> cobrado = new HashMap<>();
    private double noAtribuible;
    private double complementoRegistrado;

    public Map<Long, Double> getAbierto() { return abierto; }
    public Map<Long, Double> getCobrado() { return cobrado; }
    public double getNoAtribuible() { return noAtribuible; }
    public void setNoAtribuible(double noAtribuible) { this.noAtribuible = noAtribuible; }
    public double getComplementoRegistrado() { return complementoRegistrado; }
    public void setComplementoRegistrado(double complementoRegistrado) { this.complementoRegistrado = complementoRegistrado; }

    /** Suma lo que un cobro todavía sin grabar acredita por préstamo (para medir su complemento). */
    public void sumaCobrado(Map<Long, Double> delCobro) {
        if (delCobro == null) {
            return;
        }
        for (Map.Entry<Long, Double> e : delCobro.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                cobrado.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
    }

    public double totalAbierto() { return redondea(suma(abierto)); }

    public double totalCobrado() { return redondea(suma(cobrado)); }

    /** Σ max(cobrado − abierto, 0): lo cobrado por préstamo que la apertura no abrió. */
    public double cobradoSinAbrir() {
        double total = 0.0;
        for (Long id : prestamos()) {
            total += Math.max(valor(cobrado, id) - valor(abierto, id), 0.0);
        }
        return redondea(total);
    }

    /** Complemento objetivo acumulado del mes: lo cobrado sin abrir + lo no atribuible. */
    public double complementoObjetivo() {
        return redondea(cobradoSinAbrir() + Math.max(noAtribuible, 0.0));
    }

    /** Neteo ⑥: Σ max(abierto − cobrado, 0). */
    public double neteo() {
        double total = 0.0;
        for (Long id : prestamos()) {
            total += Math.max(valor(abierto, id) - valor(cobrado, id), 0.0);
        }
        return redondea(total);
    }

    /** Préstamos con algo abierto sin cobrar (los que netea el ⑥). */
    public int prestamosConNeteo() {
        int n = 0;
        for (Long id : prestamos()) {
            if (valor(abierto, id) - valor(cobrado, id) >= 0.005) {
                n++;
            }
        }
        return n;
    }

    /** Préstamos con algo cobrado sin abrir (los que lleva el complemento). */
    public int prestamosConComplemento() {
        int n = 0;
        for (Long id : prestamos()) {
            if (valor(cobrado, id) - valor(abierto, id) >= 0.005) {
                n++;
            }
        }
        return n;
    }

    private Set<Long> prestamos() {
        Set<Long> ids = new HashSet<>(abierto.keySet());
        ids.addAll(cobrado.keySet());
        return ids;
    }

    private static double valor(Map<Long, Double> mapa, Long id) {
        Double v = mapa.get(id);
        return v != null ? v.doubleValue() : 0.0;
    }

    private static double suma(Map<Long, Double> mapa) {
        double total = 0.0;
        for (Double v : mapa.values()) {
            total += v != null ? v.doubleValue() : 0.0;
        }
        return total;
    }

    private static double redondea(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
