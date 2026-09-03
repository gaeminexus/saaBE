package com.saa.ejb.crd.daoImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.model.crd.Aporte;
import com.saa.rubros.CrdTipoMovimientoAporte;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


@Stateless
public class AporteDaoServiceImpl extends EntityDaoImpl<Aporte> implements AporteDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/**
	 * Filtra todos los aportes por id de entidad
	 * 
	 * @param :idEntidad
	 * @return Lista de Aporte
	 */
	// ---------------------------------------------------------------
	// G43 — Imposiciones personales (tipos 9, 11 con valor > 0)
	// ---------------------------------------------------------------
	@Override
	public Long selectCountImposicionesPersonalesPorEntidad(Long codigoEntidad) throws Throwable {
		// Decisión del usuario (2026-08-27, mismo criterio que G44): cuenta MESES DE DEVENGO
		// (periodo efectivo), no filas. SQL nativo porque el periodo efectivo usa TRUNC, que
		// no es JPQL estándar.
		Query query = em.createNativeQuery(
			" select count(distinct " + PERIODO_EFECTIVO_SQL + ") " +
			" from   CRD.APRT a " +
			" where  a.ENTDCDGO = :codigoEntidad " +
			"   and  a.TPAPCDGO in (9, 11) " +
			"   and  a.APRTVLRR > 0 ");
		query.setParameter("codigoEntidad", codigoEntidad);
		Object result = query.getSingleResult();
		return result != null ? ((Number) result).longValue() : 0L;
	}

	// ---------------------------------------------------------------
	// G43 — Imposiciones patronales (tipos 13, 14 con valor > 0)
	// ---------------------------------------------------------------
	@Override
	public Long selectCountImposicionesPatronalesPorEntidad(Long codigoEntidad) throws Throwable {
		// Decisión del usuario (2026-08-27, mismo criterio que G44): cuenta MESES DE DEVENGO
		// (periodo efectivo), no filas.
		Query query = em.createNativeQuery(
			" select count(distinct " + PERIODO_EFECTIVO_SQL + ") " +
			" from   CRD.APRT a " +
			" where  a.ENTDCDGO = :codigoEntidad " +
			"   and  a.TPAPCDGO in (13, 14) " +
			"   and  a.APRTVLRR > 0 ");
		query.setParameter("codigoEntidad", codigoEntidad);
		Object result = query.getSingleResult();
		return result != null ? ((Number) result).longValue() : 0L;
	}

	// ---------------------------------------------------------------
	// G43 — SUM de aportes con valor < 0, tipoAporte.estado = 1,
	//        dentro del mes de ejecucion para una entidad
	// ---------------------------------------------------------------
	@Override
	public Double selectSumaAportesNegativosMesPorEntidad(Long codigoEntidad,
			java.time.LocalDateTime fechaInicio,
			java.time.LocalDateTime fechaFin) throws Throwable {
		Query query = em.createQuery(
			" select sum(a.valor) " +
			" from   Aporte a " +
			" where  a.entidad.codigo = :codigoEntidad " +
			"   and  a.tipoAporte.estado = 1 " +
			"   and  a.valor < 0 " +
			"   and  a.fechaTransaccion >= :fechaInicio " +
			"   and  a.fechaTransaccion <= :fechaFin ");
		query.setParameter("codigoEntidad", codigoEntidad);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		Object result = query.getSingleResult();
		return result != null ? ((Number) result).doubleValue() : 0.0;
	}

	@Override
	public Double sumaAportesPositivosPorTipoYPeriodo(Long codigoEntidad, List<Long> tiposAporte,
			Long anio, Long mes) throws Throwable {
		System.out.println("AporteDaoServiceImpl.sumaAportesPositivosPorTipoYPeriodo - entidad: "
			+ codigoEntidad + ", tipos: " + tiposAporte + ", periodo: " + mes + "/" + anio);

		if (codigoEntidad == null || tiposAporte == null || tiposAporte.isEmpty()
				|| anio == null || mes == null) {
			// No se puede evaluar: null, no 0.0, para no interpretarlo como "no aportó".
			return null;
		}

		try {
			// Periodo efectivo (D3, 2026-08-27), no YEAR/MONTH(APRTFCTR): la fecha de caja
			// es siempre el mes de la CARGA, nunca el del devengo. Con solo filas positivas
			// en el WHERE, el periodo efectivo equivale a NVL(APRTPRDV, TRUNC(APRTFCTR,'MM')).
			Query query = em.createNativeQuery(
				" select coalesce(sum(a.APRTVLRR), 0) " +
				" from   CRD.APRT a " +
				" where  a.ENTDCDGO = :codigoEntidad " +
				"   and  a.TPAPCDGO in (:tiposAporte) " +
				"   and  a.APRTVLRR > 0 " +
				"   and  " + PERIODO_EFECTIVO_SQL + " = :periodo "
			);
			query.setParameter("codigoEntidad", codigoEntidad);
			query.setParameter("tiposAporte", tiposAporte);
			query.setParameter("periodo",
				java.sql.Date.valueOf(java.time.LocalDate.of(anio.intValue(), mes.intValue(), 1)));

			Object resultado = query.getSingleResult();
			return resultado != null ? ((Number) resultado).doubleValue() : 0.0;

		} catch (Exception e) {
			System.err.println("Error al sumar aportes por tipo y periodo: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectUltimaFechaAportePorEntidad(List<Long> codigosEntidad,
			List<Long> tiposAporte, java.time.LocalDateTime corte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectUltimaFechaAportePorEntidad - entidades: "
			+ (codigosEntidad != null ? codigosEntidad.size() : 0) + ", corte: " + corte);

		if (codigosEntidad == null || codigosEntidad.isEmpty()
				|| tiposAporte == null || tiposAporte.isEmpty() || corte == null) {
			return new java.util.ArrayList<>();
		}

		Query query = em.createQuery(
			" select a.entidad.codigo, max(a.fechaTransaccion) " +
			" from   Aporte a " +
			" where  a.entidad.codigo in :codigosEntidad " +
			"   and  a.tipoAporte.codigo in :tiposAporte " +
			"   and  a.valor > 0 " +
			"   and  a.fechaTransaccion < :corte " +
			" group by a.entidad.codigo "
		);
		query.setParameter("codigosEntidad", codigosEntidad);
		query.setParameter("tiposAporte", tiposAporte);
		query.setParameter("corte", corte);

		List<Object[]> resultados = query.getResultList();
		System.out.println("  -> Entidades con al menos un aporte: " + resultados.size());
		return resultados;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Aporte> selectByEntidad(Long idEntidad) throws Throwable {
		System.out.println("metodo selectByEntidad de AporteDaoServiceImpl");
		Query query = em.createQuery(" select   b " +
				 					 " from     Aporte b " +
				 					 " where    b.entidad.codigo = :idEntidad ");
		query.setParameter("idEntidad", idEntidad);
		return  query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Aporte> selectByEntidadTipoYFechaTransaccion(Long idEntidad, Long idTipoAporte,
			LocalDateTime fechaTransaccion) throws Throwable {
		System.out.println("metodo selectByEntidadTipoYFechaTransaccion de AporteDaoServiceImpl"
				+ " - entidad: " + idEntidad + " - tipoAporte: " + idTipoAporte
				+ " - fechaTransaccion: " + fechaTransaccion);
		Query query = em.createQuery(
				" select a from Aporte a " +
				" where  a.entidad.codigo    = :idEntidad " +
				" and    a.tipoAporte.codigo = :idTipoAporte " +
				" and    a.fechaTransaccion  = :fechaTransaccion " +
				" and    a.tipoMovimiento    = :tipoMovimiento ");
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("fechaTransaccion", fechaTransaccion);
		query.setParameter("tipoMovimiento", (long) CrdTipoMovimientoAporte.DEVOLUCION);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Aporte> selectByDevolucion(Long idDevolucion) throws Throwable {
		System.out.println("metodo selectByDevolucion de AporteDaoServiceImpl - devolucion: " + idDevolucion);
		Query query = em.createQuery(
				" select a from Aporte a " +
				" where  a.devolucion.codigo = :idDevolucion ");
		query.setParameter("idDevolucion", idDevolucion);
		return query.getResultList();
	}

	/**
	 * OPTIMIZADO: Busca un aporte específico con todos los filtros en la consulta SQL
	 * Evita traer todos los aportes de la entidad y filtrar en memoria
	 */
	@Override
	public Aporte selectByEntidadTipoYCarga(Long idEntidad, Long idTipoAporte, Long idAsoprep, List<Long> estados) throws Throwable {
		Query query = em.createQuery(
			" select   b " +
			" from     Aporte b " +
			" where    b.entidad.codigo = :idEntidad " +
			"   and    b.tipoAporte.codigo = :idTipoAporte " +
			"   and    b.idAsoprep = :idAsoprep " +
			"   and    b.estado in :estados ");
		
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("idAsoprep", idAsoprep);
		query.setParameter("estados", estados);
		query.setMaxResults(1);
		
		@SuppressWarnings("unchecked")
		List<Aporte> resultados = query.getResultList();
		
		return resultados.isEmpty() ? null : resultados.get(0);
	}

	/**
	 * OPTIMIZADO: Busca aporte adelantado con saldo pendiente
	 * Excluye la carga actual para encontrar aportes del mes anterior
	 */
	@Override
	public Aporte selectAporteAdelantado(Long idEntidad, Long idTipoAporte, Long idAsoprep, List<Long> estados) throws Throwable {
		Query query = em.createQuery(
			" select   b " +
			" from     Aporte b " +
			" where    b.entidad.codigo = :idEntidad " +
			"   and    b.tipoAporte.codigo = :idTipoAporte " +
			"   and    b.idAsoprep <> :idAsoprep " +  // ✅ Excluir carga actual
			"   and    b.estado in :estados " +
			"   and    b.saldo > 0.01 " +  // ✅ Solo con saldo pendiente
			" order by b.codigo desc ");  // ✅ El más reciente primero
		
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("idAsoprep", idAsoprep);
		query.setParameter("estados", estados);
		query.setMaxResults(1);
		
		@SuppressWarnings("unchecked")
		List<Aporte> resultados = query.getResultList();
		
		return resultados.isEmpty() ? null : resultados.get(0);
	}

	/**
	 * SUPER OPTIMIZADO: Busca el aporte más antiguo (MIN codigo) con estado PARCIAL
	 * Usuario SAA_AH = Aportes creados automáticamente por el sistema
	 *
	 * @deprecated Ver {@link com.saa.ejb.crd.dao.AporteDaoService#selectMinAporteParcialSistema}.
	 */
	@Override
	@Deprecated
	public Aporte selectMinAporteParcialSistema(Long idEntidad, Long idTipoAporte) throws Throwable {
		Query query = em.createQuery(
			" select   b " +
			" from     Aporte b " +
			" where    b.entidad.codigo = :idEntidad " +
			"   and    b.tipoAporte.codigo = :idTipoAporte " +
			"   and    b.usuarioRegistro = 'SAA_AH' " +  // ✅ Creados por el sistema
			"   and    b.estado = :estadoParcial " +  // ✅ Estado PARCIAL (6)
			"   and    b.saldo > 0.01 " +  // ✅ Solo con saldo pendiente
			" order by b.codigo asc ");  // ✅ MIN codigo primero (más antiguo)
		
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("estadoParcial", (long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		query.setMaxResults(1);
		
		@SuppressWarnings("unchecked")
		List<Aporte> resultados = query.getResultList();
		
		return resultados.isEmpty() ? null : resultados.get(0);
	}

	/**
	 * Busca el aporte más antiguo (MIN codigo) con saldo pendiente, SIN restricción de usuario
	 * ✅ CORREGIDO: Busca CUALQUIER aporte con saldo > 0 (PENDIENTE o PARCIAL)
	 * Esto asegura que los pagos se apliquen primero a deudas anteriores
	 *
	 * @deprecated Ver {@link com.saa.ejb.crd.dao.AporteDaoService#selectMinAporteConSaldo}.
	 */
	@Override
	@Deprecated
	public Aporte selectMinAporteConSaldo(Long idEntidad, Long idTipoAporte) throws Throwable {
		Query query = em.createQuery(
			" select   b " +
			" from     Aporte b " +
			" where    b.entidad.codigo = :idEntidad " +
			"   and    b.tipoAporte.codigo = :idTipoAporte " +
			"   and    b.saldo > 0.01 " +  // ✅ Solo con saldo pendiente
			"   and    b.estado = :estadoParcial " +  // ✅ PENDIENTE o PARCIAL
			" order by b.codigo asc ");  // ✅ MIN codigo primero (más antiguo - FIFO)
		
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("estadoParcial", (long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		query.setMaxResults(1);
		
		@SuppressWarnings("unchecked")
		List<Aporte> resultados = query.getResultList();
		
		return resultados.isEmpty() ? null : resultados.get(0);
	}

	/**
	 * Expresión SQL del "periodo efectivo" de un aporte (D3). Ver
	 * {@link PeriodoEfectivoAporteSql} — fuente ÚNICA, no redefinir aquí ni en otra clase.
	 */
	private static final String PERIODO_EFECTIVO_SQL = PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL;

	@Override
	public Double sumValorPorEntidadTipoYDevengo(Long idEntidad, Long idTipoAporte,
			java.time.LocalDate periodo) throws Throwable {
		Query query = em.createNativeQuery(
			" select   sum(a.APRTVLRR) " +
			" from     CRD.APRT a " +
			" where    a.ENTDCDGO = :idEntidad " +
			"   and    a.TPAPCDGO = :idTipoAporte " +
			"   and  " + PERIODO_EFECTIVO_SQL + " = :periodo ");
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("periodo", java.sql.Date.valueOf(periodo));
		Object resultado = query.getSingleResult();
		return resultado != null ? ((Number) resultado).doubleValue() : 0.0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> sumValorPorEntidadTipoYRangoDevengo(Long idEntidad, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		Query query = em.createNativeQuery(
			" select   " + PERIODO_EFECTIVO_SQL + " as PERIODO_EFECTIVO, " +
			"          a.TPAPCDGO, sum(a.APRTVLRR) " +
			" from     CRD.APRT a " +
			" where    a.ENTDCDGO = :idEntidad " +
			" group by " + PERIODO_EFECTIVO_SQL + ", a.TPAPCDGO " +
			" having   " + PERIODO_EFECTIVO_SQL + " between :desde and :hasta ");
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("desde", java.sql.Date.valueOf(desde));
		query.setParameter("hasta", java.sql.Date.valueOf(hasta));

		List<Object[]> crudas = query.getResultList();
		List<Object[]> resultado = new java.util.ArrayList<>();
		for (Object[] fila : crudas) {
			resultado.add(new Object[]{
				aFecha(fila[0]),
				fila[1] != null ? ((Number) fila[1]).longValue() : null,
				fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0
			});
		}
		return resultado;
	}

	/**
	 * Convierte a {@code LocalDate} lo que devuelva el driver para una columna DATE, igual
	 * que {@code CierreCarteraDaoServiceImpl.aFecha}: según la versión, Oracle/Hibernate
	 * puede devolver {@code java.sql.Timestamp} o {@code java.sql.Date}.
	 */
	private java.time.LocalDate aFecha(Object valor) {
		if (valor == null) {
			return null;
		}
		if (valor instanceof java.sql.Timestamp) {
			return ((java.sql.Timestamp) valor).toLocalDateTime().toLocalDate();
		}
		if (valor instanceof java.sql.Date) {
			return ((java.sql.Date) valor).toLocalDate();
		}
		if (valor instanceof java.util.Date) {
			return new java.sql.Timestamp(((java.util.Date) valor).getTime())
				.toLocalDateTime().toLocalDate();
		}
		return java.time.LocalDate.parse(valor.toString().substring(0, 10));
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectMovimientosPorEntidadYRangoDevengo(Long idEntidad, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		Query query = em.createNativeQuery(
			" select   " + PERIODO_EFECTIVO_SQL + " as PERIODO_EFECTIVO, " +
			"          a.TPAPCDGO, a.APRTCDGO, a.APRTFCTR, a.APRTVLRR, a.APRTTPMV, a.APRTGLSA " +
			" from     CRD.APRT a " +
			" where    a.ENTDCDGO = :idEntidad " +
			"   and    ( " + PERIODO_EFECTIVO_SQL + " between :desde and :hasta " +
			"            or " + PERIODO_EFECTIVO_SQL + " is null ) " +
			" order by " + PERIODO_EFECTIVO_SQL + " nulls last, a.TPAPCDGO, a.APRTFCTR, a.APRTCDGO ");
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("desde", java.sql.Date.valueOf(desde));
		query.setParameter("hasta", java.sql.Date.valueOf(hasta));

		List<Object[]> crudas = query.getResultList();
		List<Object[]> resultado = new java.util.ArrayList<>();
		for (Object[] fila : crudas) {
			resultado.add(new Object[]{
				aFecha(fila[0]),
				fila[1] != null ? ((Number) fila[1]).longValue() : null,
				fila[2] != null ? ((Number) fila[2]).longValue() : null,
				aFechaHora(fila[3]),
				fila[4] != null ? ((Number) fila[4]).doubleValue() : 0.0,
				fila[5] != null ? ((Number) fila[5]).longValue() : null,
				fila[6] != null ? fila[6].toString() : null
			});
		}
		return resultado;
	}

	/** Convierte a {@code LocalDateTime} lo que devuelva el driver para una columna TIMESTAMP. */
	private java.time.LocalDateTime aFechaHora(Object valor) {
		if (valor == null) {
			return null;
		}
		if (valor instanceof java.sql.Timestamp) {
			return ((java.sql.Timestamp) valor).toLocalDateTime();
		}
		if (valor instanceof java.util.Date) {
			return new java.sql.Timestamp(((java.util.Date) valor).getTime()).toLocalDateTime();
		}
		return java.time.LocalDateTime.parse(valor.toString());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectPeriodosAnticipadosConSaldo(Long idEntidad, Long idTipoAporte,
			java.time.LocalDate mesActual) throws Throwable {
		Query query = em.createQuery(
			" select   a.periodoDevengo, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.entidad.codigo = :idEntidad " +
			"   and    a.tipoAporte.codigo = :idTipoAporte " +
			"   and    a.periodoDevengo > :mesActual " +
			" group by a.periodoDevengo " +
			" having   sum(a.valor) > 0.01 " +
			" order by a.periodoDevengo desc ");
		query.setParameter("idEntidad", idEntidad);
		query.setParameter("idTipoAporte", idTipoAporte);
		query.setParameter("mesActual", mesActual);
		return query.getResultList();
	}

	/**
	 * Obtiene KPIs globales de aportes para el dashboard
	 * Utiliza consulta SQL nativa optimizada con CTE
	 */
	@Override
	public com.saa.model.crd.dto.AporteKpiDTO selectKpisGlobales(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte) throws Throwable {
		
		String sql = 
			"WITH base AS ( " +
			"  SELECT " +
			"      a.TPAPCDGO AS tipo_aporte_id, " +
			"      a.APRTVLRR AS valor " +
			"  FROM CRD.APRT a " +
			"  WHERE (:fechaDesde IS NULL OR a.APRTFCTR >= :fechaDesde) " +
			"    AND (:fechaHasta IS NULL OR a.APRTFCTR <= :fechaHasta) " +
			"    AND (:estadoAporte IS NULL OR a.APRTIDST = :estadoAporte) " +
			") " +
			"SELECT " +
			"  COUNT(*) AS movimientos, " +
			"  COUNT(DISTINCT tipo_aporte_id) AS tipos_aporte, " +
			"  SUM(CASE WHEN valor > 0 THEN valor ELSE 0 END) AS monto_mas, " +
			"  SUM(CASE WHEN valor < 0 THEN ABS(valor) ELSE 0 END) AS monto_menos, " +
			"  SUM(valor) AS saldo_neto " +
			"FROM base";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaHasta", fechaHasta);
		query.setParameter("estadoAporte", estadoAporte);
		
		Object[] result = (Object[]) query.getSingleResult();
		
		// Manejo de valores null y conversión segura
		Long movimientos = result[0] != null ? ((Number) result[0]).longValue() : 0L;
		Long tiposAporte = result[1] != null ? ((Number) result[1]).longValue() : 0L;
		Double montoMas = result[2] != null ? ((Number) result[2]).doubleValue() : 0.0;
		Double montoMenos = result[3] != null ? ((Number) result[3]).doubleValue() : 0.0;
		Double saldoNeto = result[4] != null ? ((Number) result[4]).doubleValue() : 0.0;
		
		return new com.saa.model.crd.dto.AporteKpiDTO(
			movimientos, 
			tiposAporte, 
			montoMas, 
			montoMenos, 
			saldoNeto
		);
	}

	/**
	 * Obtiene resumen de aportes agrupados por tipo (para gráfico de dona/tarjetas)
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.AporteResumenTipoDTO> selectResumenPorTipo(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte) throws Throwable {
		
		String sql = 
			"WITH base AS ( " +
			"  SELECT " +
			"      a.TPAPCDGO AS tipo_aporte_id, " +
			"      a.APRTVLRR AS valor " +
			"  FROM CRD.APRT a " +
			"  WHERE (:fechaDesde IS NULL OR a.APRTFCTR >= :fechaDesde) " +
			"    AND (:fechaHasta IS NULL OR a.APRTFCTR <= :fechaHasta) " +
			"    AND (:estadoAporte IS NULL OR a.APRTIDST = :estadoAporte) " +
			"), " +
			"agg AS ( " +
			"  SELECT " +
			"    b.tipo_aporte_id, " +
			"    COUNT(*) AS movimientos, " +
			"    SUM(CASE WHEN b.valor > 0 THEN b.valor ELSE 0 END) AS monto_mas, " +
			"    SUM(CASE WHEN b.valor < 0 THEN ABS(b.valor) ELSE 0 END) AS monto_menos, " +
			"    SUM(b.valor) AS saldo_neto, " +
			"    ABS(SUM(b.valor)) AS magnitud_neta " +
			"  FROM base b " +
			"  GROUP BY b.tipo_aporte_id " +
			") " +
			"SELECT " +
			"  a.tipo_aporte_id, " +
			"  t.TPAPNMBR AS tipo_aporte_nombre, " +
			"  a.movimientos, " +
			"  a.monto_mas, " +
			"  a.monto_menos, " +
			"  a.saldo_neto, " +
			"  a.magnitud_neta, " +
			"  CASE " +
			"    WHEN SUM(a.magnitud_neta) OVER () = 0 THEN 0 " +
			"    ELSE ROUND(a.magnitud_neta * 100.0 / SUM(a.magnitud_neta) OVER (), 2) " +
			"  END AS porcentaje_dona " +
			"FROM agg a " +
			"JOIN CRD.TPAP t ON t.TPAPCDGO = a.tipo_aporte_id " +
			"ORDER BY a.magnitud_neta DESC";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaHasta", fechaHasta);
		query.setParameter("estadoAporte", estadoAporte);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.AporteResumenTipoDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long tipoAporteId = row[0] != null ? ((Number) row[0]).longValue() : null;
			String tipoAporteNombre = (String) row[1];
			Long movimientos = row[2] != null ? ((Number) row[2]).longValue() : 0L;
			Double montoMas = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
			Double montoMenos = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
			Double saldoNeto = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
			Double magnitudNeta = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;
			Double porcentajeDona = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;
			
			dtos.add(new com.saa.model.crd.dto.AporteResumenTipoDTO(
				tipoAporteId, tipoAporteNombre, movimientos, montoMas, 
				montoMenos, saldoNeto, magnitudNeta, porcentajeDona
			));
		}
		
		return dtos;
	}

	/**
	 * Obtiene top N entidades con mayor impacto por tipo de aporte
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.AporteTopEntidadDTO> selectTopEntidades(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte,
			Long tipoAporteId,
			Integer topN) throws Throwable {
		
		String sql = 
			"WITH base AS ( " +
			"  SELECT " +
			"    a.ENTDCDGO AS entidad_id, " +
			"    a.TPAPCDGO AS tipo_aporte_id, " +
			"    a.APRTVLRR AS valor " +
			"  FROM CRD.APRT a " +
			"  WHERE (:fechaDesde IS NULL OR a.APRTFCTR >= :fechaDesde) " +
			"    AND (:fechaHasta IS NULL OR a.APRTFCTR <= :fechaHasta) " +
			"    AND (:estadoAporte IS NULL OR a.APRTIDST = :estadoAporte) " +
			"    AND (:tipoAporteId IS NULL OR a.TPAPCDGO = :tipoAporteId) " +
			"), " +
			"agg AS ( " +
			"  SELECT " +
			"    b.entidad_id, " +
			"    b.tipo_aporte_id, " +
			"    COUNT(*) AS movimientos, " +
			"    SUM(CASE WHEN b.valor > 0 THEN b.valor ELSE 0 END) AS monto_mas, " +
			"    SUM(CASE WHEN b.valor < 0 THEN ABS(b.valor) ELSE 0 END) AS monto_menos, " +
			"    SUM(b.valor) AS saldo_neto, " +
			"    ABS(SUM(b.valor)) AS magnitud_neta " +
			"  FROM base b " +
			"  GROUP BY b.entidad_id, b.tipo_aporte_id " +
			"), " +
			"ranked AS ( " +
			"  SELECT " +
			"    a.*, " +
			"    ROW_NUMBER() OVER ( " +
			"      PARTITION BY a.tipo_aporte_id " +
			"      ORDER BY a.magnitud_neta DESC, a.entidad_id " +
			"    ) AS rn " +
			"  FROM agg a " +
			") " +
			"SELECT " +
			"  r.tipo_aporte_id, " +
			"  r.entidad_id, " +
			"  COALESCE(e.ENTDRZNS, e.ENTDNMCM) AS entidad_nombre, " +
			"  r.movimientos, " +
			"  r.monto_mas, " +
			"  r.monto_menos, " +
			"  r.saldo_neto " +
			"FROM ranked r " +
			"JOIN CRD.ENTD e ON e.ENTDCDGO = r.entidad_id " +
			"WHERE r.rn <= :topN " +
			"ORDER BY r.tipo_aporte_id, r.rn";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaHasta", fechaHasta);
		query.setParameter("estadoAporte", estadoAporte);
		query.setParameter("tipoAporteId", tipoAporteId);
		query.setParameter("topN", topN);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.AporteTopEntidadDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long tipoAporte = row[0] != null ? ((Number) row[0]).longValue() : null;
			Long entidadId = row[1] != null ? ((Number) row[1]).longValue() : null;
			String entidadNombre = (String) row[2];
			Long movimientos = row[3] != null ? ((Number) row[3]).longValue() : 0L;
			Double montoMas = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
			Double montoMenos = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
			Double saldoNeto = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;
			
			dtos.add(new com.saa.model.crd.dto.AporteTopEntidadDTO(
				tipoAporte, entidadId, entidadNombre, movimientos, 
				montoMas, montoMenos, saldoNeto
			));
		}
		
		return dtos;
	}

	/**
	 * Obtiene top N movimientos individuales más grandes por tipo
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.AporteTopMovimientoDTO> selectTopMovimientos(
			java.time.LocalDateTime fechaDesde, 
			java.time.LocalDateTime fechaHasta, 
			Long estadoAporte,
			Long tipoAporteId,
			Integer topN) throws Throwable {
		
		String sql = 
			"WITH base AS ( " +
			"  SELECT " +
			"    a.APRTCDGO AS aporte_id, " +
			"    a.TPAPCDGO AS tipo_aporte_id, " +
			"    t.TPAPNMBR AS tipo_aporte_nombre, " +
			"    a.ENTDCDGO AS entidad_id, " +
			"    COALESCE(e.ENTDRZNS, e.ENTDNMCM) AS entidad_nombre, " +
			"    a.APRTFCTR AS fecha_transaccion, " +
			"    a.APRTVLRR AS valor, " +
			"    ABS(a.APRTVLRR) AS magnitud " +
			"  FROM CRD.APRT a " +
			"  JOIN CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO " +
			"  LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO " +
			"  WHERE (:fechaDesde IS NULL OR a.APRTFCTR >= :fechaDesde) " +
			"    AND (:fechaHasta IS NULL OR a.APRTFCTR <= :fechaHasta) " +
			"    AND (:estadoAporte IS NULL OR a.APRTIDST = :estadoAporte) " +
			"    AND (:tipoAporteId IS NULL OR a.TPAPCDGO = :tipoAporteId) " +
			"), " +
			"ranked AS ( " +
			"  SELECT " +
			"    b.*, " +
			"    ROW_NUMBER() OVER ( " +
			"      PARTITION BY b.tipo_aporte_id " +
			"      ORDER BY b.magnitud DESC, b.aporte_id DESC " +
			"    ) AS rn " +
			"  FROM base b " +
			") " +
			"SELECT " +
			"  aporte_id, " +
			"  tipo_aporte_id, " +
			"  tipo_aporte_nombre, " +
			"  entidad_id, " +
			"  entidad_nombre, " +
			"  fecha_transaccion, " +
			"  valor, " +
			"  magnitud " +
			"FROM ranked " +
			"WHERE rn <= :topN " +
			"ORDER BY tipo_aporte_id, rn";
		
		Query query = em.createNativeQuery(sql);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaHasta", fechaHasta);
		query.setParameter("estadoAporte", estadoAporte);
		query.setParameter("tipoAporteId", tipoAporteId);
		query.setParameter("topN", topN);
		
		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();
		
		java.util.List<com.saa.model.crd.dto.AporteTopMovimientoDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long aporteId = row[0] != null ? ((Number) row[0]).longValue() : null;
			Long tipoAporte = row[1] != null ? ((Number) row[1]).longValue() : null;
			String tipoAporteNombre = (String) row[2];
			Long entidadId = row[3] != null ? ((Number) row[3]).longValue() : null;
			String entidadNombre = (String) row[4];
			java.time.LocalDateTime fechaTrans = row[5] != null ? 
				((java.sql.Timestamp) row[5]).toLocalDateTime() : null;
			Double valor = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;
			Double magnitud = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;
			
			dtos.add(new com.saa.model.crd.dto.AporteTopMovimientoDTO(
				aporteId, tipoAporte, tipoAporteNombre, entidadId, 
				entidadNombre, fechaTrans, valor, magnitud
			));
		}
		
		return dtos;
	}

	// ============================================================
	// G42 — Métodos de sumatoria agrupada por entidad
	// ============================================================

	/**
	 * G42 Grupo 1 — Rendimiento: SUM(valor) de aportes cuyo tipoAporte tiene
	 * estado=1 y codigoSBS='RE', agrupado por entidad.
	 * Retorna Object[]{Long codigoEntidad, Double suma}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaRendimientoPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaRendimientoPorEntidad fechaCorte: " + fechaCorte);
		Query query = em.createQuery(
			" select   a.entidad.codigo, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.estado = 1 " +
			"   and    a.tipoAporte.codigoSBS = 'RE' " +
			"   and    a.fechaTransaccion <= :fechaCorte " +
			"   and    exists (select 1 from Entidad e where e.codigo = a.entidad.codigo and e.numeroIdentificacion <> '0') " +
			" group by a.entidad.codigo "
		);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaPatronalPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaPatronalPorEntidad fechaCorte: " + fechaCorte);
		List<Long> codigosPatronal = Arrays.asList(3L, 13L, 14L);
		Query query = em.createQuery(
			" select   a.entidad.codigo, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.estado = 1 " +
			"   and    a.tipoAporte.codigo in :codigos " +
			"   and    a.fechaTransaccion <= :fechaCorte " +
			"   and    exists (select 1 from Entidad e where e.codigo = a.entidad.codigo and e.numeroIdentificacion <> '0') " +
			" group by a.entidad.codigo "
		);
		query.setParameter("codigos", codigosPatronal);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaPersonalPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaPersonalPorEntidad fechaCorte: " + fechaCorte);
		List<Long> codigosPatronal = Arrays.asList(3L, 13L, 14L);
		Query query = em.createQuery(
			" select   a.entidad.codigo, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.estado = 1 " +
			"   and    (a.tipoAporte.codigoSBS <> 'RE' or a.tipoAporte.codigoSBS is null) " +
			"   and    a.tipoAporte.codigo not in :codigos " +
			"   and    a.fechaTransaccion <= :fechaCorte " +
			"   and    exists (select 1 from Entidad e where e.codigo = a.entidad.codigo and e.numeroIdentificacion <> '0') " +
			" group by a.entidad.codigo "
		);
		query.setParameter("codigos", codigosPatronal);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectCountImposicionesJubilacionPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectCountImposicionesJubilacionPorEntidad fechaCorte: " + fechaCorte);
		// Decisión del usuario (2026-08-27): cuenta MESES DE DEVENGO (periodo efectivo), no
		// filas. SQL nativo porque el periodo efectivo usa TRUNC, que no es JPQL estándar.
		Query query = em.createNativeQuery(
			" select   a.ENTDCDGO, count(distinct " + PERIODO_EFECTIVO_SQL + ") " +
			" from     CRD.APRT a " +
			" join     CRD.TPAP ta on ta.TPAPCDGO = a.TPAPCDGO " +
			" where    ta.TPAPIDST = 1 " +
			"   and    a.APRTVLRR > 0 " +
			"   and    a.APRTFCTR <= :fechaCorte " +
			" group by a.ENTDCDGO "
		);
		query.setParameter("fechaCorte", java.sql.Timestamp.valueOf(fechaCorte));
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaSaldoCuentaJubilacionPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaSaldoCuentaJubilacionPorEntidad fechaCorte: " + fechaCorte);
		Query query = em.createQuery(
			" select   a.entidad.codigo, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.codigo = :codigo " +
			"   and    a.fechaTransaccion <= :fechaCorte " +
			" group by a.entidad.codigo "
		);
		query.setParameter("codigo", 23L);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaAportesTipo23EnRango(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaAportesTipo23EnRango desde: " + fechaInicio + " hasta: " + fechaFin);
		Query query = em.createQuery(
			" select   a.entidad.codigo, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.codigo = :codigo " +
			"   and    a.fechaTransaccion >= :fechaInicio " +
			"   and    a.fechaTransaccion <= :fechaFin " +
			" group by a.entidad.codigo "
		);
		query.setParameter("codigo", 23L);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		return query.getResultList();
	}

	@Override
	public Double selectSumaTotalPorTipoAporte(java.time.LocalDateTime fechaCorte, Long tipoAporte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaTotalPorTipoAporte tipoAporte: " + tipoAporte + " fechaCorte: " + fechaCorte);
		Query query = em.createQuery(
			" select   sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.codigo = :tipoAporte " +
			"   and    a.fechaTransaccion <= :fechaCorte "
		);
		query.setParameter("tipoAporte", tipoAporte);
		query.setParameter("fechaCorte", fechaCorte);
		Object result = query.getSingleResult();
		return result != null ? ((Number) result).doubleValue() : 0.0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectTiposAportePorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectTiposAportePorEntidad fechaCorte: " + fechaCorte);
		Query query = em.createQuery(
			" select   a.entidad.codigo, a.tipoAporte.codigo " +
			" from     Aporte a " +
			" where    a.tipoAporte.estado = 1 " +
			"   and    a.tipoAporte.codigo in (:codigos) " +
			"   and    a.fechaTransaccion <= :fechaCorte " +
			"   and    exists (select 1 from Entidad e where e.codigo = a.entidad.codigo and e.numeroIdentificacion <> '0') " +
			" group by a.entidad.codigo, a.tipoAporte.codigo "
		);
		query.setParameter("codigos", java.util.Arrays.asList(9L, 11L));
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	/**
	 * Para CPRM — Suma de aportes agrupada por entidad Y tipo de aporte hasta fechaCorte.
	 * Genera un registro por cada combinación entidad+tipoAporte con suma != 0.
	 * Retorna Object[]{Long codigoEntidad, Long codigoTipoAporte, String nombreTipoAporte, Double suma}.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSumaPorEntidadYTipoAporte(java.time.LocalDateTime fechaCorte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectSumaPorEntidadYTipoAporte fechaCorte: " + fechaCorte);
		Query query = em.createQuery(
			" select   a.entidad.codigo, a.tipoAporte.codigo, a.tipoAporte.nombre, sum(a.valor) " +
			" from     Aporte a " +
			" where    a.tipoAporte.estado = 1 " +
			"   and    a.fechaTransaccion <= :fechaCorte " +
			"   and    exists (select 1 from Entidad e where e.codigo = a.entidad.codigo and e.numeroIdentificacion <> '0') " +
			" group by a.entidad.codigo, a.tipoAporte.codigo, a.tipoAporte.nombre " +
			" having   sum(a.valor) <> 0 "
		);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> sumValorPorTipoAporteByEntidad(Long codigoEntidad) throws Throwable {
		System.out.println("AporteDaoServiceImpl.sumValorPorTipoAporteByEntidad entidad: " + codigoEntidad);

		try {
			// El saldo disponible ES la suma neta: los pagos con aportes son filas negativas.
			// Query agregada: la BD devuelve una fila por tipo, nunca las ~980.000 filas de APRT.
			Query query = em.createQuery(
				" select   a.tipoAporte.codigo, a.tipoAporte.nombre, sum(a.valor) " +
				" from     Aporte a " +
				" where    a.entidad.codigo = :codigoEntidad " +
				"   and    a.tipoAporte.estado = 1 " +
				" group by a.tipoAporte.codigo, a.tipoAporte.nombre " +
				" order by a.tipoAporte.codigo "
			);
			query.setParameter("codigoEntidad", codigoEntidad);

			List<Object[]> resultados = query.getResultList();
			System.out.println("  Tipos de aporte con saldo encontrados: " + (resultados != null ? resultados.size() : 0));
			return resultados;

		} catch (Exception e) {
			System.err.println("Error en sumValorPorTipoAporteByEntidad: " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar lista vacía para no detener el proceso
			return new java.util.ArrayList<>();
		}
	}

	@Override
	public Double sumValorByEntidadYTipo(Long codigoEntidad, Long codigoTipoAporte) throws Throwable {
		System.out.println("AporteDaoServiceImpl.sumValorByEntidadYTipo entidad: " + codigoEntidad
			+ " tipoAporte: " + codigoTipoAporte);

		try {
			Query query = em.createQuery(
				" select sum(a.valor) " +
				" from   Aporte a " +
				" where  a.entidad.codigo = :codigoEntidad " +
				"   and  a.tipoAporte.codigo = :codigoTipoAporte "
			);
			query.setParameter("codigoEntidad", codigoEntidad);
			query.setParameter("codigoTipoAporte", codigoTipoAporte);

			Object resultado = query.getSingleResult();
			Double suma = resultado != null ? ((Number) resultado).doubleValue() : 0.0;
			System.out.println("  Saldo del tipo " + codigoTipoAporte + ": " + suma);
			return suma;

		} catch (Exception e) {
			System.err.println("Error en sumValorByEntidadYTipo: " + e.getMessage());
			e.printStackTrace();
			// NO lanzar excepción - retornar 0.0 para no detener el proceso
			return 0.0;
		}
	}

	@Override
	public List<Object[]> sumValorPorTipoAporteByCarga(Long idCarga) throws Throwable {
		System.out.println("AporteDaoServiceImpl.sumValorPorTipoAporteByCarga - Carga: " + idCarga);

		try {
			// ⚠️ TRANSITORIO (2026-08-28/29) — filtra por a.idAsoprep, NO por a.cargaArchivo,
			// a propósito. CRARCDGO es la columna gobernada (FK + índice, DDL-TRAZABILIDAD-
			// CARGA-PETRO.sql) y es la que se sigue llenando en cargas nuevas, pero idAsoprep
			// es la trazabilidad de carga que YA EXISTÍA desde antes (usada en vivo por
			// AporteDaoServiceImpl.selectByEntidadTipoYCarga/selectAporteAdelantado) y es lo
			// único poblado en el histórico. Cambiar este filtro a a.cargaArchivo.codigo antes
			// de correr y verificar en producción 78_BACKFILL_CRARCDGO_APORTES.sql dejaría el
			// asiento leyendo una columna a medio llenar y saldría corto en silencio — el
			// mismo error que la trazabilidad se creó para evitar.
			// Migrar cuando: (a) el backfill corrió en producción, y (b) su control 3.1 (aportes
			// con idAsoprep sin CRARCDGO) da 0. Ahí este WHERE cambia a a.cargaArchivo.codigo.
			Query query = em.createQuery(
				" select   a.tipoAporte.codigo, sum(a.valor) " +
				" from     Aporte a " +
				" where    a.idAsoprep = :idCarga " +
				" group by a.tipoAporte.codigo " +
				" order by a.tipoAporte.codigo "
			);
			query.setParameter("idCarga", idCarga);

			List<Object[]> resultados = query.getResultList();
			System.out.println("  Tipos de aporte de la carga: " + (resultados != null ? resultados.size() : 0));
			return resultados;

		} catch (Exception e) {
			System.err.println("Error en sumValorPorTipoAporteByCarga: " + e.getMessage());
			e.printStackTrace();
			return new java.util.ArrayList<>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Aporte> selectByCarga(Long idCarga) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectByCarga - Carga: " + idCarga);

		try {
			// Mismo filtro (a.idAsoprep, no a.cargaArchivo) que sumValorPorTipoAporteByCarga —
			// ver su javadoc para el porqué transitorio. Tienen que leer exactamente la misma
			// carga o el detalle y el asiento pueden divergir.
			Query query = em.createQuery(
				" select a from Aporte a where a.idAsoprep = :idCarga "
			);
			query.setParameter("idCarga", idCarga);
			return query.getResultList();

		} catch (Exception e) {
			System.err.println("Error en selectByCarga: " + e.getMessage());
			e.printStackTrace();
			return new java.util.ArrayList<>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectAplicadoPorEntidadEnCarga(Long idCarga) throws Throwable {
		System.out.println("AporteDaoServiceImpl.selectAplicadoPorEntidadEnCarga - Carga: " + idCarga);

		try {
			Query query = em.createQuery(
				" select   a.entidad.codigo, sum(a.valor) " +
				" from     Aporte a " +
				" where    a.idAsoprep = :idCarga " +
				" group by a.entidad.codigo "
			);
			query.setParameter("idCarga", idCarga);
			return query.getResultList();

		} catch (Exception e) {
			System.err.println("Error en selectAplicadoPorEntidadEnCarga: " + e.getMessage());
			e.printStackTrace();
			return new java.util.ArrayList<>();
		}
	}

}