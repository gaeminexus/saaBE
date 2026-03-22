package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

@Local
public interface PrestamoDaoService extends EntityDao<Prestamo> {
    
    /**
     * Busca un préstamo por su idAsoprep.
     * @param idAsoprep ID del asociado préstamo
     * @return Préstamo encontrado o null si no existe
     * @throws Throwable Si ocurre algún error
     */
    Prestamo selectByIdAsoprep(Long idAsoprep) throws Throwable;

}
