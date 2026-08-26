/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.BandaCierreCartera;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad BandaCierreCartera (CRD.BDCC): el snapshot de la
 *         distribución de capital por banda de cada corrida.
 */
@Local
public interface BandaCierreCarteraDaoService extends EntityDao<BandaCierreCartera> {

    /**
     * Snapshot completo de una corrida, ordenado por producto, tipo de cartera y número de
     * banda.
     *
     * @param idCorrida  : Código de la corrida (CRD.CRCT)
     * @return           : Listado; VACÍO si la corrida no llegó a grabar el snapshot
     * @throws Throwable : Excepcion
     */
    List<BandaCierreCartera> selectByCorrida(Long idCorrida) throws Throwable;

    /**
     * Elimina el snapshot de una corrida. Solo se usa al recalcular una corrida PREPARADA
     * que todavía no generó asientos; una corrida EJECUTADA nunca pierde su snapshot.
     *
     * @param idCorrida  : Código de la corrida (CRD.CRCT)
     * @return           : Número de filas eliminadas
     * @throws Throwable : Excepcion
     */
    int deleteByCorrida(Long idCorrida) throws Throwable;
}
