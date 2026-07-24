package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ReferenciaPersonalDaoService;
import com.saa.model.crd.ReferenciaPersonal;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ReferenciaPersonalDaoServiceImpl extends EntityDaoImpl<ReferenciaPersonal> implements ReferenciaPersonalDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<ReferenciaPersonal> selectByParent(Long idEntidad) {
        System.out.println("ReferenciaPersonalDaoServiceImpl - selectByParent - idEntidad: " + idEntidad);
        Query query = em.createQuery(
            " select r from ReferenciaPersonal r " +
            " where  r.entidad.codigo = :idEntidad");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }
}
