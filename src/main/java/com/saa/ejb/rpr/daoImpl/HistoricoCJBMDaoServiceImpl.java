package com.saa.ejb.rpr.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoCJBMDaoService;
import com.saa.model.rpr.HistoricoCJBM;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistoricoCJBMDaoServiceImpl extends EntityDaoImpl<HistoricoCJBM> implements HistoricoCJBMDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "identificacion", "tipoIdentificacion", "tipoJubilacion",
            "fechaJubilacion", "imposicionesAcumuladas", "valorPension",
            "valorNetoRecibir", "saldoCuenta", "valoresCompensados", "jubilacionIess"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricoCJBM> selectByIdentificacion(String identificacion) throws Throwable {
        System.out.println("HistoricoCJBMDaoServiceImpl.selectByIdentificacion: " + identificacion);
        Query query = em.createQuery(
            " select h from HistoricoCJBM h " +
            " where h.identificacion = :identificacion "
        );
        query.setParameter("identificacion", identificacion);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricoCJBM> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable {
        if (identificaciones == null || identificaciones.isEmpty()) {
            return new ArrayList<>();
        }
        System.out.println("HistoricoCJBMDaoServiceImpl.selectByIdentificacionesIn - cantidad: " + identificaciones.size());
        Query query = em.createQuery(
            " select h from HistoricoCJBM h " +
            " where h.identificacion IN :identificaciones "
        );
        query.setParameter("identificaciones", identificaciones);
        return query.getResultList();
    }
}
