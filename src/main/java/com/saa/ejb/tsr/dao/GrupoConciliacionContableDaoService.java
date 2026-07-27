/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.GrupoConciliacionContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service GrupoConciliacionContable.
 */
@Local
public interface GrupoConciliacionContableDaoService extends EntityDao<GrupoConciliacionContable> {

    /**
     * Recupera los grupos ACTIVOS de una cabecera de conciliación contable,
     * para mostrar en pantalla lo ya conciliado.
     * @param idConciliacionContable : Id de la cabecera
     * @return                       : Grupos activos
     * @throws Throwable             : Excepcion
     */
    List<GrupoConciliacionContable> selectActivosByConciliacion(Long idConciliacionContable) throws Throwable;

}
