package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Hooks de contabilidad de los procesos de pago de préstamos (§9 de
 * ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 *
 * Se invocan al FINAL de cada proceso, dentro de la MISMA transacción REQUIRED: cuando exista
 * una implementación real, un fallo del asiento (período inexistente o MAYORIZADO, debe ≠ haber)
 * revierte el pago completo. Ese es el comportamiento correcto para operaciones online, a
 * diferencia del lote petro.
 *
 * La implementación es {@code ContabilidadPrestamoServiceImpl} (reemplazó a
 * {@code ContabilidadPrestamoNoOpImpl}, borrada el 2026-08-31 — dos beans implementando este
 * mismo {@code @Local} dejan la inyección ambigua). Fase 1 de {@code PLAN-CIERRE-CONTABLE-TOTAL.md}:
 * solo {@link #contabilizarPagoConAportes} genera asiento; los otros cuatro siguen devolviendo
 * {@code null} a propósito hasta que el árbitro defina el discriminador de origen que evite
 * duplicar el asiento de los procesos que ya pasan por {@code CobroCreditoServiceImpl} (ver el
 * javadoc de {@code generarAsientoDefinitivo} ahí).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface ContabilidadPrestamoService {

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarPagoCuota(ResultadoAplicacionPago resultado, ContextoPago ctx) throws Throwable;

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarPagoConAportes(ResultadoAplicacionPago resultado, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable;

    /**
     * Asiento de re-bandeo (2026-08-31, Fase 3 — ver {@code ContabilizacionIndividualCreditoService
     * #lineasReclasificacionAbonoCapital}): reclasifica entre bandas el capital que sigue vivo
     * tras la re-amortización. NO lleva la plata del abono — esa la lleva CBCRASN2, el asiento
     * definitivo del cobro que originó este evento (único camino de aplicación hoy: el abono se
     * aplica siempre vía {@code CobroCreditoServiceImpl}, nunca directo — ver {@code
     * PrestamoRest#abonarCapital}). Que este método reciba {@code idEmpresa} y no un
     * {@code ContextoPago} completo es a propósito: este asiento no tiene el discriminador
     * {@code idCobroCredito} porque no lo necesita, no es la misma plata que CBCRASN2.
     *
     * @param idEmpresa Empresa contable — SIEMPRE derivada del lado del servidor (la cuenta
     *                  bancaria del cobro, vía {@code derivarEmpresaCobro}), nunca la que
     *                  mandaría un cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
     * @return Código del asiento creado, o null si la contabilidad no está activa o si el
     *         abono no generó ninguna diferencia de banda que reclasificar.
     */
    Long contabilizarAbonoCapital(EventoPrestamo evento, Long idEmpresa) throws Throwable;

    /**
     * Abono a capital 100% con aportes, llamada DIRECTA (sin cobro CBCR) — AA5/AA6, 2026-10-12.
     * Genera UN solo asiento que junta el cruce de aportes consumidos, el cierre de apertura por
     * {@code T − capitalFuturo} y el re-bandeo; se usa EN LUGAR de {@link #contabilizarAbonoCapital}
     * (que con cobro sigue haciendo solo el re-bandeo).
     *
     * @param movimientos     aportes consumidos por el abono (no vacío)
     * @param fechaAfectacion fecha contable del asiento; si es null se usa la fecha del evento
     * @return Código del asiento, o null si la contabilidad no está activa
     */
    Long contabilizarAbonoCapitalConAportes(EventoPrestamo evento, List<MovimientoAporte> movimientos,
            Long idEmpresa, java.time.LocalDate fechaAfectacion) throws Throwable;

    /**
     * Las dos líneas que cierran la apertura del lado préstamos por {@code monto} (D 2.3.02.10
     * aux 4 / H 1.4.05.10 aux 2, plantilla de apertura). Lo usa el asiento DEFINITIVO de CBCR
     * (AA10) para cerrar el tramo abierto que se pagó con aportes, con la misma resolución de
     * cuentas del camino directo.
     */
    java.util.List<com.saa.model.cnt.DetalleAsiento> lineasCierreApertura(Long idEmpresa, double monto,
            String descripcionOperacion) throws Throwable;

    /**
     * Cruce de valores de una precancelación con aportes CONSUMIDOS — SOLO cuando la llamada
     * es directa (2026-08-31, circuito de cobros con aportes, decisión del usuario). Si
     * {@code ctx.getIdCobroCredito()} viene con valor, la operación nació de
     * {@code CobroCreditoServiceImpl.procesarCobro}, que ya genera su propio asiento
     * (CBCRASN2) por la misma plata — este método debe devolver {@code null} sin generar nada.
     *
     * @param movimientos Aportes consumidos por la operación (puede venir vacío/null si la
     *                     precancelación fue 100% efectivo — ahí tampoco hay nada que cruzar)
     * @param ctx          Contexto de la operación; trae {@code idEmpresa} e
     *                     {@code idCobroCredito}
     * @return Código del asiento creado, o null si la contabilidad no está activa, si no hubo
     *         aportes consumidos, o si la llamada vino de CBCR.
     */
    Long contabilizarPrecancelacion(EventoPrestamo evento, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable;

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable;

    /**
     * Asiento de ENTREGA del préstamo, al aprobarlo (PLAN-DESEMBOLSO-PRESTAMO.md §5, paso 3):
     * D capital distribuido en las bandas por plazo de la tabla de amortización + D cuenta de
     * orden "cartera de créditos" → H cuentas de orden de garantía + H
     * {@code 2.3.90.90.10 SOCIOS POR PAGAR} — la cuenta puente que tesorería debita contra
     * bancos al confirmar el desembolso.
     *
     * <p>La plantilla se resuelve por la familia del producto ({@code Producto.tipoPrestamo
     * .nombre}): PRENDARIO → alterno {@link com.saa.rubros.PlantillasCredito#ENTREGA_PRENDARIO}
     * (9), HIPOTECARIO → {@link com.saa.rubros.PlantillasCredito#ENTREGA_HIPOTECARIO} (13),
     * QUIROGRAFARIO → {@link com.saa.rubros.PlantillasCredito#ENTREGA_QUIROGRAFARIO} (34).
     * <b>Cualquier otro producto se rechaza</b>: no se elige una plantilla por defecto.</p>
     *
     * @param prestamo       Préstamo recién aprobado (aún sin guardar el paso a VIGENTE)
     * @param cuotas         Tabla de amortización completa del préstamo (todas las cuotas)
     * @param idEmpresa      Empresa contable del asiento
     * @param montoOperacion Monto del préstamo (para el cuadre contra la suma de capital de
     *                       las cuotas)
     * @param usuario        Quien aprueba, se estampa en el asiento
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     * @throws Throwable {@code IncomeException} si el producto no tiene plantilla asignada, si
     *                    falta una línea en la plantilla, o si el asiento no cuadra
     */
    /**
     * Deduce el producto de pago (PGS.PRDP) del desembolso: la cuenta de la línea SOCIOS POR PAGAR
     * de la plantilla de entrega del préstamo (el mismo alterno y aux1 de {@link #contabilizarEntrega})
     * → los productos ACTIVOS de la empresa cuyo grupo tenga esa cuenta. Exactamente uno; cero o más
     * de uno lanza {@code IncomeException} con la cuenta y la lista (no se elige al azar).
     * DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md 6.2.
     *
     * @return id del producto de pago
     */
    Long resolverProductoPagoDesembolso(Prestamo prestamo, Long idEmpresa) throws Throwable;

    /**
     * Alterno de la plantilla contable de una reestructuración o novación (§4 del diseño), según la
     * familia del original, el tipo, si el original es novado/reestructurado y si tiene atraso.
     * Lanza {@code IncomeException} si no hay plantilla (novación de prendario o hipotecario).
     */
    int alternoSustitucion(Prestamo original, boolean esNovacion, boolean conAtraso) throws Throwable;

    /**
     * Igual que {@link #alternoSustitucion} pero NO lanza: devuelve el motivo, o {@code null} si hay
     * plantilla. Existe porque una {@code IncomeException} que cruza el proxy EJB marca la
     * transacción como rollback aunque se atrape.
     */
    String motivoSinPlantillaSustitucion(Prestamo original, boolean esNovacion, boolean conAtraso) throws Throwable;

    /**
     * Asiento del traslado de una reestructuración o novación, al aprobar el préstamo NUEVO (§3):
     * DEBE bandas del nuevo (rutina de la entrega) y HABER bandas del original (clasificación del
     * cobro a hoy: por vencer o vencida), más las líneas no-banda de la plantilla
     * ({@code lineasNoBandaSustitucion}, hoy pendiente del sql/326).
     *
     * @return código del asiento, o null si la contabilidad de CRD está inactiva
     */
    Long contabilizarSustitucion(Prestamo nuevo, List<DetallePrestamo> cuotasNuevo, Prestamo original,
            com.saa.ejb.crd.service.dto.DesgloseConceptosPrestamo desglose, boolean esNovacion, double neto,
            boolean conAtraso, Long idEmpresa, String usuario) throws Throwable;

    Long contabilizarEntrega(Prestamo prestamo, List<DetallePrestamo> cuotas, Long idEmpresa,
            double montoOperacion, String usuario) throws Throwable;
}
