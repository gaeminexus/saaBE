package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG48DaoService;
import com.saa.model.rpr.HistoricoG48;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG48DaoServiceImpl extends EntityDaoImpl<HistoricoG48> implements HistoricoG48DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "numeroOperacion","tipoIdentificacion","identificacion",
            "tipoCredito","diasMorosidad","calificacionPropia","tasaInteres","valorPorVencer",
            "valorVencido","costosOperativos","interesOrdinario","interesSobreMora",
            "valorDemandaJudicial","carteraCastigada","provisionRequeridaOriginal",
            "provisionConstituida","valorTotalCuentaIndividual","valorSujetoProvision",
            "tipoSistemaAmortizacion","cuotaCredito","dividendo","fechaExigibilidadCuota" };
    }
}