package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.EjecucionReporteCarteraDaoService;
import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class EjecucionReporteCarteraDaoServiceImpl extends EntityDaoImpl<EjecucionReporteCartera> implements EjecucionReporteCarteraDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{"codigo", "mes", "anio", "usuario", "fechaGeneracion", "observaciones"};
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EjecucionReporteCartera> selectByMesAnio(Long mes, Long anio) throws Throwable {
        System.out.println("EjecucionReporteCarteraDaoServiceImpl.selectByMesAnio mes: " + mes + " anio: " + anio);
        Query query = em.createQuery(
            " select e from EjecucionReporteCartera e " +
            " where e.mes = :mes and e.anio = :anio " +
            " order by e.fechaGeneracion desc "
        );
        query.setParameter("mes", mes);
        query.setParameter("anio", anio);
        return query.getResultList();
    }
}
