package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoCPRMDaoService;
import com.saa.model.rpr.HistoricoCPRM;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistoricoCPRMDaoServiceImpl extends EntityDaoImpl<HistoricoCPRM> implements HistoricoCPRMDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "identificacion", "tipoIdentificacion", "tipoPrestacion",
            "aportePatronal", "aportePersonal", "aporteVoluntario",
            "saldoAportePatronal", "saldoAportePersonal", "saldoAporteVoluntario",
            "rendimiento"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricoCPRM> selectByIdentificacion(String identificacion) throws Throwable {
        System.out.println("HistoricoCPRMDaoServiceImpl.selectByIdentificacion: " + identificacion);
        Query query = em.createQuery(
            " select h from HistoricoCPRM h " +
            " where h.identificacion = :identificacion "
        );
        query.setParameter("identificacion", identificacion);
        return query.getResultList();
    }
}
