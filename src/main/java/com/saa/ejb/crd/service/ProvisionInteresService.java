package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.dto.ResultadoReversoProvision;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;

/**
 * Provisión de intereses (ordinario y mora) no cobrados — el libro {@code CRD.MVIC}, el paso ⑦
 * del cierre de cartera y el reverso de la provisión en cada canal de cobro.
 * {@code docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md}.
 *
 * Se va completando por ítems del frente: hoy solo el saldo provisionado en lote (ÍTEM 2). El
 * paso ⑦ del cierre, el reverso por cobro y el cobro tardío se agregan en los ítems siguientes.
 */
@Local
public interface ProvisionInteresService {

    /** 409 - Falta la plantilla contable alterno 36 (provisión de intereses). */
    String ERR_PLANTILLA_PROVISION_NO_CONFIGURADA = "PLANTILLA_PROVISION_NO_CONFIGURADA";

    /**
     * Saldo provisionado de VARIAS cuotas, por componente, EN LOTE — envoltorio de
     * {@code MovimientoInteresCuotaDaoService#selectSaldoProvisionadoPorCuotas} que arma un mapa
     * por cuota, listo para que el llamador no tenga que interpretar el {@code Object[]} ni el
     * número de componente.
     *
     * @param idsCuota códigos de cuota (CRD.DTPR) a consultar
     * @return mapa {@code idCuota -> double[2]{saldoInteres, saldoMora}}; una cuota sin ningún
     *         movimiento no aparece en el mapa (el llamador trata "ausente" igual que "0.0 en
     *         los dos"). Vacío si {@code idsCuota} es nulo o vacío
     * @throws Throwable Si ocurre un error
     */
    Map<Long, double[]> saldoProvisionadoPorCuotas(List<Long> idsCuota) throws Throwable;

    /**
     * Paso ⑦ del cierre — SOLO calcula, no escribe nada (ni MVIC ni asiento). Universo
     * {@code PRSTIDST IN (2, 8, 11)} (P8), cuotas no PAGADAS/CANCELADAS ANTICIPADAMENTE con
     * vencimiento ≤ {@code fechaCorte} (diseño §4):
     * <ul>
     *   <li>pendiente de interés = {@code interés + interésVencido − (interésPagado + IVPagado)},
     *       de PGPR vigente;</li>
     *   <li>pendiente de mora = {@code calcularMoraCuota(cuota, tasa, fechaCorte) − moraPagada}
     *       (P9: la fórmula pura a la fecha de corte, NUNCA la persistida), piso en 0;</li>
     *   <li>a provisionar = {@code max(0, pendiente − saldo ya provisionado)} por componente
     *       (P2: una sola vez).</li>
     * </ul>
     *
     * @param fechaCorte fecha de corte del cierre (fin del mes que se cierra)
     * @return mapa {@code idTipoPrestamo (CRD.TPPR) -> double[2]{aProvisionarInteres, aProvisionarMora}}
     * @throws Throwable Si ocurre un error
     */
    Map<Long, double[]> calcularProvisionPorTipoPrestamo(LocalDate fechaCorte) throws Throwable;

    /**
     * Recalcula el paso ⑦ (misma fórmula que {@link #calcularProvisionPorTipoPrestamo}, nunca
     * confía en un cálculo previo) y REGISTRA un {@code MVIC} tipo 1 (PROVISION) por cada cuota
     * y componente con "a provisionar" {@code > 0}, enlazado a la corrida y al asiento. Se llama
     * DESPUÉS de generar el asiento del paso ⑦ — necesita el código de la corrida, que todavía
     * no existe cuando se calculan los totales de la línea (contrato
     * {@code API-FECHA-AFECTACION-COBRO.md} §4).
     *
     * @param idCorrida  corrida del cierre (CRD.CRCT) que ya se grabó
     * @param idAsiento  asiento que acaba de generar el paso ⑦ — este método solo se llama
     *                   cuando la contabilidad de CRD está activa (igual que los otros seis
     *                   sub-procesos: con la contabilidad apagada, el paso 7 se omite entero,
     *                   sin asiento y sin MVIC — sería provisionar sin ningún rastro contable
     *                   que reversar después)
     * @param fechaCorte misma fecha de corte que armó la línea del asiento
     * @param usuario    quién ejecuta el cierre
     * @throws Throwable Si ocurre un error
     */
    void registrarProvisionCierre(Long idCorrida, Long idAsiento, LocalDate fechaCorte, String usuario)
            throws Throwable;

