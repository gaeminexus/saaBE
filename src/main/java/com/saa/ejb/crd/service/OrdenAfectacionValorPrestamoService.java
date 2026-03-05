package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.OrdenAfectacionValorPrestamo;

import jakarta.ejb.Local;
import java.util.List;

/**
 * Interface de servicio para OrdenAfectacionValorPrestamo.
 * @author GaemiSoft
 */
@Local
public interface OrdenAfectacionValorPrestamoService extends EntityService<OrdenAfectacionValorPrestamo> {
    
    /**
     * Obtiene un orden de afectación por su ID.
     * @param id ID del orden
     * @return OrdenAfectacionValorPrestamo encontrado
     * @throws Throwable
     */
    OrdenAfectacionValorPrestamo selectById(Long id) throws Throwable;
    
    /**
     * Obtiene todos los registros ordenados por el campo orden.
     * @return Lista ordenada
     * @throws Throwable
     */
    List<OrdenAfectacionValorPrestamo> selectAllOrdenado() throws Throwable;
    
    /**
     * Guarda un solo registro de OrdenAfectacionValorPrestamo.
     * @param ordenAfectacion Entidad a guardar
     * @return Entidad guardada con ID actualizado
     * @throws Throwable
     */
    OrdenAfectacionValorPrestamo saveSingle(OrdenAfectacionValorPrestamo ordenAfectacion) throws Throwable;
}
