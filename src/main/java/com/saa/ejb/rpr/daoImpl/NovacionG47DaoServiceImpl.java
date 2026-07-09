package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.NovacionG47DaoService;
import com.saa.model.rpr.NovacionG47;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class NovacionG47DaoServiceImpl extends EntityDaoImpl<NovacionG47> implements NovacionG47DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "numeroOperacionAnterior", "fechaNovacion"
        };
    }
}
