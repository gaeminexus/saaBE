/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Consultas AGREGADAS de cartera que alimentan el cierre mensual.
 *
 *         <p>
 *         No mapea una entidad: agrupa {@code CRD.DTPR} contra {@code CRD.PGPR} y
 *         {@code CRD.PRST} para no traer 70.000 cuotas a memoria. Todas las consultas
 *         comparten el mismo universo, que es el que hay que entender antes de tocarlas:
 *         </p>
 *
 *         <h3>Qué cuota cuenta como pendiente</h3>
 *         <ul>
 *         <li><b>Préstamo vivo:</b> {@code PRST.PRSTIDST IN (2 VIGENTE, 11 EN_MORA)}.
 *         Es {@code PRSTIDST}, no {@code ESPSCDGO} — ver la tabla de trampas de CLAUDE.md.</li>
 *         <li><b>Cuota no liquidada:</b> {@code DTPR.DTPRESTD NOT IN (4 PAGADA,
 *         7 CANCELADA_ANTICIPADA)}, incluido el {@code IS NULL}, porque en Oracle un
 *         {@code NOT IN} contra nulo descarta la fila.</li>
 *         <li><b>Saldo real:</b> componente de la cuota menos lo pagado en
 *         {@code CRD.PGPR} vigente ({@code PGPRANUL} nulo o 0), acotado a cero.</li>
 *         </ul>
 *
 *         <h3>Por qué el saldo NO sale de las columnas de DTPR</h3>
 *         <p>
 *         Verificado contra la BD local el 2026-08-25: {@code DTPRCPPG} ("capital pagado")
 *         vale lo mismo que {@code DTPRCPTL} en 50.853 de las 59.147 cuotas PENDIENTES sin
 *         fecha de pago, y vale 0 en 10.593 cuotas PAGADAS. {@code DTPRFCPG} está en nulo en
 *         11.163 cuotas PAGADAS. Son restos de la migración de la cartera: <b>ninguna de
 *         esas columnas sirve para saber qué se pagó.</b> La fuente de verdad es
 *         {@code CRD.PGPR}, igual que en {@code MotorPagoPrestamoService} y en la generación
 *         del archivo Petro. {@code DTPRSLCP} tampoco es el saldo de la cuota: es el saldo
 *         del PRÉSTAMO después de esa cuota.
 *         </p>
 */
@Local
public interface CierreCarteraDaoService {

    /**
     * Capital pendiente agrupado por producto y fecha de vencimiento.
     *
     * Es la consulta que alimenta la distribución por bandas. Agrupar por fecha de
     * vencimiento colapsa las decenas de miles de cuotas en unos pocos miles de grupos y
     * permite clasificar cada grupo UNA vez: todas las cuotas que vencen el mismo día caen
     * en la misma banda.
     *
     * @param idEmpresa  : Empresa; hoy no filtra (CRD no guarda empresa en el préstamo) y
     *                     se recibe para no cambiar la firma cuando lo haga
     * @return           : Filas {@code [idProducto (Long), fechaVencimiento (LocalDate),
     *                     capitalPendiente (Double), cantidadCuotas (Long)]}
     * @throws Throwable : Excepcion
     */
    List<Object[]> selectCapitalPorProductoYVencimiento(Long idEmpresa) throws Throwable;

    /**
     * Cuotas pendientes de la cartera viva, UNA FILA POR CUOTA — el mismo universo de
     * {@link #selectCapitalPorProductoYVencimiento}, sin agrupar por producto/vencimiento.
     *
     * <p>2026-10-13 (CCPM adopta las bandas del cierre, `DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md`):
     * CCPM necesita el capital pendiente a nivel de PRÉSTAMO, no de producto, para clasificar
     * cada cuota con {@code ContabilizacionIndividualCreditoService.tipoCarteraYDias} y
     * {@code ClasificadorBandaService}. Mismas constantes {@code PRESTAMOS_VIVOS},
     * {@code CUOTAS_PENDIENTES} y {@code PAGOS_VIGENTES} que la consulta hermana — no se
     * duplican.</p>
     *
     * <p><b>Sin {@code HAVING}:</b> a diferencia de la consulta hermana, acá SÍ vienen las
     * cuotas con capital pendiente en 0 — CCPM necesita contarlas (cantidad de cuotas por
     * banda), no solo sumar su capital.</p>
     *
     * @param idEmpresa  : Empresa; hoy no filtra (CRD no guarda empresa en el préstamo) y
     *                     se recibe para no cambiar la firma cuando lo haga
     * @return           : Filas {@code [idPrestamo (Long), idProducto (Long), idCuota (Long),
     *                     numeroCuota (Long), vencimiento (LocalDate), capitalPendiente (Double)]}
     * @throws Throwable : Excepcion
     */
    List<Object[]> selectCuotasPendientesPorPrestamo(Long idEmpresa) throws Throwable;

