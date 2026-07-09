package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoCCPMDaoService;
import com.saa.model.rpr.HistoricoCCPM;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistoricoCCPMDaoServiceImpl extends EntityDaoImpl<HistoricoCCPM> implements HistoricoCCPMDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "numeroOperacion", "tipoIdentificacion", "identificacion", "tipoCredito",
            "diasMorosidad", "calificacionPropia", "tasaInteres",
            "valorPorVencer", "valorVencido", "costosOperativos", "interesOrdinario",
            "interesSobreMora", "valorDemandaJudicial", "carteraCastigada",
            "provisionRequeridaOriginal", "provisionConstituida",
            "valorTotalCuentaIndividual", "valorSujetoProvision",
            "tipoSistemaAmortizacion", "cuotaCredito", "dividendo", "fechaExigibilidadCuota",
            "valorDesgravamen", "valorIncendio"
        };
    }

    @Override
    public HistoricoCCPM selectByNumeroOperacion(String numeroOperacion) throws Throwable {
        System.out.println("HistoricoCCPMDaoServiceImpl.selectByNumeroOperacion: " + numeroOperacion);
        Query query = em.createQuery(
            " select h from HistoricoCCPM h " +
            " where h.numeroOperacion = :numeroOperacion "
        );
        query.setParameter("numeroOperacion", numeroOperacion);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        java.util.List<HistoricoCCPM> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
