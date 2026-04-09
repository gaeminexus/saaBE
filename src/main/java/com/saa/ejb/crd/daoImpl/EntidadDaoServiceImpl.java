package com.saa.ejb.crd.daoImpl;

import java.math.BigDecimal;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.model.crd.Entidad;
import com.saa.rubros.ASPSensibilidadBusquedaCoincidencias;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class EntidadDaoServiceImpl extends EntityDaoImpl<Entidad> implements EntidadDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	@EJB
	DetalleRubroDaoService detalleRubroDaoService;
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Entidad> selectByCodigoPetro(Long codigoPetro) throws Throwable {
		try {
			// System.out.println("Ingresa al metodo selectByCodigoPetro con codigoPetro: " + codigoPetro);
			Query query = em.createQuery(" select b " +
										 " from   Entidad b" +
										 " where  b.rolPetroComercial = :codigoPetro");
			query.setParameter("codigoPetro", codigoPetro);
			return  query.getResultList();
		} catch (Exception e) {
			System.err.println("Error al buscar entidad por código Petro " + codigoPetro + ": " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			// El error se registrará como novedad en el nivel superior
			return new java.util.ArrayList<>();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BigDecimal> selectCoincidenciasByNombre(String nombre) throws Throwable {
		System.out.println("Ingresa al metodo selectCoincidenciasByNombre de asiento con empresa: " + nombre);
		Double sensibilidad = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
				Rubros.ASP_SENSIBILIDAD_BUSQUEDA_COINCIDENCIAS, 
				ASPSensibilidadBusquedaCoincidencias.PORCENTAJE_SENSIBILIDAD);
		Query query = em.createNativeQuery(" select   e.ENTDCDGO " +
									 	   " from     CRD.ENTD e " +
									 	   " where    UTL_MATCH.JARO_WINKLER_SIMILARITY(e.ENTDNMCM, :nombre) > :sensibilidad ");
		query.setParameter("nombre", nombre);		
		query.setParameter("sensibilidad", sensibilidad);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Entidad> selectByNombrePetro35(String nombre) throws Throwable {
		try {
			// System.out.println("Ingresa al metodo selectByNombrePetro35 con nombre: " + nombre);
			Query query = em.createQuery(" select b " +
										 " from   Entidad b " +
										 " where  substring(trim(b.razonSocial),1,35) = trim(:nombre) ");
			query.setParameter("nombre", nombre);
			return  query.getResultList();
		} catch (Exception e) {
			System.err.println("Error al buscar entidad por nombre Petro '" + nombre + "': " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			return new java.util.ArrayList<>();
		}
	}

	/**
	 * Obtiene resumen de entidades agrupadas por estado
	 * Query optimizada para dashboard
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.EntidadResumenEstadoDTO> selectResumenPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable {
		
		// Validar que la lista no esté vacía
		if (estadosPermitidos == null || estadosPermitidos.isEmpty()) {
			estadosPermitidos = java.util.Arrays.asList(10L, 2L, 30L); // Valores por defecto
		}
		
		String sql = 
			"SELECT " +
			"    e.ENTDIDST AS estado_id, " +
			"    COUNT(*) AS total_entidades " +
			"FROM CRD.ENTD e " +
			"WHERE e.ENTDIDST IN (:estadosPermitidos) " +
			"GROUP BY e.ENTDIDST " +
			"ORDER BY e.ENTDIDST";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("estadosPermitidos", estadosPermitidos);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.EntidadResumenEstadoDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long estadoId = row[0] != null ? ((Number) row[0]).longValue() : null;
			Long totalEntidades = row[1] != null ? ((Number) row[1]).longValue() : 0L;
			
			dtos.add(new com.saa.model.crd.dto.EntidadResumenEstadoDTO(estadoId, totalEntidades));
		}
		
		return dtos;
	}

	/**
	 * Obtiene resumen de préstamos agrupados por estado de entidad
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.EntidadResumenPrestamosDTO> selectResumenPrestamosPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable {
		
		if (estadosPermitidos == null || estadosPermitidos.isEmpty()) {
			estadosPermitidos = java.util.Arrays.asList(10L, 2L, 30L);
		}
		
		String sql = 
			"SELECT " +
			"    e.ENTDIDST AS estado_id, " +
			"    SUM(NVL(p.PRSTTTPR, NVL(p.PRSTMNSL, 0))) AS total_prestamos " +
			"FROM CRD.ENTD e " +
			"JOIN CRD.PRST p ON p.ENTDCDGO = e.ENTDCDGO " +
			"WHERE e.ENTDIDST IN (:estadosPermitidos) " +
			"GROUP BY e.ENTDIDST " +
			"ORDER BY e.ENTDIDST";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("estadosPermitidos", estadosPermitidos);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.EntidadResumenPrestamosDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long estadoId = row[0] != null ? ((Number) row[0]).longValue() : null;
			Double totalPrestamos = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
			
			dtos.add(new com.saa.model.crd.dto.EntidadResumenPrestamosDTO(estadoId, totalPrestamos));
		}
		
		return dtos;
	}

	/**
	 * Obtiene resumen de aportes agrupados por estado de entidad
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.EntidadResumenAportesDTO> selectResumenAportesPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable {
		
		if (estadosPermitidos == null || estadosPermitidos.isEmpty()) {
			estadosPermitidos = java.util.Arrays.asList(10L, 2L, 30L);
		}
		
		String sql = 
			"SELECT " +
			"    e.ENTDIDST AS estado_id, " +
			"    SUM(NVL(a.APRTVLRR, 0)) AS total_aportes " +
			"FROM CRD.ENTD e " +
			"JOIN CRD.APRT a ON a.ENTDCDGO = e.ENTDCDGO " +
			"WHERE e.ENTDIDST IN (:estadosPermitidos) " +
			"GROUP BY e.ENTDIDST " +
			"ORDER BY e.ENTDIDST";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("estadosPermitidos", estadosPermitidos);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.EntidadResumenAportesDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long estadoId = row[0] != null ? ((Number) row[0]).longValue() : null;
			Double totalAportes = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
			
			dtos.add(new com.saa.model.crd.dto.EntidadResumenAportesDTO(estadoId, totalAportes));
		}
		
		return dtos;
	}

	/**
	 * Obtiene resumen consolidado (entidades, préstamos y aportes) por estado
	 * Query optimizada con subqueries para evitar duplicados
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.EntidadResumenConsolidadoDTO> selectResumenConsolidadoPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable {
		
		if (estadosPermitidos == null || estadosPermitidos.isEmpty()) {
			estadosPermitidos = java.util.Arrays.asList(10L, 2L, 30L);
		}
		
		String sql = 
			"SELECT " +
			"    e.ENTDIDST AS estado_id, " +
			"    COUNT(*) AS total_entidades, " +
			"    NVL(SUM(pr.total_prestamos), 0) AS total_prestamos, " +
			"    NVL(SUM(ap.total_aportes), 0) AS total_aportes " +
			"FROM CRD.ENTD e " +
			"LEFT JOIN ( " +
			"    SELECT " +
			"        p.ENTDCDGO, " +
			"        SUM(NVL(p.PRSTTTPR, NVL(p.PRSTMNSL, 0))) AS total_prestamos " +
			"    FROM CRD.PRST p " +
			"    GROUP BY p.ENTDCDGO " +
			") pr ON pr.ENTDCDGO = e.ENTDCDGO " +
			"LEFT JOIN ( " +
			"    SELECT " +
			"        a.ENTDCDGO, " +
			"        SUM(NVL(a.APRTVLRR, 0)) AS total_aportes " +
			"    FROM CRD.APRT a " +
			"    GROUP BY a.ENTDCDGO " +
			") ap ON ap.ENTDCDGO = e.ENTDCDGO " +
			"WHERE e.ENTDIDST IN (:estadosPermitidos) " +
			"GROUP BY e.ENTDIDST " +
			"ORDER BY e.ENTDIDST";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("estadosPermitidos", estadosPermitidos);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.EntidadResumenConsolidadoDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long estadoId = row[0] != null ? ((Number) row[0]).longValue() : null;
			Long totalEntidades = row[1] != null ? ((Number) row[1]).longValue() : 0L;
			Double totalPrestamos = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
			Double totalAportes = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
			
			dtos.add(new com.saa.model.crd.dto.EntidadResumenConsolidadoDTO(
				estadoId, totalEntidades, totalPrestamos, totalAportes
			));
		}
		
		return dtos;
	}

}
