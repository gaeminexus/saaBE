package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.OrdenAfectacionValorPrestamo;

import jakarta.ejb.Local;
import java.util.List;

/**
 * Interface DAO para OrdenAfectacionValorPrestamo.
 * @author GaemiSoft
 */
@Local
public interface OrdenAfectacionValorPrestamoDaoService extends EntityDao<OrdenAfectacionValorPrestamo> {
    
    /**
     * Obtiene todos los registros ordenados por el campo orden.
     * @return Lista ordenada de OrdenAfectacionValorPrestamo
     * @throws Throwable
     */
    List<OrdenAfectacionValorPrestamo> selectAllOrdenado() throws Throwable;
}
