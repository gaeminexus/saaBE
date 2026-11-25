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
     * C15 — {@link #selectSaldoProvisionadoPorCuotas(List)} con solo los movimientos de fecha
     * contable {@code <= corte} (el libro tal como estaba al corte). {@code corte == null} = sin filtro.
     */
    List<Object[]> selectSaldoProvisionadoPorCuotas(List<Long> idsCuota, java.time.LocalDate corte) throws Throwable;

    /** C15 / D-017 — anula los MVIC tipo 5 origen COBRO (excedente de mora a ingreso) de esos pagos. */
    int anularDevengoCobroByPagos(List<Long> idsPago) throws Throwable;

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

    /**
     * Lote 15 (D-196 b): el devengo de mora (MVIC 5 / 6 vigentes) de cada cuota CON SU FECHA CONTABLE, para saber cuánto estaba devengado a la fecha
     * de un pago y no cuánto lo está hoy.
     *
     * @param idsCuota códigos de cuota
     * @return mapa {@code idCuota -> lista de {fechaContable (LocalDate), valor con signo (Double: + devengo, - reverso)}}
     */
    java.util.Map<Long, List<Object[]>> selectDevengoMoraDatadoPorCuotas(List<Long> idsCuota) throws Throwable;

    /**
     * Lote 15 (D-196 a): la mora contabilizada pendiente de UN préstamo a una fecha, la misma regla por préstamo del lote 7b (sin piso por cuota):
     * {@code devengo MVIC 5 - 6 (fecha contable <= fecha) - mora cobrada vigente (<= fecha) - mora condonada de los acuerdos APLICADOS con fecha <= fecha
     * (menos el acuerdo excluido)}. Puede ser negativa; el llamador aplica el piso.
     *
     * @param excluirAcuerdo acuerdo cuya condonación no se resta (el que se está contabilizando), o null
     */
    double selectMoraContabilizadaPendienteDelPrestamo(Long idPrestamo, java.time.LocalDate fecha, Long excluirAcuerdo) throws Throwable;

    /**
     * Lote 15 (D-195): la mora de CADA cuota de un préstamo a una fecha: {@code Object[]{idCuota (Long), vencimiento (LocalDate), devengo MVIC 5-6 (Double),
     * mora cobrada vigente (Double), saldo provisionado de mora (Double)}}, todo hasta la fecha, ordenado por vencimiento.
     */
    List<Object[]> selectMoraDelPrestamoPorCuota(Long idPrestamo, java.time.LocalDate fecha) throws Throwable;

    /** Lote 15 (D-195): Σ mora condonada (DACC concepto mora) de los acuerdos APLICADOS del préstamo con fecha <= la dada. */
    double selectMoraCondonadaDelPrestamo(Long idPrestamo, java.time.LocalDate fecha) throws Throwable;

    /** Lote 15 (D-195): ¿hay movimientos vigentes con ese origen e id de origen? */
    boolean existeVigentePorOrigen(String origen, Long idOrigen) throws Throwable;

    /** Lote 15 (D-195): anula (MVICANUL = 1) los movimientos vigentes con ese origen e id de origen; devuelve cuántos. */
    int anularByOrigenYIdOrigen(String origen, Long idOrigen) throws Throwable;

    /**
     * ¿Existe AL MENOS UN {@code MovimientoInteresCuota} (de cualquier tipo, vigente o anulado)
     * enlazado a esta corrida? ÍTEM T (transición del tipo 6): si la respuesta es que NO, la
     * corrida es anterior a que existiera el libro — condición necesaria para reconstruir el
     * devengo con la fórmula pura en vez de asumir "nada que reversar".
     *
     * @param idCorrida código de la corrida (CRD.CRCT)
     * @return {@code true} si hay al menos una fila de MVIC con esa corrida
     * @throws Throwable Si ocurre un error
     */
    boolean existeAlgunoByCorrida(Long idCorrida) throws Throwable;

    // =====================================================================
    // Lote 7 (D-186) — tope de la provisión de mora
    // =====================================================================

    /**
     * Mora contabilizada pendiente POR PRÉSTAMO (lo que hay en 1.4.02 por mora de ese préstamo), como mapa
     * {@code idPrestamo → valor} (los préstamos sin saldo no aparecen: valen 0). Lote 7b: sin piso por cuota, porque en 1.4.02 el
     * negativo de una cuota (mora cobrada o condonada por encima del devengo) rebaja la cuenta y compensa la mora pendiente de las
     * otras cuotas del mismo préstamo:
     * <pre>max(0, Σ cuotas devengo(5 − 6, MORA, fecha contable ≤ corte) − Σ mora cobrada vigente (≤ corte) − mora condonada)</pre>
     * La mora condonada es la de los acuerdos APLICADOS con fecha ≤ corte (DACC, concepto mora, {@code DACCVLCN}) del préstamo. Con
     * {@code corte == null} no hay límite de fecha (modo normal). El reparto del tope entre las cuotas lo hace
     * {@code TopeProvisionMora#repartir}.
     *
     * @param corte fecha de corte, o null
     */
    java.util.Map<Long, Double> selectMoraContabilizadaPendientePorPrestamo(java.time.LocalDate corte) throws Throwable;

    /**
     * Lote 18: los movimientos vigentes (no anulados y no compensados por una re-provisión) de un origen e {@code idOrigen}, de los
     * tipos dados. Sirve para restituir lo que una operación sin pago (la condonación de un acuerdo) bajó de la provisión.
     */
    List<MovimientoInteresCuota> selectVigentesPorOrigen(String origen, Long idOrigen, List<Long> tipos) throws Throwable;

    /**
     * Lote 21: cuotas con saldo de provisión distinto de cero al corte, por componente (interés o mora): Σ tipos 1, 4, 14 − Σ tipos 2, 3, 9, 10, 13,
     * vigentes, con fecha contable ≤ corte, y |saldo| > 0,004.
     *
     * @return filas {@code [idCuota (Long), idPrestamo (Long), componente (Long), saldo (Double)]}
     */
    List<Object[]> selectSaldosProvisionNoCeroAlCorte(java.time.LocalDate corte) throws Throwable;

    /** Lote 21: de {@code idsCuota}, las que tienen un movimiento vigente de alguno de los {@code tipos} con fecha contable ≤ {@code corte}. */
    java.util.Set<Long> selectCuotasConMovimientoHasta(List<Long> idsCuota, List<Long> tipos, java.time.LocalDate corte) throws Throwable;

    /**
     * Lote 18: como {@link #selectMoraContabilizadaPendientePorPrestamo(java.time.LocalDate)} pero sumando al devengo de cada
     * préstamo {@code devengoAdicional} (el ⑩ del mismo cierre, aún sin grabar), ANTES del piso en cero. Es lo que
     * daría la consulta de arriba si ese devengo ya estuviera en el libro. Con el mapa nulo o vacío es la misma consulta.
     */
    java.util.Map<Long, Double> selectMoraContabilizadaPendientePorPrestamo(java.time.LocalDate corte,
            java.util.Map<Long, Double> devengoAdicional) throws Throwable;

    /**
     * Cuotas con saldo de provisión de MORA mayor a un centavo al corte (Σ tipo 1, 4 − Σ tipo 2, 3, 9, 10, vigentes, fecha
     * contable ≤ corte).
     *
     * @return filas {@code [idCuota (Long), idPrestamo (Long), saldoMora (Double)]}
     */
    List<Object[]> selectCuotasConProvisionMora(java.time.LocalDate corte) throws Throwable;

    /** Movimientos VIGENTES de un tipo ligados a un asiento ({@code ASNTCDGO}). */
    List<MovimientoInteresCuota> selectVigentesPorAsientoYTipo(Long idAsiento, long tipoMovimiento) throws Throwable;

    /** Anula (MVICANUL = 1, nunca borra) los movimientos vigentes de un tipo ligados a un asiento. @return cuántos */
    int anularByAsientoYTipo(Long idAsiento, long tipoMovimiento) throws Throwable;
}
