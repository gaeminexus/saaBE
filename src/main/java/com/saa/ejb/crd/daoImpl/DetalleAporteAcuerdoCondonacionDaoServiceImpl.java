package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleAporteAcuerdoCondonacionDaoService;
import com.saa.model.crd.DetalleAporteAcuerdoCondonacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetalleAporteAcuerdoCondonacionDaoServiceImpl extends EntityDaoImpl<DetalleAporteAcuerdoCondonacion>
        implements DetalleAporteAcuerdoCondonacionDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DetalleAporteAcuerdoCondonacion");
        return new String[]{
            "codigo",
            "acuerdo",
            "tipoAporte",
            "valor"
        };
    }

    @Override
    public List<DetalleAporteAcuerdoCondonacion> selectByAcuerdo(Long idAcuerdo) throws Throwable {
        System.out.println("Ingresa al metodo selectByAcuerdo de DetalleAporteAcuerdoCondonacion - acuerdo: "
                + idAcuerdo);
        Query query = em.createQuery(
                " select d from DetalleAporteAcuerdoCondonacion d " +
                " where  d.acuerdo.codigo = :idAcuerdo " +
                " order by d.codigo");
        query.setParameter("idAcuerdo", idAcuerdo);
        return query.getResultList();
    }
}
