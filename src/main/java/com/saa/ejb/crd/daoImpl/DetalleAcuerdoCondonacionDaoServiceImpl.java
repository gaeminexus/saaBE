package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleAcuerdoCondonacionDaoService;
import com.saa.model.crd.DetalleAcuerdoCondonacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetalleAcuerdoCondonacionDaoServiceImpl extends EntityDaoImpl<DetalleAcuerdoCondonacion>
        implements DetalleAcuerdoCondonacionDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DetalleAcuerdoCondonacion");
        return new String[]{
            "codigo",
            "acuerdo",
            "concepto",
            "valorAdeudado",
            "valorPagado",
            "valorCondonado"
        };
    }

    @Override
    public List<DetalleAcuerdoCondonacion> selectByAcuerdo(Long idAcuerdo) throws Throwable {
        System.out.println("Ingresa al metodo selectByAcuerdo de DetalleAcuerdoCondonacion - acuerdo: " + idAcuerdo);
        Query query = em.createQuery(
                " select d from DetalleAcuerdoCondonacion d " +
                " where  d.acuerdo.codigo = :idAcuerdo " +
                " order by d.concepto");
        query.setParameter("idAcuerdo", idAcuerdo);
        return query.getResultList();
    }
}
