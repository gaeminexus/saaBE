package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CuotaXParticipeGeneracion;

import jakarta.ejb.Local;

/**
 * Interface DAO para CuotaXParticipeGeneracion (CXPG).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface CuotaXParticipeGeneracionDaoService extends EntityDao<CuotaXParticipeGeneracion> {

    /**
     * Busca todas las cuotas de un partícipe detalle generación.
     * 
     * @param codigoParticipe Código del partícipe detalle generación (PDGA)
     * @return Lista de cuotas ordenadas por número de cuota
     * @throws Throwable Si ocurre un error
     */
    List<CuotaXParticipeGeneracion> selectByParticipe(Long codigoParticipe) throws Throwable;
    
    /**
     * Busca todas las cuotas de un préstamo en una generación.
     * 
     * @param codigoPrestamo Código del préstamo
     * @param codigoGeneracion Código de la generación (GNAP)
     * @return Lista de cuotas
     * @throws Throwable Si ocurre un error
     */
    List<CuotaXParticipeGeneracion> selectByPrestamoYGeneracion(Long codigoPrestamo, Long codigoGeneracion) throws Throwable;
}
