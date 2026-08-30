package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.HistoricoPagoCuentaIndividual;

import jakarta.ejb.Local;

/**
 * Histórico de pagos de cuenta individual (CRD.HPCS). Módulo de solo lectura: las
 * operaciones de escritura heredadas de {@link EntityService} están bloqueadas.
 */
@Local
public interface HistoricoPagoCuentaIndividualService extends EntityService<HistoricoPagoCuentaIndividual> {

    /**
     * Pagos de un partícipe, del más reciente al más antiguo. Una lista vacía NO es error.
     * @param cedula Cédula del partícipe
     * @return Listado de pagos
     * @throws Throwable Si ocurre un error
     */
    List<HistoricoPagoCuentaIndividual> selectByCedula(String cedula) throws Throwable;
}
