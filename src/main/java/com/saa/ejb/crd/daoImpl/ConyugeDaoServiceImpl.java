package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ConyugeDaoService;
import com.saa.model.crd.Conyuge;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ConyugeDaoServiceImpl extends EntityDaoImpl<Conyuge> implements ConyugeDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<Conyuge> selectByParent(Long idEntidad) {
        System.out.println("ConyugeDaoServiceImpl - selectByParent - idEntidad: " + idEntidad);
        Query query = em.createQuery(
            " select c from Conyuge c " +
            " where  c.entidad.codigo = :idEntidad");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }
}
