package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG47DaoService;
import com.saa.model.rpr.HistoricoG47;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG47DaoServiceImpl extends EntityDaoImpl<HistoricoG47> implements HistoricoG47DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "numeroOperacion","tipoIdentificacion","identificacion",
            "numeroOperacionAnterior","fechaNovacion" };
    }
}