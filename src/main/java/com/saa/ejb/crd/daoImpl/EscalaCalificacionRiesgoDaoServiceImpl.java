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
}
