package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.CandidatoPlazoVencido;
import com.saa.ejb.crd.service.dto.DeclaracionPlazoVencidoDTO;
import com.saa.ejb.crd.service.dto.EncabezadoPlazoVencido;
import com.saa.ejb.crd.service.dto.ResultadoDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.ResultadoRevertirPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudLiquidarPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudRevertirPlazoVencido;
import com.saa.model.crd.DeclaracionPlazoVencido;

import jakarta.ejb.Local;

/**
 * Pase de préstamos EN MORA a DE PLAZO VENCIDO — memorando (Crédito), liquidación contable
 * (Contabilidad) y reverso. Ver {@code docs/logica-negocio/crd/API-PASE-A-PLAZO-VENCIDO.md}.
 */
@Local
public interface DeclaracionPlazoVencidoService extends EntityService<DeclaracionPlazoVencido> {

    /** 400 - Falta un campo obligatorio, la fecha de corte es futura, o un id viene repetido en el lote */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 409 - El número de memorando ya existe (en el lote o en CRD.PLVN, incluidas las revertidas) */
    String ERR_MEMORANDO_DUPLICADO = "MEMORANDO_DUPLICADO";
    /** 409 - Al confirmar, el préstamo ya no está en 11 EN_MORA */
    String ERR_PRESTAMO_NO_EN_MORA = "PRESTAMO_NO_EN_MORA";
    /** 422 - Alguna de las cinco invariantes del cuadro no se cumple */
    String ERR_CALCULO_NO_CUADRA = "CALCULO_NO_CUADRA";
    /** 404 - No existe la declaración */
    String ERR_DECLARACION_NO_ENCONTRADA = "DECLARACION_NO_ENCONTRADA";
    /** 409 - La declaración ya está LIQUIDADA */
    String ERR_YA_LIQUIDADA = "YA_LIQUIDADA";
    /** 409 - La declaración ya está REVERTIDA (liquidar o revertir sobre ella de nuevo) */
    String ERR_DECLARACION_REVERTIDA = "DECLARACION_REVERTIDA";
    /** 409 - Al revertir, el préstamo ya no está en 8 DE_PLAZO_VENCIDO */
    String ERR_PRESTAMO_NO_EN_PLAZO_VENCIDO = "PRESTAMO_NO_EN_PLAZO_VENCIDO";
    /** 409 - Se pide el documento de liquidación de una declaración todavía en estado 1 DECLARADA (§8) */
    String ERR_NO_LIQUIDADA = "NO_LIQUIDADA";

    /**
     * Préstamos EN_MORA (11) con su cuadro calculado a {@code fechaCorte}. No graba nada (§3).
     *
     * @param fechaCorte obligatoria, no puede ser futura
     * @throws Throwable Si {@code fechaCorte} es inválida
     */
    List<CandidatoPlazoVencido> obtenerCandidatos(LocalDate fechaCorte) throws Throwable;

    /**
     * PARA/CC de la última declaración grabada, para precargar la pantalla del paso 1 (D21, §4).
     *
     * @throws Throwable Si ocurre un error
     */
    EncabezadoPlazoVencido obtenerUltimoEncabezado() throws Throwable;

    /**
     * Declara en plazo vencido TODOS los préstamos del lote, en una sola transacción (§5):
     * recalcula el cuadro de cada uno a {@code fechaCorte} (nunca usa montos recibidos), valida
     * las cinco invariantes, anula el seguro de las cuotas futuras y pasa el préstamo de 11 a 8.
     * Si un solo préstamo falla cualquier validación, no se declara ninguno.
     *
     * @throws Throwable {@link com.saa.basico.util.IncomeException} prefijada con el código
     *                    correspondiente si alguna validación falla
     */
    List<ResultadoDeclararPlazoVencido> declarar(SolicitudDeclararPlazoVencido solicitud) throws Throwable;

    /**
     * Liquidación contable de una declaración DECLARADA (§6): recalcula a
     * {@code solicitud.fechaCorte} las seis filas de la liquidación, las graba junto con las
     * cuotas impagas, y pasa la declaración a LIQUIDADA. Sin asiento (D17).
     *
     * @throws Throwable Si la declaración no existe, ya está liquidada o revertida, o la fecha es inválida
     */
    DeclaracionPlazoVencidoDTO liquidar(Long idDeclaracion, SolicitudLiquidarPlazoVencido solicitud) throws Throwable;

    /**
     * Revierte una declaración DECLARADA o LIQUIDADA (§7): devuelve el seguro a las cuotas que
     * sigan sin pagar, pasa el préstamo de 8 a 11 SIEMPRE, y marca la declaración REVERTIDA. No
     * borra nada. Sin asiento (D17).
     *
     * @throws Throwable Si la declaración no existe, ya está revertida, o el préstamo ya no está en 8
     */
    ResultadoRevertirPlazoVencido revertir(Long idDeclaracion, SolicitudRevertirPlazoVencido solicitud) throws Throwable;

    /**
     * Declaraciones filtradas (§9): bandeja de Contabilidad con {@code estado=1}, historial de
     * Crédito sin filtro.
     *
     * @throws Throwable Si ocurre un error
     */
    List<DeclaracionPlazoVencidoDTO> listar(Long estado, LocalDate desde, LocalDate hasta) throws Throwable;

    /**
     * Una declaración completa, como DTO (§9) — nunca la entidad JPA cruda.
     *
     * @throws Throwable Si no existe
     */
    DeclaracionPlazoVencidoDTO obtenerPorId(Long idDeclaracion) throws Throwable;
}
