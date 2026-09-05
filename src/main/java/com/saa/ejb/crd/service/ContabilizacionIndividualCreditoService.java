package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;

/**
 * Resolución COMPARTIDA de las cuentas contables individuales (no bandas) que participan de
 * más de un asiento de crédito: intereses, mora, seguros y aportes por tipo, más el cruce de
 * aportes CONSUMIDOS. Todas se resuelven contra la plantilla alterno
 * {@link com.saa.rubros.PlantillasCredito#APLICACION_PETRO} (21) — la ÚNICA plantilla CRD que
 * está confirmada como renumerada al catálogo semántico {@link com.saa.rubros.CrdLineaAsiento}
 * (verificado contra {@code CNT.DTPL} el 2026-08-30: la 23 y la 25 siguen posicionales, no
 * renumeradas — usarlas para esto reproduce el bug que este servicio existe para evitar).
 *
 * <p>Nace de la especificación {@code docs/logica-negocio/crd/ESPECIFICACION-CBCRASN2.md}: la
 * razón de existir es que MÁS DE UN llamador necesita la MISMA cuenta para el MISMO concepto
 * (p. ej. el interés condonado de un acuerdo y el interés pagado de un cobro procesado por
 * CBCR son montos distintos, pero tienen que salir de la MISMA cuenta 1.4.02.xx). Que dos
 * lugares resuelvan la cuenta por caminos independientes es exactamente el riesgo que dejó
 * abierta la cuenta transitoria en el diseño original de {@code CobroCreditoServiceImpl}: si
 * llegan a divergir aunque sea una vez, nada lo detecta porque el asiento igual cuadra.
 *
 * <p>Es un servicio aparte (no un método más de {@code CobroCreditoServiceImpl}) a propósito:
 * lo consumen dos beans que no se deben depender entre sí (
 * {@code CobroCreditoServiceImpl} ya inyecta {@code AcuerdoCondonacionService}; que
 * {@code AcuerdoCondonacionServiceImpl} inyectara de vuelta a {@code CobroCreditoService} para
 * llegar a esto sería una dependencia circular innecesaria).
 *
 * @author Sistema SAA
 * @since 2026-08-30
 */
@Local
public interface ContabilizacionIndividualCreditoService {

    /**
     * Resuelve el código de la plantilla alterno 21 para una empresa.
     *
     * @throws Throwable {@code IncomeException} si no existe la plantilla para esa empresa
     */
    Long resolverPlantillaAplicacion(Long idEmpresa) throws Throwable;

    /**
     * Línea de interés por cobrar de un tipo de préstamo (aux1 10 ordinario / 20 mora, aux2 =
     * {@code idTipoPrestamo}). D3: mora y ordinario COMPARTEN cuenta — {@code esMora} solo
     * cambia la descripción de la línea, nunca la cuenta.
     *
     * @throws Throwable {@code IncomeException} si la plantilla no tiene la línea para ese tipo
     */
    DetalleAsiento lineaInteres(Long idPlantillaAplicacion, Long idTipoPrestamo, double valor, boolean esMora,
            String prefijoDescripcion) throws Throwable;

    /**
     * Línea de seguro de incendio (aux1 42 hipotecario / 43 prendario). Solo esos dos tipos de
     * préstamo tienen esta cuenta — para cualquier otro tipo con valor &gt; 0, falla fuerte.
     */
    DetalleAsiento lineaSeguroIncendio(Long idPlantillaAplicacion, Long idTipoPrestamo, double valor,
            String prefijoDescripcion) throws Throwable;

    /** Línea de seguro de desgravamen (aux1 60), sin distinción por tipo de préstamo. */
    DetalleAsiento lineaSeguroDesgravamen(Long idPlantillaAplicacion, double valor, String prefijoDescripcion)
            throws Throwable;

