package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import jakarta.ejb.Local;

@Local
public interface CreditoCuotasPrestamosMensualService extends EntityService<CreditoCuotasPrestamosMensual> {

    /**
     * Retorna todos los registros CCPM que pertenecen a una ejecución específica.
     * @param codigoEjecucion Código de la EjecucionReporteCartera
     * @return Lista de CreditoCuotasPrestamosMensual
     */
    List<CreditoCuotasPrestamosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;

    /**
     * Retorna un registro CCPM por ejecución y número de operación.
     */
    CreditoCuotasPrestamosMensual selectByEjecucionYOperacion(Long codigoEjecucion, String numeroOperacion) throws Throwable;
}
