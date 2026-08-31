package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleAportePrecancelacionDaoService;
import com.saa.model.crd.DetalleAportePrecancelacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetalleAportePrecancelacionDaoServiceImpl extends EntityDaoImpl<DetalleAportePrecancelacion>
        implements DetalleAportePrecancelacionDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DetalleAportePrecancelacion");
        return new String[]{
            "codigo",
            "detalleCobroCredito",
            "tipoAporte",
            "valor"
        };
    }

    @Override
    public List<DetalleAportePrecancelacion> selectByDetalleCobro(Long idDetalleCobro) throws Throwable {
        System.out.println("Ingresa al metodo selectByDetalleCobro de DetalleAportePrecancelacion - detalleCobro: "
                + idDetalleCobro);
        Query query = em.createQuery(
                " select d from DetalleAportePrecancelacion d " +
                " where  d.detalleCobroCredito.codigo = :idDetalleCobro " +
                " order by d.codigo");
        query.setParameter("idDetalleCobro", idDetalleCobro);
        return query.getResultList();
    }
}