    /**
     * Sub-proceso ④ — devengo de mora por cuota. Registra un {@code MVIC} tipo 5
     * (DEVENGO_MORA), componente MORA, por cada cuota de
     * {@code CierreCarteraDaoService#selectCuotasDevengoMoraEnRango(desde, hasta)} — el MISMO
     * universo (2, 11), rango y fuente ({@code DTPRMRAA} persistida) que ya usa la línea
     * agregada del asiento, nunca recalculado (a diferencia del paso ⑦, acá no aplica P9).
     *
     * <p><b>Invariante verificada, no exigida:</b> Σ MVIC tipo 5 por tipo de préstamo tiene que
     * coincidir, al centavo, con {@code totalMoraPorTipoAsiento} (lo que la línea del asiento
     * ya asentó). Si no coincide, el asiento MANDA — no se ajusta ningún MVIC para forzar el
     * cuadre, se deja constancia en el log con la diferencia exacta para que el árbitro la
     * revise, y el cierre sigue (no se aborta por esto).</p>
     *
     * @param idCorrida             corrida del cierre (CRD.CRCT) que ya se grabó
     * @param idAsiento             asiento que acaba de generar el paso ④
     * @param desde                 {@code fechaProceso} — primer día del mes que se abre
     * @param hasta                 {@code fechaCorteApertura} — último día del mes que se abre
     * @param totalMoraPorTipoAsiento mapa {@code idTipoPrestamo -> moraDevengada} que ya usó la
     *                              línea del asiento, para verificar la invariante
     * @param usuario               quién ejecuta el cierre
     * @throws Throwable Si ocurre un error
     */
    void registrarDevengoMoraCierre(Long idCorrida, Long idAsiento, LocalDate desde, LocalDate hasta,
            Map<Long, Double> totalMoraPorTipoAsiento, String usuario) throws Throwable;

    /**
     * ÍTEM 4, diseño §5 — reverso de la provisión al COBRAR, para los cuatro canales (CBCR,
     * Petro, cruce/jubilados, precancelación directa): por cada cuota que estos {@code pagos}
     * tocan, agregado en los pagos de ESTA MISMA operación (no el histórico de la cuota),
     * <pre>interés reversado = min(interés + interésVencido pagados en esta operación, saldo provisionado de interés)
     * mora reversada     = min(mora pagada en esta operación, saldo provisionado de mora)</pre>
     * (nunca más de lo que de verdad está provisionado — una cuota sin provisión previa no
     * genera ningún MVIC). Registra un {@code MVIC} tipo 2 (REVERSO_POR_COBRO) por cuota y
     * componente con reverso {@code > 0}, enlazado al pago que lo originó, y UN asiento
     * {@code D 149905 / H 470510} (plantilla alterno 36, papeles 81/80 — misma cuenta que la
     * provisión, lado invertido), una línea por tipo de préstamo presente entre los pagos.
     *
     * <p>No es la condonación (tipo 9, §7bis/R2): la condonación resta lo CONDONADO, esto resta
     * lo PAGADO. El llamador de la condonación usa {@code reversarPorPagos} SOLO si el acuerdo
     * tuvo una parte pagada (orden fijado por el árbitro: tipo 2 antes que tipo 9).</p>
     *
     * @param pagos     pagos de ESTA operación (un cobro, una aplicación Petro, un cruce, una
     *                  precancelación) — ya guardados, con {@code codigo}, {@code prestamo} y
     *                  {@code detallePrestamo} cargados
     * @param idEmpresa empresa contable del asiento
     * @param fecha     fecha contable del asiento (para CBCR, la fecha de afectación del
     *                  cobro — NUNCA la fecha de pago real)
     * @param origen    literal corto para {@code MVIC.origen} (p.ej. {@code "CBCR"}, {@code
     *                  "PETRO"}, {@code "CRUCE"}, {@code "PRECANCELACION"})
     * @param idOrigen  código del registro de origen (p.ej. el código del {@code CobroCredito})
     * @param usuario   quién ejecuta el cobro
     * @return {@code idAsiento} generado y el total reversado; {@code idAsiento == null} si no
     *         había nada provisionado que reversar (no se genera asiento vacío)
     * @throws Throwable Si ocurre un error
     */
    ResultadoReversoProvision reversarPorPagos(List<PagoPrestamo> pagos, Long idEmpresa, LocalDate fecha,
            String origen, Long idOrigen, String usuario) throws Throwable;

