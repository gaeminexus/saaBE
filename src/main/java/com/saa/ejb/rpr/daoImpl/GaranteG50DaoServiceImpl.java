package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.GaranteG50DaoService;
import com.saa.model.rpr.GaranteG50;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class GaranteG50DaoServiceImpl extends EntityDaoImpl<GaranteG50> implements GaranteG50DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "tipoIdentificacionGarante", "identificacionGarante", "tipoGarante",
            "fechaEliminacion", "causaEliminacion"
        };
    }
}
