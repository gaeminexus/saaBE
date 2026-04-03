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
	 * Retorna registros con estado = 99 (ACTIVO para HistorialSueldo)
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
				"  AND h.estado = 99 " +
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

	/**
	 * Busca el registro de HistorialSueldo activo (estado 99) para una entidad
	 * ✅ CRÍTICO: Estado 99 indica el registro vigente con los montos actuales de aportes
	 */
	@Override
	public HistorialSueldo selectByEntidadYEstadoActivo(Long codigoEntidad) throws Throwable {
		System.out.println("HistorialSueldoDaoService.selectByEntidadYEstadoActivo - Entidad: " + codigoEntidad);
		
		try {
			Query query = em.createQuery(
				"SELECT h " +
				"FROM HistorialSueldo h " +
				"WHERE h.entidad.codigo = :codigoEntidad " +
				"  AND h.estado = 99 " +
				"ORDER BY h.fechaIngreso DESC"
			);
			query.setParameter("codigoEntidad", codigoEntidad);
			query.setMaxResults(1); // Solo necesitamos el más reciente
			
			@SuppressWarnings("unchecked")
			List<HistorialSueldo> resultados = query.getResultList();
			
			if (resultados != null && !resultados.isEmpty()) {
				HistorialSueldo registro = resultados.get(0);
				System.out.println("  ✅ Registro encontrado - Jubilación: $" + registro.getMontoJubilacion() + 
				                   ", Cesantía: $" + registro.getMontoCesantia());
				return registro;
			}
			
			System.out.println("  ⚠️ No se encontró registro con estado 99");
			return null;
			
		} catch (Exception e) {
			System.err.println("ERROR en selectByEntidadYEstadoActivo: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

}
