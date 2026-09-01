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
}
