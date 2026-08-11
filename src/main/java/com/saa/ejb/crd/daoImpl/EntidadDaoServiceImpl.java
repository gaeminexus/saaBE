package com.saa.ejb.crd.daoImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.model.crd.Entidad;
import com.saa.rubros.ASPSensibilidadBusquedaCoincidencias;
import com.saa.rubros.EstadoParticipeEntidad;
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

	/** Código en CRD.ESPR del estado que habilita voto y elegibilidad. */
	private static final Long CODIGO_ESTADO_ACTIVO = (long) EstadoParticipeEntidad.ACTIVO;

	/**
	 * Estados considerados por defecto en los resúmenes agrupados por estado.
	 * No incluye CESANTE_FALLECIDO, JUBILADO_APORTANTE ni ACTIVO_EN_MORA:
	 * se mantiene el comportamiento histórico, revisar al completar la migración.
	 */
	private static final List<Long> ESTADOS_RESUMEN_POR_DEFECTO = Arrays.asList(
			(long) EstadoParticipeEntidad.ACTIVO,
			(long) EstadoParticipeEntidad.CESANTE,
			(long) EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO);

	/** Tipos de aporte que cuentan para el padrón: 9 = JUBILACIÓN, 11 = CESANTÍA. */
	private static final Long TIPO_APORTE_JUBILACION = 9L;
	private static final Long TIPO_APORTE_CESANTIA   = 11L;

	/** Meses hacia atrás que se revisan para determinar el estado de mora. */
	private static final int MESES_VENTANA_MORA = 2;

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
			estadosPermitidos = ESTADOS_RESUMEN_POR_DEFECTO; // Valores por defecto
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
			estadosPermitidos = ESTADOS_RESUMEN_POR_DEFECTO;
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
			estadosPermitidos = ESTADOS_RESUMEN_POR_DEFECTO;
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
			estadosPermitidos = ESTADOS_RESUMEN_POR_DEFECTO;
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

	@SuppressWarnings("unchecked")
	@Override
	public List<Entidad> selectByIdEstado(Long idEstado) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdEstado Entidad con idEstado: " + idEstado);
		Query query = em.createQuery(
			"select e from Entidad e where e.idEstado = :idEstado");
		query.setParameter("idEstado", idEstado);
		return query.getResultList();
	}

	@Override
	public Entidad findById(Long codigo) throws Throwable {
		System.out.println("Ingresa al metodo findById Entidad con codigo: " + codigo);
		return em.find(Entidad.class, codigo);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Entidad selectByNumeroIdentificacion(String numeroIdentificacion) throws Throwable {
		System.out.println("EntidadDaoServiceImpl.selectByNumeroIdentificacion: " + numeroIdentificacion);
		Query query = em.createQuery(
			" select e from Entidad e " +
			" where  e.numeroIdentificacion = :numeroIdentificacion "
		);
		query.setParameter("numeroIdentificacion", numeroIdentificacion);
		query.setMaxResults(1);
		List<Entidad> result = query.getResultList();
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Entidad> findByCodigosIn(List<Long> codigos) throws Throwable {
		if (codigos == null || codigos.isEmpty()) {
			return new java.util.ArrayList<>();
		}
		System.out.println("EntidadDaoServiceImpl.findByCodigosIn - cantidad: " + codigos.size());
		Query query = em.createQuery(
			" select e from Entidad e " +
			" where  e.codigo IN :codigos "
		);
		query.setParameter("codigos", codigos);
		return query.getResultList();
	}

	/**
	 * Genera el padrón de partícipes en UNA sola consulta nativa, para evitar N+1
	 * sobre CRD.APRT. Las fronteras de mes se calculan en Java, de modo que Oracle
	 * siempre compara un bind contra una expresión derivada de columna.
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.PadronParticipeDTO> selectPadronParticipes(
			java.time.LocalDateTime fechaEjecucion,
			Long calidadId,
			Long minimoAportes) throws Throwable {

		System.out.println("Ingresa al metodo selectPadronParticipes de Entidad con fechaEjecucion: " + fechaEjecucion
				+ ", calidadId: " + calidadId + ", minimoAportes: " + minimoAportes);

		// El padrón se evalúa al CIERRE DEL MES ANTERIOR a la fecha de ejecución,
		// no a la fecha de ejecución. Los aportes se graban con fecha del último
		// día del mes, así que el mes en curso todavía no tiene su carga procesada
		// y contarlo daría a todo el mundo como si no hubiera aportado.
		//
		// Efecto: el reporte devuelve lo mismo se corra el día 5 o el 28 del mes.
		//
		// fechaEjecucion 2026-08-11  =>  mesReferencia   2026-07-01
		//                                corte aportes   < 2026-08-01
		//                                primerMesAlDia  2026-06-01
		java.time.LocalDateTime mesReferencia = fechaEjecucion.toLocalDate()
				.withDayOfMonth(1).atStartOfDay().minusMonths(1);
		java.time.LocalDateTime corteAportes  = mesReferencia.plusMonths(1);

		// Está AL DIA quien aportó en alguno de los últimos MESES_VENTANA_MORA meses
		// contados hacia atrás desde el mes de referencia, ese mes incluido.
		// Cae EN MORA a partir de acumular MESES_VENTANA_MORA meses sin aportar.
		//
		// Con ventana = 2 y referencia julio, primerMesAlDia = junio:
		//   último aporte julio -> 0 meses sin aportar          -> AL DIA
		//   último aporte junio -> 1 mes sin aportar (jul)      -> AL DIA
		//   último aporte mayo  -> 2 meses sin aportar (jun,jul)-> EN MORA (2)
		//
		// El -1 es lo que hace que el borde caiga donde debe: sin él, mayo
		// quedaría como AL DIA con 0 meses de mora pese a llevar dos meses sin aportar.
		java.time.LocalDateTime primerMesAlDia = mesReferencia
				.minusMonths(MESES_VENTANA_MORA - 1L);

		String sql =
			"WITH aportes AS ( " +
			// Un mes con uno o con varios aportes positivos 9/11 cuenta como UN aporte.
			// El corte es "< primer instante del mes de ejecución", es decir, se
			// consideran los aportes hasta el último día del mes anterior inclusive.
			"  SELECT a.ENTDCDGO                              AS entidad_id, " +
			"         COUNT(DISTINCT TRUNC(a.APRTFCTR, 'MM')) AS numero_aportes, " +
			"         MAX(TRUNC(a.APRTFCTR, 'MM'))            AS ultimo_mes_aporte " +
			"  FROM   CRD.APRT a " +
			"  WHERE  a.TPAPCDGO IN (:tiposAporte) " +
			"    AND  a.APRTVLRR > 0 " +
			"    AND  a.APRTFCTR <  :corteAportes " +
			"  GROUP BY a.ENTDCDGO " +
			"), " +
			"base AS ( " +
			"  SELECT e.ENTDCDGO       AS entidad_id, " +
			"         e.ENTDNMID       AS cedula, " +
			"         TRIM(e.ENTDRZNS) AS nombres_apellidos, " +
			"         e.ENTDIDST       AS calidad_id, " +
			"         NVL(TRIM(esp.ESPRNMBR), 'SIN ESTADO') AS calidad_nombre, " +
			"         CASE WHEN e.ENTDIDST = :codigoEstadoActivo THEN 1 ELSE 0 END AS es_activo, " +
			"         NVL(ap.numero_aportes, 0) AS numero_aportes, " +
			// AL DIA si el último mes con aporte cae en [primerMesAlDia .. mesReferencia].
			"         CASE WHEN ap.ultimo_mes_aporte IS NOT NULL " +
			"                   AND ap.ultimo_mes_aporte >= :primerMesAlDia " +
			"              THEN 'AL DIA' ELSE 'EN MORA' END AS estado_mora, " +
			// NULL cuando nunca aportó: no hay último aporte desde el cual contar.
			"         CASE WHEN ap.ultimo_mes_aporte IS NULL THEN NULL " +
			"              WHEN ap.ultimo_mes_aporte >= :primerMesAlDia THEN 0 " +
			"              ELSE ROUND(MONTHS_BETWEEN(:mesReferencia, ap.ultimo_mes_aporte)) " +
			"         END AS meses_en_mora " +
			"  FROM   CRD.ENTD e " +
			"  LEFT JOIN CRD.ESPR esp ON esp.ESPRCDEX  = e.ENTDIDST " +
			"  LEFT JOIN aportes  ap  ON ap.entidad_id = e.ENTDCDGO " +
			"  WHERE  NVL(TRIM(e.ENTDNMID), '0') <> '0' " +
			"    AND  (:calidadId IS NULL OR e.ENTDIDST = :calidadId) " +
			") " +
			"SELECT ROW_NUMBER() OVER (ORDER BY UPPER(b.nombres_apellidos), b.entidad_id) AS numero, " +
			"       b.entidad_id, " +
			"       b.cedula, " +
			"       b.nombres_apellidos, " +
			"       b.calidad_id, " +
			"       b.calidad_nombre, " +
			"       b.numero_aportes, " +
			"       b.estado_mora, " +
			"       b.meses_en_mora, " +
			"       CASE WHEN b.es_activo = 1 AND b.estado_mora = 'AL DIA' " +
			"            THEN 'SI' ELSE 'NO' END AS habilitado_voto, " +
			"       CASE WHEN b.es_activo = 1 AND b.numero_aportes >= :minimoAportes " +
			"            THEN 'SI' ELSE 'NO' END AS elegible_miembro " +
			"FROM   base b " +
			"ORDER BY UPPER(b.nombres_apellidos), b.entidad_id";

		Query query = em.createNativeQuery(sql);
		query.setParameter("tiposAporte", Arrays.asList(TIPO_APORTE_JUBILACION, TIPO_APORTE_CESANTIA));
		query.setParameter("corteAportes", corteAportes);
		query.setParameter("primerMesAlDia", primerMesAlDia);
		query.setParameter("mesReferencia", mesReferencia);
		query.setParameter("codigoEstadoActivo", CODIGO_ESTADO_ACTIVO);
		query.setParameter("calidadId", calidadId);
		query.setParameter("minimoAportes", minimoAportes);

		@SuppressWarnings("unchecked")
		java.util.List<Object[]> results = query.getResultList();

		java.util.List<com.saa.model.crd.dto.PadronParticipeDTO> dtos = new java.util.ArrayList<>();
		for (Object[] row : results) {
			Long   numero            = row[0]  != null ? ((Number) row[0]).longValue()  : null;
			Long   entidadId         = row[1]  != null ? ((Number) row[1]).longValue()  : null;
			String cedula            = row[2]  != null ? row[2].toString()              : null;
			String nombresApellidos  = row[3]  != null ? row[3].toString()              : null;
			Long   codigoCalidad     = row[4]  != null ? ((Number) row[4]).longValue()  : null;
			String calidadParticipe  = row[5]  != null ? row[5].toString()              : null;
			Long   numeroAportes     = row[6]  != null ? ((Number) row[6]).longValue()  : 0L;
			String estadoMora        = row[7]  != null ? row[7].toString()              : null;
			Long   mesesEnMora       = row[8]  != null ? ((Number) row[8]).longValue()  : null;
			String habilitadoVoto    = row[9]  != null ? row[9].toString()              : null;
			String elegibleMiembro   = row[10] != null ? row[10].toString()             : null;

			dtos.add(new com.saa.model.crd.dto.PadronParticipeDTO(
				numero, entidadId, cedula, nombresApellidos, codigoCalidad, calidadParticipe,
				numeroAportes, estadoMora, mesesEnMora, habilitadoVoto, elegibleMiembro
			));
		}

		System.out.println("selectPadronParticipes - filas devueltas: " + dtos.size());
		return dtos;
	}

}
