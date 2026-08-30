package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.EstadoCuentaAportesDTO;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.model.crd.Aporte;

import jakarta.ejb.Local;

@Local
public interface AporteService extends EntityService<Aporte> {

    // ========================================================================
    // Códigos de error de negocio (prefijo del mensaje de IncomeException)
    // ========================================================================

    /** 400 - Falta un parámetro obligatorio o viene malformado */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 404 - La entidad (partícipe) no existe */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 422 - El tipo de aporte no existe o no está vigente */
    String ERR_TIPO_APORTE_NO_VIGENTE = "TIPO_APORTE_NO_VIGENTE";
    /** 422 - El valor recibido no es válido */
    String ERR_VALOR_INVALIDO = "VALOR_INVALIDO";
    /** 422 - La fecha recibida no es válida */
    String ERR_FECHA_INVALIDA = "FECHA_INVALIDA";

    /**
     * Registra un pago de aportes recibido en ventanilla: genera para el partícipe un aporte
     * YA PAGADO del tipo indicado, en UNA transacción.
     *
     * Crea una fila POSITIVA en CRD.APRT con {@code valor = valorPagado = X} y
     * {@code saldo = 0}, estado PAGADA(4), más su {@code PagoAporte}. En el modelo vigente
     * (Fase 1 del plan de devengo de aportes, D1) toda fila nace pagada por construcción: no
     * hay abono posterior ni saldo pendiente que un proceso distinto pueda volver a tocar.
     *
     * El saldo disponible del partícipe sube de inmediato, porque el saldo ES la suma neta de
     * {@code APRTVLRR} (los consumos son filas negativas). Es la operación espejo del pago de
     * préstamo con aportes.
     *
     * @param solicitud Entidad, tipo de aporte, valor, usuario, observación y fecha
     * @return Datos del aporte creado y el saldo del tipo tras el registro
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Ante cualquier fallo de validación (revierte todo)
     */
    ResultadoRegistroAporte registrarAporte(SolicitudRegistroAporte solicitud) throws Throwable;

    /** G42 Grupo 1 — Rendimiento: SUM por entidad donde tipoAporte.estado=1 y codigoSBS='RE', fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaRendimientoPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G42 Grupo 2 — Patronal: SUM por entidad donde tipoAporte.estado=1 y codigo IN (3,13,14), fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaPatronalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G42 Grupo 3 — Personal: SUM por entidad donde tipoAporte.estado=1, excluyendo grupos 1 y 2, fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaPersonalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 — Imposiciones acumuladas: COUNT de aportes con tipoAporte.codigo IN (9, 11), fechaTransaccion <= fechaCorte */
    List<Object[]> selectCountImposicionesJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 — Saldo de cuenta: SUM del campo valor de aportes con tipoAporte.codigo = 23, fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaSaldoCuentaJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 ex-jubilados — SUM de aportes tipo 23 en el rango del mes (BETWEEN fechaInicio AND fechaFin) */
    List<Object[]> selectSumaAportesTipo23EnRango(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

    /** G42 — Tipo de prestación: Obtiene los códigos de tipoAporte distintos (9, 11) por entidad, fechaTransaccion <= fechaCorte */
    List<Object[]> selectTiposAportePorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G40 — Suma total global de aportes con tipoAporte.codigo = tipoAporte, fechaTransaccion <= fechaCorte */
    Double selectSumaTotalPorTipoAporte(LocalDateTime fechaCorte, Long tipoAporte) throws Throwable;

    /**
     * CPRM — Suma de aportes agrupada por entidad Y tipo de aporte hasta fechaCorte.
     * Retorna Object[]{Long codigoEntidad, Long codigoTipoAporte, String nombreTipoAporte, Double suma}.
     */
    List<Object[]> selectSumaPorEntidadYTipoAporte(LocalDateTime fechaCorte) throws Throwable;

    /**
     * G43 — Imposiciones personales: tipoAporte.codigo IN (9, 11), valor &gt; 0, para una
     * entidad.
     *
     * <p><b>Cambio del 2026-08-27 (decisión del usuario, mismo criterio que G44):</b> cuenta
     * MESES DE DEVENGO distintos ({@code COUNT(DISTINCT} periodo efectivo{@code )}), no
     * filas. "Imposiciones" significa meses aportados — bajo el modelo de devengo filas ≠
     * meses: un partícipe puede tener varias filas del mismo mes (pago parcial completado
     * después, anticipos, ajustes) y una sola fila puede cubrir un mes distinto al de su
     * fecha de caja. Ver {@code PeriodoEfectivoAporteSql}.</p>
     */
    Long selectCountImposicionesPersonalesPorEntidad(Long codigoEntidad) throws Throwable;

    /**
     * G43 — Imposiciones patronales: tipoAporte.codigo IN (13, 14), valor &gt; 0, para una
     * entidad.
     *
     * <p>Mismo cambio del 2026-08-27 que {@link #selectCountImposicionesPersonalesPorEntidad}:
     * cuenta meses de devengo distintos, no filas.</p>
     */
    Long selectCountImposicionesPatronalesPorEntidad(Long codigoEntidad) throws Throwable;

    /**
     * G43 — SUM de aportes con valor < 0, tipoAporte.estado = 1,
     * dentro del mes de ejecucion para una entidad.
     * Retorna Double suma (negativa), o 0.0 si no hay registros.
     */
    Double selectSumaAportesNegativosMesPorEntidad(Long codigoEntidad,
            java.time.LocalDateTime fechaInicio,
            java.time.LocalDateTime fechaFin) throws Throwable;

    /**
     * Estado de cuenta de aportes por devengo (§4.2 del plan de devengo de aportes).
     * Agrupa por PERIODO EFECTIVO (ver {@code PeriodoEfectivoAporteSql}) y tipo de aporte;
     * "esperado" sale de {@link VigenciaContratoService#esperadoPorEntidad}. Los movimientos
     * sin periodo efectivo (histórico sin backfillear, retiros de saldo) van en un grupo con
     * {@code periodo = null} y {@code estado = "SIN PERIODO"} — nunca se esconden.
     *
     * <p>Una entidad sin ningún movimiento en el rango devuelve {@code periodos} vacío, NO
     * lanza (pedido 1: "sin aportes" no es un error).</p>
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @param desde     Primer día del mes de devengo, inclusive
     * @param hasta     Primer día del mes de devengo, inclusive
     * @return Estado de cuenta
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException {@link #ERR_ENTIDAD_NO_ENCONTRADA} si la
     *                                             entidad no existe
     */
    EstadoCuentaAportesDTO estadoCuenta(Long idEntidad, LocalDate desde, LocalDate hasta) throws Throwable;
}
