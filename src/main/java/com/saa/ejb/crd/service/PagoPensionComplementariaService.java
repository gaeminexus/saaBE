package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.DetallePagoPension;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionPagosPension;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.model.crd.PagoPensionComplementaria;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA
 *         Pago MENSUAL de pensión complementaria a los jubilados (ítem 4 de jubilados,
 *         2026-08-31 — LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md §4.b, J5: "las pensiones
 *         mensuales se descuentan de la pensión complementaria"). Proceso, no tabla de valores:
 *         {@code ValorPagoPensionComplementaria} (VPPC) sigue siendo solo la parametrización de
 *         cuánto le corresponde a cada jubilado; este servicio es el que efectivamente genera
 *         el pago del mes, mueve el saldo y lo integra con tesorería.
 *
 *         Mismo circuito que {@code DevolucionAporteService}: dinero saliendo hacia un tercero
 *         vía CXP, con el mismo reconciliador async (CXP no puede nombrar a CRD, así que CRD
 *         lee su estado, no al revés).
 */
@Local
public interface PagoPensionComplementariaService extends EntityService<PagoPensionComplementaria> {

    /** 404 - No existe el jubilado */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 422 - El jubilado no tiene una configuración VPPC activa, o tiene más de una */
    String ERR_SIN_VALOR_PENSION = "SIN_VALOR_PENSION";
    /** 422 - El saldo de pensión complementaria (tipo 23) no alcanza para el pago del mes */
    String ERR_SALDO_INSUFICIENTE = "SALDO_INSUFICIENTE";
    /** 422 - El jubilado no tiene exactamente una cuenta bancaria activa */
    String ERR_SIN_CUENTA_BANCARIA = "SIN_CUENTA_BANCARIA";
    /** 404 - No existe el pago */
    String ERR_PAGO_NO_ENCONTRADO = "PAGO_NO_ENCONTRADO";

    /**
     * Genera los pagos del período para TODOS los jubilados JUBILADO_COMPLEMENTARIO con VPPC
     * activo. Un jubilado con datos malos (sin VPPC, sin cuenta bancaria, saldo insuficiente)
     * NO aborta el lote — se cuenta como error y el resto sigue (cada jubilado en su propia
     * transacción, {@code REQUIRES_NEW}, mismo criterio que
     * {@code DevolucionAporteService#sincronizarPagos}).
     *
     * <b>Idempotente por diseño</b>: si ya existe un PGPC para (entidad, año, mes) —
     * {@code UK_PGPC_ENTD_ANIO_MES} en la base, no solo un chequeo de Java— ese jubilado se
     * cuenta como "ya generado", no se duplica el pago. Correr esto dos veces sobre el mismo
     * mes es seguro.
     *
     * @param idEmpresa Empresa contable sobre la que se genera la orden de pago. Obligatorio
     *                  (Fase 0, contrato API-EMPRESA-CONTABLE-CRD.md — el mismo criterio: nunca
     *                  se infiere, viaja explícito)
     * @param anio      Año del período a generar
     * @param mes       Mes del período a generar (1-12)
     * @param usuario   Usuario/proceso que dispara la generación
     * @return Resumen: evaluados, generados, ya generados, con error (y el detalle de cada error)
     * @throws Throwable Si ocurre un error
     */
    ResultadoGeneracionPagosPension generarPagosDelMes(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable;

    /**
     * Genera (o confirma que ya existe) el pago de UN jubilado para un período. Es lo que
     * {@link #generarPagosDelMes} llama por cada uno, a través del proxy EJB para que corra en
     * su propia transacción — igual que {@code DevolucionAporteService#sincronizarDevolucion}.
     * Expuesto también para regenerar un jubilado puntual sin correr el lote completo.
     *
     * @return el detalle del jubilado — {@code estado = "YA_EXISTIA"} si el período ya existía
     *         (idempotencia, no es error), {@code "GENERADO"} si creó un PGPC nuevo
     * @throws Throwable {@code IncomeException} con el motivo si el jubilado no se puede pagar
     *                    este período (sin VPPC, sin cuenta, sin saldo)
     */
    DetallePagoPension generarPagoIndividual(Long idEntidad, Long idEmpresa, Integer anio, Integer mes, String usuario)
            throws Throwable;

    /**
     * Reconciliador: lee el estado real de la orden de pago en CXP de cada PGPC pendiente
     * (REGISTRADA o EN_PAGO) y actualiza PGPC en consecuencia — PAGADA si se confirmó,
     * RECHAZADA (con el contra-movimiento positivo en APRT) si se rechazó o reversó. Mismo
     * patrón que {@code DevolucionAporteService#sincronizarPagos}: CXP no avisa, CRD consulta.
     *
     * @return Resumen de la corrida
     * @throws Throwable Si ocurre un error
     */
    ResultadoSincronizacion sincronizarPagos() throws Throwable;

    /**
     * Reconcilia UN pago puntual. Expuesto para que {@link #sincronizarPagos} invoque cada uno
     * en su propia transacción ({@code REQUIRES_NEW}) a través del proxy EJB.
     *
     * @throws Throwable Si ocurre un error
     */
    ResultadoSincronizacion sincronizarPago(Long idPago) throws Throwable;

    /**
     * Historial de pagos de un jubilado, del más reciente al más antiguo.
     *
     * @throws Throwable {@code IncomeException} {@link #ERR_ENTIDAD_NO_ENCONTRADA} si no existe
     */
    List<PagoPensionComplementaria> listarPorEntidad(Long idEntidad) throws Throwable;

    /**
     * Todos los pagos de un período — el informe mensual completo. Existe porque
     * {@link #generarPagosDelMes} no puede reconstruirlo en una segunda corrida: su rama
     * YA_EXISTIA sólo arma un renglón liviano de cinco campos (contrato REST §4).
     *
     * <b>Un período sin pagos es una respuesta válida</b>: devuelve lista vacía, NO lanza
     * {@code IncomeException}. A propósito — no copiar acá el patrón de
     * "lista vacía = error" que usa el resto del repositorio.
     *
     * @throws Throwable Si ocurre un error
     */
    List<PagoPensionComplementaria> listarPorPeriodo(Integer anio, Integer mes) throws Throwable;
}
