package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.EstadoCierreService;
import com.saa.ejb.cnt.service.dto.EstadoPeriodo;
import com.saa.model.cnt.CierrePeriodoListener;
import com.saa.model.cnt.Periodo;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCorridaCierreCartera;
import com.saa.rubros.EstadoPeriodos;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * C17 — Implementación de {@link EstadoCierreService}. Solo lecturas (JPQL directo, sin los DAO genéricos que
 * lanzan cuando no hay fila). docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md.
 */
@Stateless
public class EstadoCierreServiceImpl implements EstadoCierreService {

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// Consultas base
	// =====================================================================

	@SuppressWarnings("unchecked")
	private List<Periodo> periodos(Long idEmpresa, Long anio, Long mes) {
		String jpql = "select p from Periodo p where p.anio = :anio and p.mes = :mes"
				+ (idEmpresa != null ? " and p.empresa.codigo = :empresa" : "");
		jakarta.persistence.Query q = em.createQuery(jpql).setParameter("anio", anio).setParameter("mes", mes);
		if (idEmpresa != null) {
			q.setParameter("empresa", idEmpresa);
		}
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	private CorridaCierreCartera corridaEjecutada(Long idEmpresa, Long anio, Long mes) {
		String jpql = "select c from CorridaCierreCartera c where c.anio = :anio and c.mes = :mes"
				+ " and c.estado = :activo and c.idEstado = :ejecutada"
				+ (idEmpresa != null ? " and c.empresa.codigo = :empresa" : "")
				+ " order by c.codigo desc";
		jakarta.persistence.Query q = em.createQuery(jpql)
				.setParameter("anio", anio).setParameter("mes", mes)
				.setParameter("activo", Long.valueOf(Estado.ACTIVO))
				.setParameter("ejecutada", Long.valueOf(EstadoCorridaCierreCartera.EJECUTADA));
		if (idEmpresa != null) {
			q.setParameter("empresa", idEmpresa);
		}
		q.setMaxResults(1);
		List<CorridaCierreCartera> r = q.getResultList();
		return r.isEmpty() ? null : r.get(0);
	}

	private static String mesTexto(Long anio, Long mes) {
		return anio + "-" + (mes != null && mes < 10 ? "0" + mes : String.valueOf(mes));
	}

	private static Long anio(LocalDate f) {
		return Long.valueOf(f.getYear());
	}

	private static Long mes(LocalDate f) {
		return Long.valueOf(f.getMonthValue());
	}

	// =====================================================================
	// Estado
	// =====================================================================

	@Override
	public EstadoPeriodo estadoPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable {
		if (anio == null || mes == null || mes < 1 || mes > 12) {
			throw new IncomeException("Año y mes (1 a 12) son obligatorios para consultar el estado del período.");
		}
		EstadoPeriodo e = new EstadoPeriodo();
		e.setIdEmpresa(idEmpresa);
		e.setAnio(anio);
		e.setMes(mes);
		e.setEstado(EstadoPeriodo.ABIERTO);
		List<Periodo> lista = periodos(idEmpresa, anio, mes);
		e.setExistePeriodo(!lista.isEmpty());
		for (Periodo p : lista) {
			if (e.getIdPeriodo() == null) {
				e.setIdPeriodo(p.getCodigo());
				e.setEstadoMayorizacion(p.getEstado());
			}
			if (CierrePeriodoListener.contableCerrado(p)) {
				e.setEstado(EstadoPeriodo.CERRADO);
				e.setIdPeriodo(p.getCodigo());
				e.setEstadoMayorizacion(p.getEstado());
				e.setFechaCierre(p.getFechaCierreContable());
				e.setUsuarioCierre(p.getUsuarioCierreContable());
			}
		}
		CorridaCierreCartera c = corridaEjecutada(idEmpresa, anio, mes);
		e.setEstadoCartera(c != null ? EstadoPeriodo.CERRADO : EstadoPeriodo.ABIERTO);
		if (c != null) {
			e.setIdCorridaCartera(c.getCodigo());
			e.setFechaCierreCartera(c.getFechaModificacion() != null ? c.getFechaModificacion() : c.getFechaRegistro());
			e.setUsuarioCierreCartera(c.getUsuarioModificacion() != null ? c.getUsuarioModificacion() : c.getUsuarioRegistro());
		}
		return e;
	}

	@Override
	public boolean contableCerrado(Long idEmpresa, LocalDate fecha) throws Throwable {
		return fecha != null && contableCerrado(idEmpresa, anio(fecha), mes(fecha));
	}

	@Override
	public boolean contableCerrado(Long idEmpresa, Long anio, Long mes) throws Throwable {
		for (Periodo p : periodos(idEmpresa, anio, mes)) {
			if (CierrePeriodoListener.contableCerrado(p)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean carteraCerrada(Long idEmpresa, LocalDate fecha) throws Throwable {
		return fecha != null && carteraCerrada(idEmpresa, anio(fecha), mes(fecha));
	}

	@Override
	public boolean carteraCerrada(Long idEmpresa, Long anio, Long mes) throws Throwable {
		return corridaEjecutada(idEmpresa, anio, mes) != null;
	}

	// =====================================================================
	// Validaciones
	// =====================================================================

	@Override
	public void validarContableAbierto(Long idEmpresa, LocalDate fecha, String operacion) throws Throwable {
		if (fecha == null) {
			return;
		}
		for (Periodo p : periodos(idEmpresa, anio(fecha), mes(fecha))) {
			if (CierrePeriodoListener.contableCerrado(p)) {
				throw new IncomeException(CierrePeriodoListener.mensaje(p, operacion));
			}
		}
	}

	@Override
	public void validarCarteraAbierta(Long idEmpresa, LocalDate fecha, String operacion) throws Throwable {
		if (fecha == null) {
			return;
		}
		validarCartera(idEmpresa, anio(fecha), mes(fecha), operacion);
	}

	private void validarCartera(Long idEmpresa, Long anio, Long mes, String operacion) {
		CorridaCierreCartera c = corridaEjecutada(idEmpresa, anio, mes);
		if (c != null) {
			throw new IncomeException("CARTERA_CERRADA: el mes " + mesTexto(anio, mes)
					+ " ya pasó por el cierre–apertura de cartera (corrida " + c.getCodigo() + "); no admite "
					+ (operacion != null ? operacion : "movimientos de cartera ni de cuentas individuales")
					+ ". Solo consulta. Para corregir, reabra la cartera del mes (POST /cierrecartera/reabrir) "
					+ "o registre el movimiento en un mes abierto.");
		}
	}

	@Override
	public void validarMovimientoCartera(Long idEmpresa, LocalDate fecha, String operacion) throws Throwable {
		if (fecha == null) {
			return;
		}
		validarMovimientoCartera(idEmpresa, anio(fecha), mes(fecha), operacion);
	}

	@Override
	public void validarMovimientoCartera(Long idEmpresa, Long anio, Long mes, String operacion) throws Throwable {
		if (anio == null || mes == null) {
			return;
		}
		validarCartera(idEmpresa, anio, mes, operacion);
		for (Periodo p : periodos(idEmpresa, anio, mes)) {
			if (CierrePeriodoListener.contableCerrado(p)) {
				throw new IncomeException(CierrePeriodoListener.mensaje(p, operacion));
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public LocalDate fechaReversoEnPeriodoAbierto(Long idEmpresa, LocalDate fechaOriginal) throws Throwable {
		LocalDate hoy = LocalDate.now();
		LocalDate base = fechaOriginal != null ? fechaOriginal : hoy;
		long clave = base.getYear() * 100L + base.getMonthValue();
		String jpql = "select p from Periodo p where (p.anio * 100 + p.mes) > :clave"
				+ (idEmpresa != null ? " and p.empresa.codigo = :empresa" : "")
				+ " order by p.anio, p.mes, p.codigo";
		jakarta.persistence.Query q = em.createQuery(jpql).setParameter("clave", Long.valueOf(clave));
		if (idEmpresa != null) {
			q.setParameter("empresa", idEmpresa);
		}
		List<Periodo> lista = q.getResultList();
		for (Periodo p : lista) {
			if (CierrePeriodoListener.contableCerrado(p)
					|| (p.getEstado() != null && p.getEstado().longValue() == EstadoPeriodos.MAYORIZADO)) {
				continue;
			}
			LocalDate primero = p.getPrimerDia() != null ? p.getPrimerDia()
					: LocalDate.of(p.getAnio().intValue(), p.getMes().intValue(), 1);
			LocalDate ultimo = p.getUltimoDia() != null ? p.getUltimoDia() : primero.withDayOfMonth(primero.lengthOfMonth());
			if (primero.isAfter(hoy)) {
				break;
			}
			return hoy.isAfter(ultimo) ? primero : hoy;
		}
		throw new IncomeException("PERIODO_CERRADO: no hay un período contable abierto (no cerrado ni mayorizado) "
				+ "posterior a " + mesTexto(anio(base), mes(base)) + " y hasta hoy donde registrar el reverso. "
				+ "Abra o desmayorice un período, o indique la fecha del reverso.");
	}

	@Override
	public LocalDate validarAnulacionCartera(Long idEmpresa, LocalDate fechaOriginal, String operacion) throws Throwable {
		if (fechaOriginal == null || !carteraCerrada(idEmpresa, fechaOriginal)) {
			return null;
		}
		if (!contableCerrado(idEmpresa, fechaOriginal)) {
			validarCarteraAbierta(idEmpresa, fechaOriginal, operacion);
		}
		LocalDate fechaReverso = fechaReversoEnPeriodoAbierto(idEmpresa, fechaOriginal);
		validarCarteraAbierta(idEmpresa, fechaReverso, operacion + " (con fecha de reverso " + fechaReverso + ")");
		return fechaReverso;
	}

	@Override
	public void validarReporteCartera(Long idEmpresa, Long anio, Long mes, String reporte) throws Throwable {
		if (anio == null || mes == null) {
			throw new IncomeException("REPORTE_MES_NO_CERRADO: año y mes son obligatorios para generar " + reporte + ".");
		}
		if (!carteraCerrada(idEmpresa, anio, mes)) {
			throw new IncomeException("REPORTE_MES_NO_CERRADO: " + reporte + " solo se genera para meses que ya pasaron "
					+ "por el cierre–apertura de cartera; " + mesTexto(anio, mes) + " no tiene una corrida de cierre "
					+ "EJECUTADA. Ejecute el cierre de cartera del mes (POST /cierrecartera/ejecutar) y vuelva a generar.");
		}
	}

	@Override
	public void validarReporteFinanciero(Long idEmpresa, Long anio, Long mes, String reporte) throws Throwable {
		validarReporteCartera(idEmpresa, anio, mes, reporte);
		if (!contableCerrado(idEmpresa, anio, mes)) {
			throw new IncomeException("REPORTE_MES_NO_CERRADO: " + reporte + " definitivo solo se genera para "
					+ "períodos contables cerrados; " + mesTexto(anio, mes) + " está ABIERTO (cierre con POST /prdo/cerrar).");
		}
	}

	@Override
	public boolean permiteFueraDeOrden() {
		return Boolean.parseBoolean(System.getProperty(PROPIEDAD_FUERA_DE_ORDEN, "false").trim());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long ultimoMesCerradoCartera(Long idEmpresa) throws Throwable {
		String jpql = "select max(c.anio * 100 + c.mes) from CorridaCierreCartera c where c.estado = :activo"
				+ " and c.idEstado = :ejecutada" + (idEmpresa != null ? " and c.empresa.codigo = :empresa" : "");
		jakarta.persistence.Query q = em.createQuery(jpql)
				.setParameter("activo", Long.valueOf(Estado.ACTIVO))
				.setParameter("ejecutada", Long.valueOf(EstadoCorridaCierreCartera.EJECUTADA));
		if (idEmpresa != null) {
			q.setParameter("empresa", idEmpresa);
		}
		List<Object> r = q.getResultList();
		return r.isEmpty() || r.get(0) == null ? null : ((Number) r.get(0)).longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long ultimoMesCerradoContable(Long idEmpresa) throws Throwable {
		String jpql = "select max(p.anio * 100 + p.mes) from Periodo p where (p.cerradoContable = 1 or p.estado = :cerrado)"
				+ (idEmpresa != null ? " and p.empresa.codigo = :empresa" : "");
		jakarta.persistence.Query q = em.createQuery(jpql).setParameter("cerrado", Long.valueOf(EstadoPeriodos.CERRADO));
		if (idEmpresa != null) {
			q.setParameter("empresa", idEmpresa);
		}
		List<Object> r = q.getResultList();
		return r.isEmpty() || r.get(0) == null ? null : ((Number) r.get(0)).longValue();
	}
}
