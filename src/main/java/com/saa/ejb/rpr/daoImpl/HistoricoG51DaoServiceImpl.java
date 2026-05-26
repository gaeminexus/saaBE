package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG51DaoService;
import com.saa.model.rpr.HistoricoG51;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG51DaoServiceImpl extends EntityDaoImpl<HistoricoG51> implements HistoricoG51DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "numeroGarantia","tipoIdentificacion","identificacion","numeroOperacion",
            "tipoGarantia","descripcionGarantia","valorAvaluo","fechaAvaluo",
            "numeroRegistroGarantia","fechaContabilizacion","porcentajeCubreGarantia","estadoRegistro" };
    }
}