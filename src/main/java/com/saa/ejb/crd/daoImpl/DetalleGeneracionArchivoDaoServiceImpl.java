package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleGeneracionArchivoDaoService;
import com.saa.model.crd.DetalleGeneracionArchivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para DetalleGeneracionArchivo (DTGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class DetalleGeneracionArchivoDaoServiceImpl extends EntityDaoImpl<DetalleGeneracionArchivo> 
        implements DetalleGeneracionArchivoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<DetalleGeneracionArchivo> selectByGeneracion(Long codigoGeneracion) throws Throwable {
        System.out.println("Buscando detalles de la generación: " + codigoGeneracion);
        
        try {
            Query query = em.createNamedQuery("DetalleGeneracionArchivoByGeneracion");
            query.setParameter("codigoGeneracion", codigoGeneracion);
            
            @SuppressWarnings("unchecked")
            List<DetalleGeneracionArchivo> resultados = query.getResultList();
            
            System.out.println("Detalles encontrados: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar detalles de generación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public DetalleGeneracionArchivo selectByGeneracionYProducto(Long codigoGeneracion, String codigoProducto) throws Throwable {
        System.out.println("Buscando detalle - Generación: " + codigoGeneracion + " - Producto: " + codigoProducto);
        
        try {
            String jpql = "SELECT d FROM DetalleGeneracionArchivo d " +
                         "WHERE d.generacionArchivoPetro.codigo = :codigoGeneracion " +
                         "AND d.codigoProductoPetro = :codigoProducto";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoGeneracion", codigoGeneracion);
            query.setParameter("codigoProducto", codigoProducto);
            
            DetalleGeneracionArchivo resultado = (DetalleGeneracionArchivo) query.getSingleResult();
            System.out.println("Detalle encontrado: " + resultado.getCodigo());
            return resultado;
            
        } catch (NoResultException e) {
            System.out.println("No existe detalle para el producto especificado");
            return null;
        } catch (Exception e) {
            System.err.println("Error al buscar detalle por generación y producto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
