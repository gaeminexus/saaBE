/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.ConciliacionContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service ConciliacionContable.
 */
@Local
public interface ConciliacionContableDaoService extends EntityDao<ConciliacionContable> {

    /**
     * Busca la cabecera de conciliación contable para una cuenta bancaria y
     * período dados. Null si aún no se ha generado (ver
     * ConciliacionContableService.obtenerOCrear).
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : ConciliacionContable si existe, null si no
     * @throws Throwable       : Excepcion
     */
    ConciliacionContable selectByCuentaYPeriodo(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

}
