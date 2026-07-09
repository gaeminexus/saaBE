package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.CreditoJubiladosMensualDaoService;
import com.saa.model.rpr.CreditoJubiladosMensual;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class CreditoJubiladosMensualDaoServiceImpl extends EntityDaoImpl<CreditoJubiladosMensual> implements CreditoJubiladosMensualDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "tipoJubilacion",
            "fechaJubilacion", "imposicionesAcumuladas", "valorPension",
            "valorNetoRecibir", "saldoCuenta", "valoresCompensados", "jubilacionIess",
            "valorJubilacion", "valorSeguro"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CreditoJubiladosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoJubiladosMensualDaoServiceImpl.selectByEjecucion codigoEjecucion: " + codigoEjecucion);
        Query query = em.createQuery(
            " select c from CreditoJubiladosMensual c " +
            " where c.ejecucionReporte.codigo = :codigoEjecucion "
        );
        query.setParameter("codigoEjecucion", codigoEjecucion);
        return query.getResultList();
    }

    @Override
    public int deleteByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("CreditoJubiladosMensualDaoServiceImpl.deleteByEjecucion codigoEjecucion: " + codigoEjecucion);
        return em.createQuery(
            "DELETE FROM CreditoJubiladosMensual c WHERE c.ejecucionReporte.codigo = :codigoEjecucion"
        )
        .setParameter("codigoEjecucion", codigoEjecucion)
        .executeUpdate();
    }
}
