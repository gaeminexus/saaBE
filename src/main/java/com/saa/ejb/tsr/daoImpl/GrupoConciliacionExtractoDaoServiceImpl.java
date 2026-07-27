/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.GrupoConciliacionExtractoDaoService;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.GrupoConciliacionExtracto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion GrupoConciliacionExtractoDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class GrupoConciliacionExtractoDaoServiceImpl extends EntityDaoImpl<GrupoConciliacionExtracto>
        implements GrupoConciliacionExtractoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{"codigo", "grupo", "detalleExtractoBancario"};
    }

    @Override
    public List<GrupoConciliacionExtracto> selectByGrupo(Long idGrupo) throws Throwable {
        Query query = em.createQuery(
            " select e from GrupoConciliacionExtracto e where e.grupo.codigo = :idGrupo ");
        query.setParameter("idGrupo", idGrupo);
        return query.getResultList();
    }

    @Override
    public List<DetalleExtractoBancario> selectPendientes(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo selectPendientes (extracto) con idCuentaBancaria: " + idCuentaBancaria
                + ", idPeriodo: " + idPeriodo);
        Query query = em.createQuery(
            " select d from DetalleExtractoBancario d " +
            " where d.cuentaBancaria.codigo = :idCuentaBancaria " +
            " and d.periodo.codigo = :idPeriodo " +
            " and d.estado = :estadoActivo " +
            " and d.codigo not in ( " +
            "     select g.detalleExtractoBancario.codigo from GrupoConciliacionExtracto g " +
            "     where g.grupo.estado = :estadoActivo " +
            " ) " +
            " order by d.fechaTransaccion, d.numeroFila ");
        query.setParameter("idCuentaBancaria", idCuentaBancaria);
        query.setParameter("idPeriodo", idPeriodo);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

    @Override
    public Long contarPendientes(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        Query query = em.createQuery(
            " select count(d) from DetalleExtractoBancario d " +
            " where d.cuentaBancaria.codigo = :idCuentaBancaria " +
            " and d.periodo.codigo = :idPeriodo " +
            " and d.estado = :estadoActivo " +
            " and d.codigo not in ( " +
            "     select g.detalleExtractoBancario.codigo from GrupoConciliacionExtracto g " +
            "     where g.grupo.estado = :estadoActivo " +
            " ) ");
        query.setParameter("idCuentaBancaria", idCuentaBancaria);
        query.setParameter("idPeriodo", idPeriodo);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return (Long) query.getSingleResult();
    }

    @Override
    public List<Long> selectIdsEnGrupoActivo(List<Long> idsDetalleExtracto) throws Throwable {
        if (idsDetalleExtracto == null || idsDetalleExtracto.isEmpty()) {
            return List.of();
        }
        Query query = em.createQuery(
            " select g.detalleExtractoBancario.codigo from GrupoConciliacionExtracto g " +
            " where g.detalleExtractoBancario.codigo in :ids " +
            " and g.grupo.estado = :estadoActivo ");
        query.setParameter("ids", idsDetalleExtracto);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }
}
