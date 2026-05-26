package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.DetalleEjecucionReporteDaoService;
import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;

@Stateless
public class DetalleEjecucionReporteDaoServiceImpl extends EntityDaoImpl<DetalleEjecucionReporte>
        implements DetalleEjecucionReporteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "ejecucionReporte", "tipoReporte", "estado",
            "fechaGeneracion", "cantidadRegistros", "novedades", "detalleOriginal"
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DetalleEjecucionReporte> selectByEjecucion(Long idEjecucion) throws Throwable {
        System.out.println("Ingresa al metodo selectByEjecucion DetalleEjecucionReporte con idEjecucion: " + idEjecucion);
        Query query = em.createQuery(
            " select d from DetalleEjecucionReporte d " +
            " where  d.ejecucionReporte.codigo = :idEjecucion " +
            " order by d.tipoReporte");
        query.setParameter("idEjecucion", idEjecucion);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DetalleEjecucionReporte> selectConNovedadesByEjecucion(Long idEjecucion) throws Throwable {
        System.out.println("Ingresa al metodo selectConNovedadesByEjecucion con idEjecucion: " + idEjecucion);
        Query query = em.createQuery(
            " select d from DetalleEjecucionReporte d " +
            " where  d.ejecucionReporte.codigo = :idEjecucion " +
            "        and d.estado = 2 " +
            " order by d.tipoReporte");
        query.setParameter("idEjecucion", idEjecucion);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DetalleEjecucionReporte> selectPendientesYNovedadesByEjecucion(Long idEjecucion) throws Throwable {
        System.out.println("Ingresa al metodo selectPendientesYNovedadesByEjecucion con idEjecucion: " + idEjecucion);
        Query query = em.createQuery(
            " select d from DetalleEjecucionReporte d " +
            " where  d.ejecucionReporte.codigo = :idEjecucion " +
            "        and d.estado <> 1 " +
            " order by d.tipoReporte");
        query.setParameter("idEjecucion", idEjecucion);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public DetalleEjecucionReporte selectByEjecucionYTipo(Long idEjecucion, String tipoReporte) throws Throwable {
        System.out.println("selectByEjecucionYTipo ejrc: " + idEjecucion + " tipo: " + tipoReporte);
        Query query = em.createQuery(
            " select d from DetalleEjecucionReporte d " +
            " where  d.ejecucionReporte.codigo = :idEjecucion " +
            "   and  d.tipoReporte = :tipoReporte "
        );
        query.setParameter("idEjecucion", idEjecucion);
        query.setParameter("tipoReporte", tipoReporte);
        query.setMaxResults(1);
        List<DetalleEjecucionReporte> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
