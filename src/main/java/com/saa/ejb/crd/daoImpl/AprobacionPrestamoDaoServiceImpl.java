package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AprobacionPrestamoDaoService;
import com.saa.model.crd.AprobacionPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class AprobacionPrestamoDaoServiceImpl extends EntityDaoImpl<AprobacionPrestamo> implements AprobacionPrestamoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<AprobacionPrestamo> selectVigentesByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("AprobacionPrestamoDaoServiceImpl - selectVigentesByPrestamo - idPrestamo: " + idPrestamo);
        Query query = em.createQuery(
            " select e from AprobacionPrestamo e where e.idPrestamo = :idPrestamo and e.estado = :activo  order by e.orden, e.codigo");
        query.setParameter("idPrestamo", idPrestamo);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

}
