package com.saa.ejb.rpr.daoImpl;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG44DaoService;
import com.saa.model.rpr.HistoricoG44;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG44DaoServiceImpl extends EntityDaoImpl<HistoricoG44> implements HistoricoG44DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "identificacion","tipoIdentificacion","tipoJubilacion",
            "fechaJubilacion","imposicionesAcumuladas","valorPension","valorNetoRecibir",
            "saldoCuenta","valoresCompensados","jubilacionIess" };
    }
    @Override
    public List<HistoricoG44> selectByIdentificacion(String identificacion) {
        return em.createQuery(
            "select e from HistoricoG44 e where e.identificacion = :ced", HistoricoG44.class)
            .setParameter("ced", identificacion)
            .getResultList();
    }

    @Override
    public List<HistoricoG44> selectExJubilados() throws Throwable {
        return em.createQuery(
            "select h from HistoricoG44 h " +
            "where exists (" +
            "  select e from Entidad e " +
            "  where e.numeroIdentificacion = h.identificacion " +
            "  and e.idEstado <> 30 " +
            ")", HistoricoG44.class)
            .getResultList();
    }

    @Override
    public List<HistoricoG44> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable {
        if (identificaciones == null || identificaciones.isEmpty()) {
            return new ArrayList<>();
        }
        System.out.println("HistoricoG44DaoServiceImpl.selectByIdentificacionesIn - cantidad: " + identificaciones.size());
        return em.createQuery(
            "select h from HistoricoG44 h where h.identificacion IN :identificaciones", 
            HistoricoG44.class)
            .setParameter("identificaciones", identificaciones)
            .getResultList();
    }
}