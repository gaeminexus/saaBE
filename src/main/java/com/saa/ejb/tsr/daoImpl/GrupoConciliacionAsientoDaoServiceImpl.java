/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.tsr.GrupoConciliacionAsiento;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion GrupoConciliacionAsientoDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class GrupoConciliacionAsientoDaoServiceImpl extends EntityDaoImpl<GrupoConciliacionAsiento>
        implements GrupoConciliacionAsientoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{"codigo", "grupo", "detalleAsiento"};
    }

    @Override
    public List<GrupoConciliacionAsiento> selectByGrupo(Long idGrupo) throws Throwable {
        Query query = em.createQuery(
            " select e from GrupoConciliacionAsiento e where e.grupo.codigo = :idGrupo ");
        query.setParameter("idGrupo", idGrupo);
        return query.getResultList();
    }

    @Override
    public List<DetalleAsiento> selectPendientes(Long idPlanCuenta, Long idEmpresa, LocalDate primerDia,
            LocalDate ultimoDia) throws Throwable {
        System.out.println("Ingresa al metodo selectPendientes (asiento) con idPlanCuenta: " + idPlanCuenta
                + ", idEmpresa: " + idEmpresa + ", entre " + primerDia + " y " + ultimoDia);
        Query query = em.createQuery(
            " select d from DetalleAsiento d " +
            " where d.planCuenta.codigo = :idPlanCuenta " +
            " and d.asiento.empresa.codigo = :idEmpresa " +
            " and d.asiento.fechaAsiento between :primerDia and :ultimoDia " +
            " and d.asiento.estado in (1,3) " +
            " and d.codigo not in ( " +
            "     select g.detalleAsiento.codigo from GrupoConciliacionAsiento g " +
            "     where g.grupo.estado = :estadoActivo " +
            " ) " +
            " order by d.asiento.fechaAsiento, d.asiento.codigo ");
        query.setParameter("idPlanCuenta", idPlanCuenta);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("primerDia", primerDia);
        query.setParameter("ultimoDia", ultimoDia);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

    @Override
    public Long contarPendientes(Long idPlanCuenta, Long idEmpresa, LocalDate primerDia, LocalDate ultimoDia)
            throws Throwable {
        Query query = em.createQuery(
            " select count(d) from DetalleAsiento d " +
            " where d.planCuenta.codigo = :idPlanCuenta " +
            " and d.asiento.empresa.codigo = :idEmpresa " +
            " and d.asiento.fechaAsiento between :primerDia and :ultimoDia " +
            " and d.asiento.estado in (1,3) " +
            " and d.codigo not in ( " +
            "     select g.detalleAsiento.codigo from GrupoConciliacionAsiento g " +
            "     where g.grupo.estado = :estadoActivo " +
            " ) ");
        query.setParameter("idPlanCuenta", idPlanCuenta);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("primerDia", primerDia);
        query.setParameter("ultimoDia", ultimoDia);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return (Long) query.getSingleResult();
    }

    @Override
    public List<Long> selectIdsEnGrupoActivo(List<Long> idsDetalleAsiento) throws Throwable {
        if (idsDetalleAsiento == null || idsDetalleAsiento.isEmpty()) {
            return List.of();
        }
        Query query = em.createQuery(
            " select g.detalleAsiento.codigo from GrupoConciliacionAsiento g " +
            " where g.detalleAsiento.codigo in :ids " +
            " and g.grupo.estado = :estadoActivo ");
        query.setParameter("ids", idsDetalleAsiento);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }
}