    /**
     * Línea de un aporte REGISTRADO — el socio ENTREGA dinero, su saldo SUBE (aux1 50
     * cesantía / 51 jubilación / 52 adicional). Movimiento tal cual lo define la plantilla
     * (HABER): esto es lo correcto para REGISTRO_APORTE/COBRO_MIXTO, el sentido normal del
     * dinero entrando como aporte.
     *
     * @throws Throwable {@code IncomeException} si el tipo de aporte no tiene cuenta asignada
     *                    (hoy solo 9 jubilación, 11 cesantía, 2 adicional)
     */
    DetalleAsiento lineaAporteRegistrado(Long idPlantillaAplicacion, Long idTipoAporte, double valor,
            String prefijoDescripcion) throws Throwable;

    /**
     * Líneas del cruce de aportes CONSUMIDOS — se CONSUME saldo que el socio ya tenía, su
     * saldo BAJA (precancelación mixta, acuerdo de condonación). Usa las MISMAS cuentas que
     * {@link #lineaAporteRegistrado} (50/51/52) pero las fuerza al DEBE
     * independientemente de lo que diga {@code DetallePlantilla.movimiento} — esa plantilla
     * define esas cuentas para el sentido opuesto (aporte entrando), y aquí el sentido es el
     * contrario: la cuenta de pasivo de aportes BAJA porque se usó para pagar, no porque
     * entró dinero nuevo. Compartido por {@code CobroCreditoServiceImpl} (CBCRASN2, la mitad
     * de aportes de una operación con dos fuentes) y
     * {@code AcuerdoCondonacionServiceImpl#aplicarAcuerdo} (acuerdo 100% aportes, sin CBCR).
     *
     * <p>
     * <b>NO la usa {@code DevolucionAporteServiceImpl}</b> — su asiento de reclasificación
     * cubre CUALQUIER tipo de aporte (2026-08-31), no solo los tres de la plantilla 21
     * (aux1 50/51/52); resuelve sus cuentas contra {@code CRD.CTAP}, una tabla de
     * configuración, no una plantilla. Ver el javadoc de {@code CuentaTipoAporte}.
     *
     * <p><b>Contraste a propósito con {@link #lineasReparto}</b> (vecino de este método,
     * mismo servicio): ese NO fuerza el Debe/Haber, respeta {@code DetallePlantilla
     * .movimiento} — ahí sí hace falta leer la configuración real porque no hay una dirección
     * fija por el sentido del dinero. Los dos criterios son opuestos a propósito, cada uno
     * por su propia razón — no "uniformarlos".</p>
     *
     * @return Una línea por tipo de aporte del desglose, todas al Debe
     */
    List<DetalleAsiento> lineasCruceAportesConsumidos(Long idPlantillaAplicacion, List<DesgloseAporte> aportes,
            String prefijoDescripcion) throws Throwable;

    /**
     * Clasifica un monto de capital en su banda ({@link ClasificadorBandaService}) y arma la
     * línea de asiento (H) con la cuenta de {@code CRD.BNDP} — nunca de una plantilla.
     */
    DetalleAsiento lineaBandaCapital(Long idProducto, Long idEmpresa, double capital, LocalDate fechaVencimiento,
            LocalDate fechaCorte, String prefijoDescripcion) throws Throwable;

