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
			String jpql = " SELECT d FROM DetallePrestamo d, Prestamo p " +
						 " WHERE d.prestamo.codigo = p.codigo " +
						 " AND d.fechaVencimiento >= :fechaDesde " +
						 " AND d.fechaVencimiento <= :fechaHasta " +
						 " AND p.idEstado IN (:estadoActivo, :estadoEnMora) " +
						 " ORDER BY d.fechaVencimiento";
			
			Query query = em.createQuery(jpql);
			query.setParameter("fechaDesde", fechaDesde);
			query.setParameter("fechaHasta", fechaHasta);
			query.setParameter("estadoActivo", 2L);
			query.setParameter("estadoEnMora", 11L);
			
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

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaCuotasPagadasPorEntidad(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectSumaCuotasPagadasPorEntidad desde: " + fechaInicio + " hasta: " + fechaFin);
		Query query = em.createQuery(
			" select   d.prestamo.entidad.codigo, sum(d.cuota) " +
			" from     DetallePrestamo d " +
			" where    d.estado = :estadoPagado " +
			"   and    d.fechaVencimiento >= :fechaInicio " +
			"   and    d.fechaVencimiento <= :fechaFin " +
			" group by d.prestamo.entidad.codigo "
		);
		query.setParameter("estadoPagado", 4L);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

	@Override
	public DetallePrestamo selectMenorCuotaActiva(Long codigoPrestamo, LocalDateTime fechaCorte) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectMenorCuotaActiva prestamo: " + codigoPrestamo + ", fechaCorte: " + fechaCorte);
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" where d.prestamo.codigo = :codigoPrestamo " +
			"   and d.estado in (2, 3, 4, 5, 6) " +
			"   and d.fechaVencimiento <= :fechaCorte " +
			" order by d.numeroCuota asc "
		);
		query.setParameter("codigoPrestamo", codigoPrestamo);
		query.setParameter("fechaCorte", fechaCorte);
		query.setMaxResults(1);
		@SuppressWarnings("unchecked")
		List<DetallePrestamo> resultados = query.getResultList();
		return resultados.isEmpty() ? null : resultados.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetallePrestamo> selectCuotasDelMes(Long codigoPrestamo, LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectCuotasDelMes prestamo: " + codigoPrestamo + ", rango: " + fechaInicio + " a " + fechaFin);
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" where d.prestamo.codigo = :codigoPrestamo " +
			"   and d.estado <> 7 " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin " +
			" order by d.numeroCuota asc "
		);
		query.setParameter("codigoPrestamo", codigoPrestamo);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

	@Override
	public DetallePrestamo selectMenorCuotaAnteriorAlMes(Long codigoPrestamo, LocalDateTime fechaInicio) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectMenorCuotaAnteriorAlMes prestamo: " + codigoPrestamo + ", fechaInicio: " + fechaInicio);
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" where d.prestamo.codigo = :codigoPrestamo " +
			"   and d.estado in (2, 3, 5, 6) " +
			"   and d.fechaVencimiento < :fechaInicio " +
			"   and d.prestamo.estadoPrestamo in (2, 8, 11) " +
			" order by d.numeroCuota asc "
		);
		query.setParameter("codigoPrestamo", codigoPrestamo);
		query.setParameter("fechaInicio", fechaInicio);
		query.setMaxResults(1);
		@SuppressWarnings("unchecked")
		List<DetallePrestamo> resultados = query.getResultList();
		return resultados.isEmpty() ? null : resultados.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetallePrestamo> selectCuotasDelMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectCuotasDelMesGlobal rango: " + fechaInicio + " a " + fechaFin);
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" join fetch d.prestamo " +
			" join fetch d.prestamo.entidad " +
			" left join fetch d.prestamo.producto " +
			" where d.estado <> 7 " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin "
		);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetallePrestamo> selectMenorCuotaAnteriorAlMesGlobal(LocalDateTime fechaInicio) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectMenorCuotaAnteriorAlMesGlobal fechaInicio: " + fechaInicio);
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" join fetch d.prestamo " +
			" join fetch d.prestamo.entidad " +
			" left join fetch d.prestamo.producto " +
			" where d.estado in (2, 3, 5, 6) " +
			"   and d.fechaVencimiento < :fechaInicio " +
			"   and d.prestamo.estadoPrestamo in (2, 8, 11) " +
			"   and d.numeroCuota = (" +
			"     select min(d2.numeroCuota) from DetallePrestamo d2 " +
			"     where d2.prestamo.codigo = d.prestamo.codigo " +
			"       and d2.estado in (2, 3, 5, 6) " +
			"       and d2.fechaVencimiento < :fechaInicio " +
			"       and d2.prestamo.estadoPrestamo in (2, 8, 11) " +
			"   ) "
		);
		query.setParameter("fechaInicio", fechaInicio);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePrestamo> selectCuotasNoPagadasByPrestamo(Long codigoPrestamo) throws Throwable {
		System.out.println("Buscando cuotas NO pagadas del préstamo: " + codigoPrestamo);
		
		try {
			// ✅ OPTIMIZACIÓN: Filtrar en BD en lugar de traer todas las cuotas
			String jpql = "SELECT d FROM DetallePrestamo d " +
						 "WHERE d.prestamo.codigo = :codigoPrestamo " +
						 "AND d.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
						 "ORDER BY d.numeroCuota";
			
			Query query = em.createQuery(jpql);
			query.setParameter("codigoPrestamo", codigoPrestamo);
			// ✅ Convertir explícitamente a Long para que coincida con el tipo del campo
			query.setParameter("estadoPagada", (long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			query.setParameter("estadoCanceladaAnticipada", (long) com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
			
			List<DetallePrestamo> resultados = query.getResultList();
			System.out.println("Cuotas pendientes encontradas: " + (resultados != null ? resultados.size() : 0));
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar cuotas no pagadas del préstamo: " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			return new java.util.ArrayList<>();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePrestamo> selectMinCuotaNoPagadaByPrestamo(Long codigoPrestamo) throws Throwable {
		System.out.println("Buscando la mínima cuota NO pagada del préstamo: " + codigoPrestamo);
		
		try {
			// ✅ OPTIMIZACIÓN: Buscar solo la mínima cuota pendiente (por número de cuota)
			String jpql = "SELECT d FROM DetallePrestamo d " +
						 "WHERE d.prestamo.codigo = :codigoPrestamo " +
						 "AND d.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
						 "AND d.numeroCuota = (" +
						 "    SELECT MIN(d2.numeroCuota) FROM DetallePrestamo d2 " +
						 "    WHERE d2.prestamo.codigo = :codigoPrestamo " +
						 "    AND d2.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada)" +
						 ")";
			
			Query query = em.createQuery(jpql);
			query.setParameter("codigoPrestamo", codigoPrestamo);
			// ✅ Convertir explícitamente a Long para que coincida con el tipo del campo
			query.setParameter("estadoPagada", (long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			query.setParameter("estadoCanceladaAnticipada", (long) com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
			
			List<DetallePrestamo> resultados = query.getResultList();
			System.out.println("Mínima cuota pendiente encontrada: " + (resultados != null && !resultados.isEmpty() ? 
				"Cuota #" + resultados.get(0).getNumeroCuota() : "Ninguna"));
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar la mínima cuota no pagada del préstamo: " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			return new java.util.ArrayList<>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetallePrestamo> selectMaxCuotaPagadaDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectMaxCuotaPagadaDelMesGlobal rango: " + fechaInicio + " a " + fechaFin);
		// Trae la cuota con mayor numeroCuota del préstamo cuyo estado sea pagada (4)
		// y cuya fechaVencimiento esté en el mes, y que ese numeroCuota sea el máximo del préstamo completo.
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" join fetch d.prestamo " +
			" join fetch d.prestamo.entidad " +
			" where d.estado = 4 " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin " +
			"   and d.numeroCuota = (" +
			"     select max(d2.numeroCuota) from DetallePrestamo d2 " +
			"     where d2.prestamo.codigo = d.prestamo.codigo " +
			"   ) "
		);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetallePrestamo> selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal rango: " + fechaInicio + " a " + fechaFin);
		// Trae la cuota con mayor numeroCuota pagada (estado=4) dentro del mes,
		// cuyo préstamo esté en estadoPrestamo=4 (cancelado anticipado),
		// y que ese numeroCuota sea el máximo entre las cuotas pagadas del préstamo.
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" join fetch d.prestamo " +
			" join fetch d.prestamo.entidad " +
			" where d.estado = 4 " +
			"   and d.prestamo.estadoPrestamo = 4 " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin " +
			"   and d.numeroCuota = (" +
			"     select max(d2.numeroCuota) from DetallePrestamo d2 " +
			"     where d2.prestamo.codigo = d.prestamo.codigo " +
			"       and d2.estado = 4 " +
			"   ) "
		);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

}