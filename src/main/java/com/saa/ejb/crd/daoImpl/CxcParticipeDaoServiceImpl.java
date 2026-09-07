package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CxcParticipeDaoService;
import com.saa.model.crd.CxcParticipe;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


@Stateless
public class CxcParticipeDaoServiceImpl extends EntityDaoImpl<CxcParticipe> implements CxcParticipeDaoService{

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<CxcParticipe> selectByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad CxcParticipe con codigoEntidad: " + codigoEntidad);
        Query query = em.createQuery(
            "select c from CxcParticipe c where c.entidad.codigo = :codigoEntidad");
        query.setParameter("codigoEntidad", codigoEntidad);
        return query.getResultList();
    }
}
