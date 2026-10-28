package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.dto.OperacionCarteraCorte;

import jakarta.ejb.Local;

/**
 * C15 (H1 + D-015) — la cartera POR OPERACIÓN a una fecha de corte, con calificación y
 * provisión requerida por la regla del SAA (la del generador G48: {@code VSAP = max(0, saldo −
 * cuenta individual)}, la cuenta individual solo en la primera operación del partícipe y 0 si es
 * jubilado complementario; {@code PRRO = VSAP × %} de la escala {@code CRD.CFCR/ESCR} vigente al
 * corte).
 *
 * <p>Es el ÚNICO cálculo de la provisión de cartera: la G48 al corte, el CCPM al corte y el paso
 * ⑧ del cierre (asiento de {@code 1.3.99}) lo usan, así que no pueden diferir (D-011/D-015).</p>
 *
 * <p><b>Provisión constituida = requerida.</b> El paso ⑧ ajusta {@code 1.3.99} a la requerida
 * con un asiento fechado al corte; desde ese asiento lo constituido al corte ES lo requerido.
 * Así la G48 reporta en {@code PRCN} lo que de verdad está en el balance (B17 = G48).</p>
 */
@Local
public interface CarteraAlCorteService {

    /**
     * @param corte          : Fecha de corte
     * @param corteInclusivo : Convención de vencimiento (ver {@link ModoCorteCartera#corteInclusivo})
     * @return               : Una fila por operación con saldo al corte, ordenadas por préstamo
     */
    List<OperacionCarteraCorte> calcular(LocalDate corte, boolean corteInclusivo) throws Throwable;

    /** Igual que {@link #calcular}, indexado por préstamo. */
    Map<Long, OperacionCarteraCorte> calcularPorPrestamo(LocalDate corte, boolean corteInclusivo) throws Throwable;
}
