package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.HistoricoPagoCuentaIndividualDaoService;
import com.saa.model.crd.HistoricoPagoCuentaIndividual;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class HistoricoPagoCuentaIndividualDaoServiceImpl extends EntityDaoImpl<HistoricoPagoCuentaIndividual>
        implements HistoricoPagoCuentaIndividualDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "cedula",
            "fechaPago",
            "observacion",
            "tipo",
            "valor",
            "fechaRegistro",
            "usuarioRegistro"
        };
    }

    @Override
    public List<HistoricoPagoCuentaIndividual> selectByCedula(String cedula) throws Throwable {
        System.out.println("Ingresa al metodo selectByCedula con cedula: " + cedula);
        Query query = em.createQuery(
                " select h from HistoricoPagoCuentaIndividual h " +
                " where  h.cedula = :cedula " +
                " order by h.fechaPago desc, h.codigo desc");
        query.setParameter("cedula", cedula);
        return query.getResultList();
    }

    @Override
    public List<HistoricoPagoCuentaIndividual> selectByCedulaAndTipos(String cedula, List<String> tipos)
            throws Throwable {
        System.out.println("Ingresa al metodo selectByCedulaAndTipos con cedula: " + cedula
                + " y tipos: " + tipos);
        List<String> tiposMayusculas = new ArrayList<>();
        if (tipos != null) {
            for (String tipo : tipos) {
                if (tipo != null) {
                    tiposMayusculas.add(tipo.trim().toUpperCase());
                }
            }
        }
        if (tiposMayusculas.isEmpty()) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
                " select h from HistoricoPagoCuentaIndividual h " +
                " where  h.cedula = :cedula " +
                " and    upper(trim(h.tipo)) in (:tipos) " +
                " order by h.fechaPago desc, h.codigo desc");
        query.setParameter("cedula", cedula);
        query.setParameter("tipos", tiposMayusculas);
        return query.getResultList();
    }
}
