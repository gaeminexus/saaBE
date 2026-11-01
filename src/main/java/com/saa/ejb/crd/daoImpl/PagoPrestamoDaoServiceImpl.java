package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PagoPrestamoDaoServiceImpl extends EntityDaoImpl<PagoPrestamo> implements PagoPrestamoDaoService {

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/**
	 * Tipos de {@code PagoPrestamo} (PGPRTPOO) cuyo {@code saldoOtros} (PGPRSLOT) es de verdad
	 * un abono a capital, para {@link #selectDatosPagosVigentes}. NO todo {@code saldoOtros} es
	 * capital: los pagos tipo "DEP" (migración vieja del 2025-04-03) lo usan para otra cosa, y
	 * sumarlo como capital fabrica un sobrante inexistente (8 préstamos reales deshabilitados en
	 * plazo vencido el 2026-09-30: 61538, 65991, 64561, 63392, 62890, 62086, 61839, 60994 — su
	 * tabla de amortización ya suma exacto el monto sin ese extra). "MIGRACION" no tiene
	 * constante propia (literal en {@code PrestamoServiceImpl:951}) porque es la única migración
	 * donde el extra SÍ es un abono real (préstamo 62439).
	 */
	private static final List<String> TIPOS_PAGO_CON_ABONO_CAPITAL = List.of(
		ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL,
		ProcesoPagoPrestamoService.TIPO_PRECANCELACION,
		"MIGRACION"
	);

	/**
	 * Busca todos los pagos asociados a un DetallePrestamo específico
	 * 
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo encontrados (vacía si hay error o no se encuentran)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectByIdDetallePrestamo(Long codigoDetallePrestamo) {
		System.out.println("PagoPrestamoDaoService.selectByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);
		
		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);
			
			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos encontrados: " + resultados.size());
			
			return resultados;
			
		} catch (Exception e) {
			System.err.println("ERROR en selectByIdDetallePrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

	// ========================================================================
	// SERVICIOS DE PAGO DE PRÉSTAMOS (§5.2 ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md)
	// ========================================================================

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectVigentesByPrestamo(Long idPrestamo) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectVigentesByPrestamo - Prestamo: " + idPrestamo);
		// anulado IS NULL cubre los pagos históricos anteriores al ALTER de CRD.PGPR. Sin
		// try/catch a propósito: un error no puede convertirse en "sin pagos".
		Query query = em.createQuery(
			"SELECT p FROM PagoPrestamo p " +
			"WHERE p.prestamo.codigo = :idPrestamo " +
			"AND (p.anulado IS NULL OR p.anulado = 0) " +
			"ORDER BY p.codigo ASC");
		query.setParameter("idPrestamo", idPrestamo);
		return query.getResultList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectVigentesByIdDetallePrestamo(Long codigoDetallePrestamo) {
		System.out.println("PagoPrestamoDaoService.selectVigentesByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);

		try {
			// anulado IS NULL cubre los pagos históricos anteriores al ALTER de CRD.PGPR
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo " +
				"AND (p.anulado IS NULL OR p.anulado = 0) " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);

			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos vigentes encontrados: " + resultados.size());

			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectVigentesByIdDetallePrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectDatosPagosVigentes(List<Long> codigosDetallePrestamo) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectDatosPagosVigentes - cuotas solicitadas: "
				+ (codigosDetallePrestamo != null ? codigosDetallePrestamo.size() : 0));

		if (codigosDetallePrestamo == null || codigosDetallePrestamo.isEmpty()) {
			return new ArrayList<>();
		}

		// anulado IS NULL cubre los pagos históricos anteriores al ALTER de CRD.PGPR. A
		// propósito NO atrapa la excepción, ver javadoc de la interfaz.
		//
		// Proyección ESCALAR a propósito: "SELECT p" instanciaba PagoPrestamo, y sus tres
		// @ManyToOne (prestamo, detallePrestamo, cargaArchivo) son EAGER (default de JPA), cada
		// uno con lo suyo — Hibernate hidrataba el grafo completo de cada pago.
		// p.saldoOtros (PGPRSLOT) agregado el 2026-09-30, AL FINAL, índice 7: es el "pago extra"
		// (abono a capital) — AbonoCapitalPrestamoServiceImpl:232-240 lo graba con
		// capitalPagado=0, así que sin esto un abono desaparece del capital cobrado
		// (API-PASE-A-PLAZO-VENCIDO.md §2, caso real préstamo 62439). Los índices 0-6 no se
		// tocan: verificado que ningún llamador depende del largo del arreglo.
		//
		// ⛔ Corrección 2026-09-30 (mismo día, producción): el índice 7 YA NO es
		// "p.saldoOtros" crudo — es un CASE que lo filtra por TIPO_CON_ABONO_CAPITAL. Medido
		// contra 8 préstamos reales (61538, 65991, 64561, 63392, 62890, 62086, 61839, 60994):
		// todos tienen pagos tipo "DEP" de una migración vieja (2025-04-03) cuyo PGPRSLOT NO es
		// capital — su tabla de amortización ya suma exacto el monto (capitalPagado + saldo =
		// monto al centavo), y en el 65991 el "extra" de la cuota 0 es igual a su capital:
		// sumarlo fabrica un sobrante que no existe. El 62439 (tipo "MIGRACION") sí es un abono
		// real que rehizo la tabla — por eso el filtro es por TIPO de pago, no "todo PGPRSLOT".
		String jpql = "SELECT p.detallePrestamo.codigo, p.desgravamen, p.moraPagada, " +
			"       p.interesVencidoPagado, p.interesPagado, p.capitalPagado, p.valorSeguroIncendio, " +
			"       CASE WHEN p.tipo IN :tiposConAbonoCapital THEN p.saldoOtros ELSE 0.0 END " +
			"FROM PagoPrestamo p " +
			"WHERE p.detallePrestamo.codigo IN :codigos " +
			"AND (p.anulado IS NULL OR p.anulado = 0) " +
			"ORDER BY p.detallePrestamo.codigo";

		// Fragmentado en bloques de 900: Oracle limita IN (...) a 1000 elementos (ORA-01795).
		final int TAMANIO_BLOQUE = 900;
		List<Object[]> resultados = new ArrayList<>();
		for (int inicio = 0; inicio < codigosDetallePrestamo.size(); inicio += TAMANIO_BLOQUE) {
			List<Long> bloque = codigosDetallePrestamo.subList(inicio,
					Math.min(inicio + TAMANIO_BLOQUE, codigosDetallePrestamo.size()));

			Query query = em.createQuery(jpql);
			query.setParameter("codigos", bloque);
			query.setParameter("tiposConAbonoCapital", TIPOS_PAGO_CON_ABONO_CAPITAL);
			resultados.addAll(query.getResultList());
		}

		System.out.println("  Pagos vigentes encontrados: " + resultados.size());
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectDatosPagosVigentesAlCorte(List<Long> codigosDetallePrestamo, java.time.LocalDate corte)
			throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectDatosPagosVigentesAlCorte - cuotas: "
				+ (codigosDetallePrestamo != null ? codigosDetallePrestamo.size() : 0) + " - corte: " + corte);
		if (codigosDetallePrestamo == null || codigosDetallePrestamo.isEmpty()) {
			return new ArrayList<>();
		}
		String jpql = "SELECT p.detallePrestamo.codigo, p.desgravamen, p.moraPagada, " +
			"       p.interesVencidoPagado, p.interesPagado, p.capitalPagado, p.valorSeguroIncendio, " +
			"       CASE WHEN p.tipo IN :tiposConAbonoCapital THEN p.saldoOtros ELSE 0.0 END " +
			"FROM PagoPrestamo p " +
			"WHERE p.detallePrestamo.codigo IN :codigos " +
			"AND (p.anulado IS NULL OR p.anulado = 0) " +
			"AND p.fecha < :corteSig " +
			"ORDER BY p.detallePrestamo.codigo";
		final int TAMANIO_BLOQUE = 900;
		List<Object[]> resultados = new ArrayList<>();
		for (int inicio = 0; inicio < codigosDetallePrestamo.size(); inicio += TAMANIO_BLOQUE) {
			List<Long> bloque = codigosDetallePrestamo.subList(inicio,
					Math.min(inicio + TAMANIO_BLOQUE, codigosDetallePrestamo.size()));
			Query query = em.createQuery(jpql);
			query.setParameter("codigos", bloque);
			query.setParameter("tiposConAbonoCapital", TIPOS_PAGO_CON_ABONO_CAPITAL);
			query.setParameter("corteSig", corte.plusDays(1).atStartOfDay());
			resultados.addAll(query.getResultList());
		}
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectCapitalAbonadoEnMoraOParcialByPrestamos(List<Long> codigosPrestamo) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectCapitalAbonadoEnMoraOParcialByPrestamos - préstamos solicitados: "
				+ (codigosPrestamo != null ? codigosPrestamo.size() : 0));

		if (codigosPrestamo == null || codigosPrestamo.isEmpty()) {
			return new ArrayList<>();
		}

		// SUM AGRUPADO en la base a propósito: una fila por préstamo, no una por pago — el
		// usuario pidió explícitamente no traer filas de pago a Java para este cálculo, tras
		// dos episodios de lentitud en este mismo endpoint por consultas por fila.
		//
		// Llega al préstamo por p.detallePrestamo.prestamo.codigo (navega por la cuota), NO por
		// p.prestamo.codigo: esa FK directa puede venir NULL en pagos viejos.
		//
		// SOLO EN_MORA (5) y PARCIAL (6), no "cualquier no liquidada": PENDIENTE (1) aporta 0
		// aunque tenga pagos vigentes — segunda decisión del usuario, 2026-09-10 (ver javadoc
		// de la interfaz y API-SALDOS-PRESTAMO.md §3bis). Constantes de EstadoCuotaPrestamo,
		// no literales.
		String jpql = "SELECT p.detallePrestamo.prestamo.codigo, SUM(p.capitalPagado) " +
			"FROM PagoPrestamo p " +
			"WHERE p.detallePrestamo.prestamo.codigo IN :codigos " +
			"AND (p.anulado IS NULL OR p.anulado = 0) " +
			"AND p.detallePrestamo.estado IN (:estadoEnMora, :estadoParcial) " +
			"GROUP BY p.detallePrestamo.prestamo.codigo";

		// Fragmentado en bloques de 900: Oracle limita IN (...) a 1000 elementos (ORA-01795).
		final int TAMANIO_BLOQUE = 900;
		List<Object[]> resultados = new ArrayList<>();
		for (int inicio = 0; inicio < codigosPrestamo.size(); inicio += TAMANIO_BLOQUE) {
			List<Long> bloque = codigosPrestamo.subList(inicio,
					Math.min(inicio + TAMANIO_BLOQUE, codigosPrestamo.size()));

			Query query = em.createQuery(jpql);
			query.setParameter("codigos", bloque);
			query.setParameter("estadoEnMora", (long) com.saa.rubros.EstadoCuotaPrestamo.EN_MORA);
			query.setParameter("estadoParcial", (long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
			resultados.addAll(query.getResultList());
		}

		System.out.println("  Préstamos con capital abonado en cuotas EN_MORA/PARCIAL: " + resultados.size());
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectByEvento(Long codigoEvento) {
		System.out.println("PagoPrestamoDaoService.selectByEvento - Evento: " + codigoEvento);

		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.eventoPrestamo.codigo = :codigoEvento " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("codigoEvento", codigoEvento);

			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos del evento encontrados: " + resultados.size());

			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectByEvento: " + e.getMessage());
			e.printStackTrace();
			// Retornar lista vacía en lugar de lanzar excepción
			return new ArrayList<>();
		}
	}

	@Override
	public Long contarVigentesByIdDetallePrestamo(Long codigoDetallePrestamo) {
		System.out.println("PagoPrestamoDaoService.contarVigentesByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);

		try {
			Query query = em.createQuery(
				"SELECT COUNT(p) " +
				"FROM PagoPrestamo p " +
				"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo " +
				"AND (p.anulado IS NULL OR p.anulado = 0)"
			);
			query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);

			Long total = (Long) query.getSingleResult();
			System.out.println("  Pagos vigentes: " + total);
			return total;

		} catch (Exception e) {
			System.err.println("ERROR en contarVigentesByIdDetallePrestamo: " + e.getMessage());
			e.printStackTrace();
			// Retornar 0 en lugar de lanzar excepción
			return 0L;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectVigentesByCargaArchivo(Long idCarga) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectVigentesByCargaArchivo - Carga: " + idCarga);

		try {
			Query query = em.createQuery(
				"SELECT p " +
				"FROM PagoPrestamo p " +
				"WHERE p.cargaArchivo.codigo = :idCarga " +
				"AND (p.anulado IS NULL OR p.anulado = 0) " +
				"ORDER BY p.codigo ASC"
			);
			query.setParameter("idCarga", idCarga);

			List<PagoPrestamo> resultados = query.getResultList();
			System.out.println("  Pagos vigentes de la carga: " + resultados.size());
			return resultados;

		} catch (Exception e) {
			System.err.println("ERROR en selectVigentesByCargaArchivo: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectAplicadoPorEntidadEnCarga(Long idCarga) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectAplicadoPorEntidadEnCarga - Carga: " + idCarga);

		try {
			// Prefijo estable de e7b76c8: 'AFECTACION_MANUAL:%' es la ruta manual, cualquier
			// otra cosa (incluido observacion null) es la ruta automática.
			Query query = em.createQuery(
				"SELECT pr.entidad.codigo, " +
				"       SUM(CASE WHEN p.observacion LIKE 'AFECTACION_MANUAL:%' THEN p.valor ELSE 0.0 END), " +
				"       SUM(CASE WHEN p.observacion NOT LIKE 'AFECTACION_MANUAL:%' OR p.observacion IS NULL THEN p.valor ELSE 0.0 END), " +
				"       SUM(p.valor) " +
				"FROM PagoPrestamo p JOIN p.prestamo pr " +
				"WHERE p.cargaArchivo.codigo = :idCarga " +
				"AND (p.anulado IS NULL OR p.anulado = 0) " +
				"GROUP BY pr.entidad.codigo"
			);
			query.setParameter("idCarga", idCarga);

			return query.getResultList();

		} catch (Exception e) {
			System.err.println("ERROR en selectAplicadoPorEntidadEnCarga: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	@Override
	public long countByIdDetallePrestamo(Long codigoDetallePrestamo) throws Throwable {
		System.out.println("PagoPrestamoDaoService.countByIdDetallePrestamo - DetallePrestamo: " + codigoDetallePrestamo);

		// Sin try/catch: a propósito. Ver el javadoc en la interfaz — es una guarda de
		// borrado, y una guarda que ante un error de consulta dice "0 pagos" deja borrar
		// cuotas que en realidad no se pudieron verificar.
		Query query = em.createQuery(
			"SELECT COUNT(p) " +
			"FROM PagoPrestamo p " +
			"WHERE p.detallePrestamo.codigo = :codigoDetallePrestamo"
		);
		query.setParameter("codigoDetallePrestamo", codigoDetallePrestamo);

		long total = (Long) query.getSingleResult();
		System.out.println("  Pagos totales (incluye anulados): " + total);
		return total;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> selectMaxFechaPagoByPrestamos(List<Long> codigosPrestamo) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectMaxFechaPagoByPrestamos - préstamos solicitados: "
				+ (codigosPrestamo != null ? codigosPrestamo.size() : 0));

		if (codigosPrestamo == null || codigosPrestamo.isEmpty()) {
			return new ArrayList<>();
		}

		// MAX AGRUPADO en la base, una fila por préstamo. Llega por p.detallePrestamo.prestamo.codigo
		// (no p.prestamo.codigo, que puede venir NULL en pagos viejos) — mismo criterio que
		// selectCapitalAbonadoEnMoraOParcialByPrestamos.
		String jpql = "SELECT p.detallePrestamo.prestamo.codigo, MAX(p.fecha) " +
			"FROM PagoPrestamo p " +
			"WHERE p.detallePrestamo.prestamo.codigo IN :codigos " +
			"AND (p.anulado IS NULL OR p.anulado = 0) " +
			"GROUP BY p.detallePrestamo.prestamo.codigo";

		// Fragmentado en bloques de 900: Oracle limita IN (...) a 1000 elementos (ORA-01795).
		final int TAMANIO_BLOQUE = 900;
		List<Object[]> resultados = new ArrayList<>();
		for (int inicio = 0; inicio < codigosPrestamo.size(); inicio += TAMANIO_BLOQUE) {
			List<Long> bloque = codigosPrestamo.subList(inicio,
					Math.min(inicio + TAMANIO_BLOQUE, codigosPrestamo.size()));

			Query query = em.createQuery(jpql);
			query.setParameter("codigos", bloque);
			resultados.addAll(query.getResultList());
		}

		System.out.println("  Préstamos con pago vigente: " + resultados.size());
		return resultados;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PagoPrestamo> selectVigentesLegadoPorObservacionCarga(Long idCarga) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectVigentesLegadoPorObservacionCarga - Carga: " + idCarga);

		// Sin try/catch a propósito: es la base de una anulación (reproceso de carga). Un error
		// que devolviera lista vacía dejaría la aplicación vieja viva debajo de la nueva.
		Query query = em.createQuery(
			"SELECT p " +
			"FROM PagoPrestamo p " +
			"WHERE p.cargaArchivo IS NULL " +
			"AND (p.anulado IS NULL OR p.anulado = 0) " +
			"AND p.observacion LIKE :patron " +
			"ORDER BY p.codigo ASC"
		);
		query.setParameter("patron", "%[CargaArchivo: " + idCarga + "]%");

		List<PagoPrestamo> resultados = query.getResultList();
		System.out.println("  Pagos vigentes del esquema viejo de la carga: " + resultados.size());
		return resultados;
	}

	@Override
	public Integer selectMaxCorridaReproceso(Long idCarga) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectMaxCorridaReproceso - Carga: " + idCarga);

		// Nativo: REGEXP_SUBSTR no existe en JPQL. La etiqueta es fija
		// ("REPROCESO carga N corrida M - motivo"), se extrae el M.
		Query query = em.createNativeQuery(
			" SELECT MAX(TO_NUMBER(REGEXP_SUBSTR(p.PGPRMTAN, 'corrida (\\d+)', 1, 1, NULL, 1))) " +
			" FROM   CRD.PGPR p " +
			" WHERE  p.PGPRMTAN LIKE :patron "
		);
		query.setParameter("patron", "REPROCESO carga " + idCarga + " corrida %");
		Object resultado = query.getSingleResult();
		return resultado != null ? Integer.valueOf(((Number) resultado).intValue()) : null;
	}

	/**
	 * Observación con que la migración de Delta dejó sus pagos (D-025): en la base dice
	 * "Pago cargado desde Excel - Migración de datos" (con tilde, UTF-8). El patrón deja un
	 * comodín en la vocal acentuada para no depender de la codificación del cliente.
	 */
	private static final String PATRON_OBSERVACION_PAGO_MIGRADO = "Pago cargado desde Excel - Migraci_n de datos%";

	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> selectMigradosDelMesConFilaEnCarga(Long idCarga, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		System.out.println("PagoPrestamoDaoService.selectMigradosDelMesConFilaEnCarga - Carga: " + idCarga
			+ " - " + desde + " a " + hasta);

		// Sin try/catch a propósito: es la base de una anulación (D-025, reproceso de carga).
		// Fila de la carga: PXCA (rol) + DTCA (producto Petro), descuento REAL = PXCADSDO
		// (totalDescontado, lo que el motor aplica), sumado por rol y producto por si el archivo
		// trae dos filas. El pago migrado lleva el desgravamen aparte (PGPRDSGR): el descuento
		// de Petro lo incluye, así que se compara PGPRVLRR + PGPRDSGR (medido 2026-10-28 en la
		// carga 352: 42 grupos coinciden al centavo así, 26.841,61).
		Query query = em.createNativeQuery(
			" WITH fila AS ( " +
			"   SELECT x.PXCACDPT rol, TRIM(c.DTCACDPP) prod, SUM(NVL(x.PXCADSDO,0)) descuento " +
			"   FROM   CRD.PXCA x JOIN CRD.DTCA c ON c.DTCACDGO = x.DTCACDGO " +
			"   WHERE  c.CRARCDGO = :carga " +
			"   GROUP BY x.PXCACDPT, TRIM(c.DTCACDPP) ) " +
			" SELECT g.PGPRCDGO, e.ENTDRLPC, TRIM(pr.PRDCCDPT), " +
			"        ROUND(NVL(g.PGPRVLRR,0) + NVL(g.PGPRDSGR,0), 2), ROUND(f.descuento, 2), " +
			"        p.PRSTCDGO, d.DTPRNMCT " +
			" FROM   CRD.PGPR g " +
			" JOIN   CRD.DTPR d  ON d.DTPRCDGO = g.DTPRCDGO " +
			" JOIN   CRD.PRST p  ON p.PRSTCDGO = d.PRSTCDGO " +
			" JOIN   CRD.ENTD e  ON e.ENTDCDGO = p.ENTDCDGO " +
			" JOIN   CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO " +
			" JOIN   fila f      ON f.rol = e.ENTDRLPC AND f.prod = TRIM(pr.PRDCCDPT) " +
			" WHERE  g.PGPRTPOO = 'MIGRACION' " +
			" AND    g.PGPROBSR LIKE :observacion " +
			" AND    g.CRARCDGO IS NULL " +
			" AND    g.EVPRCDGO IS NULL " +
			" AND    NVL(g.PGPRANUL, 0) = 0 " +
			" AND    g.PGPRFCHA >= :desde AND g.PGPRFCHA < :hastaSig " +
			" ORDER BY e.ENTDRLPC, TRIM(pr.PRDCCDPT), g.PGPRCDGO "
		);
		query.setParameter("carga", idCarga);
		query.setParameter("observacion", PATRON_OBSERVACION_PAGO_MIGRADO);
		query.setParameter("desde", java.sql.Date.valueOf(desde));
		query.setParameter("hastaSig", java.sql.Date.valueOf(hasta.plusDays(1)));

		List<Object[]> filas = query.getResultList();
		List<Object[]> resultado = new ArrayList<>();
		for (Object[] f : filas) {
			resultado.add(new Object[] {
				f[0] != null ? ((Number) f[0]).longValue() : null,
				f[1] != null ? ((Number) f[1]).longValue() : null,
				f[2] != null ? f[2].toString() : null,
				f[3] != null ? ((Number) f[3]).doubleValue() : 0.0,
				f[4] != null ? ((Number) f[4]).doubleValue() : 0.0,
				f[5] != null ? ((Number) f[5]).longValue() : null,
				f[6] != null ? ((Number) f[6]).longValue() : null });
		}
		System.out.println("  Pagos migrados del mes con fila en la carga: " + resultado.size());
		return resultado;
	}

}
