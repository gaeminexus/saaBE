package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ReferenciaFamiliarDaoService;
import com.saa.model.crd.ReferenciaFamiliar;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ReferenciaFamiliarDaoServiceImpl extends EntityDaoImpl<ReferenciaFamiliar> implements ReferenciaFamiliarDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<ReferenciaFamiliar> selectByParent(Long idEntidad) {
        System.out.println("ReferenciaFamiliarDaoServiceImpl - selectByParent - idEntidad: " + idEntidad);
        Query query = em.createQuery(
            " select r from ReferenciaFamiliar r " +
            " where  r.entidad.codigo = :idEntidad");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }
}
