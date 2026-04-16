package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.CuotaXParticipeGeneracion;

import jakarta.ejb.Local;

/**
 * Interface Service para CuotaXParticipeGeneracion (CXPG).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface CuotaXParticipeGeneracionService extends EntityService<CuotaXParticipeGeneracion> {

    /**
     * Crea un nuevo registro de cuota.
     * 
     * @param cuota Objeto con los datos de la cuota
     * @return Cuota creada con ID asignado
     * @throws Throwable Si ocurre un error
     */
    CuotaXParticipeGeneracion crear(CuotaXParticipeGeneracion cuota) throws Throwable;
    
    /**
     * Actualiza un registro existente.
     * 
     * @param cuota Objeto con los datos actualizados
     * @return Cuota actualizada
     * @throws Throwable Si ocurre un error
     */
    CuotaXParticipeGeneracion actualizar(CuotaXParticipeGeneracion cuota) throws Throwable;
    
    /**
     * Busca una cuota por ID.
     * 
     * @param codigo Código de la cuota
     * @return Cuota encontrada o null
     * @throws Throwable Si ocurre un error
     */
    CuotaXParticipeGeneracion buscarPorId(Long codigo) throws Throwable;
    
    /**
     * Lista todas las cuotas de un partícipe detalle generación.
     * 
     * @param codigoParticipe Código del partícipe detalle (PDGA)
     * @return Lista de cuotas ordenadas por número de cuota
     * @throws Throwable Si ocurre un error
     */
    List<CuotaXParticipeGeneracion> listarPorParticipe(Long codigoParticipe) throws Throwable;
    
    /**
     * Lista todas las cuotas de un préstamo en una generación.
     * 
     * @param codigoPrestamo Código del préstamo
     * @param codigoGeneracion Código de la generación (GNAP)
     * @return Lista de cuotas
     * @throws Throwable Si ocurre un error
     */
    List<CuotaXParticipeGeneracion> listarPorPrestamoYGeneracion(Long codigoPrestamo, Long codigoGeneracion) throws Throwable;
}
