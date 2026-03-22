package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PrestamoDaoServiceImpl extends EntityDaoImpl<Prestamo> implements PrestamoDaoService {

    // Inicializa persistence context
    @PersistenceContext
    EntityManager em;

    @Override
    public Prestamo selectByIdAsoprep(Long idAsoprep) throws Throwable {
        System.out.println("Buscando préstamo por idAsoprep: " + idAsoprep);
        
        if (idAsoprep == null) {
            return null;
        }
        
        try {
            Query query = em.createQuery("SELECT p FROM Prestamo p WHERE p.idAsoprep = :idAsoprep");
            query.setParameter("idAsoprep", idAsoprep);
            
            @SuppressWarnings("unchecked")
            List<Prestamo> resultados = query.getResultList();
            
            if (resultados != null && !resultados.isEmpty()) {
                System.out.println("Préstamo encontrado con código: " + resultados.get(0).getCodigo());
                return resultados.get(0);
            }
            
            System.out.println("No se encontró préstamo con idAsoprep: " + idAsoprep);
            return null;
            
        } catch (NoResultException e) {
            System.out.println("No se encontró préstamo con idAsoprep: " + idAsoprep);
            return null;
        } catch (Exception e) {
            System.err.println("Error al buscar préstamo por idAsoprep: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

}
