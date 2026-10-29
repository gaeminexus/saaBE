package com.saa.ejb.crd.daoImpl;

/**
 * Fragmento compartido de "aporte vigente" sobre {@code CRD.APRT} (C15, 2026-10-28). ÚNICA
 * fuente de la regla: toda lectura de aportes que filtre por signo, por tipo de movimiento o
 * que cuente filas/meses tiene que usar esta constante, nunca redefinirla.
 *
 * <p><b>Qué excluye.</b> Las anulaciones del reproceso de carga Petro y de la corrida de ancla
 * ({@code ReprocesoCargaPetroServiceImpl}, el ÚNICO productor) dejan un par por cada aporte
 * anulado:</p>
 * <ul>
 *   <li>el original, con su valor intacto y marcado {@code APRTIDST = 0} ("reversado por
 *       reproceso");</li>
 *   <li>su contra-movimiento: {@code APRTTPMV = 5} (REVERSO), valor opuesto, misma entidad,
 *       tipo, fecha de caja y periodo efectivo, glosa {@code "... - reverso APRT <id>"} e
 *       {@code APRTIDST = 4}.</li>
 * </ul>
 * <p>El par suma cero, así que una suma NETA sin filtros (saldo del partícipe, G42, CPRM,
 * cuenta individual) ya da lo correcto. Pero cualquier lectura que filtre por signo
 * ({@code APRTVLRR > 0}), por tipo de movimiento ({@code APRTTPMV IN (1, 8)}) o que cuente
 * meses rompe el par: cuenta el original y no su contra (o al revés). Medido en junio 2025: el
 * cierre contaba 322.509,73 "registrados" en vez de 161.802,91 — el esquema viejo de la carga
 * 352 (160.706,82) seguía sumando.</p>
 *
 * <p><b>Por qué excluir los dos y no "netear".</b> Excluir el marcado Y su contra da, en una
 * suma neta, exactamente lo mismo que netearlos (el par suma cero, verificado: 3.331 pares, 0
 * marcados sin contra, 0 contras sin marcado). Y además funciona fila a fila, que es lo que
 * necesitan las lecturas con filtro de signo/tipo y los conteos de meses (G43/G44, padrón):
 * netear exigiría reconstruir el par en cada consulta. Las otras 13 filas tipo 5 (reversos de
 * eventos, devoluciones anuladas, cobros) NO llevan la marca y siguen contando.</p>
 *
 * <p>La contra se reconoce por la glosa porque {@code CRD.APRT} no tiene columna que apunte al
 * original; el SELECT de control de {@code CIERRE-CARTERA-A-FECHA-DE-CORTE.md} §4 verifica que
 * la marca y la glosa sigan emparejadas.</p>
 *
 * @since 2026-10-28
 */
final class AporteVigenteSql {

    /** Marca de la glosa del contra-movimiento de una anulación (ReprocesoCargaPetroServiceImpl). */
    static final String MARCA_GLOSA_CONTRA = "reverso APRT ";

    /**
     * Condición SQL nativa (alias {@code a} sobre {@code CRD.APRT}): el aporte cuenta. Va
     * entre paréntesis, lista para {@code AND}.
     */
    static final String VIGENTE_SQL =
        " (NVL(a.APRTIDST, 4) <> 0 AND NOT (NVL(a.APRTTPMV, 0) = 5 "
        + " AND NVL(a.APRTGLSA, ' ') LIKE '%" + MARCA_GLOSA_CONTRA + "%')) ";

    /** La misma condición en JPQL (alias {@code a} sobre {@code Aporte}). */
    static final String VIGENTE_JPQL =
        " ((a.estado is null or a.estado <> 0) and not (coalesce(a.tipoMovimiento, 0) = 5 "
        + " and coalesce(a.glosa, ' ') like '%" + MARCA_GLOSA_CONTRA + "%')) ";

    private AporteVigenteSql() {
    }
}
