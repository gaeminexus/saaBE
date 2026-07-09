package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.SaldoCuentaG42DaoService;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.SaldoCuentaG42;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class SaldoCuentaG42DaoServiceImpl extends EntityDaoImpl<SaldoCuentaG42> implements SaldoCuentaG42DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "tipoPrestacion",
            "aportePatronal", "aportePersonal", "aporteVoluntario",
            "saldoAportePatronal", "saldoAportePersonal", "saldoAporteVoluntario",
            "rendimiento", "entidad"
        };
    }

    /**
     * Busca un registro de CG42 por entidad y detalle de ejecución.
     * Clave para lógica INSERT-or-UPDATE del G42.
     */
    @Override
    @SuppressWarnings("unchecked")
    public SaldoCuentaG42 selectByEntidadYDetalle(Long codigoEntidad, DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("SaldoCuentaG42DaoServiceImpl.selectByEntidadYDetalle entidad: " + codigoEntidad);
        Query query = em.createQuery(
            " select g " +
            " from   SaldoCuentaG42 g " +
            " where  g.entidad.codigo = :codigoEntidad " +
            "   and  g.detalleEjecucion.codigo = :codigoDetalle "
        );
        query.setParameter("codigoEntidad", codigoEntidad);
        query.setParameter("codigoDetalle", detalle.getCodigo());
        query.setMaxResults(1);
        List<SaldoCuentaG42> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Retorna todos los registros CG42 que pertenecen a un EJRD específico.
     * Usado en G43 para obtener el universo de entidades del mes anterior.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SaldoCuentaG42> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("SaldoCuentaG42DaoServiceImpl.selectByDetalle codigoDetalle: " + codigoDetalle);
        Query query = em.createQuery(
            " select g from SaldoCuentaG42 g " +
            " where g.detalleEjecucion.codigo = :codigoDetalle "
        );
        query.setParameter("codigoDetalle", codigoDetalle);
        return query.getResultList();
    }

    /**
     * Retorna los registros del G42 del mes anterior que NO tienen correspondencia
     * en el G42 del mes actual — comparación por identificacion con NOT EXISTS en BD.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SaldoCuentaG42> selectCesantesDesdeG42Previo(
            Long codigoDetallePrevio, Long codigoDetalleActual) throws Throwable {
        System.out.println("SaldoCuentaG42DaoServiceImpl.selectCesantesDesdeG42Previo previo: "
                + codigoDetallePrevio + " actual: " + codigoDetalleActual);
        Query query = em.createQuery(
            " select prev " +
            " from   SaldoCuentaG42 prev " +
            " where  prev.detalleEjecucion.codigo = :codigoDetallePrevio " +
            "   and  NOT EXISTS ( " +
            "        select 1 from SaldoCuentaG42 act " +
            "        where  act.detalleEjecucion.codigo = :codigoDetalleActual " +
            "          and  act.identificacion = prev.identificacion " +
            "   ) "
        );
        query.setParameter("codigoDetallePrevio", codigoDetallePrevio);
        query.setParameter("codigoDetalleActual", codigoDetalleActual);
        return query.getResultList();
    }
}
