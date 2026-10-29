package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.ejb.rpr.dao.SujetoReportadoG41DaoService;
import com.saa.model.rpr.SujetoReportadoG41;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class SujetoReportadoG41DaoServiceImpl implements SujetoReportadoG41DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<SujetoReportadoG41> selectAnterioresA(Long periodo) throws Throwable {
        return em.createQuery("select s from SujetoReportadoG41 s where s.periodo < :periodo order by s.periodo, s.codigo",
                SujetoReportadoG41.class)
                .setParameter("periodo", periodo)
                .getResultList();
    }

    @Override
    public List<SujetoReportadoG41> selectDelPeriodo(Long periodo) throws Throwable {
        return em.createQuery("select s from SujetoReportadoG41 s where s.periodo = :periodo order by s.codigo",
                SujetoReportadoG41.class)
                .setParameter("periodo", periodo)
                .getResultList();
    }

    @Override
    public long countEjecucionPosterioresA(Long periodo) throws Throwable {
        return em.createQuery("select count(s) from SujetoReportadoG41 s where s.periodo > :periodo and s.origen = :origen",
                Long.class)
                .setParameter("periodo", periodo)
                .setParameter("origen", SujetoReportadoG41.ORIGEN_EJECUCION)
                .getSingleResult();
    }

    @Override
    public int deleteEjecucionDelPeriodo(Long periodo) throws Throwable {
        return em.createQuery("delete from SujetoReportadoG41 s where s.periodo = :periodo and s.origen = :origen")
                .setParameter("periodo", periodo)
                .setParameter("origen", SujetoReportadoG41.ORIGEN_EJECUCION)
                .executeUpdate();
    }

    @Override
    public void insertar(SujetoReportadoG41 sujeto) throws Throwable {
        em.persist(sujeto);
    }

    @Override
    public List<String> selectIdentificacionesG43(Long idEjrdG43) throws Throwable {
        return em.createQuery("select g.identificacion from ParticipeCesanteG43 g"
                + " where g.detalleEjecucion.codigo = :ejrd and g.identificacion is not null", String.class)
                .setParameter("ejrd", idEjrdG43)
                .getResultList();
    }

    @Override
    public List<String> selectIdentificacionesG44(Long idEjrdG44) throws Throwable {
        return em.createQuery("select g.identificacion from ParticipeJubiladoG44 g"
                + " where g.detalleEjecucion.codigo = :ejrd and g.identificacion is not null", String.class)
                .setParameter("ejrd", idEjrdG44)
                .getResultList();
    }
}
