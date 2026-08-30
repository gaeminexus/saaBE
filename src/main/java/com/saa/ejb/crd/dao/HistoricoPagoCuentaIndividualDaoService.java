package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.HistoricoPagoCuentaIndividual;

import jakarta.ejb.Local;

/**
 * Lectura del histórico de pagos de cuenta individual (CRD.HPCS). Solo consultas: la
 * tabla la alimentan los procesos de liquidación, no este backend.
 */
@Local
public interface HistoricoPagoCuentaIndividualDaoService extends EntityDao<HistoricoPagoCuentaIndividual> {

    /**
     * Pagos de un partícipe, del más reciente al más antiguo.
     * @param cedula     : Cédula del partícipe (CRD.ENTD.ENTDNMID)
     * @return           : Listado de pagos; lista VACÍA si no tiene ninguno
     * @throws Throwable : Excepción
     */
    List<HistoricoPagoCuentaIndividual> selectByCedula(String cedula) throws Throwable;

    /**
     * Pagos de un partícipe de ciertos tipos, del más reciente al más antiguo.
     * La comparación del tipo es insensible a mayúsculas (la base tiene "crv" y "CRV").
     * @param cedula     : Cédula del partícipe (CRD.ENTD.ENTDNMID)
     * @param tipos      : Tipos de pago (constantes {@code HistoricoPagoCuentaIndividual.TIPO_*})
     * @return           : Listado de pagos; lista VACÍA si no tiene ninguno
     * @throws Throwable : Excepción
     */
    List<HistoricoPagoCuentaIndividual> selectByCedulaAndTipos(String cedula, List<String> tipos) throws Throwable;
}
