/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.ControlExtractoBancario;

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

}
