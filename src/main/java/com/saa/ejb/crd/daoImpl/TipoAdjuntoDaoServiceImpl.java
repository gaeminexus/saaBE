package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.TipoAdjuntoDaoService;
import com.saa.model.crd.TipoAdjunto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class TipoAdjuntoDaoServiceImpl extends EntityDaoImpl<TipoAdjunto> implements TipoAdjuntoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<TipoAdjunto> selectByNombre(String nombre) {
        System.out.println("TipoAdjuntoDaoServiceImpl - selectByNombre: " + nombre);
        Query query = em.createQuery(
            " select t from TipoAdjunto t " +
            " where  UPPER(t.nombre) = UPPER(:nombre) " +
            " and    t.estado = :activo");
        query.setParameter("nombre", nombre);
        query.setParameter("activo", (long) Estado.ACTIVO);
        return query.getResultList();
    }
}