    /**
     * Interés ordinario y mora pendientes de las cuotas con vencimiento EN EL RANGO
     * {@code [desde, hasta]} (ambos inclusive), agrupados por TIPO DE PRÉSTAMO (quirografario /
     * hipotecario / prendario), que es la dimensión por la que cambia la cuenta contable del
     * devengo.
     *
     * <p>2026-08-31, decisión del usuario: el asiento ④ del cierre de cartera solo devenga los
     * intereses de las cuotas del mes que se ABRE, no las acumuladas de meses anteriores — por
     * eso es un rango, no un "hasta" abierto por abajo. Antes se llamaba
     * {@code selectInteresPorTipoPrestamoHasta} y hacía {@code TRUNC(DTPRFCVN) <= :hasta}
     * (acumulado); se renombró en el mismo cambio porque era su único llamador
     * ({@code CierreCarteraServiceImpl.armaDevengoIntereses}) — un método que se llama "Hasta"
     * y hace un {@code BETWEEN} es una trampa para el próximo que lo lea.</p>
     *
     * @param desde      : Primer día del rango (inclusive)
     * @param hasta      : Último día del rango (inclusive)
     * @return           : Filas {@code [idTipoPrestamo (Long), interesPendiente (Double),
     *                     moraPendiente (Double)]}
     * @throws Throwable : Excepcion
     */
    List<Object[]> selectInteresPorTipoPrestamoEnRango(LocalDate desde, LocalDate hasta) throws Throwable;

    /**
     * Versión POR CUOTA de {@link #selectInteresPorTipoPrestamoEnRango} — mismo universo
     * ({@code PRSTIDST IN (2, 11)}, sin el 8), mismo rango y la MISMA fuente (mora
     * {@code DTPRMRAA} PERSISTIDA, nunca recalculada — a diferencia del paso ⑦, acá no aplica
     * P9), pero una fila por cuota en vez de un total agregado. Para el libro
     * {@code CRD.MVIC} tipo 5 (DEVENGO_MORA) del sub-proceso ④ — el total que el asiento ya
     * asienta (vía {@link #selectInteresPorTipoPrestamoEnRango}) sigue siendo la fuente de
     * verdad de la LÍNEA contable; esta consulta es solo para el detalle por cuota del libro.
     *
     * @param desde Primer día del rango (inclusive) — {@code fechaProceso}
     * @param hasta Último día del rango (inclusive) — {@code fechaCorteApertura}
     * @return Filas {@code [idCuota (Long), idPrestamo (Long), idTipoPrestamo (Long),
     *         moraDevengada (Double)]}, solo las que dan {@code > 0}
     * @throws Throwable Si ocurre un error
     */
    List<Object[]> selectCuotasDevengoMoraEnRango(LocalDate desde, LocalDate hasta) throws Throwable;

    /**
     * Total por cobrar de préstamos de las cuotas con vencimiento hasta la fecha:
     * capital + interés + mora + desgravamen + seguro de incendio, todos por su SALDO real.
     *
     * Es la "b" de la apertura y del neteo (§3.2 ③ y ⑥: "incluyendo todos los valores").
     *
     * @param hasta      : Último día a considerar (inclusive)
     * @return           : Filas {@code [idProducto (Long), capital, interes, mora,
     *                     desgravamen, seguroIncendio, cantidadCuotas]}
     * @throws Throwable : Excepcion
     */
    List<Object[]> selectCobrablePrestamosHasta(LocalDate hasta) throws Throwable;

