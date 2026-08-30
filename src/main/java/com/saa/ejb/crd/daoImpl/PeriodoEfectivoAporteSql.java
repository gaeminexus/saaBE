package com.saa.ejb.crd.daoImpl;

/**
 * Fragmento SQL compartido del "periodo efectivo" de un aporte (D3 del plan de devengo de
 * aportes, corregido el 2026-08-27). ÚNICA fuente de esta expresión: cualquier consulta que
 * necesite comparar o agrupar aportes por mes debe usar esta constante, nunca redefinirla.
 *
 * <p>NO es {@code NVL(APRTPRDV, TRUNC(APRTFCTR,'MM'))} a secas — eso está mal para los
 * movimientos NEGATIVOS: una devolución sin devengo (retiro de saldo, D5) no pertenece a
 * ningún mes, y caer en su mes de caja lo dejaría "incompleto" y se volvería a cobrar. Un
 * aporte POSITIVO sin devengo (dato histórico sin backfillear) sí pertenece a algún mes: el
 * de su fecha de caja.</p>
 *
 * <p>Pensada para SQL nativo (usa {@code TRUNC}, que no es JPQL estándar). El alias de tabla
 * esperado en la consulta que la use es {@code a} sobre {@code CRD.APRT}.</p>
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
final class PeriodoEfectivoAporteSql {

    static final String PERIODO_EFECTIVO_SQL =
        " (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV " +
        "       WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM') " +
        "       ELSE NULL END) ";

    private PeriodoEfectivoAporteSql() {
    }
}
