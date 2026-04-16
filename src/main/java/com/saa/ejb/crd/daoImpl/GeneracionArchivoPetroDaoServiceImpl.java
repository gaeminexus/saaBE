package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.GeneracionArchivoPetroDaoService;
import com.saa.model.crd.GeneracionArchivoPetro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para GeneracionArchivoPetro (GNAP).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class GeneracionArchivoPetroDaoServiceImpl extends EntityDaoImpl<GeneracionArchivoPetro> 
        implements GeneracionArchivoPetroDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public GeneracionArchivoPetro selectByPeriodo(Integer mes, Integer anio, Long codigoFilial) throws Throwable {
        System.out.println("Buscando generación por periodo: " + mes + "/" + anio + " - Filial: " + codigoFilial);
        
        try {
            Query query = em.createNamedQuery("GeneracionArchivoPetroByPeriodo");
            query.setParameter("mes", mes);
            query.setParameter("anio", anio);
            query.setParameter("codigoFilial", codigoFilial);
            
            GeneracionArchivoPetro resultado = (GeneracionArchivoPetro) query.getSingleResult();
            System.out.println("Generación encontrada: " + resultado.getCodigo());
            return resultado;
            
        } catch (NoResultException e) {
            System.out.println("No existe generación para el periodo especificado");
            return null;
        } catch (Exception e) {
            System.err.println("Error al buscar generación por periodo: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<GeneracionArchivoPetro> selectByFilial(Long codigoFilial) throws Throwable {
        System.out.println("Listando generaciones de la filial: " + codigoFilial);
        
        try {
            String jpql = "SELECT g FROM GeneracionArchivoPetro g " +
                         "WHERE g.filial.codigo = :codigoFilial " +
                         "ORDER BY g.fechaGeneracion DESC";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoFilial", codigoFilial);
            
            @SuppressWarnings("unchecked")
            List<GeneracionArchivoPetro> resultados = query.getResultList();
            
            System.out.println("Generaciones encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al listar generaciones por filial: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<GeneracionArchivoPetro> selectByEstado(Integer estado) throws Throwable {
        System.out.println("Listando generaciones con estado: " + estado);
        
        try {
            String jpql = "SELECT g FROM GeneracionArchivoPetro g " +
                         "WHERE g.estado = :estado " +
                         "ORDER BY g.fechaGeneracion DESC";
            
            Query query = em.createQuery(jpql);
            query.setParameter("estado", estado);
            
            @SuppressWarnings("unchecked")
            List<GeneracionArchivoPetro> resultados = query.getResultList();
            
            System.out.println("Generaciones encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al listar generaciones por estado: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
