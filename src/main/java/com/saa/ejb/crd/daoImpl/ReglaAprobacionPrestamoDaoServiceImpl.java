package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ReglaAprobacionPrestamoDaoService;
import com.saa.model.crd.ReglaAprobacionPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ReglaAprobacionPrestamoDaoServiceImpl extends EntityDaoImpl<ReglaAprobacionPrestamo> implements ReglaAprobacionPrestamoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<ReglaAprobacionPrestamo> selectActivas() throws Throwable {
        System.out.println("ReglaAprobacionPrestamoDaoServiceImpl - selectActivas");
        Query query = em.createQuery(" select e from ReglaAprobacionPrestamo e where e.estado = :activo order by e.prioridad, e.codigo");
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

}