    /**
     * ÍTEM 4, diseño §5 — re-provisión al DESHACER un cobro ({@code anularOperacion}, una sola
     * vez, diseño 0c). Por cada {@code MVIC} tipo 2 VIGENTE que algún pago de {@code pagosAnulados}
     * originó ({@code MovimientoInteresCuotaDaoService#selectVigentesPorPagosYTipos}), registra
     * un {@code MVIC} tipo 4 (RE_PROVISION) por el MISMO valor, componente y cuota, enlazado
     * como {@code movimientoReversado} del tipo 2 que compensa, y UN asiento espejo
     * {@code D 470510 / H 149905} (misma plantilla, lado de la provisión original) por el total.
     *
     * <p>Petro sin deshacer: la carga Petro no construye este camino (diseño 0c) — solo lo usan
     * los canales que sí pueden anular (CBCR, cruce, precancelación).</p>
     *
     * @param pagosAnulados pagos que se están anulando en esta misma operación
     * @param idEmpresa     empresa contable del asiento
     * @param fecha         fecha contable del asiento (la de la anulación, no la del cobro original)
     * @param origen        literal corto para {@code MVIC.origen} (p.ej. {@code "REVERSO_CBCR"})
     * @param idOrigen      código del registro que ordena la anulación
     * @param usuario       quién ejecuta la anulación
     * @return {@code idAsiento} generado y el total re-provisionado; {@code idAsiento == null}
     *         si ninguno de estos pagos tenía un tipo 2 vigente que compensar
     * @throws Throwable Si ocurre un error
     */
    ResultadoReversoProvision reProvisionarPorAnulacion(List<PagoPrestamo> pagosAnulados, Long idEmpresa,
            LocalDate fecha, String origen, Long idOrigen, String usuario) throws Throwable;

    /**
     * 7bis/R2 — condonación: la parte CONDONADA (nunca cobrada, nunca pasó por un
     * {@code PagoPrestamo} — K9 de {@code AcuerdoCondonacionServiceImpl}) también reduce lo
     * provisionado, con su propio {@code MVIC} tipo 9 (REVERSO_POR_CONDONACION) y su propio
     * asiento {@code D 149905 / H 470510} (misma plantilla y lado que {@link #reversarPorPagos},
     * distinto de {@code GASTO_CONDONACION_PRESTAMOS} — ese ya castigó la cuenta por cobrar; este
     * solo da de baja la provisión que quedaría huérfana).
     *
     * <p>Orden fijado por el árbitro (2026-10-05): el llamador invoca ESTO después de
     * {@link #reversarPorPagos} para la parte pagada del mismo acuerdo (si hubo), nunca antes —
     * para que la condonación consuma lo que de verdad QUEDA provisionado.</p>
     *
     * <p>Distribución: {@code cuotas} en el orden que da el llamador (antigüedad, igual que el
     * capital condonado en {@code generarAsientoCondonacion} — incluido a propósito, para que la
     * cuota más provisionada se libere primero), consumiendo el saldo provisionado de cada una
     * hasta agotar {@code interesCondonado}/{@code moraCondonada} o las cuotas, lo que pase
     * primero. <b>Que la suma de lo escrito no alcance el condonado es ACEPTABLE</b> (aprobado
     * por el árbitro): una cuota puede estar condonada sin haber tenido provisión — no es un
     * error, no se fuerza a cuadrar.</p>
     *
     * @param cuotas          cuotas del préstamo del acuerdo, en el orden de consumo
     * @param interesCondonado total de interés ORDINARIO condonado (sin mora)
     * @param moraCondonada    total de MORA condonada
     * @param idEmpresa       empresa contable del asiento
     * @param fecha           fecha contable (la del acuerdo, {@code acuerdo.getFecha()} —
     *                        NUNCA {@code LocalDate.now()}, regla §3 de condonación)
     * @param origen          literal corto para {@code MVIC.origen} (p.ej. {@code "CONDONACION"})
     * @param idOrigen        código del acuerdo
     * @param usuario         quién aplica el acuerdo
     * @return {@code idAsiento} generado y el total condonado que SÍ se pudo escribir;
     *         {@code idAsiento == null} si no había nada provisionado que condonar
     * @throws Throwable Si ocurre un error
     */
    ResultadoReversoProvision condonarProvision(List<DetallePrestamo> cuotas, double interesCondonado,
            double moraCondonada, Long idEmpresa, LocalDate fecha, String origen, Long idOrigen, String usuario)
            throws Throwable;

