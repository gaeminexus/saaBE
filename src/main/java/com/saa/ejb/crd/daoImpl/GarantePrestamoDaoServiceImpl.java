package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.GarantePrestamoDaoService;
import com.saa.model.crd.GarantePrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class GarantePrestamoDaoServiceImpl extends EntityDaoImpl<GarantePrestamo> implements GarantePrestamoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public GarantePrestamo selectActivoByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("GarantePrestamoDaoServiceImpl - selectActivoByPrestamo - idPrestamo: " + idPrestamo);
        // Sin try/catch: un error de base no puede pasar por "no tiene garante" (el pagaré saldría sin su firma).
        Query query = em.createQuery(
            " select g from GarantePrestamo g " +
            " where  g.idPrestamo = :idPrestamo and g.estado = :activo " +
            " order  by g.codigo desc");
        query.setParameter("idPrestamo", idPrestamo);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setMaxResults(1);
        List<GarantePrestamo> lista = query.getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }
}
