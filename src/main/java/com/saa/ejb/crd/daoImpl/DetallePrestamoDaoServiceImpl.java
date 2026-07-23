package com.saa.ejb.crd.daoImpl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
			// Truncar las fechas en Java para comparar solo por días
			// fechaDesde: inicio del día (00:00:00)
			// fechaHasta: fin del día (23:59:59.999999999)
			LocalDateTime fechaDesdeAjustada = fechaDesde.toLocalDate().atStartOfDay();
			LocalDateTime fechaHastaAjustada = fechaHasta.toLocalDate().atTime(23, 59, 59, 999999999);
			
			System.out.println("Rango ajustado: " + fechaDesdeAjustada + " hasta " + fechaHastaAjustada);
			
			String jpql = " SELECT d FROM DetallePrestamo d, Prestamo p " +
						 " WHERE d.prestamo.codigo = p.codigo " +
						 " AND d.fechaVencimiento >= :fechaDesde " +
						 " AND d.fechaVencimiento <= :fechaHasta " +
						 " AND p.idEstado IN (:estadoActivo, :estadoEnMora) " +
						 " ORDER BY d.fechaVencimiento";
			
			Query query = em.createQuery(jpql);
			query.setParameter("fechaDesde", fechaDesdeAjustada);
			query.setParameter("fechaHasta", fechaHastaAjustada);
			query.setParameter("estadoActivo", 2L);
			query.setParameter("estadoEnMora", 11L);
			
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
			return new ArrayList<>();
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
			return new ArrayList<>();
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
			"   and d.estado in (1, 2, 3, 5, 6) " +
			"   and d.fechaVencimiento < :fechaInicio " +
			"   and d.prestamo.idEstado in (2, 8, 11) " +
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
			" where d.estado = 4 " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin "
		);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetallePrestamo> selectMenorCuotaAnteriorAlMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectMenorCuotaAnteriorAlMesGlobal hasta: " + fechaFin);
		Query query = em.createQuery(
			" select d from DetallePrestamo d " +
			" join fetch d.prestamo " +
			" join fetch d.prestamo.entidad " +
			" left join fetch d.prestamo.producto " +
			" where d.estado in (1, 2, 3, 5, 6, 7) " +
			"   and d.fechaVencimiento <= :fechaFin " +
			"   and d.prestamo.idEstado in (2, 8, 11) " +
			"   and d.numeroCuota = (" +
			"     select min(d2.numeroCuota) from DetallePrestamo d2 " +
			"     where d2.prestamo.codigo = d.prestamo.codigo " +
			"       and d2.estado in (1, 2, 3, 5, 6, 7) " +
			"       and d2.fechaVencimiento <= :fechaFin " +
			"       and d2.prestamo.idEstado in (2, 8, 11) " +
			"   ) "
		);
		query.setParameter("fechaFin", fechaFin);
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
			return new ArrayList<>();
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
			return new ArrayList<>();
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
			"   and d.prestamo.idEstado = 4 " +
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

	@Override
	public Object[] selectSumaCapitalInteresGrupo2(Long codigoPrestamo, Double numeroCuotaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectSumaCapitalInteresGrupo2 prestamo: " + codigoPrestamo 
			+ " cuotaInicio: " + numeroCuotaInicio + " hasta: " + fechaFin);
		
		// Suma capital e interés de todas las cuotas desde numeroCuotaInicio
		// hasta la máxima cuota con fechaVencimiento <= fechaFin
		Query query = em.createQuery(
			" select sum(d.capital), sum(d.interes) " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo = :codigoPrestamo " +
			"   and d.numeroCuota >= :numeroCuotaInicio " +
			"   and d.fechaVencimiento <= :fechaFin "
		);
		query.setParameter("codigoPrestamo", codigoPrestamo);
		query.setParameter("numeroCuotaInicio", numeroCuotaInicio);
		query.setParameter("fechaFin", fechaFin);
		
		Object[] result = (Object[]) query.getSingleResult();
		
		// Convertir nulls a 0.0
		Double sumaCapital = result[0] != null ? ((Number) result[0]).doubleValue() : 0.0;
		Double sumaInteres = result[1] != null ? ((Number) result[1]).doubleValue() : 0.0;
		
		System.out.println("  → sumaCapital: " + sumaCapital + ", sumaInteres: " + sumaInteres);
		
		return new Object[]{sumaCapital, sumaInteres};
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectCuotasParaMora(Long codigoCuotaOrigen, java.time.LocalDateTime fechaHasta) throws Throwable {
		System.out.println("DetallePrestamoDaoServiceImpl.selectCuotasParaMora cuotaOrigen: " + codigoCuotaOrigen + " hasta: " + fechaHasta);
		// Trae capital, fechaVencimiento e interesNominal del préstamo
		// de todas las cuotas del mismo préstamo, desde la cuota origen (inclusive)
		// hasta la máxima con fechaVencimiento <= fechaHasta
		Query query = em.createQuery(
			" select d.capital, d.fechaVencimiento, d.prestamo.interesNominal " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo = (" +
			"     select d0.prestamo.codigo from DetallePrestamo d0 where d0.codigo = :codigoCuotaOrigen" +
			" ) " +
			"   and d.numeroCuota >= (" +
			"     select d0.numeroCuota from DetallePrestamo d0 where d0.codigo = :codigoCuotaOrigen" +
			" ) " +
			"   and d.fechaVencimiento <= :fechaHasta " +
			" order by d.numeroCuota asc "
		);
		query.setParameter("codigoCuotaOrigen", codigoCuotaOrigen);
		query.setParameter("fechaHasta", fechaHasta);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaCapitalInteresGrupo2Batch(List<Long> codigosCuotasOrigen, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		if (codigosCuotasOrigen == null || codigosCuotasOrigen.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("DetallePrestamoDaoServiceImpl.selectSumaCapitalInteresGrupo2Batch - cuotas origen: " + codigosCuotasOrigen.size());
		// El SUM y GROUP BY ocurren en la BD: devuelve exactamente 1 fila por préstamo.
		// CORRECCIÓN: fechaVencimiento < fechaInicio excluye la cuota del mes de ejecución del vencido,
		// ya que por política del fondo las cuotas vencen el último día del mes y no están en mora
		// hasta que ese día haya pasado. La cuota del mes va a valorPorVencer, no a valorVencido.
		Query query = em.createQuery(
			" select d.prestamo.codigo, sum(d.capital), sum(d.interes), sum(d.desgravamen), sum(d.valorSeguroIncendio) " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo in (" +
			"     select d0.prestamo.codigo from DetallePrestamo d0 where d0.codigo in :codigosOrigen" +
			" ) " +
			"   and d.numeroCuota >= (" +
			"     select d0.numeroCuota from DetallePrestamo d0 " +
			"     where d0.codigo in :codigosOrigen " +
			"       and d0.prestamo.codigo = d.prestamo.codigo" +
			" ) " +
			"   and d.fechaVencimiento < :fechaInicio " +
			" group by d.prestamo.codigo "
		);
		query.setParameter("codigosOrigen", codigosCuotasOrigen);
		query.setParameter("fechaInicio", fechaInicio);
		List<Object[]> filas = query.getResultList();
		List<Object[]> resultado = new java.util.ArrayList<>(filas.size());
		for (Object[] fila : filas) {
			Long   codPrest       = (Long)   fila[0];
			Double sumaCapital    = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
			Double sumaInteres    = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
			Double sumaDesgrav    = fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0;
			Double sumaIncendio   = fila[4] != null ? ((Number) fila[4]).doubleValue() : 0.0;
			resultado.add(new Object[]{codPrest, sumaCapital, sumaInteres, sumaDesgrav, sumaIncendio});
		}
		System.out.println("  → Sumas grupo 2 calculadas en BD: " + resultado.size() + " préstamos");
		return resultado;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> calcularInteresMoraBatch(List<Long> codigosCuotas, java.time.LocalDateTime fechaHasta) throws Throwable {
		if (codigosCuotas == null || codigosCuotas.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("DetallePrestamoDaoServiceImpl.calcularInteresMoraBatch - cantidad: " + codigosCuotas.size());

		// Query 1 (pequeña): obtener codigoPrestamo de cada cuota origen para el mapa de retorno.
		Query queryOrigen = em.createQuery(
			" select d0.codigo, d0.prestamo.codigo " +
			" from DetallePrestamo d0 where d0.codigo in :codigosCuotas "
		);
		queryOrigen.setParameter("codigosCuotas", codigosCuotas);
		java.util.Map<Long, Long> mapaCuotaAPrestamo = new java.util.HashMap<>();
		for (Object[] row : (List<Object[]>) queryOrigen.getResultList()) {
			mapaCuotaAPrestamo.put((Long) row[0], (Long) row[1]);
		}

		// Query 2: trae SOLO las cuotas relevantes para el cálculo de mora.
		// El filtro numeroCuota >= numeroCuotaOrigen se aplica en el SQL mediante subconsulta,
		// evitando traer cuotas anteriores a la cuota en mora.
		Query queryMora = em.createQuery(
			" select d.prestamo.codigo, d.capital, d.fechaVencimiento, d.prestamo.interesNominal " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo in (" +
			"     select d0.prestamo.codigo from DetallePrestamo d0 where d0.codigo in :codigosCuotas" +
			" ) " +
			"   and d.numeroCuota >= (" +
			"     select d0.numeroCuota from DetallePrestamo d0 " +
			"     where d0.codigo in :codigosCuotas " +
			"       and d0.prestamo.codigo = d.prestamo.codigo" +
			" ) " +
			"   and d.fechaVencimiento <= :fechaHasta "
		);
		queryMora.setParameter("codigosCuotas", codigosCuotas);
		queryMora.setParameter("fechaHasta", fechaHasta);
		List<Object[]> cuotasMora = queryMora.getResultList();
		System.out.println("  → Cuotas para mora traídas de BD: " + cuotasMora.size());

		// Agrupar las cuotas traídas por codigoPrestamo
		java.util.Map<Long, List<Object[]>> cuotasPorPrestamo = new java.util.HashMap<>();
		for (Object[] row : cuotasMora) {
			Long codPrest = (Long) row[0];
			cuotasPorPrestamo.computeIfAbsent(codPrest, k -> new java.util.ArrayList<>()).add(row);
		}

		// Calcular mora por cuota origen (la fórmula requiere días por fila, no se puede hacer SUM en BD)
		List<Object[]> resultado = new java.util.ArrayList<>();
		for (Long codCuotaOrigen : codigosCuotas) {
			Long codPrest = mapaCuotaAPrestamo.get(codCuotaOrigen);
			if (codPrest == null) {
				resultado.add(new Object[]{codCuotaOrigen, 0.0});
				continue;
			}
			List<Object[]> cuotasPrestamo = cuotasPorPrestamo.get(codPrest);
			double totalMora = 0.0;
			if (cuotasPrestamo != null) {
				for (Object[] cuota : cuotasPrestamo) {
					Double capital        = cuota[1] != null ? ((Number) cuota[1]).doubleValue() : 0.0;
					LocalDateTime fechaVenc = (LocalDateTime) cuota[2];
					Double interesNominal = cuota[3] != null ? ((Number) cuota[3]).doubleValue() : 0.0;
					if (capital <= 0.0 || fechaVenc == null) continue;
					if (interesNominal <= 0.0) interesNominal = 9.0;
					long diasMora = ChronoUnit.DAYS.between(
						fechaVenc.toLocalDate(), fechaHasta.toLocalDate()
					);
					if (diasMora <= 0) continue;
					totalMora += capital * (interesNominal / 100.0 / 360.0) * diasMora;
				}
			}
			resultado.add(new Object[]{codCuotaOrigen, totalMora});
		}
		System.out.println("  → Mora calculada para " + resultado.size() + " cuotas");
		return resultado;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectCapitalCuotasFuturasBatch(List<Long> codigosPrestamos, java.time.LocalDateTime fechaEjecucion) throws Throwable {
		if (codigosPrestamos == null || codigosPrestamos.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("DetallePrestamoDaoServiceImpl.selectCapitalCuotasFuturasBatch - préstamos: " + codigosPrestamos.size());
		// Trae todas las cuotas futuras (vencen después del cierre del mes de ejecución).
		// Se excluye SOLO estado 7 (cancelada anticipada).
		// Las cuotas pagadas (estado 4) y cualquier otro estado SÍ se incluyen.
		Query query = em.createQuery(
			" select d.prestamo.codigo, d.numeroCuota, d.capital, d.saldoOtros " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo IN :codigosPrestamos " +
			"   and d.fechaVencimiento > :fechaEjecucion " +
			"   and d.estado <> 7 " +
			" order by d.prestamo.codigo asc, d.numeroCuota asc "
		);
		query.setParameter("codigosPrestamos", codigosPrestamos);
		query.setParameter("fechaEjecucion", fechaEjecucion);
		List<Object[]> resultados = query.getResultList();
		System.out.println("  → Cuotas futuras obtenidas: " + resultados.size());
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSaldoInicialCapitalDelMesBatch(List<Long> codigosPrestamos,
			java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		if (codigosPrestamos == null || codigosPrestamos.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("DetallePrestamoDaoServiceImpl.selectSaldoInicialCapitalDelMesBatch - préstamos: " + codigosPrestamos.size());
		// Para cada préstamo del Grupo 2, busca la cuota que cae dentro del mes de ejecución
		// (fechaVencimiento BETWEEN fechaInicio y fechaFin) con el menor numeroCuota.
		// Retorna saldoInicialCapital y capital para calcular: valorPorVencer = saldoInicialCapital - capital.
		Query query = em.createQuery(
			" select d.prestamo.codigo, d.saldoInicialCapital, d.capital, d.interes " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo IN :codigosPrestamos " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin " +
			"   and d.numeroCuota = (" +
			"     select min(d2.numeroCuota) from DetallePrestamo d2 " +
			"     where d2.prestamo.codigo = d.prestamo.codigo " +
			"       and d2.fechaVencimiento >= :fechaInicio " +
			"       and d2.fechaVencimiento <= :fechaFin " +
			"   ) "
		);
		query.setParameter("codigosPrestamos", codigosPrestamos);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		List<Object[]> resultados = query.getResultList();
		System.out.println("  → Cuotas del mes para Grupo 2 obtenidas: " + resultados.size());
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectCapitalCuotasDesdeInicioMesBatch(List<Long> codigosPrestamos,
			java.time.LocalDateTime fechaInicio) throws Throwable {
		if (codigosPrestamos == null || codigosPrestamos.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("DetallePrestamoDaoServiceImpl.selectCapitalCuotasDesdeInicioMesBatch - préstamos: " + codigosPrestamos.size());
		// Para el Grupo 2, el desglose de capital por vencer comienza desde la cuota
		// del mes de ejecución (fechaVencimiento >= fechaInicio), no desde la siguiente.
		// Se excluye SOLO estado 7 (cancelada anticipada).
		// Las cuotas pagadas (estado 4) y cualquier otro estado SÍ se incluyen.
		Query query = em.createQuery(
			" select d.prestamo.codigo, d.numeroCuota, d.capital, d.saldoOtros " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo IN :codigosPrestamos " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.estado <> 7 " +
			" order by d.prestamo.codigo asc, d.numeroCuota asc "
		);
		query.setParameter("codigosPrestamos", codigosPrestamos);
		query.setParameter("fechaInicio", fechaInicio);
		List<Object[]> resultados = query.getResultList();
		System.out.println("  → Cuotas desde inicio mes Grupo 2 obtenidas: " + resultados.size());
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> calcularInteresMoraDelMesBatch(List<Long> codigosPrestamos,
			java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		if (codigosPrestamos == null || codigosPrestamos.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("DetallePrestamoDaoServiceImpl.calcularInteresMoraDelMesBatch - préstamos: " + codigosPrestamos.size());
		// Obtiene la cuota cuya fechaVencimiento cae dentro del mes (BETWEEN fechaInicio y fechaFin)
		// con el menor numeroCuota por préstamo, para calcular la mora solo de esa cuota.
		Query query = em.createQuery(
			" select d.prestamo.codigo, d.capital, d.fechaVencimiento, d.prestamo.interesNominal " +
			" from DetallePrestamo d " +
			" where d.prestamo.codigo IN :codigosPrestamos " +
			"   and d.fechaVencimiento >= :fechaInicio " +
			"   and d.fechaVencimiento <= :fechaFin " +
			"   and d.estado <> 7 " +
			"   and d.numeroCuota = (" +
			"     select min(d2.numeroCuota) from DetallePrestamo d2 " +
			"     where d2.prestamo.codigo = d.prestamo.codigo " +
			"       and d2.fechaVencimiento >= :fechaInicio " +
			"       and d2.fechaVencimiento <= :fechaFin " +
			"       and d2.estado <> 7 " +
			"   ) "
		);
		query.setParameter("codigosPrestamos", codigosPrestamos);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		List<Object[]> filas = query.getResultList();
		System.out.println("  → Cuotas del mes para mora individual obtenidas: " + filas.size());

		List<Object[]> resultado = new java.util.ArrayList<>();
		for (Object[] fila : filas) {
			Long   codPrest       = (Long)   fila[0];
			Double capital        = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
			LocalDateTime fechaVenc = (LocalDateTime) fila[2];
			Double interesNominal = fila[3] != null ? ((Number) fila[3]).doubleValue() : 9.0;
			if (interesNominal <= 0.0) interesNominal = 9.0;

			double mora = 0.0;
			if (capital > 0.0 && fechaVenc != null) {
				long diasMora = ChronoUnit.DAYS.between(
					fechaVenc.toLocalDate(), fechaFin.toLocalDate()
				);
				if (diasMora > 0) {
					mora = capital * (interesNominal / 100.0 / 360.0) * diasMora;
				}
			}
			resultado.add(new Object[]{codPrest, mora});
		}
		System.out.println("  → Mora del mes calculada para " + resultado.size() + " préstamos");
		return resultado;
	}

}
