/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.AsientoCargaPetro;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad AsientoCargaPetro (CRD.ANCP): el asiento que generó
 *         cada sub-proceso contable de una carga Petro. Espejo de
 *         {@code AsientoCierreCarteraDaoService}.
 */
@Local
public interface AsientoCargaPetroDaoService extends EntityDao<AsientoCargaPetro> {

    /**
     * Asientos de una carga, ordenados por sub-proceso (que es el orden de ejecución:
     * transitorio, reparto, aplicación).
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Listado; VACÍO si todavía no se contabilizó nada
     * @throws Throwable : Excepcion
     */
    List<AsientoCargaPetro> selectByCarga(Long idCarga) throws Throwable;

    /**
     * El asiento VIGENTE (no reversado) de una carga para un sub-proceso puntual. Como
     * mucho hay uno por {@code (carga, subProceso)} — lo garantiza {@code UK_ANCP_VIGENTE}.
     *
     * @param idCarga     : Código de la carga (CRD.CRAR)
     * @param subProceso  : {@link com.saa.rubros.SubProcesoCobroPetro}
     * @return            : El asiento vigente, o {@code null} si no existe
     * @throws Throwable  : Excepcion
     */
    AsientoCargaPetro selectVigenteByCargaYSubProceso(Long idCarga, int subProceso) throws Throwable;
}
