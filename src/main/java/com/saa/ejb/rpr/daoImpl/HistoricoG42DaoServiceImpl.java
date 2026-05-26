package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG42DaoService;
import com.saa.model.rpr.HistoricoG42;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistoricoG42DaoServiceImpl extends EntityDaoImpl<HistoricoG42> implements HistoricoG42DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{ "identificacion","tipoIdentificacion","tipoPrestacion",
            "aportePatronal","aportePersonal","aporteVoluntario","saldoAportePatronal",
            "saldoAportePersonal","saldoAporteVoluntario","rendimiento" };
    }

    /**
     * Retorna los registros del HistoricoG42 cuya identificacion NO aparece
     * en el G42 del mes actual. Comparación con NOT EXISTS en BD.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricoG42> selectCesantesDesdeHistorico(Long codigoDetalleActual) throws Throwable {
        System.out.println("HistoricoG42DaoServiceImpl.selectCesantesDesdeHistorico detalleActual: " + codigoDetalleActual);
        Query query = em.createQuery(
            " select h " +
            " from   HistoricoG42 h " +
            " where  NOT EXISTS ( " +
            "        select 1 from SaldoCuentaG42 act " +
            "        where  act.detalleEjecucion.codigo = :codigoDetalleActual " +
            "          and  act.identificacion = h.identificacion " +
            "   ) "
        );
        query.setParameter("codigoDetalleActual", codigoDetalleActual);
        return query.getResultList();
    }
}
