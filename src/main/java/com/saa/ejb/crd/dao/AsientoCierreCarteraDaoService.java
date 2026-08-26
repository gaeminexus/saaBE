/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.AsientoCierreCartera;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad AsientoCierreCartera (CRD.ANCC): el asiento que
 *         generó cada sub-proceso de una corrida.
 */
@Local
public interface AsientoCierreCarteraDaoService extends EntityDao<AsientoCierreCartera> {

    /**
     * Asientos de una corrida, ordenados por sub-proceso — que es el orden de ejecución.
     *
     * @param idCorrida  : Código de la corrida (CRD.CRCT)
     * @return           : Listado; VACÍO si la corrida está PREPARADA
     * @throws Throwable : Excepcion
     */
    List<AsientoCierreCartera> selectByCorrida(Long idCorrida) throws Throwable;

    /**
     * Asientos GENERADOS (no anulados) de una corrida. Es lo que recorre el reverso.
     *
     * @param idCorrida  : Código de la corrida (CRD.CRCT)
     * @return           : Listado; VACÍO si ya se reversó o si no hay asientos
     * @throws Throwable : Excepcion
     */
    List<AsientoCierreCartera> selectGeneradosByCorrida(Long idCorrida) throws Throwable;
}
