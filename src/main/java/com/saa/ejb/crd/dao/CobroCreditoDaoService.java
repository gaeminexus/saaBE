/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CobroCredito;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad CobroCredito (CRD.CBCR): cabecera del cobro
 *         pendiente de autorización de contabilidad.
 */
@Local
public interface CobroCreditoDaoService extends EntityDao<CobroCredito> {

    /**
     * Bandeja de un estado puntual, para las pantallas de crédito y de contabilidad.
     *
     * @param estado     : {@link com.saa.rubros.CrdEstadoCobro}
     * @return           : Listado ordenado por fecha de registro ascendente (FIFO);
     *                     VACÍO si no hay ninguno en ese estado
     * @throws Throwable : Excepcion
     */
    List<CobroCredito> selectByEstado(Long estado) throws Throwable;

    /**
     * Los cobros de una entidad (partícipe), para su ficha.
     *
     * @param idEntidad  : Código de la entidad (CRD.ENTD)
     * @return           : Listado; VACÍO si no tiene ningún cobro registrado
     * @throws Throwable : Excepcion
     */
    List<CobroCredito> selectByEntidad(Long idEntidad) throws Throwable;
}
