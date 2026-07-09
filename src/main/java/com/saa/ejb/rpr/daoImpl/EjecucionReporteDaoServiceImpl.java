package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.EjecucionReporteDaoService;
import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class EjecucionReporteDaoServiceImpl extends EntityDaoImpl<EjecucionReporte>
        implements EjecucionReporteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "mes", "anio", "usuario", "fechaGeneracion",
            "tipoEjecucion", "estado", "observaciones"
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<EjecucionReporte> selectByMesAnio(Long mes, Long anio) throws Throwable {
        System.out.println("Ingresa al metodo selectByMesAnio EjecucionReporte con mes: " + mes + ", anio: " + anio);
        Query query = em.createQuery(
            " select e from EjecucionReporte e " +
            " where  e.mes  = :mes " +
            "        and e.anio = :anio " +
            " order by e.fechaGeneracion desc");
        query.setParameter("mes", mes);
        query.setParameter("anio", anio);
        return query.getResultList();
    }
}
