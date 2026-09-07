package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.EscalaCalificacionRiesgoDaoService;
import com.saa.model.crd.EscalaCalificacionRiesgo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class EscalaCalificacionRiesgoDaoServiceImpl extends EntityDaoImpl<EscalaCalificacionRiesgo>
        implements EscalaCalificacionRiesgoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "configuracion", "calificacion", "diaDesde", "diaHasta",
            "porcentajeProvision", "orden", "fechaRegistro", "usuarioRegistro", "estado"
        };
    }

    @Override
    public List<EscalaCalificacionRiesgo> selectByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("EscalaCalificacionRiesgoDaoService.selectByConfiguracion - configuracion: "
            + idConfiguracion);
        Query query = em.createQuery(
            "select e from EscalaCalificacionRiesgo e "
                + "where e.configuracion.codigo = :idConfiguracion "
                + "order by e.orden asc, e.diaDesde asc");
        query.setParameter("idConfiguracion", idConfiguracion);
        return query.getResultList();
    }

    @Override
    public List<EscalaCalificacionRiesgo> selectByConfiguraciones(List<Long> idsConfiguracion) throws Throwable {
        System.out.println("EscalaCalificacionRiesgoDaoService.selectByConfiguraciones - configuraciones: "
            + (idsConfiguracion != null ? idsConfiguracion.size() : 0));
        if (idsConfiguracion == null || idsConfiguracion.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        Query query = em.createQuery(
            "select e from EscalaCalificacionRiesgo e "
                + "where e.configuracion.codigo in :ids "
                + "order by e.configuracion.codigo, e.orden asc, e.diaDesde asc");
        query.setParameter("ids", idsConfiguracion);
        return query.getResultList();
    }

    @Override
    public int deleteByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("EscalaCalificacionRiesgoDaoService.deleteByConfiguracion - configuracion: "
            + idConfiguracion);
        Query query = em.createQuery(
            "delete from EscalaCalificacionRiesgo e "
                + "where e.configuracion.codigo = :idConfiguracion");
        query.setParameter("idConfiguracion", idConfiguracion);
        int eliminadas = query.executeUpdate();
        // El DELETE masivo va a la base de inmediato, antes de los INSERT de las calificaciones
        // nuevas -- mismo motivo que BandaProductoDaoServiceImpl. Sin em.clear(): desprenderia la
        // configuracion que el servicio sigue usando.
        em.flush();
        return eliminadas;
    }
}
