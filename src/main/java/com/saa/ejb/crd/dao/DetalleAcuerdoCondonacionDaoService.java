/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleAcuerdoCondonacion;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad DetalleAcuerdoCondonacion (CRD.DACC): las 5 filas por
 *         concepto de un {@code AcuerdoCondonacion}.
 */
@Local
public interface DetalleAcuerdoCondonacionDaoService extends EntityDao<DetalleAcuerdoCondonacion> {

    /**
     * Las 5 líneas de un acuerdo, ordenadas por concepto.
     *
     * @param idAcuerdo  : Código del acuerdo (CRD.ACCN)
     * @return           : Listado; nunca vacío para un acuerdo existente
     * @throws Throwable : Excepcion
     */
    List<DetalleAcuerdoCondonacion> selectByAcuerdo(Long idAcuerdo) throws Throwable;
}
