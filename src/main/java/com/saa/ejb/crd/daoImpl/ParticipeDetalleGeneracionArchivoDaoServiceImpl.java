package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ParticipeDetalleGeneracionArchivoDaoService;
import com.saa.model.crd.ParticipeDetalleGeneracionArchivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para ParticipeDetalleGeneracionArchivo (PDGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class ParticipeDetalleGeneracionArchivoDaoServiceImpl extends EntityDaoImpl<ParticipeDetalleGeneracionArchivo> 
        implements ParticipeDetalleGeneracionArchivoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<ParticipeDetalleGeneracionArchivo> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("Buscando partícipes del detalle: " + codigoDetalle);
        
        try {
            Query query = em.createNamedQuery("ParticipeDetalleGeneracionArchivoByDetalle");
            query.setParameter("codigoDetalle", codigoDetalle);
            
            @SuppressWarnings("unchecked")
            List<ParticipeDetalleGeneracionArchivo> resultados = query.getResultList();
            
            System.out.println("Partícipes encontrados: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar partícipes por detalle: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> selectByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("Buscando registros de la entidad: " + codigoEntidad);
        
        try {
            Query query = em.createNamedQuery("ParticipeDetalleGeneracionArchivoByEntidad");
            query.setParameter("codigoEntidad", codigoEntidad);
            
            @SuppressWarnings("unchecked")
            List<ParticipeDetalleGeneracionArchivo> resultados = query.getResultList();
            
            System.out.println("Registros encontrados: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar registros por entidad: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> selectByGeneracion(Long codigoGeneracion) throws Throwable {
        System.out.println("Buscando todos los registros de la generación: " + codigoGeneracion);
        
        try {
            String jpql = "SELECT p FROM ParticipeDetalleGeneracionArchivo p " +
                         "WHERE p.detalleGeneracionArchivo.generacionArchivoPetro.codigo = :codigoGeneracion " +
                         "ORDER BY p.codigoProductoPetro, p.numeroLinea";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoGeneracion", codigoGeneracion);
            
            @SuppressWarnings("unchecked")
            List<ParticipeDetalleGeneracionArchivo> resultados = query.getResultList();
            
            System.out.println("Total registros encontrados: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar registros por generación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
