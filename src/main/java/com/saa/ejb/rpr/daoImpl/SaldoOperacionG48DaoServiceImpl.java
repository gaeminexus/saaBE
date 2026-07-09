package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.SaldoOperacionG48DaoService;
import com.saa.model.rpr.SaldoOperacionG48;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class SaldoOperacionG48DaoServiceImpl extends EntityDaoImpl<SaldoOperacionG48> implements SaldoOperacionG48DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "tipoCredito", "diasMorosidad", "calificacionPropia", "tasaInteres",
            "valorPorVencer", "valorVencido", "costosOperativos", "interesOrdinario",
            "interesMora", "valorDemandaJudicial", "carteraCastigada",
            "provisionRequeridaOriginal", "provisionConstituida",
            "valorTotalCuentaIndividual", "valorSujetoProvision",
            "tipoSistemaAmortizacion", "cuotaCredito", "dividendo", "fechaExigibilidad"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SaldoOperacionG48> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("SaldoOperacionG48DaoServiceImpl.selectByDetalle codigoDetalle: " + codigoDetalle);
        Query query = em.createQuery(
            " select g from SaldoOperacionG48 g " +
            " where g.detalleEjecucion.codigo = :codigoDetalle "
        );
        query.setParameter("codigoDetalle", codigoDetalle);
        return query.getResultList();
    }

    @Override
    public SaldoOperacionG48 selectByDetalleYOperacion(Long codigoDetalle, String numeroOperacion) throws Throwable {
        System.out.println("SaldoOperacionG48DaoServiceImpl.selectByDetalleYOperacion detalle: " + codigoDetalle + " op: " + numeroOperacion);
        Query query = em.createQuery(
            " select g from SaldoOperacionG48 g " +
            " where g.detalleEjecucion.codigo = :codigoDetalle " +
            "   and g.numeroOperacion = :numeroOperacion "
        );
        query.setParameter("codigoDetalle", codigoDetalle);
        query.setParameter("numeroOperacion", numeroOperacion);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<SaldoOperacionG48> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public int deleteByDetalleYOperacion(Long codigoDetalle, String numeroOperacion) throws Throwable {
        System.out.println("SaldoOperacionG48DaoServiceImpl.deleteByDetalleYOperacion detalle: " + codigoDetalle + " op: " + numeroOperacion);
        return em.createQuery(
            "DELETE FROM SaldoOperacionG48 g " +
            "WHERE g.detalleEjecucion.codigo = :codigoDetalle " +
            "  AND g.numeroOperacion = :numeroOperacion"
        )
        .setParameter("codigoDetalle", codigoDetalle)
        .setParameter("numeroOperacion", numeroOperacion)
        .executeUpdate();
    }

    @Override
    public long countByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("SaldoOperacionG48DaoServiceImpl.countByDetalle codigoDetalle: " + codigoDetalle);
        return (Long) em.createQuery(
            "SELECT COUNT(g) FROM SaldoOperacionG48 g WHERE g.detalleEjecucion.codigo = :codigoDetalle"
        )
        .setParameter("codigoDetalle", codigoDetalle)
        .getSingleResult();
    }
}
