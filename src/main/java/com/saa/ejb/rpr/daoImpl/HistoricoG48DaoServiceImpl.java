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

    @Override
    public List<HistoricoG48> selectEnHm48NoEnCg48Junio2025() throws Throwable {
        System.out.println("HistoricoG48DaoServiceImpl.selectEnHm48NoEnCg48Junio2025");
        String jpql =
            "SELECT h FROM HistoricoG48 h " +
            "WHERE NOT EXISTS (" +
            "    SELECT 1 FROM SaldoOperacionG48 g48 " +
            "    WHERE g48.numeroOperacion = h.numeroOperacion " +
            "    AND g48.detalleEjecucion.ejecucionReporte.mes  = 6 " +
            "    AND g48.detalleEjecucion.ejecucionReporte.anio = 2025 " +
            ")";
        return em.createQuery(jpql, HistoricoG48.class).getResultList();
    }
}