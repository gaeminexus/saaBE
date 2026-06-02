package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ProvinciaDaoService;
import com.saa.model.crd.Provincia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


@Stateless
public class ProvinciaDaoServiceImpl extends EntityDaoImpl<Provincia> implements ProvinciaDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Provincia selectByNombre(String nombre) throws Throwable {
        System.out.println("ProvinciaDaoServiceImpl.selectByNombre nombre: " + nombre);
        Query query = em.createQuery(
            "select p from Provincia p where upper(p.nombre) = upper(:nombre)"
        );
        query.setParameter("nombre", nombre);
        query.setMaxResults(1);
        List<Provincia> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}