package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG50DaoService;
import com.saa.model.rpr.HistoricoG50;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG50DaoServiceImpl extends EntityDaoImpl<HistoricoG50> implements HistoricoG50DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "numeroOperacion","tipoIdentificacion","identificacion",
            "tipoIdentificacionGarante","identificacionGarante","tipoGarante",
            "fechaEliminacionGarante","causaEliminacionGarante" };
    }
}