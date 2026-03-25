package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AfectacionValoresParticipeCargaDaoService;
import com.saa.model.crd.AfectacionValoresParticipeCarga;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class AfectacionValoresParticipeCargaDaoServiceImpl extends EntityDaoImpl<AfectacionValoresParticipeCarga>
        implements AfectacionValoresParticipeCargaDaoService {

    // Inicializa persistence context
    @PersistenceContext
    EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<AfectacionValoresParticipeCarga> selectByNovedad(Long codigoNovedad) throws Throwable {
        System.out.println("Ingresa al metodo selectByNovedad con codigoNovedad: " + codigoNovedad);
        Query query = em.createQuery(
            " SELECT a " +
            " FROM   AfectacionValoresParticipeCarga a " +
            " WHERE  a.novedadParticipeCarga.codigo = :codigoNovedad " +
            " ORDER BY a.codigo "
        );
        query.setParameter("codigoNovedad", codigoNovedad);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AfectacionValoresParticipeCarga> selectByPrestamo(Long codigoPrestamo) throws Throwable {
        System.out.println("Ingresa al metodo selectByPrestamo con codigoPrestamo: " + codigoPrestamo);
        Query query = em.createQuery(
            " SELECT a " +
            " FROM   AfectacionValoresParticipeCarga a " +
            " WHERE  a.prestamo.codigo = :codigoPrestamo " +
            " ORDER BY a.codigo "
        );
        query.setParameter("codigoPrestamo", codigoPrestamo);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AfectacionValoresParticipeCarga> selectByCuota(Long codigoCuota) throws Throwable {
        System.out.println("Ingresa al metodo selectByCuota con codigoCuota: " + codigoCuota);
        Query query = em.createQuery(
            " SELECT a " +
            " FROM   AfectacionValoresParticipeCarga a " +
            " WHERE  a.detallePrestamo.codigo = :codigoCuota " +
            " ORDER BY a.codigo "
        );
        query.setParameter("codigoCuota", codigoCuota);
        return query.getResultList();
    }
}
