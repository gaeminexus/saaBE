package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.SaldoOperacionG48DaoService;
import com.saa.model.rpr.SaldoOperacionG48;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
}
