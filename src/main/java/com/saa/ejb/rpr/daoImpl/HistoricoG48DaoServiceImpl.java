package com.saa.ejb.rpr.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG48DaoService;
import com.saa.model.rpr.HistoricoG48;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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

    @Override
    public HistoricoG48 selectByNumeroOperacion(String numeroOperacion) throws Throwable {
        System.out.println("HistoricoG48DaoServiceImpl.selectByNumeroOperacion op: " + numeroOperacion);
        Query query = em.createQuery(
            " select h from HistoricoG48 h where h.numeroOperacion = :numeroOperacion "
        );
        query.setParameter("numeroOperacion", numeroOperacion);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<HistoricoG48> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}