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
import com.saa.model.tsr.CuentaBancaria;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoConciliacionContable;
import com.saa.rubros.EstadoCuentasBancarias;

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
            "estado",
            "cerrado",
            "usuarioCierre",
            "fechaCierre"
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
        // ConciliacionContable (verificacion linea a linea extracto-vs-asiento)
        // reemplazo al viejo Conciliacion/rubroEstadoH, que quedo huerfano sin
        // ningun llamador real - ver conversacion de auditoria previa.
        Query query = em.createQuery(
            " select count(distinct c.cuentaBancaria.codigo) from ConciliacionContable c " +
            " where c.cuentaBancaria.codigo in :idsCuenta " +
            " and c.periodo.codigo = :idPeriodo " +
            " and c.estadoRevision = :estadoVerificado " +
            " and c.estado = :estadoActivo");
        query.setParameter("idsCuenta", idsCuenta);
        query.setParameter("idPeriodo", idPeriodo);
        query.setParameter("estadoVerificado", Long.valueOf(EstadoConciliacionContable.VERIFICADO));
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return (Long) query.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CuentaBancaria> selectCuentasBancariasActivas(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectCuentasBancariasActivas con idEmpresa: " + idEmpresa);
        Query query = em.createQuery(
            " select c from CuentaBancaria c " +
            " where c.banco.empresa.codigo = :idEmpresa " +
            " and c.estado = :estadoActivo " +
            " order by c.banco.nombre, c.numeroCuenta");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("estadoActivo", Long.valueOf(EstadoCuentasBancarias.ACTIVO));
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Long> selectCuentasConciliadas(List<Long> idsCuenta, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo selectCuentasConciliadas con " + idsCuenta.size()
                + " cuentas, idPeriodo: " + idPeriodo);
        if (idsCuenta.isEmpty()) {
            return List.of();
        }
        // Ver nota en contarCuentasConciliadas: ConciliacionContable reemplaza
        // al viejo Conciliacion/rubroEstadoH.
        Query query = em.createQuery(
            " select distinct c.cuentaBancaria.codigo from ConciliacionContable c " +
            " where c.cuentaBancaria.codigo in :idsCuenta " +
            " and c.periodo.codigo = :idPeriodo " +
            " and c.estadoRevision = :estadoVerificado " +
            " and c.estado = :estadoActivo");
        query.setParameter("idsCuenta", idsCuenta);
        query.setParameter("idPeriodo", idPeriodo);
        query.setParameter("estadoVerificado", Long.valueOf(EstadoConciliacionContable.VERIFICADO));
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Long> selectPeriodosCerrados(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectPeriodosCerrados con idEmpresa: " + idEmpresa);
        Query query = em.createQuery(
            " select c.periodo.codigo from ControlExtractoBancario c " +
            " where c.empresa.codigo = :idEmpresa " +
            " and c.cerrado = 1 ");
        query.setParameter("idEmpresa", idEmpresa);
        return query.getResultList();
    }
}
