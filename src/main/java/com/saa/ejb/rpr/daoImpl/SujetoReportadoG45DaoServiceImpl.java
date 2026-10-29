package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.ejb.rpr.dao.SujetoReportadoG45DaoService;
import com.saa.model.rpr.SujetoReportadoG45;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class SujetoReportadoG45DaoServiceImpl implements SujetoReportadoG45DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<SujetoReportadoG45> selectAll() throws Throwable {
        return em.createQuery("select s from SujetoReportadoG45 s", SujetoReportadoG45.class).getResultList();
    }

    @Override
    public int deleteEjecucionDelPeriodo(Long periodo) throws Throwable {
        return em.createQuery("delete from SujetoReportadoG45 s where s.periodo = :periodo and s.origen = :origen")
                .setParameter("periodo", periodo)
                .setParameter("origen", SujetoReportadoG45.ORIGEN_EJECUCION)
                .executeUpdate();
    }

    @Override
    public void insertar(SujetoReportadoG45 sujeto) throws Throwable {
        em.persist(sujeto);
    }

    @Override
    public SujetoReportadoG45 actualizar(SujetoReportadoG45 sujeto) throws Throwable {
        return em.merge(sujeto);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> selectGarantesG50(Long idEjrdG50) throws Throwable {
        return em.createQuery("select g.tipoIdentificacionGarante, g.identificacionGarante from GaranteG50 g"
                + " where g.detalleEjecucion.codigo = :ejrd and g.identificacionGarante is not null")
                .setParameter("ejrd", idEjrdG50)
                .getResultList();
    }
}
