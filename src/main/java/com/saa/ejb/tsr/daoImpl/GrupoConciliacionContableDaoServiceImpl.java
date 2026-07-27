/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.GrupoConciliacionContableDaoService;
import com.saa.model.tsr.GrupoConciliacionContable;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion GrupoConciliacionContableDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class GrupoConciliacionContableDaoServiceImpl extends EntityDaoImpl<GrupoConciliacionContable>
        implements GrupoConciliacionContableDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "conciliacionContable",
            "valorExtracto",
            "valorAsiento",
            "diferencia",
            "fechaMinima",
            "fechaMaxima",
            "toleranciaDiasAplicada",
            "usuarioConcilia",
            "fechaConciliacion",
            "observaciones",
            "estado"
        };
    }

    @Override
    public List<GrupoConciliacionContable> selectActivosByConciliacion(Long idConciliacionContable) throws Throwable {
        System.out.println("Ingresa al metodo selectActivosByConciliacion con idConciliacionContable: "
                + idConciliacionContable);
        Query query = em.createQuery(
            " select e from GrupoConciliacionContable e " +
            " where e.conciliacionContable.codigo = :idConciliacionContable " +
            " and e.estado = :estadoActivo " +
            " order by e.fechaConciliacion desc ");
        query.setParameter("idConciliacionContable", idConciliacionContable);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }
}
