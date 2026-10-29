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
 * <li>Pagado al corte: pagos vigentes con {@code PGPRFCHA <= corte}; capital pagado de la cuota =
 *     {@code PGPRSLOT} si es mayor que cero, si no {@code PGPRCPPG} — salvo el abono a capital
 *     ({@code PGPRTPOO = ABONO_CAPITAL}), que solo descuenta {@code PGPRCPPG}: ver
 *     {@link #CAPITAL_PAGADO_CUOTA_SQL} (C14b).</li>
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

    /**
     * C14b (2026-10-29) — capital que un pago vigente ({@code CRD.PGPR}, columnas sin alias) descuenta
     * de SU cuota al corte. Regla de la cuadratura C14 ({@code PGPRSLOT} si &gt; 0, si no
     * {@code PGPRCPPG}: los pagos migrados DEP/MIGRACION traen el capital en "otros"), con una
     * excepción: el pago de <b>abono a capital</b> ({@code PGPRTPOO = ABONO_CAPITAL}). Su
     * {@code PGPRSLOT} no es pago de la cuota donde quedó registrado: es una reducción del saldo
     * por vencer del préstamo que la tabla ya refleja — re-amortizada (abono de siempre) o sin
     * regenerar (C16b, {@code Σ DTPRCPTL + Σ DTPRSLOT = monto}). Contarlo en la cuota la daba por
     * pagada (préstamo 8052, cuota 42: capital 891,80, abono 1.293,24 del 13-jun-2025, quedaban sin
     * vencido los 445,72 impagos). Del abono la cuota solo descuenta {@code PGPRCPPG} (0 en el motor).
     * La contabilidad no cambia: el asiento del abono sigue bandeando {@code PGPRSLOT} contra capital
     * ({@code ContabilizacionIndividualCreditoService#haberDesdePagos}).
     */
    public static final String CAPITAL_PAGADO_CUOTA_SQL =
            " CASE WHEN PGPRTPOO = '" + ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL + "' THEN NVL(PGPRCPPG,0)"
            + " WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END ";

    /**
     * D-032 (29-oct-2026): propiedad que activa el neteo ⑥ y el complemento de apertura de cuotas
     * POR PRÉSTAMO: {@code siempre} | {@code nunca} | {@code auto} (defecto = cuando el mes se procesa
     * al corte, que es donde la apertura por préstamo se puede reproducir exacta).
     */
    public static final String PROPIEDAD_NETEO_POR_PRESTAMO = "saa.cartera.neteoPorPrestamo";

    private ModoCorteCartera() {
    }

    /**
     * D-032 — ¿el mes que termina en {@code finMes} usa el neteo y el complemento por préstamo?
     *
     * <p>{@code auto}: sí cuando el mes es "al corte" ({@code alCorte}, o si viene null
     * {@link #esAlCorte(LocalDate, Boolean)} con la regla de la propiedad). La apertura por préstamo
     * se reconstruye con la lectura al corte del mes anterior, que es la que usa el ③ de una corrida al
     * corte; el ③ de una corrida normal (estado de hoy) no se puede reconstruir después, por eso fuera
     * del modo al corte se mantiene el D-024 por cuenta salvo que se fuerce {@code siempre}.</p>
     */
    public static boolean neteoPorPrestamo(LocalDate finMes, Boolean alCorte) {
        String modo = System.getProperty(PROPIEDAD_NETEO_POR_PRESTAMO, "auto").trim().toLowerCase();
        if ("siempre".equals(modo)) {
            return true;
        }
        if ("nunca".equals(modo)) {
            return false;
        }
        return alCorte != null ? alCorte.booleanValue() : esAlCorte(finMes, null);
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
