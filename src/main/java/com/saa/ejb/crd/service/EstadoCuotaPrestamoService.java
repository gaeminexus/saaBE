package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.EstadoCuotaPrestamo;

import jakarta.ejb.Local;

/**
 * Interface de servicio para EstadoCuotaPrestamo.
 * @author GaemiSoft
 */
@Local
public interface EstadoCuotaPrestamoService extends EntityService<EstadoCuotaPrestamo> {
    
    /**
     * Obtiene un estado de cuota de préstamo por su ID.
     * @param id ID del estado
     * @return EstadoCuotaPrestamo encontrado
     * @throws Throwable
     */
    EstadoCuotaPrestamo selectById(Long id) throws Throwable;
    
    /**
     * Guarda un solo registro de EstadoCuotaPrestamo.
     * @param estadoCuotaPrestamo Entidad a guardar
     * @return Entidad guardada con ID actualizado
     * @throws Throwable
     */
    EstadoCuotaPrestamo saveSingle(EstadoCuotaPrestamo estadoCuotaPrestamo) throws Throwable;
}
