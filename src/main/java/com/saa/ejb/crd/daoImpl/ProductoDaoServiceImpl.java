package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.model.crd.Producto;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ProductoDaoServiceImpl extends EntityDaoImpl<Producto> implements ProductoDaoService {

    // Inicializa persistence context
    @PersistenceContext
    EntityManager em;

    @Override
    public Producto selectByCodigoPetro(String codigoPetro) throws Throwable {
        try {
            // Aplicar TRIM para evitar problemas con espacios
            String codigoPetroTrim = (codigoPetro != null) ? codigoPetro.trim() : null;
            
            String jpql = "SELECT p FROM Producto p " +
                         "WHERE TRIM(p.codigoPetro) = :codigoPetro " +
                         "ORDER BY p.codigo";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoPetro", codigoPetroTrim);
            
            @SuppressWarnings("unchecked")
            List<Producto> resultados = query.getResultList();
            
            if (resultados == null || resultados.isEmpty()) {
                return null;
            }
            
            return resultados.get(0);
            
        } catch (Exception e) {
            System.err.println("Error al buscar producto por código Petro: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<Producto> selectAllByCodigoPetro(String codigoPetro) throws Throwable {
        try {
            // Aplicar TRIM para evitar problemas con espacios
            String codigoPetroTrim = (codigoPetro != null) ? codigoPetro.trim() : null;
            
            String jpql = "SELECT p FROM Producto p " +
                         "WHERE TRIM(p.codigoPetro) = :codigoPetro " +
                         "ORDER BY p.codigo";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoPetro", codigoPetroTrim);
            
            @SuppressWarnings("unchecked")
            List<Producto> resultados = query.getResultList();
            
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar productos por código Petro: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

}