/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.ConciliacionContableDaoService;
import com.saa.model.tsr.ConciliacionContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ConciliacionContableDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class ConciliacionContableDaoServiceImpl extends EntityDaoImpl<ConciliacionContable>
        implements ConciliacionContableDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "cuentaBancaria",
            "periodo",
            "estadoRevision",
            "totalGrupos",
            "totalPendientesExtracto",
            "totalPendientesAsiento",
            "usuarioVerifica",
            "fechaVerificacion",
            "fechaCreacion",
            "estado"
        };
    }

    @Override
    public ConciliacionContable selectByCuentaYPeriodo(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo selectByCuentaYPeriodo con idCuentaBancaria: " + idCuentaBancaria
                + ", idPeriodo: " + idPeriodo);
        try {
            Query query = em.createQuery(
                " select e from ConciliacionContable e " +
                " where e.cuentaBancaria.codigo = :idCuentaBancaria " +
                " and e.periodo.codigo = :idPeriodo ");
            query.setParameter("idCuentaBancaria", idCuentaBancaria);
            query.setParameter("idPeriodo", idPeriodo);
            return (ConciliacionContable) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