    /**
     * Igual que {@link #selectCobrablePrestamosHasta}, pero con el universo de LA TRANSICIÓN
     * D27 (2026-10-05): {@code (2, 11)} + los préstamos en 8 (DE_PLAZO_VENCIDO) con una
     * declaración PLVN VIVA (DECLARADA o LIQUIDADA) cuya {@code fechaCorte} sea POSTERIOR a
     * {@code fechaCorteAnterior} — es decir, "lo que el ③ de la corrida anterior realmente
     * abrió" (el grupo A: ya estaba en 11, entró como 11; el grupo B, declarado antes de esa
     * corrida anterior, quedó fuera, y sigue fuera acá).
     *
     * <p>Solo para {@code CierreCarteraServiceImpl#armaNeteo} de la PRIMERA corrida después de
     * D27 — se usa cuando la corrida anterior (la que abrió el mes que se cierra) no tiene
     * ningún {@code MVIC} ({@code MovimientoInteresCuotaDaoService#existeAlgunoByCorrida}): esa
     * es la marca de que corrió con el código viejo {@code (2, 11)}, antes de este cambio. Si
     * la corrida anterior SÍ tiene MVIC, el ⑥ usa {@link #selectCobrablePrestamosHasta} normal,
     * con el universo nuevo — el mismo que usó su propio ③.</p>
     *
     * @param hasta             Último día a considerar (inclusive) — el corte de ESTE cierre
     * @param fechaCorteAnterior Corte de la corrida ANTERIOR (la que abrió el mes que se cierra)
     * @return Mismas filas que {@link #selectCobrablePrestamosHasta}
     * @throws Throwable Si ocurre un error
     */
    List<Object[]> selectCobrablePrestamosHastaTransicionPlazoVencido(LocalDate hasta, LocalDate fechaCorteAnterior)
            throws Throwable;

    /**
     * Aporte mensual esperado de los partícipes activos: la vigencia ABIERTA de CRD.VGCN del
     * contrato ACTIVO de cada entidad en estado ACTIVO (1) o ACTIVO_EN_MORA (8), por tipo de
     * aporte (9 jubilación, 11 cesantía). Migrado de {@code CRD.HSTR} a {@code CRD.VGCN} en
     * la Fase 3 (docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §3.5): si no se
     * migraba, este esperado contable y el que usa la carga Petro para el cobro quedaban
     * leyendo fuentes distintas y podían divergir en silencio.
     *
     * Es el mismo universo que usa {@code GeneracionArchivoPetroServiceImpl.recopilarAportes}
     * para el producto AH, sin el recargo por meses de mora: aquí se devenga el aporte del
     * mes, no la deuda acumulada.
     *
     * @return           : {@code [jubilacion (Double), cesantia (Double), participes (Long)]}
     * @throws Throwable : Excepcion
     */
    Object[] selectAporteMensualEsperado() throws Throwable;

    // NOTA (2026-08-31): existió acá selectAporteDescontadoPorMes (CRD.DTCA, producto AH),
    // segunda fuente propuesta para el asiento ③. Se descartó y se eliminó: el archivo del
    // mes que se ABRE nunca existe todavía cuando corre la apertura (el proceso real carga
    // Petro DESPUÉS del cierre/apertura), así que esa consulta siempre habría dado $0. La
    // fuente final es {@code GeneracionArchivoPetroService#calcularAportesEsperados} — el
    // mismo algoritmo que genera el archivo real, no depende de que el archivo exista.

    /**
     * Consulta de control (§3.5 del plan): compara, para el mismo universo de entidades
     * ACTIVO/ACTIVO_EN_MORA, el esperado que salía de {@code CRD.HSTR} (estado 99) contra el
     * que sale ahora de {@code CRD.VGCN} (vigencia abierta del contrato activo). Sólo lectura,
     * no la usa ningún proceso — es para reportar la diferencia tras la migración de 3.3.
     *
     * @return : {@code [jubilacionHstr (Double), cesantiaHstr (Double), jubilacionVgcn (Double), cesantiaVgcn (Double)]}
     * @throws Throwable : Excepcion
     */
    Object[] selectControlEsperadoHstrVsVgcn() throws Throwable;

