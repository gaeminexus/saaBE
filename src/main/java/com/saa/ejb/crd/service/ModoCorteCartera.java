package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.saa.rubros.TipoCarteraBanda;

/**
 * C15 (cuadre Superintendencia, 2026-10-28) — modo "a fecha de corte" de la cartera.
 *
 * <p>El cierre de cartera, la provisión de intereses, la G48 y el CCPM leían el estado de HOY
 * ({@code PRSTIDST}, {@code DTPRESTD}, todos los {@code CRD.PGPR} vigentes, todo el libro
 * {@code CRD.MVIC}). Para re-procesar un mes pasado (jun-2025 en adelante) con la operación de
 * meses posteriores ya cargada, esos procesos tienen que ver la cartera COMO ESTABA al corte
 * (hueco H1 de C14). Este helper decide cuándo se usa ese modo y con qué convención de
 * vencimiento, en UN solo lugar, para que el cierre, la G48 y el CCPM no puedan divergir.</p>
 *
 * <h3>Cuándo es "al corte"</h3>
 * <ul>
 * <li>Propiedad de sistema {@code saa.cartera.alCorte}: {@code siempre} | {@code nunca} |
 *     {@code auto} (defecto).</li>
 * <li>{@code auto}: al corte si la fecha de corte es ANTERIOR al último día del mes anterior a
 *     hoy (un mes ya cerrado y re-procesado). El mes que se cierra normalmente (el anterior al
 *     de hoy) y el mes en curso siguen con el comportamiento de siempre, intacto.</li>
 * <li>El cierre de cartera acepta además {@code estadoAlCorte} explícito en la solicitud.</li>
 * </ul>
 *
 * <h3>Regla de corte (la de C3 §2.1 y la cuadratura C14)</h3>
 * <ul>
 * <li>Préstamo en el universo: {@code PRSTFCHA <= corte}, no anulado (12) ni nunca aprobado
 *     (1 generado, 6 pendiente de aprobación, 7 rechazado).</li>
 * <li>Cuota pagada al corte: {@code DTPRESTD IN (4,7) AND DTPRFCPG <= corte}.</li>
 * <li>Pagado al corte: pagos vigentes con {@code PGPRFCHA <= corte}; capital pagado =
 *     {@code PGPRSLOT} si es mayor que cero, si no {@code PGPRCPPG}.</li>
 * <li>Mora al corte: fórmula del proceso diario ({@code calcularMoraCuota}): capital de la cuota
 *     × tasa nominal (9 si falta) / 36000 × días desde el vencimiento, menos la mora pagada al
 *     corte. No lee {@code DTPRMRAA}.</li>
 * <li>Libro {@code CRD.MVIC}: solo movimientos con fecha contable {@code <= corte}.</li>
 * </ul>
 *
 * <h3>Convención de vencimiento ({@code saa.cartera.corteInclusivo}, defecto {@code true})</h3>
 * <p>Todas las cuotas vencen el último día del mes. Con {@code true} (convención de la G48 de
 * Delta aceptada en mayo 2025 y de la cuadratura C14): una cuota con vencimiento {@code <=}
 * corte e impaga es VENCIDA con {@code corte - vto + 1} días; por vencer con
 * {@code vto - corte} días. Con {@code false}: la regla del SAA del 2026-09-04
 * ({@code ContabilizacionIndividualCreditoService#tipoCarteraYDias}: el día del vencimiento aún es
 * POR VENCER). Solo aplica en modo al corte; el modo normal usa siempre la regla del SAA.</p>
 */
public final class ModoCorteCartera {

    /** Propiedad de sistema que fuerza el modo: siempre | nunca | auto. */
    public static final String PROPIEDAD_MODO = "saa.cartera.alCorte";

    /** Propiedad de sistema de la convención de vencimiento en modo al corte. */
    public static final String PROPIEDAD_CORTE_INCLUSIVO = "saa.cartera.corteInclusivo";

    /** Préstamos que nunca formaron cartera: raíz, generado, pendiente, rechazado, anulado. */
    public static final String ESTADOS_FUERA_DE_CARTERA = "0, 1, 6, 7, 12";

    private ModoCorteCartera() {
    }

    /**
     * Decide si una fecha de corte se procesa "al corte".
     *
     * @param corte    : Último día del mes que se procesa
     * @param explicito: Valor pedido por quien llama; null = regla de la propiedad / automática
     * @return         : true si hay que leer la cartera como estaba al corte
     */
    public static boolean esAlCorte(LocalDate corte, Boolean explicito) {
        if (explicito != null) {
            return explicito.booleanValue();
        }
        String modo = System.getProperty(PROPIEDAD_MODO, "auto").trim().toLowerCase();
        if ("siempre".equals(modo)) {
            return true;
        }
        if ("nunca".equals(modo)) {
            return false;
        }
        if (corte == null) {
            return false;
        }
        LocalDate primeroMesActual = LocalDate.now().withDayOfMonth(1);
        LocalDate finMesAnterior = primeroMesActual.minusDays(1);
        return corte.isBefore(finMesAnterior);
    }

    /**
     * Convención de vencimiento en modo al corte.
     *
     * @param explicito : Valor pedido por quien llama; null = propiedad (defecto true)
     * @return          : true si la cuota que vence el día del corte ya cuenta como vencida
     */
    public static boolean corteInclusivo(Boolean explicito) {
        if (explicito != null) {
            return explicito.booleanValue();
        }
        return !"false".equalsIgnoreCase(System.getProperty(PROPIEDAD_CORTE_INCLUSIVO, "true").trim());
    }

    /**
     * Clasificación de una cuota impaga al corte con la convención inclusiva.
     *
     * @param vencimiento : Fecha de vencimiento de la cuota
     * @param corte       : Fecha de corte
     * @return            : {tipoCartera ({@link TipoCarteraBanda}), días}
     */
    public static long[] tipoYDiasInclusivo(LocalDate vencimiento, LocalDate corte) {
        if (!vencimiento.isAfter(corte)) {
            return new long[]{TipoCarteraBanda.VENCIDO, ChronoUnit.DAYS.between(vencimiento, corte) + 1};
        }
        return new long[]{TipoCarteraBanda.POR_VENCER, ChronoUnit.DAYS.between(corte, vencimiento)};
    }

    /** Día siguiente al corte, para filtros {@code fecha < corte + 1} sobre columnas con hora. */
    public static LocalDate diaSiguiente(LocalDate corte) {
        return corte.plusDays(1);
    }
}
