package com.saa.ejb.crd.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ExterDaoService;
import com.saa.model.crd.Exter;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class ExterDaoServiceImpl extends EntityDaoImpl<Exter> implements ExterDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<Exter> selectByCedula(String cedula) throws Throwable {
        System.out.println("Ingresa al metodo selectByCedula Exter con cedula: " + cedula);
        Query query = em.createQuery(
            "select e from Exter e where e.cedula = :cedula");
        query.setParameter("cedula", cedula);
        return query.getResultList();
    }
}
