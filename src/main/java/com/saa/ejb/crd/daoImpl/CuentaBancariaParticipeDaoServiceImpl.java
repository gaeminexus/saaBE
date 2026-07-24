package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.model.crd.CuentaBancariaParticipe;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class CuentaBancariaParticipeDaoServiceImpl extends EntityDaoImpl<CuentaBancariaParticipe> implements CuentaBancariaParticipeDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<CuentaBancariaParticipe> selectByParent(Long idEntidad) {
        System.out.println("CuentaBancariaParticipeDaoServiceImpl - selectByParent - idEntidad: " + idEntidad);
        Query query = em.createQuery(
            " select c from CuentaBancariaParticipe c " +
            " where  c.entidad.codigo = :idEntidad");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }
}
