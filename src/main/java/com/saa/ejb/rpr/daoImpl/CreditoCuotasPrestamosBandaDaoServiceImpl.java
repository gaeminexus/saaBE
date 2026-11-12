package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosBandaDaoService;
import com.saa.model.rpr.CreditoCuotasPrestamosBanda;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Implementación de {@link CreditoCuotasPrestamosBandaDaoService}. */
@Stateless
public class CreditoCuotasPrestamosBandaDaoServiceImpl implements CreditoCuotasPrestamosBandaDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public void insertar(CreditoCuotasPrestamosBanda detalle) throws Throwable {
        em.persist(detalle);
    }

    @Override
    public int deleteByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoCuotasPrestamosBandaDaoServiceImpl.deleteByEjecucion codigoEjecucion: " + codigoEjecucion);
        return em.createQuery("DELETE FROM CreditoCuotasPrestamosBanda c WHERE c.idEjecucion = :codigoEjecucion")
                .setParameter("codigoEjecucion", codigoEjecucion)
                .executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CreditoCuotasPrestamosBanda> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        return em.createQuery("select c from CreditoCuotasPrestamosBanda c where c.idEjecucion = :codigoEjecucion"
                + " order by c.numeroOperacion, c.rubro, c.numeroBanda")
                .setParameter("codigoEjecucion", codigoEjecucion)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> selectTotalesPorRubro(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoCuotasPrestamosBandaDaoServiceImpl.selectTotalesPorRubro codigoEjecucion: " + codigoEjecucion);
        return em.createQuery("select c.rubro, c.idTipoPrestamo, c.numeroBanda, max(c.etiquetaBanda), c.cuentaContable,"
                + " sum(c.valor), count(distinct c.numeroOperacion)"
                + " from CreditoCuotasPrestamosBanda c where c.idEjecucion = :codigoEjecucion"
                + " group by c.rubro, c.idTipoPrestamo, c.numeroBanda, c.cuentaContable"
                + " order by c.rubro, c.cuentaContable, c.idTipoPrestamo, c.numeroBanda")
                .setParameter("codigoEjecucion", codigoEjecucion)
                .getResultList();
    }
}