    /**
     * Todas las líneas de haber (cuentas por cobrar liquidadas) que salen de un conjunto de
     * {@code PagoPrestamo} VIGENTES — típicamente los de UN {@code EventoPrestamo}, pero
     * también sirve para el único pago de un acuerdo de condonación (lista de un elemento).
     *
     * <p>UNA SOLA REGLA para las trampas de "abono a capital"/"capital futuro de
     * precancelación" (los dos graban en {@code saldoOtros} con {@code capitalPagado = 0},
     * nunca al revés): el capital a bandear es {@code saldoOtros} si es &gt; 0, si no
     * {@code capitalPagado}. Para un pago normal, la banda se resuelve con la fecha de
     * vencimiento de la cuota real que se pagó.
     *
     * <p><b>Re-bandeo del abono a capital (2026-08-31, PLAN-CIERRE-CONTABLE-TOTAL, Fase 3):</b>
     * cuando {@code pago.getTipo()} es {@code ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL}, el
     * capital NO se bandea contra la cuota ANCLA (la última cuota pagada, o la primera cuota
     * nueva si el préstamo no tenía ninguna pagada — donde {@code AbonoCapitalPrestamoServiceImpl
     * .aplicar} simplemente ACUMULA {@code saldoOtros} para no perder el rastro del dinero, sin
     * que esa cuota tenga nada que ver con lo que el abono canceló). Se reparte PROPORCIONAL al
     * capital de cada {@code HistDetallePrestamo} que ese mismo evento historizó en
     * {@code CRD.HDTP} — esas SÍ son las cuotas que el abono canceló — clasificando cada una por
     * su propia fecha de vencimiento. Ver el javadoc de la implementación para el detalle del
     * prorrateo.
     *
     * <p><b>Re-bandeo del capital futuro de una precancelación (2026-09-01, la trampa gemela
     * de arriba, ya cerrada).</b> Cuando {@code pago.getTipo()} es {@code
     * ProcesoPagoPrestamoService.TIPO_PRECANCELACION} Y {@code saldoOtros &gt; 0} (el mismo
     * discriminador que separa esta trampa de un pago de cuota exigible normal — las exigibles
     * de la misma precancelación comparten el tipo pero pagan con {@code capitalPagado},
     * {@code saldoOtros = 0}, así que nunca entran acá), el capital NO se bandea 100% contra la
     * ancla — se reparte, SIN prorrateo, contra las cuotas que {@code
     * ProcesoPagoPrestamoServiceImpl.precancelar} dejó en {@code CRD.DTPR} con {@code estado =
     * CANCELADA_ANTICIPADA(7)}: cada una aporta exactamente su propio {@code capital}, porque
     * una precancelación cancela SIEMPRE la totalidad de las cuotas futuras (verificado: el
     * método exige que el valor recibido cuadre exacto contra la simulación completa ANTES de
     * tocar nada, así que nunca deja una cuota a medias — a diferencia del abono, acá no hace
     * falta prorratear). Ver el javadoc de la implementación para el detalle y el cuadre.</p>
     *
     * <p>Compartido por {@code CobroCreditoServiceImpl} (CBCRASN2),
     * {@code AcuerdoCondonacionServiceImpl} (condonación 100% aportes) y
     * {@code ProcesoPagoPrestamoServiceImpl} (cruce de valores / precancelación 100% aportes,
     * PLAN-CIERRE-CONTABLE-TOTAL) — la MISMA regla en los cuatro lugares, nunca una copia por
     * cada uno.
     *
     * @param pagos     {@code PagoPrestamo} a clasificar; los anulados se ignoran
     * @throws Throwable {@code IncomeException} si algún pago con capital no tiene producto o
     *                    cuota con fecha de vencimiento, o si falta una línea en la plantilla
     */
    List<DetalleAsiento> haberDesdePagos(List<PagoPrestamo> pagos, Long idEmpresa, Long idPlantillaAplicacion,
            LocalDate fechaCorte, String prefijoDescripcion) throws Throwable;

