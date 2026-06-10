package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class CreditoCuotasPrestamosMensualDaoServiceImpl extends EntityDaoImpl<CreditoCuotasPrestamosMensual> implements CreditoCuotasPrestamosMensualDaoService {

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
            "tipoSistemaAmortizacion", "cuotaCredito", "dividendo", "fechaExigibilidad",
            "valorDesgravamen", "valorIncendio"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CreditoCuotasPrestamosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoCuotasPrestamosMensualDaoServiceImpl.selectByEjecucion codigoEjecucion: " + codigoEjecucion);
        Query query = em.createQuery(
            " select c from CreditoCuotasPrestamosMensual c " +
            " where c.ejecucionReporte.codigo = :codigoEjecucion "
        );
        query.setParameter("codigoEjecucion", codigoEjecucion);
        return query.getResultList();
    }

    @Override
    public CreditoCuotasPrestamosMensual selectByEjecucionYOperacion(Long codigoEjecucion, String numeroOperacion) throws Throwable {
        System.out.println("CreditoCuotasPrestamosMensualDaoServiceImpl.selectByEjecucionYOperacion ejecucion: " + codigoEjecucion + " op: " + numeroOperacion);
        Query query = em.createQuery(
            " select c from CreditoCuotasPrestamosMensual c " +
            " where c.ejecucionReporte.codigo = :codigoEjecucion " +
            "   and c.numeroOperacion = :numeroOperacion "
        );
        query.setParameter("codigoEjecucion", codigoEjecucion);
        query.setParameter("numeroOperacion", numeroOperacion);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<CreditoCuotasPrestamosMensual> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public long countByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoCuotasPrestamosMensualDaoServiceImpl.countByEjecucion codigoEjecucion: " + codigoEjecucion);
        return (Long) em.createQuery(
            "SELECT COUNT(c) FROM CreditoCuotasPrestamosMensual c WHERE c.ejecucionReporte.codigo = :codigoEjecucion"
        )
        .setParameter("codigoEjecucion", codigoEjecucion)
        .getSingleResult();
    }

    @Override
    public int deleteByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoCuotasPrestamosMensualDaoServiceImpl.deleteByEjecucion codigoEjecucion: " + codigoEjecucion);
        return em.createQuery(
            "DELETE FROM CreditoCuotasPrestamosMensual c WHERE c.ejecucionReporte.codigo = :codigoEjecucion"
        )
        .setParameter("codigoEjecucion", codigoEjecucion)
        .executeUpdate();
    }
}
