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
        if (idAsoprep == null) {
            return null;
        }
        
        try {
            Query query = em.createQuery("SELECT p FROM Prestamo p WHERE p.idAsoprep = :idAsoprep");
            query.setParameter("idAsoprep", idAsoprep);
            
            @SuppressWarnings("unchecked")
            List<Prestamo> resultados = query.getResultList();
            
            if (resultados != null && !resultados.isEmpty()) {
                return resultados.get(0);
            }
            
            return null;
            
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            System.err.println("Error al buscar préstamo por idAsoprep: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectByEntidadYProductoActivos(String rolPetroComercial, String codigoPetroProducto) throws Throwable {
        try {
            // Convertir el rolPetroComercial a Long ya que el campo en la entidad es Long
            Long rolPetroLong = null;
            if (rolPetroComercial != null && !rolPetroComercial.trim().isEmpty()) {
                try {
                    rolPetroLong = Long.parseLong(rolPetroComercial);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El ROL Petro debe ser un número válido: " + rolPetroComercial, e);
                }
            }
            
            String jpql = "SELECT p FROM Prestamo p " +
                         "WHERE p.entidad.rolPetroComercial = :rolPetro " +
                         "AND p.producto.codigoPetro = :codigoProducto " +
                         "AND p.idEstado IN (2, 8, 10, 11)";
            
            Query query = em.createQuery(jpql);
            query.setParameter("rolPetro", rolPetroLong);
            query.setParameter("codigoProducto", codigoPetroProducto);
            
            return query.getResultList();
            
        } catch (Exception e) {
            System.err.println("Error al buscar préstamos activos: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectByEntidadYProductoActivosById(Long codigoEntidad, Long codigoProducto) throws Throwable {
        try {
            String jpql = "SELECT p FROM Prestamo p " +
                         "WHERE p.entidad.codigo = :codigoEntidad " +
                         "AND p.producto.codigo = :codigoProducto " +
                         "AND p.idEstado IN (2, 8, 10, 11)";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoEntidad", codigoEntidad);
            query.setParameter("codigoProducto", codigoProducto);
            
            return query.getResultList();
            
        } catch (Exception e) {
            System.err.println("Error al buscar préstamos activos por IDs: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso
            // El error se registrará como novedad en el nivel superior
            return new java.util.ArrayList<>();
        }
    }

}
