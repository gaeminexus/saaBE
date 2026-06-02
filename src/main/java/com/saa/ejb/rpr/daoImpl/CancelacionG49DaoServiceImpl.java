package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.model.rpr.CancelacionG49;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class CancelacionG49DaoServiceImpl extends EntityDaoImpl<CancelacionG49> implements CancelacionG49DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "fechaCancelacion", "formaCancelacion"
        };
    }

    @Override
    public List<CancelacionG49> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("CancelacionG49DaoServiceImpl.selectByDetalle codigoDetalle: " + codigoDetalle);
        String jpql = "SELECT c FROM CancelacionG49 c WHERE c.detalleEjecucion.codigo = :codigoDetalle";
        return em.createQuery(jpql, CancelacionG49.class)
                .setParameter("codigoDetalle", codigoDetalle)
                .getResultList();
    }
}
