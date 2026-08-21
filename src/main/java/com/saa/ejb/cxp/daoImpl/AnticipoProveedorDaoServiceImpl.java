package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.AnticipoProveedorDaoService;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.rubros.EstadoAnticipoProveedor;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AnticipoProveedorDaoServiceImpl extends EntityDaoImpl<AnticipoProveedor>
        implements AnticipoProveedorDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "titular",
            "empresa",
            "fechaAnticipo",
            "fechaRecepcion",
            "numeroDoc",
            "valor",
            "saldo",
            "formaPago",
            "referencia",
            "banco",
            "observacion",
            "estado",
            "usuario",
            "asiento",
            "fechaRegistro"
        };
    }

    @Override
    public List<AnticipoProveedor> selectDisponiblesByTitular(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo selectDisponiblesByTitular con titular: " + idTitular
                + " | empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select a from AnticipoProveedor a " +
                " where  a.titular.codigo = :idTitular " +
                " and    a.empresa.codigo = :idEmpresa " +
                " and    a.estado = :confirmado " +
                " and    a.valor > 0 " +
                " and    a.saldo > 0 " +
                " order by a.fechaAnticipo, a.id");
        query.setParameter("idTitular", idTitular);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("confirmado", Long.valueOf(EstadoAnticipoProveedor.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<AnticipoProveedor> selectMovimientosByTitular(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo selectMovimientosByTitular con titular: " + idTitular
                + " | empresa: " + idEmpresa);
        // Se excluyen los movimientos negativos históricos (estado Migrado):
        // el cruce se lee ahora de PGS.APLP, incluirlos duplicaría la resta.
        Query query = em.createQuery(
                " select a from AnticipoProveedor a " +
                " where  a.titular.codigo = :idTitular " +
                " and    a.empresa.codigo = :idEmpresa " +
                " and    a.estado <> :migrado " +
                " and    a.valor > 0 " +
                " order by a.fechaAnticipo desc, a.id desc");
        query.setParameter("idTitular", idTitular);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("migrado", Long.valueOf(EstadoAnticipoProveedor.MIGRADO));
        return query.getResultList();
    }

    @Override
    public Double sumaSaldoDisponible(Long idTitular, Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo sumaSaldoDisponible con titular: " + idTitular
                + " | empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select coalesce(sum(a.saldo), 0) from AnticipoProveedor a " +
                " where  a.titular.codigo = :idTitular " +
                " and    a.empresa.codigo = :idEmpresa " +
                " and    a.estado = :confirmado " +
                " and    a.valor > 0");
        query.setParameter("idTitular", idTitular);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("confirmado", Long.valueOf(EstadoAnticipoProveedor.CONFIRMADO));
        Object resultado = query.getSingleResult();
        return (resultado != null) ? ((Number) resultado).doubleValue() : 0.0;
    }
}
