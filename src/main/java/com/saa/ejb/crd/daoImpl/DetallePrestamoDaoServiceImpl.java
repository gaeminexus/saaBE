package com.saa.ejb.crd.daoImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.model.crd.DetallePrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetallePrestamoDaoServiceImpl extends EntityDaoImpl<DetallePrestamo> implements DetallePrestamoDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePrestamo> selectByRangoFechas(LocalDateTime fechaDesde, LocalDateTime fechaHasta)
			throws Throwable {
		System.out.println("Buscando detalles de préstamo entre fechas: " + fechaDesde + " y " + fechaHasta);
		
		try {
			String jpql = "SELECT d FROM DetallePrestamo d " +
						 "WHERE d.fechaVencimiento >= :fechaDesde " +
						 "AND d.fechaVencimiento <= :fechaHasta " +
						 "ORDER BY d.fechaVencimiento";
			
			Query query = em.createQuery(jpql);
			query.setParameter("fechaDesde", fechaDesde);
			query.setParameter("fechaHasta", fechaHasta);
			
			// @SuppressWarnings("unchecked")
			List<DetallePrestamo> resultados = query.getResultList();
			
			System.out.println("Detalles encontrados: " + (resultados != null ? resultados.size() : 0));
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar detalles por rango de fechas: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePrestamo> selectByPrestamoYMesAnio(Long codigoPrestamo, Integer mes, Integer anio) throws Throwable {
		System.out.println("Buscando cuota del préstamo " + codigoPrestamo + " para " + mes + "/" + anio);
		
		try {
			String jpql = "SELECT d FROM DetallePrestamo d " +
						 "WHERE d.prestamo.codigo = :codigoPrestamo " +
						 "AND MONTH(d.fechaVencimiento) = :mes " +
						 "AND YEAR(d.fechaVencimiento) = :anio " +
						 "ORDER BY d.numeroCuota";
			
			Query query = em.createQuery(jpql);
			query.setParameter("codigoPrestamo", codigoPrestamo);
			query.setParameter("mes", mes);
			query.setParameter("anio", anio);
			
			List<DetallePrestamo> resultados = query.getResultList();
			System.out.println("Cuotas encontradas: " + (resultados != null ? resultados.size() : 0));
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar cuota por mes/año: " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			return new java.util.ArrayList<>();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePrestamo> selectByPrestamo(Long codigoPrestamo) throws Throwable {
		System.out.println("Buscando todas las cuotas del préstamo: " + codigoPrestamo);
		
		try {
			String jpql = "SELECT d FROM DetallePrestamo d " +
						 "WHERE d.prestamo.codigo = :codigoPrestamo " +
						 "ORDER BY d.numeroCuota";
			
			Query query = em.createQuery(jpql);
			query.setParameter("codigoPrestamo", codigoPrestamo);
			
			List<DetallePrestamo> resultados = query.getResultList();
			System.out.println("Total de cuotas encontradas: " + (resultados != null ? resultados.size() : 0));
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar cuotas del préstamo: " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			return new java.util.ArrayList<>();
		}
	}

}