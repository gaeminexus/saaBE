package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.CreditoParticipesMensualDaoService;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.EjecucionReporteCartera;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class CreditoParticipesMensualDaoServiceImpl extends EntityDaoImpl<CreditoParticipesMensual> implements CreditoParticipesMensualDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion",
            "tipoAporte", "total", "entidad", "nombreEstado"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public CreditoParticipesMensual selectByEntidadTipoAporteYEjecucion(
            Long codigoEntidad, Long codigoTipoAporte, EjecucionReporteCartera ejecucion) throws Throwable {
        System.out.println("CreditoParticipesMensualDaoServiceImpl.selectByEntidadTipoAporteYEjecucion entidad: "
                + codigoEntidad + " tipoAporte: " + codigoTipoAporte);
        Query query = em.createQuery(
            " select c " +
            " from   CreditoParticipesMensual c " +
            " where  c.entidad.codigo      = :codigoEntidad " +
            "   and  c.tipoAporte.codigo   = :codigoTipoAporte " +
            "   and  c.ejecucionReporte.codigo = :codigoEjecucion "
        );
        query.setParameter("codigoEntidad",    codigoEntidad);
        query.setParameter("codigoTipoAporte", codigoTipoAporte);
        query.setParameter("codigoEjecucion",  ejecucion.getCodigo());
        query.setMaxResults(1);
        List<CreditoParticipesMensual> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CreditoParticipesMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoParticipesMensualDaoServiceImpl.selectByEjecucion codigoEjecucion: " + codigoEjecucion);
        Query query = em.createQuery(
            " select c from CreditoParticipesMensual c " +
            " where c.ejecucionReporte.codigo = :codigoEjecucion "
        );
        query.setParameter("codigoEjecucion", codigoEjecucion);
        return query.getResultList();
    }

    @Override
    public int deleteByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoParticipesMensualDaoServiceImpl.deleteByEjecucion codigoEjecucion: " + codigoEjecucion);
        return em.createQuery(
            "DELETE FROM CreditoParticipesMensual c WHERE c.ejecucionReporte.codigo = :codigoEjecucion"
        )
        .setParameter("codigoEjecucion", codigoEjecucion)
        .executeUpdate();
    }
}