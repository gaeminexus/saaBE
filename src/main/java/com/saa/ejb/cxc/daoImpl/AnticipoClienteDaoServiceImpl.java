package com.saa.ejb.cxc.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.rubros.EstadoAnticipoCliente;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AnticipoClienteDaoServiceImpl extends EntityDaoImpl<AnticipoCliente>
        implements AnticipoClienteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "titular",
            "fechaAnticipo",
            "fechaRecepcion",
            "usuario",
            "fechaRegistro",
            "numeroDoc",
            "valor",
            "asiento",
            "estado",
            "empresa",
            "observacion"
        };
    }

    @Override
    public List<AnticipoCliente> selectDisponiblesByTitular(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo selectDisponiblesByTitular con titular: " + idTitular
                + " | empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select a from AnticipoCliente a " +
                " where  a.titular.codigo = :idTitular " +
                " and    a.empresa.codigo = :idEmpresa " +
                " and    a.estado = :confirmado " +
                " and    a.valor > 0 " +
                " and    a.saldo > 0 " +
                " order by a.fechaAnticipo, a.id");
        query.setParameter("idTitular", idTitular);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("confirmado", Long.valueOf(EstadoAnticipoCliente.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<AnticipoCliente> selectMovimientosByTitular(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo selectMovimientosByTitular con titular: " + idTitular
                + " | empresa: " + idEmpresa);
        // Se excluyen los movimientos negativos históricos (estado Migrado):
        // el cruce se lee ahora de CBR.APLC, incluirlos duplicaría la resta.
        Query query = em.createQuery(
                " select a from AnticipoCliente a " +
                " where  a.titular.codigo = :idTitular " +
                " and    a.empresa.codigo = :idEmpresa " +
                " and    a.estado <> :migrado " +
                " and    a.valor > 0 " +
                " order by a.fechaAnticipo desc, a.id desc");
        query.setParameter("idTitular", idTitular);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("migrado", Long.valueOf(EstadoAnticipoCliente.MIGRADO));
        return query.getResultList();
    }

    @Override
    public Double sumaSaldoDisponible(Long idTitular, Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo sumaSaldoDisponible con titular: " + idTitular
                + " | empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select coalesce(sum(a.saldo), 0) from AnticipoCliente a " +
                " where  a.titular.codigo = :idTitular " +
                " and    a.empresa.codigo = :idEmpresa " +
                " and    a.estado = :confirmado " +
                " and    a.valor > 0");
        query.setParameter("idTitular", idTitular);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("confirmado", Long.valueOf(EstadoAnticipoCliente.CONFIRMADO));
        Object resultado = query.getSingleResult();
        return (resultado != null) ? ((Number) resultado).doubleValue() : 0.0;
    }

    @Override
    public List<AnticipoCliente> selectConDevolucionPendiente() throws Throwable {
        System.out.println("Ingresa al metodo selectConDevolucionPendiente");
        Query query = em.createQuery(
                " select a from AnticipoCliente a " +
                " where  a.idPagoDevolucion is not null " +
                " and    a.aplicado = 0 " +
                " order by a.id");
        return query.getResultList();
    }
}
