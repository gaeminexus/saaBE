package com.saa.ejb.rpr.daoImpl;

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
}
