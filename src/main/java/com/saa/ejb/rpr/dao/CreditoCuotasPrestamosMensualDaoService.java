package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import jakarta.ejb.Local;

@Local
public interface CreditoCuotasPrestamosMensualDaoService extends EntityDao<CreditoCuotasPrestamosMensual> {

    /** Retorna todos los registros CCPM de una ejecución específica. */
    List<CreditoCuotasPrestamosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;

    /** Retorna el registro CCPM de una ejecución específica filtrado por numeroOperacion. */
    CreditoCuotasPrestamosMensual selectByEjecucionYOperacion(Long codigoEjecucion, String numeroOperacion) throws Throwable;

    /** Cuenta los registros CCPM de una ejecución específica. */
    long countByEjecucion(Long codigoEjecucion) throws Throwable;

    /** Elimina todos los registros CCPM de una ejecución específica. */
    int deleteByEjecucion(Long codigoEjecucion) throws Throwable;

    /**
     * Retorna un mapa de numeroOperacion → interesMora para todos los registros
     * de una ejecución específica. Se usa para calcular el delta de mora entre ejecuciones.
     * @return Lista de Object[]{String numeroOperacion, Double interesMora}
     */
    List<Object[]> selectInteresMoraPorEjecucion(Long codigoEjecucion) throws Throwable;
}