    /**
     * Aportes personales de jubilación (tipo 9) y cesantía (tipo 11) efectivamente
     * registrados en el rango de fechas. Solo movimientos POSITIVOS: los negativos son
     * devoluciones y pagos con aportes, no recaudación de la planilla.
     *
     * @param desde      : Primer día del rango (inclusive)
     * @param hasta      : Último día del rango (inclusive)
     * @return           : {@code [jubilacion (Double), cesantia (Double)]}
     * @throws Throwable : Excepcion
     */
    Object[] selectAportesRegistrados(LocalDate desde, LocalDate hasta) throws Throwable;

    // =====================================================================
    // C15 — variantes "a fecha de corte" (ver com.saa.ejb.crd.service.ModoCorteCartera).
    // Con corte == null son idénticas a las de arriba (el comportamiento normal no cambia).
    // Con corte: préstamos aprobados con PRSTFCHA <= corte (no 0/1/6/7/12), cuotas no
    // liquidadas AL CORTE, pagos vigentes con PGPRFCHA <= corte (capital = PGPRSLOT si > 0, si
    // no PGPRCPPG; el abono a capital solo PGPRCPPG — C14b, ModoCorteCartera.CAPITAL_PAGADO_CUOTA_SQL)
    // y mora con la fórmula del proceso diario a la fecha de corte.
    // =====================================================================

    /** {@link #selectCapitalPorProductoYVencimiento(Long)} al corte. */
    List<Object[]> selectCapitalPorProductoYVencimiento(Long idEmpresa, LocalDate corte) throws Throwable;

    /** {@link #selectCuotasPendientesPorPrestamo(Long)} al corte (solo cuotas con capital pendiente). */
    List<Object[]> selectCuotasPendientesPorPrestamo(Long idEmpresa, LocalDate corte) throws Throwable;

    /** {@link #selectInteresPorTipoPrestamoEnRango(LocalDate, LocalDate)} al corte. */
    List<Object[]> selectInteresPorTipoPrestamoEnRango(LocalDate desde, LocalDate hasta, LocalDate corte)
            throws Throwable;

    /** {@link #selectCuotasDevengoMoraEnRango(LocalDate, LocalDate)} al corte. */
    List<Object[]> selectCuotasDevengoMoraEnRango(LocalDate desde, LocalDate hasta, LocalDate corte)
            throws Throwable;

    /** {@link #selectCobrablePrestamosHasta(LocalDate)} medido al corte. */
    List<Object[]> selectCobrablePrestamosHasta(LocalDate hasta, LocalDate corte) throws Throwable;

    /**
     * Cartera al corte POR OPERACIÓN: la base común de la G48, el CCPM y la provisión de
     * cartera ⑧ del cierre (D-015: el mismo cálculo).
     *
     * @param corte          : Fecha de corte (obligatoria)
     * @param corteInclusivo : true = la cuota que vence el día del corte e impaga ya es vencida
     * @return Filas {@code [idPrestamo, idProducto, idTipoPrestamo, idEntidad, porVencer,
     *         vencido, diasMora, interesVencidoPendiente, moraPendiente, idCuotaAncla,
     *         interesCuotasDelMes, cuentaIndividual]}; la cuenta individual (aportes TPAPIDST = 1
     *         con caja <= corte) va solo en la primera operación del partícipe y en 0 si es
     *         jubilado complementario — regla del generador G48.
     */
    List<Object[]> selectOperacionesAlCorte(LocalDate corte, boolean corteInclusivo) throws Throwable;

    /**
     * D-017 (H4) — mora a devengar al corte por cuota: mora bruta de la cuota vencida impaga
     * (fórmula del proceso diario a la fecha de corte) menos lo ya reconocido en {@code CRD.MVIC}
     * (tipo 5 − tipo 6, fecha contable {@code <= corte}). Requiere corte (modo al corte).
     *
     * @return Filas {@code [idCuota, idPrestamo, idTipoPrestamo, moraADevengar]}, solo > 0
     */
    List<Object[]> selectMoraPorDevengarAlCorte(LocalDate corte, LocalDate inicioDevengo) throws Throwable;

