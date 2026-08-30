package com.saa.ejb.tsr.daoImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.DetalleTransitoDaoService;
import com.saa.model.tsr.DetalleTransito;
import com.saa.rubros.EstadoPartidaTransito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.NoResultException;

/**
 * @author GaemiSoft
 * Implementacion DetalleTransitoDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class DetalleTransitoDaoServiceImpl extends EntityDaoImpl<DetalleTransito>
        implements DetalleTransitoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{"codigo", "cierre", "detalleAsiento", "movimientoBanco", "detalleExtracto",
                "tipo", "valor", "estado", "cierreSaldo", "observacion", "fechaRegistro"};
    }

    @Override
    public List<DetalleTransito> selectByCierre(Long idCierre) throws Throwable {
        Query query = em.createQuery(
                " select dt from DetalleTransito dt where dt.cierre.codigo = :idCierre "
                        + " order by dt.codigo ");
        query.setParameter("idCierre", idCierre);
        return query.getResultList();
    }

    @Override
    public DetalleTransito selectPendientePorDetalleAsiento(Long idDetalleAsiento) throws Throwable {
        Query query = em.createQuery(
                " select dt from DetalleTransito dt "
                        + " where dt.detalleAsiento.codigo = :id and dt.estado = :pendiente ");
        query.setParameter("id", idDetalleAsiento);
        query.setParameter("pendiente", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        try {
            return (DetalleTransito) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public DetalleTransito selectPendientePorDetalleExtracto(Long idDetalleExtracto) throws Throwable {
        Query query = em.createQuery(
                " select dt from DetalleTransito dt "
                        + " where dt.detalleExtracto.codigo = :id and dt.estado = :pendiente ");
        query.setParameter("id", idDetalleExtracto);
        query.setParameter("pendiente", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        try {
            return (DetalleTransito) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public DetalleTransito selectPendientePorAsiento(Long idAsiento) throws Throwable {
        Query query = em.createQuery(
                " select dt from DetalleTransito dt "
                        + " where dt.detalleAsiento.asiento.codigo = :id and dt.estado = :pendiente ");
        query.setParameter("id", idAsiento);
        query.setParameter("pendiente", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        query.setMaxResults(1);
        List<DetalleTransito> encontradas = query.getResultList();
        return (encontradas == null || encontradas.isEmpty()) ? null : encontradas.get(0);
    }

    @Override
    public DetalleTransito selectPorAsiento(Long idAsiento) throws Throwable {
        Query query = em.createQuery(
                " select dt from DetalleTransito dt where dt.detalleAsiento.asiento.codigo = :id ");
        query.setParameter("id", idAsiento);
        query.setMaxResults(1);
        List<DetalleTransito> encontradas = query.getResultList();
        return (encontradas == null || encontradas.isEmpty()) ? null : encontradas.get(0);
    }

    @Override
    public DetalleTransito selectPorDetalleExtracto(Long idDetalleExtracto) throws Throwable {
        Query query = em.createQuery(
                " select dt from DetalleTransito dt where dt.detalleExtracto.codigo = :id ");
        query.setParameter("id", idDetalleExtracto);
        try {
            return (DetalleTransito) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<DetalleTransito> selectPendientesPorCuenta(Long idCuentaBancaria) throws Throwable {
        // El lado de libros (tipo 1/2) ancla en DetalleAsiento desde el 2026-08-27 (§7bis): se
        // ubica la cuenta bancaria por el plan de cuentas de la linea, no por MovimientoBanco -
        // ver la nota en la entidad DetalleTransito.
        Query query = em.createQuery(
                " select dt from DetalleTransito dt "
                        + " where dt.estado = :pendiente "
                        + " and ( "
                        + "   (dt.detalleAsiento is not null and dt.detalleAsiento.planCuenta.codigo = "
                        + "     (select cb.planCuenta.codigo from CuentaBancaria cb where cb.codigo = :idCuenta)) "
                        + "   or "
                        + "   (dt.detalleExtracto is not null and dt.detalleExtracto.cuentaBancaria.codigo = :idCuenta) "
                        + " ) "
                        + " order by dt.fechaRegistro ");
        query.setParameter("pendiente", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        query.setParameter("idCuenta", idCuentaBancaria);
        return query.getResultList();
    }

    @Override
    public List<DetalleTransito> selectPendientesAntiguas(Long idEmpresa, LocalDateTime diasCorte) throws Throwable {
        StringBuilder jpql = new StringBuilder(
                " select dt from DetalleTransito dt "
                        + " where dt.estado = :pendiente "
                        + " and dt.fechaRegistro < :diasCorte ");
        if (idEmpresa != null) {
            jpql.append(" and ( "
                    + "   (dt.detalleAsiento is not null and dt.detalleAsiento.asiento.empresa.codigo = :idEmpresa) "
                    + "   or "
                    + "   (dt.detalleExtracto is not null and dt.detalleExtracto.cuentaBancaria.banco.empresa.codigo = :idEmpresa) "
                    + " ) ");
        }
        jpql.append(" order by dt.fechaRegistro ");
        Query query = em.createQuery(jpql.toString());
        query.setParameter("pendiente", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        query.setParameter("diasCorte", diasCorte);
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        return query.getResultList();
    }
}
