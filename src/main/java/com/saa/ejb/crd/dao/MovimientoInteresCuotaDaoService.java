package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.MovimientoInteresCuota;

import jakarta.ejb.Local;

/**
 * DAO de {@code CRD.MVIC} — el libro de movimientos de intereses y clasificación de capital por
 * cuota. {@code docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md} §3.
 */
@Local
public interface MovimientoInteresCuotaDaoService extends EntityDao<MovimientoInteresCuota> {

    /**
     * Saldo provisionado de VARIAS cuotas, por componente, EN LOTE — nunca una consulta por
     * cuota. Fórmula completa (diseño §3, extendida a la condonación de 7bis/R2, APROBADA por
     * el árbitro 2026-10-05): para cada {@code (idCuota, componente)},
     * <pre>Σ tipo 1 (PROVISION) + Σ tipo 4 (RE_PROVISION)
     * − Σ tipo 2 (REVERSO_POR_COBRO) − Σ tipo 3 (REVERSO_POR_COBRO_TARDIO) − Σ tipo 9 (REVERSO_POR_CONDONACION)</pre>
     * — el texto original del diseño (anterior a 7bis) solo nombraba el 2 y el 3; el 9 reduce lo
     * provisionado exactamente igual, se agregó con 7bis/R2. Solo cuenta lo VIGENTE
     * ({@code MVICANUL = 0}). Nunca el componente CAPITAL (3): esa es la clasificación de banda
     * (tipos 7/8), no una provisión que se acumule o se reverse de la misma manera.
     *
     * @param idsCuota códigos de cuota (CRD.DTPR) a consultar
     * @return filas {@code Object[]{Long idCuota, Long componente, Double saldo}}; vacía si
     *         {@code idsCuota} es nulo o vacío
     * @throws Throwable Si ocurre un error
     */
    List<Object[]> selectSaldoProvisionadoPorCuotas(List<Long> idsCuota) throws Throwable;

    /**
     * Anula (marca {@code MVICANUL = 1}, nunca borra) los movimientos VIGENTES de tipo 1
     * (PROVISION), 5 (DEVENGO_MORA) y 7 (CLASIFICACION_CAPITAL) de una corrida del cierre de
     * cartera — {@code CierreCarteraServiceImpl.reversar}, contrato
     * {@code API-FECHA-AFECTACION-COBRO.md} §4 (que prevalece sobre el "tipo 1 y 5" del diseño
     * original: el contrato es el vigente, aprobado por el árbitro el 2026-10-05).
     *
     * @param idCorrida código de la corrida (CRD.CRCT)
     * @param usuario   quién reversa (para auditoría; MVIC no tiene su propia columna de
     *                  usuario de anulación — queda solo en el registro de auditoría de la corrida)
     * @return cuántas filas se anularon
     * @throws Throwable Si ocurre un error
     */
    int anularByCorrida(Long idCorrida, String usuario) throws Throwable;

    /**
     * Movimientos VIGENTES ({@code MVICANUL = 0}) de alguno de los {@code tipos} dados,
     * originados en alguno de los pagos {@code idsPago} — para {@code ProvisionInteresService}
     * ubicar, al anular un cobro, qué reversos (tipo 2) hay que compensar con una re-provisión
     * (tipo 4), diseño §5 ("re-provisión al deshacer").
     *
     * <p><b>Idempotente (pedido del árbitro 2026-10-05):</b> excluye un tipo 2 que YA tiene un
     * tipo 4 vigente apuntándolo como {@code movimientoReversado} — {@code not exists (select 1
     * from MovimientoInteresCuota r where r.movimientoReversado = m and r.anulado = 0)}. Sin
     * esto, un reintento de {@code reProvisionarPorAnulacion} sobre los mismos pagos
     * re-provisionaría dos veces lo mismo.</p>
     *
     * @param idsPago códigos de {@code CRD.PGPR} (el cobro que se está anulando)
     * @param tipos   tipos de {@code MovimientoInteresCuota} a buscar (p.ej. solo el 2)
     * @return movimientos encontrados, entidad completa (el llamador necesita
     *         {@code idCuota}, {@code componente}, {@code valor} y el propio código para
     *         enlazar el tipo 4 como reverso de este). Vacía si no hay pagos, o si ya están
     *         todos compensados
     * @throws Throwable Si ocurre un error
     */
    List<MovimientoInteresCuota> selectVigentesPorPagosYTipos(List<Long> idsPago, List<Long> tipos)
            throws Throwable;

    /**
     * Saldo DEVENGADO de mora de varias cuotas, EN LOTE — mismo patrón que
     * {@link #selectSaldoProvisionadoPorCuotas} pero para el par tipo 5 (DEVENGO_MORA) / tipo 6
     * (REVERSO_DEVENGO_POR_COBRO_TARDIO), ÍTEM 5 (§6.2/§7bis). Solo existe para MORA: el tipo 5
     * nunca escribe componente INTERÉS (paso ④ solo devenga mora, el interés ordinario se
     * devenga en el asiento de intereses por cobrar, no en MVIC).
     *
     * <pre>Σ tipo 5 (DEVENGO_MORA) − Σ tipo 6 (REVERSO_DEVENGO_POR_COBRO_TARDIO)</pre>
     *
     * — solo filas con {@code MVICANUL = 0}.
     *
     * @param idsCuota códigos de cuota (CRD.DTPR) a consultar
     * @return mapa {@code idCuota -> saldoDevengadoMora}; una cuota sin ningún movimiento no
     *         aparece (el llamador trata "ausente" como 0.0). Vacío si {@code idsCuota} es nulo
     *         o vacío
     * @throws Throwable Si ocurre un error
     */
    java.util.Map<Long, Double> selectSaldoDevengadoPorCuotas(List<Long> idsCuota) throws Throwable;
}