    /**
     * Suma el capital de un conjunto de {@code PagoPrestamo} (típicamente los de UN
     * {@code EventoPrestamo}) que vence DESPUÉS de {@code fechaCorteApertura} — el tramo de un
     * abono a capital o de una precancelación que la apertura mensual NUNCA abrió, y que por
     * eso el asiento del cobro NO debe cerrar (2026-09-05, ver
     * {@code docs/logica-negocio/crd/DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md}).
     *
     * <p><b>El nombre es histórico</b> (nació en el diseño revertido de la apertura
     * extraordinaria, `96a492b8`/`0b665f2d`): la lógica de cálculo es exactamente la misma, lo
     * único que cambió es qué hace el llamador con el resultado — antes lo abría con un asiento
     * nuevo, ahora lo excluye del monto que {@code generarAsientoReparto}/
     * {@code generarAsientoDefinitivo} usan para cerrar la cuenta de apertura.</p>
     *
     * <p>Reusa EXACTAMENTE el mismo reparto y la misma identificación de cuotas que
     * {@link #haberDesdePagos} usa para bandear estos mismos pagos — nunca un cálculo aparte
     * que pueda desalinearse:</p>
     * <ul>
     * <li>Abono a capital ({@code saldoOtros > 0} y tipo {@code TIPO_ABONO_CAPITAL}): reparte
     * proporcional entre las {@code HistDetallePrestamo} del evento (mismo reparto que
     * {@link #lineasReclasificacionAbonoCapital}/la banda del abono) y suma la parte de las
     * que vencen después del corte.</li>
     * <li>Capital futuro de precancelación ({@code saldoOtros > 0} y tipo
     * {@code TIPO_PRECANCELACION}): suma el capital de las {@code DetallePrestamo} en estado
     * {@code CANCELADA_ANTICIPADA(7)} del préstamo cuyo vencimiento es posterior al corte —
     * misma fuente que la banda del capital futuro de precancelación.</li>
     * <li>Cualquier otro pago (cuota exigible normal, capital dentro del mes abierto) no
     * suma nada acá: ya lo abrió la apertura mensual, así que sí corresponde cerrarlo.</li>
     * </ul>
     *
     * @param pagos               {@code PagoPrestamo} a evaluar; los anulados se ignoran
     * @param fechaCorteApertura  el corte del período abierto vigente ({@code CRCTFCCR} de la
     *                            corrida de cierre viva) — nunca recalculado acá
     * @return la suma del capital futuro, redondeada; 0 si ninguno de los pagos tiene capital
     *         posterior al corte
     */
    double capitalFuturoPosteriorACorte(List<PagoPrestamo> pagos, LocalDate fechaCorteApertura,
            String prefijoDescripcion) throws Throwable;

    /**
     * Asiento de re-bandeo de un abono a capital (2026-08-31, PLAN-CIERRE-CONTABLE-TOTAL, Fase
     * 3, §9.1 C2 del levantamiento) — reclasifica entre bandas el capital que SIGUE VIVO tras
     * la re-amortización, nunca la plata del abono (esa la banda {@link #haberDesdePagos}, en
     * el asiento del cobro). Mismo patrón que {@code CierreCarteraServiceImpl.armaCambioBandas}
     * (diferencias netas por banda), aplicado a UN préstamo.
     *
     * <p>El lado viejo de la comparación es el REMANENTE de cada historizada después de que el
     * abono se llevó su parte — nunca las historizadas tal cual, que incluirían el abono y lo
     * contarían dos veces. Ver el javadoc de la implementación para el detalle completo.</p>
     *
     * @param historizadas Las {@code HistDetallePrestamo} del evento — las mismas que se le
     *                     pasan a {@link #haberDesdePagos} para el asiento del abono.
     * @param capitalAbono El mismo monto del abono — solo para calcular el remanente, nunca
     *                     para armar una línea acá.
     * @param nuevas       Las cuotas vivas que quedaron tras rehacer la tabla de amortización.
     * @throws Throwable {@code IncomeException} si el remanente no coincide con el capital de
     *                    las cuotas nuevas, o si el asiento resultante no cuadra Debe/Haber —
     *                    este asiento nunca puede entrar ni salir plata de la cartera.
     */
    List<DetalleAsiento> lineasReclasificacionAbonoCapital(Long idProducto, Long idEmpresa,
            List<HistDetallePrestamo> historizadas, double capitalAbono, List<DetallePrestamo> nuevas,
            LocalDate fechaCorte, String prefijoDescripcion) throws Throwable;

