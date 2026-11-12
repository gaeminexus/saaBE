package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.saa.rubros.EstadoCuotaPrestamo;
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
 *     (1 generado, 6 pendiente de aprobación, 7 rechazado), y no cancelado antes del corte
 *     ({@link #CANCELADO_TERMINADO_AL_CORTE_SQL}, C23).</li>
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
    public static final String ESTADOS_FUERA_DE_CARTERA = "0, 1, 6, 7, 12, 14";

    /**
     * Préstamos cancelados: normal, anticipado, por novación, por revisar y por reestructuración
     * (3, 4, 5, 9, 13).
     */
    public static final String ESTADOS_CANCELADOS = "3, 4, 5, 9, 13";

    /**
     * C23 (2026-11-05, regla del líder): un préstamo cancelado cuenta en la cartera al corte hasta
     * la fecha de su última cuota PAGADA, incluido ese día; desde el día siguiente ya no existe,
     * aunque sus cuotas canceladas anticipadas no tengan fecha de pago (así quedan la novación, la
     * migración y el cambio directo de estado). Sin esta regla el universo al corte los revivía con
     * todo su capital: el cierre de octubre 2026 en producción los llevó a más de 360 días.
     *
     * <p>Solo sale si TODAS sus demás cuotas están pagadas, canceladas anticipadas o anuladas. Si
     * le queda alguna en mora, parcial o pendiente, es un dato incoherente de la migración: sigue
     * contando como antes (así estaban en la G48 de mayo 2025 aceptada las operaciones 68353, 68783
     * y 61578) y el cierre lo avisa ({@link #CANCELADO_CON_CUOTAS_PENDIENTES_SQL}) para que se
     * depure con su documento.</p>
     *
     * <p><b>C27-C (2026-11-11):</b> tampoco sale si tiene algún pago vigente ({@code CRD.PGPR}) con
     * fecha posterior al corte. Caso de la operación 70882: el acuerdo de condonación del 26-sep-2025
     * dejó sus últimas cuotas canceladas anticipadas sin fecha de pago y el préstamo cancelado; con la
     * regla de C23 sola quedaba terminado al 31-may-2025 (su última cuota PAGADA) y salía del universo
     * de junio a agosto 2025, cuando la G48 y la contabilidad de esos meses lo tienen vivo. En la base
     * del 9-nov-2026 los únicos pagos que activan esta condición son acuerdos de condonación.</p>
     *
     * <p>SQL nativo: alias {@code p} = {@code CRD.PRST}, parámetro {@code :corteSig} = corte + 1 día.</p>
     */
    public static final String CANCELADO_TERMINADO_AL_CORTE_SQL =
            " (NVL(p.PRSTIDST,0) IN (" + ESTADOS_CANCELADOS + ")"
            + " AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO"
            + "      AND NVL(cx.DTPRESTD,0) NOT IN (" + EstadoCuotaPrestamo.PAGADA + ", "
            + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ", " + EstadoCuotaPrestamo.ANULADA + "))"
            + " AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO"
            + "      AND cy.DTPRESTD = " + EstadoCuotaPrestamo.PAGADA + " AND cy.DTPRFCPG >= :corteSig - 1)"
            // C27-C: un pago vigente posterior al corte (acuerdo de condonación, precancelación, …)
            // prueba que el préstamo seguía vivo al corte, aunque su última cuota PAGADA sea anterior.
            + " AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO"
            + "      AND NVL(cz.PGPRANUL,0) = 0 AND cz.PGPRFCHA >= :corteSig)) ";

    /**
     * C23: préstamo cancelado que sigue en el universo al corte porque le quedan cuotas en mora,
     * parciales o pendientes (ver {@link #CANCELADO_TERMINADO_AL_CORTE_SQL}). Mismo alias.
     */
    public static final String CANCELADO_CON_CUOTAS_PENDIENTES_SQL =
            " (NVL(p.PRSTIDST,0) IN (" + ESTADOS_CANCELADOS + ")"
            + " AND EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO"
            + "      AND NVL(cx.DTPRESTD,0) NOT IN (" + EstadoCuotaPrestamo.PAGADA + ", "
            + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ", " + EstadoCuotaPrestamo.ANULADA + "))) ";

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
     * C27-B (2026-11-11): propiedad con la fecha (AAAA-MM-DD, EXCLUSIVA) hasta la que los abonos a
     * capital posteriores al corte se suman de vuelta al capital pendiente al corte. Defecto
     * {@link #LIMITE_ABONOS_POSTERIORES_DEFECTO}. Ver {@link #abonosPosterioresSql()}.
     */
    public static final String PROPIEDAD_LIMITE_ABONOS_POSTERIORES = "saa.cartera.abonosPosterioresHasta";

    /**
     * Defecto de {@link #PROPIEDAD_LIMITE_ABONOS_POSTERIORES}: 1-ago-2026, desde cuando toda la
     * operación corre en el SAA. Los abonos a capital que registró la operación del SAA desde esa
     * fecha (9 en la base del 9-nov-2026, 54.628,76) ya estaban descontados en la G48 de mayo 2025
     * aceptada (el ancla) y en las G48 y balances de jun–sep 2025 cerrados: sumarlos de vuelta rompería
     * esos cierres. Los abonos con fecha anterior (los que se cargan al re-procesar 2025-2026 desde los
     * soportes del cliente) sí son posteriores reales al corte.
     */
    public static final LocalDate LIMITE_ABONOS_POSTERIORES_DEFECTO = LocalDate.of(2026, 8, 1);

    /** Fecha límite (exclusiva) de los abonos posteriores que se suman de vuelta (C27-B). */
    public static LocalDate limiteAbonosPosteriores() {
        String valor = System.getProperty(PROPIEDAD_LIMITE_ABONOS_POSTERIORES, "").trim();
        if (valor.isEmpty()) {
            return LIMITE_ABONOS_POSTERIORES_DEFECTO;
        }
        return LocalDate.parse(valor);
    }

    /**
     * C27-B (2026-11-11) — ajuste de capital por los abonos a capital POSTERIORES al corte, como filas
     * {@code (DTPRCDGO, cap)} para unirlas ({@code UNION ALL}) a los pagos al corte de cada cuota: así
     * {@code GREATEST(DTPRCPTL - cap, 0)} devuelve el capital que cada cuota debía al corte sin tocar
     * ninguna otra consulta. Un {@code cap} negativo SUMA capital.
     *
     * <p>Por qué: la tabla de hoy ya refleja el abono (re-amortizada, o sin regenerar en C16b, donde
     * {@code Σ DTPRCPTL + Σ DTPRSLOT = monto}). Al corte anterior al abono ese capital todavía se
     * debía. Casos de la base del 9-nov-2026: operación 66940 (11.300,00 del 29-sep-2025) y 70473
     * (32.170,90 del 31-jul-2025) faltaban en el cálculo de jun–ago 2025 frente a su G48 y su balance.</p>
     *
     * <p>Cómo: el abono guarda la tabla de antes en {@code CRD.HDTP} (motivo ABONO_CAPITAL, mismo
     * {@code EVPRCDGO} que el pago). Para el PRIMER abono posterior al corte de cada préstamo:</p>
     * <ol>
     * <li>cada cuota viva al corte que existe en esa tabla anterior (mismo {@code DTPRNMCT}) toma su
     *     capital de antes: {@code cap = DTPRCPTL de hoy - DTPRCPTL de antes};</li>
     * <li>las cuotas de la tabla anterior que ya no existen (el abono acortó el plazo) van a la última
     *     cuota viva del préstamo (la cola);</li>
     * <li>si el abono no guardó tabla anterior (abonos sin {@code HDTP}), el {@code PGPRSLOT} de todos
     *     los abonos posteriores va a la cola (el total es exacto; la banda, aproximada).</li>
     * </ol>
     * <p>Con la tabla anterior las bandas también se reproducen: jun–sep 2025, cada cuenta 1.3 (menos
     * 1.3.99) al centavo con el balance. Las cuotas vivas al corte no pueden estar pagadas antes del
     * corte, así que la tabla guardada (posterior al corte) las contiene a todas.</p>
     *
     * <p>Solo abonos con fecha anterior a {@link #limiteAbonosPosteriores()} (ver su defecto).
     * Parámetro {@code :corteSig}; la fecha límite va como literal (viene de un {@link LocalDate}).</p>
     */
    public static String abonosPosterioresSql() {
        String limite = "DATE '" + limiteAbonosPosteriores() + "'";
        String viva = " v.DTPRFCVN IS NOT NULL AND NVL(v.DTPRESTD,0) <> " + EstadoCuotaPrestamo.ANULADA
                + " AND NOT (NVL(v.DTPRESTD,0) IN (" + EstadoCuotaPrestamo.PAGADA + ", "
                + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + ") AND NVL(v.DTPRFCPG, :corteSig) < :corteSig) ";
        // Abonos posteriores al corte por préstamo y el evento del primero.
        String ab = " (SELECT PRSTCDGO, SUM(PGPRSLOT) SLOT,"
                + "         MIN(EVPRCDGO) KEEP (DENSE_RANK FIRST ORDER BY PGPRFCHA, PGPRCDGO) EV"
                + "  FROM CRD.PGPR WHERE PGPRTPOO = '" + ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL + "'"
                + "  AND NVL(PGPRANUL,0) = 0 AND NVL(PGPRSLOT,0) > 0"
                + "  AND PGPRFCHA >= :corteSig AND PGPRFCHA < " + limite
                + "  GROUP BY PRSTCDGO) ";
        // Tabla anterior al primer abono posterior (CRD.HDTP), por número de cuota.
        String sn = " (SELECT h.PRSTCDGO, h.DTPRNMCT, SUM(NVL(h.DTPRCPTL,0)) CPTL FROM CRD.HDTP h"
                + "  JOIN " + ab + " a1 ON a1.EV = h.EVPRCDGO AND a1.PRSTCDGO = h.PRSTCDGO"
                + "  GROUP BY h.PRSTCDGO, h.DTPRNMCT) ";
        // Última cuota viva al corte de los préstamos con abono posterior.
        String cola = " (SELECT v.PRSTCDGO,"
                + "         MAX(v.DTPRCDGO) KEEP (DENSE_RANK LAST ORDER BY TRUNC(v.DTPRFCVN), v.DTPRCDGO) DTPRCDGO"
                + "  FROM CRD.DTPR v WHERE " + viva
                + "  AND v.PRSTCDGO IN (SELECT a2.PRSTCDGO FROM " + ab + " a2)"
                + "  GROUP BY v.PRSTCDGO) ";
        return " SELECT v.DTPRCDGO, NVL(v.DTPRCPTL,0) - s.CPTL cap"
                + " FROM CRD.DTPR v JOIN " + sn + " s ON s.PRSTCDGO = v.PRSTCDGO AND s.DTPRNMCT = v.DTPRNMCT"
                + " WHERE " + viva
                + " UNION ALL"
                + " SELECT c.DTPRCDGO, -SUM(s.CPTL) FROM " + sn + " s JOIN " + cola + " c ON c.PRSTCDGO = s.PRSTCDGO"
                + " WHERE NOT EXISTS (SELECT 1 FROM CRD.DTPR v WHERE v.PRSTCDGO = s.PRSTCDGO"
                + "                   AND v.DTPRNMCT = s.DTPRNMCT AND " + viva + ")"
                + " GROUP BY c.DTPRCDGO"
                + " UNION ALL"
                + " SELECT c.DTPRCDGO, -a3.SLOT FROM " + ab + " a3 JOIN " + cola + " c ON c.PRSTCDGO = a3.PRSTCDGO"
                + " WHERE NOT EXISTS (SELECT 1 FROM CRD.HDTP h WHERE h.EVPRCDGO = a3.EV AND h.PRSTCDGO = a3.PRSTCDGO) ";
    }

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
