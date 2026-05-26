package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG43DaoService;
import com.saa.model.rpr.HistoricoG43;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG43DaoServiceImpl extends EntityDaoImpl<HistoricoG43> implements HistoricoG43DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "identificacion","tipoIdentificacion","fechaTerminoRelacionLaboral",
            "numeroImposicionesPersonales","numeroImposicionesPatronales","fechaLiquidacion",
            "saldoCuentaIndividual","valoresCompensados","valoresPagados" };
    }
}