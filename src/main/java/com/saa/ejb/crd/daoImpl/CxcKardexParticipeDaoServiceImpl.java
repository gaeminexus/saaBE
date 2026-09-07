package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CxcKardexParticipeDaoService;
import com.saa.model.crd.CxcKardexParticipe;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class CxcKardexParticipeDaoServiceImpl extends EntityDaoImpl<CxcKardexParticipe> implements CxcKardexParticipeDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<CxcKardexParticipe> selectRecientesByCxcParticipe(Long codigoCxcParticipe, int limite) throws Throwable {
        System.out.println("Ingresa al metodo selectRecientesByCxcParticipe CxcKardexParticipe con codigoCxcParticipe: "
                + codigoCxcParticipe + " - limite: " + limite);
        Query query = em.createQuery(
            "select k from CxcKardexParticipe k where k.cxcpParticipe.codigo = :codigoCxcParticipe"
            + " order by k.fechaCreado desc");
        query.setParameter("codigoCxcParticipe", codigoCxcParticipe);
        query.setMaxResults(limite);
        return query.getResultList();
    }
}
