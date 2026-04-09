package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.model.crd.Aporte;

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

	/**
	 * Cuenta la cantidad de aportes por entidad
	 */
	@Override
	public Long selectCountByEntidad(Long idEntidad) throws Throwable {
	    System.out.println("metodo selectCountByEntidad de AporteDaoServiceImpl");
	    Query query = em.createQuery( " select   count(b) " + 
	    							  " from     Aporte b " +
	    							  " where    b.entidad.codigo = :idEntidad ");
	    query.setParameter("idEntidad", idEntidad);
	    return (Long) query.getSingleResult();
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
	 */
	@Override
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
	 */
	@Override
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

}