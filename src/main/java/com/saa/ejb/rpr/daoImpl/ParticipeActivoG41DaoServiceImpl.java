package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.ParticipeActivoG41DaoService;
import com.saa.model.rpr.ParticipeActivoG41;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ParticipeActivoG41DaoServiceImpl extends EntityDaoImpl<ParticipeActivoG41> implements ParticipeActivoG41DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "genero", "estadoCivil",
            "fechaNacimiento", "fechaIngreso", "estadoParticipe", "tipoSistema",
            "baseCalculoAportacion", "tipoRelacionLaboral", "estadoRegistro",
            "fechaActualizacionEstado"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ParticipeActivoG41> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("ParticipeActivoG41DaoServiceImpl.selectByDetalle codigoDetalle: " + codigoDetalle);
        Query query = em.createQuery(
            " select g from ParticipeActivoG41 g " +
            " where  g.detalleEjecucion.codigo = :codigoDetalle "
        );
        query.setParameter("codigoDetalle", codigoDetalle);
        return query.getResultList();
    }
}
