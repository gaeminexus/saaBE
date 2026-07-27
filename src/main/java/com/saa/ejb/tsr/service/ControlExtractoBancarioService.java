/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.model.tsr.DetalleCumplimientoCuenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ControlExtractoBancario.
 * Accede a los métodos DAO y procesa los datos para el cliente.
 * Ademas de las operaciones CRUD estandar, expone las dos acciones que
 * el frontend dispara explicitamente para este tablero de cumplimiento:
 * generar (sembrar el periodo si no existe) y recalcular (actualizar
 * los contadores de un periodo ya generado).</p>
 */
@Local
public interface ControlExtractoBancarioService extends EntityService<ControlExtractoBancario> {

    /**
     * Genera el registro de control para una empresa/periodo si todavia no existe.
     * Si ya existe, lo devuelve sin modificar totalCuentas (el total queda fijo
     * al momento en que se genero por primera vez, ver ControlExtractoBancario.totalCuentas).
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable (CNT.PRDO)
     * @return          : ControlExtractoBancario generado o ya existente
     * @throws Throwable : Excepcion
     */
    ControlExtractoBancario generarPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable;

    /**
     * Recalcula cuantasCargadas y cuantasConciliadas de un registro de control
     * ya existente, contra el estado actual de CuentaBancaria/ExtractoBancario.
     * Requiere que generarPeriodo se haya llamado antes para esa empresa/periodo.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable (CNT.PRDO)
     * @return          : ControlExtractoBancario con los contadores actualizados
     * @throws Throwable : Excepcion, incluye el caso en que el periodo no ha sido generado todavia
     */
    ControlExtractoBancario recalcularPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable;

    /**
     * Detalle por cuenta bancaria (drill-down) para un empresa/periodo: cuales
     * cuentas ya cargaron su extracto y cuales ya estan conciliadas, para que
     * el tablero muestre exactamente que cuentas faltan en vez de solo un
     * porcentaje agregado.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable (CNT.PRDO)
     * @return          : Una fila por cuenta bancaria activa de la empresa
     * @throws Throwable : Excepcion
     */
    List<DetalleCumplimientoCuenta> detalleCuentas(Long idEmpresa, Long idPeriodo) throws Throwable;

    /**
     * Indica si un período ya fue cerrado para conciliación bancaria (cierre
     * exclusivo de TSR, ver javadoc de ControlExtractoBancario.cerrado). Si
     * el registro de control ni siquiera existe todavía (nunca se generó ni
     * se cerró), se considera abierto - ausencia de registro nunca implica
     * cierre.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable
     * @return          : true si está cerrado
     * @throws Throwable : Excepcion
     */
    boolean estaCerrado(Long idEmpresa, Long idPeriodo) throws Throwable;

    /**
     * Cierra un período para conciliación bancaria (genera el registro de
     * control si todavía no existe). Es responsabilidad del llamador (ver
     * ConciliacionContableMatchService.cerrarMes) validar que todas las
     * cuentas estén verificadas antes de invocar esto - este método no repite
     * esa validación.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable
     * @param usuario   : Usuario que cierra (auditoria)
     * @return          : ControlExtractoBancario actualizado
     * @throws Throwable : Excepcion (si ya estaba cerrado)
     */
    ControlExtractoBancario cerrarPeriodo(Long idEmpresa, Long idPeriodo, String usuario) throws Throwable;

    /**
     * Reabre un período previamente cerrado para conciliación bancaria. Sin
     * validaciones adicionales - reabrir siempre debe ser posible mientras
     * esté cerrado.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable
     * @return          : ControlExtractoBancario actualizado
     * @throws Throwable : Excepcion (si no estaba cerrado)
     */
    ControlExtractoBancario reabrirPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable;

    /**
     * Ids de los períodos contables ya cerrados (para conciliación bancaria)
     * de una empresa - usado por las pantallas de TSR para deshabilitar esos
     * períodos en sus selectores, sin depender de Periodo.estado (que es un
     * concepto de CNT, independiente de este cierre).
     * @param idEmpresa : Id de la empresa
     * @return          : Ids de Periodo cerrados
     * @throws Throwable : Excepcion
     */
    List<Long> selectPeriodosCerrados(Long idEmpresa) throws Throwable;

}