    /**
     * ÍTEM 5, §6.2/§7bis — cobro tardío: exceso de mora PROVISIONADA. El saldo provisionado de
     * cada cuota tiene que quedar en lo que de verdad se debe a la fecha efectiva de pago, ni un
     * centavo menos — no en el "exceso" bruto de la recalculación.
     *
     * <p><b>Fórmula (corregida 2026-10-05, el árbitro encontró el defecto de la primera
     * versión):</b> por cuota, {@code pendiente = max(0, moraNueva − moraPagada ANTES de este
     * cobro)}; {@code aReversar = max(0, saldoProvisionadoMora − pendiente)}. {@code moraPagada}
     * es la que ya está en {@code CRD.PGPR} vigente ANTES de que este cobro aplique su propio
     * pago (el motor corre después de este método) — el tipo 2 de este mismo cobro reversa,
     * aparte, lo que SÍ se cobre de ese {@code pendiente}: nunca los dos tocan el mismo dólar.
     * La primera versión reversaba {@code min(moraAnterior − moraNueva, saldoProvisionado)}, que
     * ignoraba que {@code moraNueva} puede seguir siendo mora legítima todavía por cobrar — eso
     * reversaba de más.</p>
     *
     * <p>No es el tipo 5/6 (devengo e inverso del devengo, §6.2): ese es un hecho DISTINTO,
     * sobre cuentas de ingreso/por cobrar, no sobre la provisión — se resuelve aparte.</p>
     *
     * @param idPrestamo   préstamo al que pertenecen las cuotas de {@code detalleMora}
     * @param detalleMora  filas de {@code recalcularMoraALaFechaDePagoDetalle} para ESE préstamo
     *                     ({@code [idCuota, moraAnterior, moraNueva]})
     * @param idEmpresa    empresa contable del asiento
     * @param fecha        fecha contable (fecha de afectación del cobro)
     * @param origen       literal corto para {@code MVIC.origen} (p.ej. {@code "CBCR_TARDIO"})
     * @param idOrigen     código del cobro
     * @param usuario      quién ejecuta el cobro
     * @return {@code idAsiento} generado y el total reversado; {@code idAsiento == null} si
     *         ninguna cuota tenía saldo provisionado que reversar
     * @throws Throwable Si ocurre un error
     */
    ResultadoReversoProvision reversarExcesoProvisionPorCobroTardio(Long idPrestamo, List<Object[]> detalleMora,
            Long idEmpresa, LocalDate fecha, String origen, Long idOrigen, String usuario) throws Throwable;

