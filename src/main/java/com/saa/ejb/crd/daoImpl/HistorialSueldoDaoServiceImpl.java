package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.HistorialSueldoDaoService;
import com.saa.model.crd.HistorialSueldo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistorialSueldoDaoServiceImpl extends EntityDaoImpl<HistorialSueldo> implements HistorialSueldoDaoService{

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/**
	 * Busca todos los registros de HistorialSueldo para una entidad específica
	 * Retorna registros con estado = 1 (ACTIVO)
	 * 
	 * @param codigoEntidad Código de la entidad
	 * @return Lista de HistorialSueldo encontrados (vacía si hay error o no se encuentran)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<HistorialSueldo> selectByEntidad(Long codigoEntidad) {
		System.out.println("HistorialSueldoDaoService.selectByEntidad - Entidad: " + codigoEntidad);
		
		try {
			Query query = em.createQuery(
				"SELECT h " +
				"FROM HistorialSueldo h " +
				"WHERE h.entidad.codigo = :codigoEntidad " +
				"  AND h.estado = 1 " +
				"ORDER BY h.fechaIngreso DESC"
			);
			query.setParameter("codigoEntidad", codigoEntidad);
			
			List<HistorialSueldo> resultados = query.getResultList();
			System.out.println("  Registros encontrados: " + resultados.size());
			
			return resultados;
			
		} catch (Exception e) {
			System.err.println("ERROR en selectByEntidad: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

}
