package com.saa.ejb.crd.daoImpl;


import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ProcesamientoCargaArchivoDaoService;
import com.saa.model.crd.ProcesamientoCargaArchivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class ProcesamientoCargaArchivoDaoServiceImpl extends EntityDaoImpl<ProcesamientoCargaArchivo> 
    implements ProcesamientoCargaArchivoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public ProcesamientoCargaArchivo selectByParticipeXCarga(Long idParticipeXCarga) throws Throwable {
        try {
            String jpql = "SELECT p FROM ProcesamientoCargaArchivo p " +
                         "WHERE p.participeXCargaArchivo.codigo = :idParticipe " +
                         "ORDER BY p.fechaRegistro DESC";
            
            TypedQuery<ProcesamientoCargaArchivo> query = em.createQuery(jpql, ProcesamientoCargaArchivo.class);
            query.setParameter("idParticipe", idParticipeXCarga);
            query.setMaxResults(1);
            
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public boolean yaFueProcesado(Long idParticipeXCarga) throws Throwable {
        String jpql = "SELECT COUNT(p) FROM ProcesamientoCargaArchivo p " +
                     "WHERE p.participeXCargaArchivo.codigo = :idParticipe " +
                     "AND p.procesado = 1";
        
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        query.setParameter("idParticipe", idParticipeXCarga);
        
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

}
