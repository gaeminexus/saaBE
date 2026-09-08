package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.DetallePagoPension;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionPagosPension;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionSeguroMedico;
import com.saa.ejb.crd.service.dto.ResultadoPrevisualizacionCorrida;
import com.saa.ejb.crd.service.dto.ResultadoSeguimientoCorridaJubilados;
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
    /**
     * 422 - La cuenta bancaria activa del jubilado no tiene certificado bancario cargado.
     * Decisión del usuario, 2026-09-04 (API-PAGO-PENSION-COMPLEMENTARIA.md §6). Problema DE LA
     * ENTIDAD: falta el documento, hay que pedirlo.
     */
    String ERR_SIN_CERTIFICADO_BANCARIO = "SIN_CERTIFICADO_BANCARIO";
    /**
     * 422 - NO se pudo verificar el certificado bancario porque el catálogo CRD.TPDJ no
     * resuelve 'CERTIFICADO BANCARIO' ({@code CuentaBancariaParticipeService.ERR_TIPO_ADJUNTO_NO_CONFIGURADO}).
     * Problema DEL SISTEMA, afecta a todos los jubilados por igual — NUNCA se confunde con
     * {@link #ERR_SIN_CERTIFICADO_BANCARIO}, que es "falta el documento de este jubilado".
     */
    String ERR_CERTIFICADO_NO_VERIFICABLE = "TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO";
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
     * @deprecated 2026-09-07 (API-DOS-PROCESOS-MENSUALES-JUBILADOS.md): reemplazado por DOS
     *             procesos separados en el tiempo — {@link #generarSeguroDelMes} al inicio del
     *             mes y {@link #generarPensionesDelMes} al final — porque el seguro médico se
     *             descuenta de la pensión pero se paga a un proveedor aparte, y eso exige que
     *             el valor del seguro se fije ANTES y la pensión lo descuente sin recalcular.
     *             No se borra: sigue funcionando exactamente igual que siempre (un solo acto,
     *             seguro y pensión juntos) para no dejar sin salida a quien todavía lo llame.
     */
    @Deprecated
    ResultadoGeneracionPagosPension generarPagosDelMes(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable;

    /**
     * Genera (o confirma que ya existe) el pago de UN jubilado para un período. Es lo que
     * {@link #generarPagosDelMes} llama por cada uno, a través del proxy EJB para que corra en
     * su propia transacción — igual que {@code DevolucionAporteService#sincronizarDevolucion}.
     * Expuesto también para regenerar un jubilado puntual sin correr el lote completo.
     *
     * @param idUsuario Código del usuario ({@code com.saa.model.scp.Usuario.codigo}, tabla
     *                   SCP.PJRQ) que registra, ya resuelto por el llamador a partir de
     *                   {@code usuario}. Lo pide
     *                   {@code PagoProgramadoService.registrarPagoDeOrigenExterno}; pasarlo en
     *                   {@code null} hace que CXP reviente con {@code em.find(Class, null)}.
     * @return el detalle del jubilado — {@code estado = "YA_EXISTIA"} si el período ya existía
     *         (idempotencia, no es error), {@code "GENERADO"} si creó un PGPC nuevo
     * @throws Throwable {@code IncomeException} con el motivo si el jubilado no se puede pagar
     *                    este período (sin VPPC, sin cuenta, sin saldo)
     */
    DetallePagoPension generarPagoIndividual(Long idEntidad, Long idEmpresa, Integer anio, Integer mes,
            String usuario, Long idUsuario) throws Throwable;

    /**
     * PENSIONES (fin de mes) — mismo comportamiento que {@link #generarPagoIndividual}, pero
     * NO recalcula el seguro: lee {@code PGPCVLSG} de la fila que dejó
     * {@link #generarSeguroIndividual} para este mismo (entidad, año, mes) y la completa (D1,
     * API-DOS-PROCESOS-MENSUALES-JUBILADOS.md). Un mes sin esa fila (el jubilado entró al
     * padrón después del proceso de seguro) genera la pensión SIN descuento de seguro —
     * {@link DetallePagoPension#isSinSeguroDelPeriodo()} queda en {@code true} — nunca
     * bloqueado ni con un seguro inventado.
     *
     * @throws Throwable {@code IncomeException} con el motivo si el jubilado no se puede pagar
     *                    este período (sin VPPC, sin cuenta, sin saldo)
     */
    DetallePagoPension generarPensionIndividual(Long idEntidad, Long idEmpresa, Integer anio, Integer mes,
            String usuario, Long idUsuario) throws Throwable;

    /**
     * SEGURO MÉDICO (inicio de mes, §4.1 de API-DOS-PROCESOS-MENSUALES-JUBILADOS.md). Recorre
     * el padrón de jubilados VIGENTE a la fecha de ejecución (D4) y, para cada uno, calcula su
     * seguro desde VPPC y fija {@code PGPCVLSG} en la fila del período — creándola si no
     * existe, sin tocar pensión, cruce, orden individual ni devengo (eso lo hace
     * {@link #generarPensionesDelMes}). Al final genera UNA orden agregada al proveedor por el
     * total del período y sella la cabecera {@code CRD.CRJB}.
     *
     * <b>Guarda de idempotencia por CABECERA</b> ({@code CRJBESSG = 1}), no solo por fila:
     * rechaza explícitamente una segunda corrida del mismo período, nombrando fecha y usuario
     * de la primera.
     *
     * @param idEmpresa Empresa contable sobre la que se genera la orden al proveedor. Obligatorio
     * @param anio      Año del período
     * @param mes       Mes del período (1-12)
     * @param usuario   Usuario/proceso que dispara la generación
     * @return Resumen — forma canónica {@code {jubilados, total, idOrdenPago, mensaje}}, más
     *         diagnóstico adicional
     * @throws Throwable {@code IncomeException} si el seguro de ese período ya se generó, o si
     *                    falla algún guard de precondición (proveedor, cuenta bancaria, producto
     *                    de pago del seguro)
     */
    ResultadoGeneracionSeguroMedico generarSeguroDelMes(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable;

    /**
     * Fija (o completa) {@code PGPCVLSG} de UN jubilado para un período — lo que
     * {@link #generarSeguroDelMes} llama por cada uno, a través del proxy EJB para su propia
     * transacción ({@code REQUIRES_NEW}), mismo criterio que {@link #generarPagoIndividual}.
     *
     * @return el valor de seguro fijado en esta llamada, o {@code null} si la fila del período
     *         ya tenía el seguro fijado (idempotencia, no es error)
     * @throws Throwable {@code IncomeException} si el jubilado no tiene VPPC activa
     */
    Double generarSeguroIndividual(Long idEntidad, Integer anio, Integer mes, String usuario) throws Throwable;

    /**
     * PENSIONES (fin de mes, §4.2 de API-DOS-PROCESOS-MENSUALES-JUBILADOS.md). Es
     * {@link #generarPagosDelMes} de hoy MENOS el seguro (lo lee, no lo recalcula — D1) MÁS la
     * guarda D2: rechaza de entrada si el seguro del período no está generado en
     * {@code CRD.CRJB} — sin generar ni una orden. NO genera ninguna orden al proveedor (eso ya
     * lo hizo {@link #generarSeguroDelMes}). Sella {@code CRJBESPN = 1} en la cabecera.
     *
     * @throws Throwable {@code IncomeException} con el mensaje de D2 si el seguro del período
     *                    no se generó todavía, o si las pensiones de ese período ya se generaron
     */
    ResultadoGeneracionPagosPension generarPensionesDelMes(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable;

    /**
     * Seguimiento de la corrida de un período — §4.3 del contrato. SIEMPRE 200 (con los dos
     * estados en 0 si el período nunca se corrió): "este mes no se corrió nada" es una
     * respuesta válida, nunca un error. {@code nombreEstado} y los dos {@code puedeGenerar*}
     * los calcula el backend con la MISMA regla que aplican los dos endpoints de generación.
     *
     * @throws Throwable Si ocurre un error inesperado (no si el período no existe — eso es 200)
     */
    ResultadoSeguimientoCorridaJubilados obtenerSeguimientoCorrida(Long idEmpresa, Integer anio, Integer mes)
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

    /**
     * Previsualización de {@link #generarPagosDelMes} — MISMOS parámetros, MISMA regla de
     * decisión, CERO escritura: ni PGPC, ni movimientos de APRT, ni asientos, ni órdenes en
     * CXP. API-PAGO-PENSION-COMPLEMENTARIA.md §4bis.
     *
     * <b>La garantía de que no escribe es lo único que hace útil este endpoint</b>: el
     * operador tiene que poder apretarlo tantas veces como quiera, sin miedo. Nunca llama a
     * {@code pagarConAportes} — sólo calcula el mismo tope ({@code min(pensiones acumuladas,
     * deuda exigible a la fecha de corrida, saldo del aporte 23)}) que usa la corrida real,
     * reusando en la implementación los mismos helpers privados que resuelven el ancla y la
     * deuda exigible por préstamo, para no duplicar esa lógica.
     *
     * ⚠️ <b>{@code montoACruzar} es una ESTIMACIÓN.</b> El motor calcula mora e interés al
     * aplicar de verdad, y esa parte NO se simula acá — sería una cuarta copia de esa
     * matemática. El monto real de la corrida puede diferir.
     *
     * @throws Throwable Si ocurre un error
     */
    ResultadoPrevisualizacionCorrida previsualizarCorrida(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable;
}
