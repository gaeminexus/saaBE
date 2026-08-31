package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.model.cnt.DetalleAsiento;
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
     * {@code capitalPagado}. La banda se resuelve con la fecha de vencimiento de la cuota DE
     * ESE {@code PagoPrestamo} — la ancla para un abono/capital futuro, la cuota real para un
     * pago normal.
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
     * tipoCartera + días para {@link ClasificadorBandaService#clasificar}, unificados con la
     * regla documentada (el primer día vencido es 1, nunca 0): por vencer = días de la fecha
     * de corte a la de vencimiento (mínimo 1); vencido = días de la de vencimiento a la de
     * corte, MÁS 1. Antes de este servicio había tres copias de esta cuenta en el código
     * (Petro, condonación, cierre de cartera) y solo la de cierre de cartera tenía el +1 —
     * verificado el 2026-08-30, no corregido en esas tres (no se tocan acá), pero esta es la
     * versión que usan CBCRASN2 y todo lo nuevo.
     *
     * @return {@code [tipoCartera, dias]}
     */
    long[] tipoCarteraYDias(LocalDate fechaVencimiento, LocalDate fechaCorte);
}