    /**
     * tipoCartera + días para {@link ClasificadorBandaService#clasificar}.
     *
     * <p><b>2026-09-04, corregido — regla de negocio confirmada por el usuario directamente:
     * el día del vencimiento la cuota TODAVÍA está POR VENCER; recién al día siguiente pasa a
     * VENCIDA.</b> Por eso la comparación es {@code fechaVencimiento.isBefore(fechaCorte)}
     * (estrictamente anterior), NUNCA {@code !isAfter} — una cuota que vence el mismo día del
     * corte cae del lado POR_VENCER, con 1 día (por el {@code Math.max(1, ...)} de abajo, ya
     * que la resta da 0).</p>
     *
     * <p><b>Y por eso NO lleva {@code +1} en la rama VENCIDO.</b> La versión vieja sumaba 1
     * porque el día del vencimiento contaba como vencido — con esa premisa cambiada, el +1
     * pasa a contar un día de más en TODAS las cuotas vencidas, y en los bordes de banda (30,
     * 90, 180, 360 días) manda la plata a la cuenta contable equivocada (30 días vencidos daría
     * 31 → banda siguiente). Si alguien ve un {@code isBefore} acá donde "debería" haber un
     * {@code <=}, o le falta un {@code +1} que "debería" estar, NO es un descuido: es
     * exactamente la corrección de este día.</p>
     *
     * <p>Por vencer = días de la fecha de corte a la de vencimiento (mínimo 1); vencido = días
     * de la de vencimiento a la de corte (mínimo 1, ya sin sumar nada). Antes de este servicio
     * había tres copias de esta cuenta en el código (Petro, condonación, cierre de cartera) y
     * ninguna se toca acá — siguen con su propia lógica, correcta o no, hasta que se decida
     * unificarlas.</p>
     *
     * @return {@code [tipoCartera, dias]}
     */
    long[] tipoCarteraYDias(LocalDate fechaVencimiento, LocalDate fechaCorte);

    // =====================================================================
    // Reparto (2026-08-31, circuito de cobros con aportes — tres asientos por cobro,
    // decisión del usuario): plantilla alterno
    // {@link com.saa.rubros.PlantillasCredito#REPARTO_TRANSITORIA} (20), POSICIONAL (aux1 1/2/3,
    // NO renumerada a CrdLineaAsiento — como la 23/25, ver el javadoc de la clase). Extraída
    // de {@code CobroPetroContableServiceImpl.contabilizarReparto} (único dueño hasta hoy)
    // para que CBCR use la MISMA implementación en vez de una copia — dos asientos "reparto"
    // divergen tarde o temprano si son dos códigos distintos.
    // =====================================================================

    /**
     * Resuelve el código de la plantilla alterno 20 (reparto) para una empresa.
     *
     * @throws Throwable {@code IncomeException} si no existe la plantilla para esa empresa
     */
    Long resolverPlantillaReparto(Long idEmpresa) throws Throwable;