    /**
     * Saldo contable deudor por cuenta al corte (asientos 1/3 con fecha <= corte), misma regla
     * de la cuadratura C14.
     *
     * @return Filas {@code [cuenta (String), idPlanCuenta (Long), saldoDeudor (Double)]}
     */
    List<Object[]> selectSaldoContablePorCuenta(Long idEmpresa, String prefijo, String prefijoExcluido,
            LocalDate corte) throws Throwable;

    /**
     * Igual que {@link #selectSaldoContablePorCuenta(Long, String, String, LocalDate)} pero excluyendo una LISTA de
     * cuentas exactas en vez de un prefijo (P4-B, 2026-11-01: las cuentas de la provisión de cartera salen de la
     * plantilla, no de un literal). {@code cuentasExcluidas} nula o vacía no excluye nada.
     */
    List<Object[]> selectSaldoContablePorCuentaExcluyendo(Long idEmpresa, String prefijo, List<String> cuentasExcluidas,
            LocalDate corte) throws Throwable;

    /**
     * H6: préstamos aprobados (no 0/1/6/7/12) sin asiento de entrega ({@code PRSTASEN} nulo) con
     * fecha de inicio en el rango.
     *
     * @return Filas {@code [idPrestamo, idAsoprep, fechaInicio, monto, idEstado, esSustitucion(0/1)]}
     */
    List<Object[]> selectPrestamosSinAsientoEntrega(LocalDate desde, LocalDate hasta) throws Throwable;

    // =====================================================================
    // D-032 (29-oct-2026) — cuenta por cobrar de cuotas (1.4.05.10) préstamo por préstamo.
    // =====================================================================

    /**
     * Lo que la apertura ③ abrió POR PRÉSTAMO: la misma lectura de
     * {@link #selectCobrablePrestamosHasta(LocalDate, LocalDate)} (cuotas con vto {@code <= hasta}
     * pendientes al {@code corte}: capital + interés no condonado + mora bruta al corte + desgravamen
     * + seguro), agrupada por préstamo en vez de por producto. Con {@code hasta} = fin del mes M y
     * {@code corte} = fin de M−1 reproduce el ③ que abrió M (corrida al corte).
     *
     * @return Filas {@code [idPrestamo (Long), abierto (Double)]}
     */
    List<Object[]> selectAperturaPorPrestamo(LocalDate hasta, LocalDate corte) throws Throwable;

    /**
     * Lo que los cobros con asiento en {@code [desde, hasta]} acreditaron en {@code cuenta} (y sus
     * hijas) POR PRÉSTAMO, sin contar las líneas de complemento de apertura:
     * <ul>
     * <li>Petro/Arch: componentes (capital, interés, mora, desgravamen, seguro, interés vencido) de los
     *     pagos vigentes de las cargas con reparto vigente ({@code CRD.ANCP} sub-proceso 2) en el rango;</li>
     * <li>cobro CBCR: haber neto de la cuenta en sus asientos ({@code CBCRASN1/ASN2/ASRP}), prorrateado
     *     entre sus líneas de préstamo por {@code DCBCVLRR} (partes iguales si todas valen 0);</li>
     * <li>camino directo (precancelación, pago con aportes, cruce): haber neto de la cuenta en el asiento
     *     {@code PGPRASNT}, prorrateado por los componentes de sus pagos.</li>
     * </ul>
     *
     * @param excluirCarga carga Petro que se está contabilizando (sus pagos ya existen; null = ninguna)
     * @return Filas {@code [idPrestamo (Long), cobrado (Double)]}
     */
    List<Object[]> selectCobradoPuentePorPrestamo(Long idEmpresa, String cuenta, LocalDate desde, LocalDate hasta,
            Long excluirCarga) throws Throwable;

    /**
     * Haber de {@code cuenta} en los repartos Petro vigentes del rango que no es pago de préstamo
     * (haber del reparto − componentes de los pagos de la carga): los excedentes D-010.
     */
    double selectNoAtribuiblePetro(Long idEmpresa, String cuenta, LocalDate desde, LocalDate hasta, Long excluirCarga)
            throws Throwable;

    /** Débito neto de {@code cuenta} en las líneas de complemento de apertura de los asientos vigentes del rango. */
    double selectComplementoRegistrado(Long idEmpresa, String cuenta, LocalDate desde, LocalDate hasta)
            throws Throwable;
}
