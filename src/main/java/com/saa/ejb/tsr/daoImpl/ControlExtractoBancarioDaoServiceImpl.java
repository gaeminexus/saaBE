/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.ControlExtractoBancarioDaoService;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.rubros.EstadoCuentasBancarias;
import com.saa.rubros.EstadosConciliacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ControlExtractoBancarioDaoService.
 */
@Stateless
public class ControlExtractoBancarioDaoServiceImpl extends EntityDaoImpl<ControlExtractoBancario>
        implements ControlExtractoBancarioDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) ControlExtractoBancario");
        return new String[]{
            "codigo",
            "empresa",
            "periodo",
            "mes",
            "anio",
            "fechaVencimiento",
            "totalCuentas",
            "cuentasCargadas",
            "cuentasConciliadas",
            "observaciones",
            "fechaCreacion",
            "estado"
        };
    }

    @Override
    public ControlExtractoBancario selectByEmpresaYPeriodo(Long idEmpresa, Long mes, Long anio) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresaYPeriodo con idEmpresa: " + idEmpresa
                + ", mes: " + mes + ", anio: " + anio);
        try {
            Query query = em.createNamedQuery("ControlExtractoBancarioByEmpresaYPeriodo");
            query.setParameter("idEmpresa", idEmpresa);
            query.setParameter("mes", mes);
            query.setParameter("anio", anio);
            return (ControlExtractoBancario) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Long> selectCuentasActivasPorEmpresa(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectCuentasActivasPorEmpresa con idEmpresa: " + idEmpresa);
        Query query = em.createQuery(
            " select c.codigo from CuentaBancaria c " +
            " where c.banco.empresa.codigo = :idEmpresa " +
            " and c.estado = :estadoActivo");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("estadoActivo", Long.valueOf(EstadoCuentasBancarias.ACTIVO));
        return query.getResultList();
    }

    @Override
    public Long contarCuentasConciliadas(List<Long> idsCuenta, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo contarCuentasConciliadas con " + idsCuenta.size()
                + " cuentas, idPeriodo: " + idPeriodo);
        if (idsCuenta.isEmpty()) {
            return 0L;
        }
        Query query = em.createQuery(
            " select count(distinct c.cuentaBancaria.codigo) from Conciliacion c " +
            " where c.cuentaBancaria.codigo in :idsCuenta " +
            " and c.idPeriodo = :idPeriodo " +
            " and c.rubroEstadoH = :estadoConciliado");
        query.setParameter("idsCuenta", idsCuenta);
        query.setParameter("idPeriodo", idPeriodo);
        query.setParameter("estadoConciliado", Long.valueOf(EstadosConciliacion.CONCILIADO));
        return (Long) query.getSingleResult();
    }
}
