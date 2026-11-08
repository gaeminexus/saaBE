package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.NivelAprobacionPrestamoDaoService;
import com.saa.model.crd.NivelAprobacionPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class NivelAprobacionPrestamoDaoServiceImpl extends EntityDaoImpl<NivelAprobacionPrestamo> implements NivelAprobacionPrestamoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<NivelAprobacionPrestamo> selectActivosByRegla(Long idRegla) throws Throwable {
        System.out.println("NivelAprobacionPrestamoDaoServiceImpl - selectActivosByRegla - idRegla: " + idRegla);
        Query query = em.createQuery(
            " select e from NivelAprobacionPrestamo e where e.idRegla = :idRegla and e.estado = :activo  order by e.orden");
        query.setParameter("idRegla", idRegla);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

}