    /**
     * ÍTEM 5, §6.2/§7bis — cobro tardío: exceso de mora DEVENGADA (distinto del exceso
     * PROVISIONADO de {@link #reversarExcesoProvisionPorCobroTardio}, otra cuenta, otro hecho
     * económico). El devengo (MVIC tipo 5, paso ④) reconoció ingreso por una mora que, a la
     * fecha efectiva de pago, resultó menor — el exceso de ingreso ya reconocido se reversa con
     * un {@code MVIC} tipo 6 (REVERSO_DEVENGO_POR_COBRO_TARDIO), componente MORA.
     *
     * <p><b>Fórmula (corregida 2026-10-05, el árbitro encontró el defecto de la primera
     * versión):</b> por cuota, {@code aReversar = max(0, devengado − moraNueva)}, con
     * {@code devengado} = saldo del libro ({@code MovimientoInteresCuotaDaoService
     * #selectSaldoDevengadoPorCuotas}, tipo 5 − tipo 6) o el tope de transición. <b>La mora
     * PAGADA no se resta acá</b>: el devengo es ingreso GANADO, esté cobrado o no — lo único que
     * lo reversa es que la mora recalculada a la fecha de pago haya quedado por DEBAJO de lo
     * devengado. La primera versión reversaba {@code min(moraAnterior − moraNueva, devengado)},
     * que podía reversar TODO el devengo aunque la mora recalculada siguiera siendo mayor que lo
     * devengado (ejemplo real: devengado 0,13, moraNueva 1,20 — no había nada que reversar, y la
     * primera versión reversaba igual los 0,13 porque el exceso bruto —moraAnterior 6,60 menos
     * moraNueva 1,20— era mayor).</p>
     *
     * <p>Asiento ESPEJO del devengo (plantilla 17/DEVENGO_INTERESES, MISMOS papeles
     * {@code INTERES_MORA_POR_COBRAR}/{@code INGRESO_INTERES_MORA} que ya usa
     * {@code CierreCarteraServiceImpl.armaDevengoIntereses}, lado invertido): {@code D
     * INGRESO_INTERES_MORA / H INTERES_MORA_POR_COBRAR} — se da de baja el ingreso que ya no
     * corresponde y el "por cobrar" que nunca se va a cobrar.</p>
     *
     * @param idPrestamo  préstamo al que pertenecen las cuotas de {@code detalleMora}
     * @param detalleMora filas de {@code recalcularMoraALaFechaDePagoDetalle} para ESE préstamo
     * @param idEmpresa   empresa contable del asiento
     * @param fecha       fecha contable (fecha de afectación del cobro)
     * @param origen      literal corto para {@code MVIC.origen}
     * @param idOrigen    código del cobro
     * @param usuario     quién ejecuta el cobro
     * @return {@code idAsiento} generado y el total reversado; {@code idAsiento == null} si
     *         ninguna cuota tenía saldo devengado que reversar
     * @throws Throwable Si ocurre un error
     */
    ResultadoReversoProvision reversarExcesoDevengoPorCobroTardio(Long idPrestamo, List<Object[]> detalleMora,
            Long idEmpresa, LocalDate fecha, String origen, Long idOrigen, String usuario) throws Throwable;

    /**
     * ÍTEM 5, §6.2/§7bis, tipo 8 — reclasificación de bandas: un cobro tardío puede dejar en
     * evidencia que el último cierre clasificó el capital de una cuota en una banda que ya no
     * corresponde (esa cuota, en la realidad, ya estaba pagada antes del corte de ese cierre).
     * Este método NO resuelve bandas ni corridas — eso lo hace el llamador (diseño aprobado por
     * el árbitro 2026-10-05: gate = existe una corrida EJECUTADA con corte posterior a
     * {@code cobro.getFecha()}; banda del último cierre = {@code tipoCarteraYDias} + el
     * clasificador vigente a la {@code fechaProceso} de esa corrida; banda a la fecha de pago =
     * la MISMA que ya usó {@code haberDesdePagos} para este pago, nunca recalculada). Acá solo
     * se arma el asiento (agregado por cuenta de banda, D la de {@code bandaFechaPago} / H la de
     * {@code bandaUltimoCierre} — sin plantilla, las cuentas salen de
     * {@code BandaProductoDetalle.getIdPlanCuenta()}, igual que
     * {@code AcuerdoCondonacionServiceImpl.generarAsientoCondonacion}) y el {@code MVIC} tipo 8
     * por cuota, componente CAPITAL, con {@code tipoCartera}/{@code idBanda} de la banda a la
     * fecha de pago (el estado correcto, vigente de acá en más).
     *
     * <p>Un ítem con las dos bandas iguales (mismo número) no genera ninguna línea ni MVIC — el
     * llamador puede filtrarlos antes o pasarlos igual, este método los descarta sin error.</p>
     *
     * @param items     cuotas a reclasificar, con las dos bandas ya resueltas
     * @param idEmpresa empresa contable del asiento
     * @param fecha     fecha contable (fecha de afectación del cobro)
     * @param origen    literal corto para {@code MVIC.origen}
     * @param idOrigen  código del cobro
     * @param usuario   quién ejecuta el cobro
     * @return {@code idAsiento} generado y el total reclasificado; {@code idAsiento == null} si
     *         ningún ítem tenía bandas distintas
     * @throws Throwable Si ocurre un error
     */
    ResultadoReversoProvision registrarReclasificacionBandaPorCobroTardio(
            List<com.saa.ejb.crd.service.dto.ItemReclasificacionBanda> items, Long idEmpresa, LocalDate fecha,
            String origen, Long idOrigen, String usuario) throws Throwable;
}
