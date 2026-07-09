package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.model.crd.Participe;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ParticipeDaoServiceImpl extends EntityDaoImpl<Participe> implements ParticipeDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<Participe> selectByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad Participe con codigoEntidad: " + codigoEntidad);
        Query query = em.createQuery(
            "select p from Participe p where p.entidad.codigo = :codigoEntidad");
        query.setParameter("codigoEntidad", codigoEntidad);
        return query.getResultList();
    }
}