    /**
     * Líneas de aportes/préstamos "por cobrar" (activo, {@code 1.4.05.05}/{@code 1.4.05.10},
     * aux1 2/3 de la plantilla de reparto) — la contrapartida de la cuenta transitoria cuando
     * el reparto la cierra contra los mismos activos que abrió el asiento ③ de apertura.
     *
     * <p><b>NO incluye la línea de transitoria (aux1=1).</b> Cada llamador la resuelve con SU
     * PROPIO método compartido de transitoria — {@code CobroCreditoServiceImpl
     * #resolverLineaTransitoria} para CBCR (plantilla 19), {@code
     * CobroPetroContableServiceImpl} sigue resolviendo la suya inline con esta misma plantilla
     * 20 aux1=1, sin cambios. Que hoy las dos plantillas resuelvan a la misma cuenta
     * ({@code 2.3.01.15.01}) es una coincidencia, no un diseño —
     * {@code ESPECIFICACION-CBCRASN2.md} exige explícitamente que la transitoria tenga un
     * ÚNICO camino de resolución por circuito, nunca dos, aunque hoy coincidan.</p>
     *
     * <p><b>Respeta {@code DetallePlantilla.movimiento}, NO fuerza Debe ni Haber — a
     * propósito, y por el motivo CONTRARIO al de {@link #lineasCruceAportesConsumidos}.</b>
     * Ese método SÍ fuerza el Debe, porque ahí la dirección está fijada por el sentido del
     * dinero (un aporte que se CONSUME siempre baja el pasivo, sin importar cómo esté
     * configurada la plantilla — forzarlo es lo correcto). Acá no hay esa certeza a priori, y
     * no hay forma de verificar la configuración real de la plantilla 20 sin acceso a la
     * base. {@code CobroPetroContableServiceImpl} siempre leyó el movimiento en vez de
     * asumirlo, así que esta extracción hace lo mismo: es la diferencia entre un método que
     * "funciona con los datos de hoy" (forzar Haber, que da el resultado correcto MIENTRAS la
     * plantilla esté configurada como hoy) y uno que "funciona" (leer el movimiento, correcto
     * sin importar cómo esté configurada). Forzar un lado aquí habría funcionado hoy y se
     * habría invertido en silencio —el asiento cuadrando igual— el día que alguien
     * reconfigure la plantilla 20. <b>No "uniformar" este método con
     * {@code lineasCruceAportesConsumidos}/{@code lineaAporteRegistrado}: los criterios son
     * opuestos a propósito, cada uno por su propia razón, no por descuido.</b></p>
     *
     * @return 0, 1 o 2 líneas (una por total &gt; 0)
     * @throws Throwable {@code IncomeException} si falta la línea de aportes o de préstamos
     *                    en la plantilla para un total que sí es &gt; 0
     */
    List<DetalleAsiento> lineasReparto(Long idPlantilla, double totalAportes, double totalPrestamos,
            String prefijoDescripcion) throws Throwable;

    /**
     * Líneas "por aplicar" ({@code CrdLineaAsiento#APORTES_POR_APLICAR}/{@code
     * PRESTAMOS_POR_APLICAR}) — cierran contra la MISMA cuenta que abrió el asiento ③ de
     * apertura del cierre de cartera ({@code CierreCarteraServiceImpl#armaApertura}) y que ya
     * usa {@code CobroPetroContableServiceImpl#contabilizarAplicacion} para su asiento de
     * aplicación (2026-08-31, corrección del asiento 3/CBCRASN2 de {@code
     * CobroCreditoServiceImpl}: antes volvía a debitar la cuenta transitoria, que el asiento de
     * reparto ya había cerrado — la dejaba en negativo en vez de en cero).
     *
     * <p>Se resuelve contra {@code idPlantillaAplicacion} (alterno {@link
     * com.saa.rubros.PlantillasCredito#APLICACION_PETRO}, 21) — el catálogo semántico {@code
     * CrdLineaAsiento}, NO la plantilla posicional de {@link #lineasReparto} (alterno 20): son
     * dos plantillas distintas resolviendo lo que debería ser la misma cuenta 1.4.05.05/
     * 1.4.05.10 — verificación de que de verdad coinciden pendiente
     * (crd/sql/102_VERIFICACION_PLANTILLAS_POR_APLICAR.sql, 2026-08-31). Si algún día divergen,
     * el arreglo es la configuración de esa plantilla, no volver a mezclar los dos caminos
     * acá.</p>
     *
     * <p>Respeta {@code DetallePlantilla.movimiento} (no fuerza Debe/Haber) — mismo criterio y
     * mismo motivo que {@link #lineasReparto}, réplica de {@code
     * CobroPetroContableServiceImpl#lineaDesdePlantilla}.</p>
     *
     * @return 0, 1 o 2 líneas (una por total &gt; 0)
     * @throws Throwable {@code IncomeException} si falta la línea de aportes o de préstamos
     *                    "por aplicar" en la plantilla para un total que sí es &gt; 0
     */
    List<DetalleAsiento> lineasAplicacionPorAplicar(Long idPlantillaAplicacion, double totalAportes,
            double totalPrestamos, String prefijoDescripcion) throws Throwable;
}
