package com.saa.ejb.crd.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.EstadoCuotaPrestamoDaoService;
import com.saa.model.crd.EstadoCuotaPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación DAO para EstadoCuotaPrestamo.
 * @author GaemiSoft
 */
@Stateless
public class EstadoCuotaPrestamoDaoServiceImpl extends EntityDaoImpl<EstadoCuotaPrestamo> 
        implements EstadoCuotaPrestamoDaoService {
    
    @PersistenceContext
    EntityManager em;

}
