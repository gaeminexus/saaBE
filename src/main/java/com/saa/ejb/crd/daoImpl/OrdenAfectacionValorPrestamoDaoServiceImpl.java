package com.saa.ejb.crd.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.OrdenAfectacionValorPrestamoDaoService;
import com.saa.model.crd.OrdenAfectacionValorPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Implementación DAO para OrdenAfectacionValorPrestamo.
 * @author GaemiSoft
 */
@Stateless
public class OrdenAfectacionValorPrestamoDaoServiceImpl extends EntityDaoImpl<OrdenAfectacionValorPrestamo> 
        implements OrdenAfectacionValorPrestamoDaoService {
    
    @PersistenceContext
    EntityManager em;

    @Override
    public List<OrdenAfectacionValorPrestamo> selectAllOrdenado() throws Throwable {
        System.out.println("Dao selectAllOrdenado de OrdenAfectacionValorPrestamo");
        Query query = em.createNamedQuery("OrdenAfectacionValorPrestamoByOrden");
        @SuppressWarnings("unchecked")
        List<OrdenAfectacionValorPrestamo> result = query.getResultList();
        return result;
    }
}
