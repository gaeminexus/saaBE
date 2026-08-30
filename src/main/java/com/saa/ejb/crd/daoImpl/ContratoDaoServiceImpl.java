package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.model.crd.Contrato;
import com.saa.rubros.EstadoContrato;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ContratoDaoServiceImpl extends EntityDaoImpl<Contrato> implements ContratoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<Contrato> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de Contrato con idEntidad: " + idEntidad);
        Query query = em.createQuery(
            " select c from Contrato c " +
            " where  c.entidad.codigo = :idEntidad " +
            " order by c.codigo desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public Contrato selectActivoPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectActivoPorEntidad de Contrato con idEntidad: " + idEntidad);
        try {
            Query query = em.createQuery(
                " select c from Contrato c " +
                " where  c.entidad.codigo = :idEntidad " +
                "   and  c.estado = :activo " +
                " order by c.codigo desc");
            query.setParameter("idEntidad", idEntidad);
            query.setParameter("activo", Long.valueOf(EstadoContrato.ACTIVO));
            query.setMaxResults(1);
            List<Contrato> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (NoResultException e) {
            return null;
        }
    }
}
