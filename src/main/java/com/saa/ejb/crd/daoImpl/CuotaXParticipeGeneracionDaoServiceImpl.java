package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CuotaXParticipeGeneracionDaoService;
import com.saa.model.crd.CuotaXParticipeGeneracion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para CuotaXParticipeGeneracion (CXPG).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class CuotaXParticipeGeneracionDaoServiceImpl extends EntityDaoImpl<CuotaXParticipeGeneracion> 
        implements CuotaXParticipeGeneracionDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<CuotaXParticipeGeneracion> selectByParticipe(Long codigoParticipe) throws Throwable {
        System.out.println("Buscando cuotas del partícipe detalle: " + codigoParticipe);
        
        try {
            Query query = em.createNamedQuery("CuotaXParticipeGeneracionByParticipe");
            query.setParameter("codigoParticipe", codigoParticipe);
            
            @SuppressWarnings("unchecked")
            List<CuotaXParticipeGeneracion> resultados = query.getResultList();
            
            System.out.println("Cuotas encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar cuotas por partícipe: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<CuotaXParticipeGeneracion> selectByPrestamoYGeneracion(Long codigoPrestamo, Long codigoGeneracion) throws Throwable {
        System.out.println("Buscando cuotas - Préstamo: " + codigoPrestamo + " - Generación: " + codigoGeneracion);
        
        try {
            String jpql = "SELECT c FROM CuotaXParticipeGeneracion c " +
                         "WHERE c.prestamo.codigo = :codigoPrestamo " +
                         "AND c.participeDetalleGeneracion.detalleGeneracionArchivo.generacionArchivoPetro.codigo = :codigoGeneracion " +
                         "ORDER BY c.numeroCuota";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoPrestamo", codigoPrestamo);
            query.setParameter("codigoGeneracion", codigoGeneracion);
            
            @SuppressWarnings("unchecked")
            List<CuotaXParticipeGeneracion> resultados = query.getResultList();
            
            System.out.println("Cuotas encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar cuotas por préstamo y generación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
