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

    /**
     * D-044 (29-oct-2026): compara SOLO contra el detalle G48 de la ejecución que se genera. Antes
     * comparaba contra la G48 de cualquier ejecución de junio 2025 (mes = 6 y año = 2025): una
     * operación que salió de la cartera en la corrida nueva pero seguía en la G48 de una ejecución
     * anterior del mes no entraba a la G49 (68489 / préstamo 6201 en la ejecución 584: G49 35 en vez
     * de 36 y el control §5.4 del manual 1.594 + 29 − 35 ≠ 1.587).
     */
    @Override
    public List<HistoricoG48> selectEnHm48NoEnCg48(Long codigoDetalleG48) throws Throwable {
        System.out.println("HistoricoG48DaoServiceImpl.selectEnHm48NoEnCg48 - detalle G48: " + codigoDetalleG48);
        if (codigoDetalleG48 == null) {
            throw new IllegalArgumentException("selectEnHm48NoEnCg48: falta el detalle G48 de la ejecución");
        }
        String jpql =
            "SELECT h FROM HistoricoG48 h " +
            "WHERE NOT EXISTS (" +
            "    SELECT 1 FROM SaldoOperacionG48 g48 " +
            "    WHERE g48.numeroOperacion = h.numeroOperacion " +
            "    AND g48.detalleEjecucion.codigo = :detalle " +
            ")";
        return em.createQuery(jpql, HistoricoG48.class)
            .setParameter("detalle", codigoDetalleG48)
            .getResultList();
    }
}