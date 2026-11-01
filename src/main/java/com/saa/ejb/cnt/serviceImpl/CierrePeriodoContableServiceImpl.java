package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.CierrePeriodoContableService;
import com.saa.ejb.cnt.service.EstadoCierreService;
import com.saa.ejb.cnt.service.dto.EstadoPeriodo;
import com.saa.model.cnt.CierrePeriodoListener;
import com.saa.model.cnt.HistorialCierrePeriodo;
import com.saa.model.cnt.Periodo;
import com.saa.rubros.EstadoPeriodos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * C17 — Implementación de {@link CierrePeriodoContableService}. docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md.
 */
@Stateless
public class CierrePeriodoContableServiceImpl implements CierrePeriodoContableService {

	@PersistenceContext
	private EntityManager em;

	@EJB
	private EstadoCierreService estadoCierreService;

	private static String mesTexto(Long anio, Long mes) {
		return anio + "-" + (mes != null && mes < 10 ? "0" + mes : String.valueOf(mes));
	}

	private static long clave(Long anio, Long mes) {
		return anio.longValue() * 100L + mes.longValue();
	}

	private static long claveAnterior(Long anio, Long mes) {
		return mes.longValue() == 1L ? (anio.longValue() - 1L) * 100L + 12L : clave(anio, mes) - 1L;
	}

	private static String textoClave(long c) {
		long m = c % 100L;
		return (c / 100L) + "-" + (m < 10 ? "0" + m : String.valueOf(m));
	}

	private static void validaParametros(Long idEmpresa, Long anio, Long mes, String usuario) {
		if (idEmpresa == null || anio == null || mes == null || mes < 1 || mes > 12) {
			throw new IncomeException("idEmpresa, anio y mes (1 a 12) son obligatorios.");
		}
		if (usuario == null || usuario.trim().isEmpty()) {
			throw new IncomeException("El usuario es obligatorio.");
		}
	}

	private Periodo periodo(Long idEmpresa, Long anio, Long mes) {
		List<Periodo> lista = em.createQuery("select p from Periodo p where p.empresa.codigo = :e and p.anio = :a"
				+ " and p.mes = :m order by p.codigo", Periodo.class)
				.setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes)
				.getResultList();
		if (lista.isEmpty()) {
			throw new IncomeException("No existe el período contable " + mesTexto(anio, mes) + " en la empresa "
					+ idEmpresa + " (CNT.PRDO).");
		}
		return lista.get(0);
	}

	private boolean cerrado(String tipo, Long idEmpresa, long c) throws Throwable {
		Long anio = c / 100L;
		Long mes = c % 100L;
		return HistorialCierrePeriodo.TIPO_CARTERA.equals(tipo)
				? estadoCierreService.carteraCerrada(idEmpresa, anio, mes)
				: estadoCierreService.contableCerrado(idEmpresa, anio, mes);
	}

	private Long ultimoCerrado(String tipo, Long idEmpresa) throws Throwable {
		return HistorialCierrePeriodo.TIPO_CARTERA.equals(tipo)
				? estadoCierreService.ultimoMesCerradoCartera(idEmpresa)
				: estadoCierreService.ultimoMesCerradoContable(idEmpresa);
	}

	// =====================================================================
	// Orden
	// =====================================================================

	@Override
	public boolean validarOrdenCierre(String tipo, Long idEmpresa, Long anio, Long mes, boolean esArranque)
			throws Throwable {
		long c = clave(anio, mes);
		String problema = null;
		Long ultimo = ultimoCerrado(tipo, idEmpresa);
		if (ultimo != null && ultimo.longValue() > c) {
			problema = "ya hay un mes posterior cerrado (" + textoClave(ultimo) + "): se cierra en orden, del más"
					+ " antiguo al más reciente";
		} else if (!esArranque && ultimo != null && !cerrado(tipo, idEmpresa, claveAnterior(anio, mes))) {
			problema = "el mes anterior " + textoClave(claveAnterior(anio, mes)) + " no está cerrado (último "
					+ "cerrado: " + textoClave(ultimo) + "): cierre primero los meses intermedios";
		}
		if (problema == null) {
			return false;
		}
		if (estadoCierreService.permiteFueraDeOrden()) {
			System.out.println("C17: cierre " + tipo + " " + mesTexto(anio, mes) + " FUERA DE ORDEN permitido por "
					+ EstadoCierreService.PROPIEDAD_FUERA_DE_ORDEN + ": " + problema);
			return true;
		}
		throw new IncomeException("CIERRE_FUERA_DE_ORDEN: no se puede cerrar " + tipo.toLowerCase() + " de "
				+ mesTexto(anio, mes) + ": " + problema + ".");
	}

	@Override
	public boolean validarOrdenReapertura(String tipo, Long idEmpresa, Long anio, Long mes) throws Throwable {
		long c = clave(anio, mes);
		Long ultimo = ultimoCerrado(tipo, idEmpresa);
		if (ultimo == null || ultimo.longValue() <= c) {
			return false;
		}
		if (estadoCierreService.permiteFueraDeOrden()) {
			System.out.println("C17: reapertura " + tipo + " " + mesTexto(anio, mes) + " FUERA DE ORDEN permitida por "
					+ EstadoCierreService.PROPIEDAD_FUERA_DE_ORDEN + " (último cerrado " + textoClave(ultimo) + ")");
			return true;
		}
		throw new IncomeException("CIERRE_FUERA_DE_ORDEN: solo se reabre el último mes cerrado; " + mesTexto(anio, mes)
				+ " tiene meses posteriores cerrados para " + tipo.toLowerCase() + " (el último es " + textoClave(ultimo)
				+ "). Reabra en cascada desde el más reciente.");
	}

	// =====================================================================
	// Cierre / reapertura contable
	// =====================================================================

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public EstadoPeriodo cerrar(Long idEmpresa, Long anio, Long mes, String usuario, String ip, String motivo)
			throws Throwable {
		System.out.println("C17 cerrar periodo contable " + mesTexto(anio, mes) + " empresa " + idEmpresa + " usuario " + usuario);
		validaParametros(idEmpresa, anio, mes, usuario);
		Periodo periodo = periodo(idEmpresa, anio, mes);
		if (CierrePeriodoListener.contableCerrado(periodo)) {
			throw new IncomeException("El período contable " + mesTexto(anio, mes) + " ya está CERRADO"
					+ (periodo.getUsuarioCierreContable() != null ? " (por " + periodo.getUsuarioCierreContable() + ")" : "") + ".");
		}
		if (periodo.getEstado() == null || periodo.getEstado().longValue() != EstadoPeriodos.MAYORIZADO) {
			throw new IncomeException("El período contable " + mesTexto(anio, mes) + " no está MAYORIZADO (estado "
					+ periodo.getEstado() + "): mayorice el período antes de cerrarlo, para que los estados financieros"
					+ " definitivos salgan de saldos mayorizados.");
		}
		if (!estadoCierreService.carteraCerrada(idEmpresa, anio, mes)) {
			throw new IncomeException("El cierre contable de " + mesTexto(anio, mes) + " exige la cartera del mes cerrada:"
					+ " ejecute antes el cierre–apertura de cartera (POST /cierrecartera/ejecutar).");
		}
		boolean fueraDeOrden = validarOrdenCierre(HistorialCierrePeriodo.TIPO_CONTABLE, idEmpresa, anio, mes, false);

		LocalDateTime ahora = LocalDateTime.now();
		periodo.setCerradoContable(1L);
		periodo.setFechaCierreContable(ahora);
		periodo.setUsuarioCierreContable(usuario.trim());
		em.merge(periodo);
		registrarBitacora(HistorialCierrePeriodo.TIPO_CONTABLE, HistorialCierrePeriodo.ACCION_CIERRE, idEmpresa, anio, mes,
				usuario, ip, motivo, periodo.getCodigo(), null, fueraDeOrden);
		em.flush();
		return estadoCierreService.estadoPeriodo(idEmpresa, anio, mes);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public EstadoPeriodo reabrir(Long idEmpresa, Long anio, Long mes, String usuario, String ip, String motivo)
			throws Throwable {
		System.out.println("C17 reabrir periodo contable " + mesTexto(anio, mes) + " empresa " + idEmpresa + " usuario " + usuario);
		validaParametros(idEmpresa, anio, mes, usuario);
		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("El motivo de la reapertura es obligatorio (queda en la bitácora CNT.HCPR).");
		}
		Periodo periodo = periodo(idEmpresa, anio, mes);
		if (!CierrePeriodoListener.contableCerrado(periodo)) {
			throw new IncomeException("El período contable " + mesTexto(anio, mes) + " no está cerrado.");
		}
		boolean fueraDeOrden = validarOrdenReapertura(HistorialCierrePeriodo.TIPO_CONTABLE, idEmpresa, anio, mes);

		periodo.setCerradoContable(0L);
		periodo.setFechaCierreContable(null);
		periodo.setUsuarioCierreContable(null);
		if (periodo.getEstado() != null && periodo.getEstado().longValue() == EstadoPeriodos.CERRADO) {
			// Estado legado 4 (nadie lo pone hoy): se devuelve a MAYORIZADO si tiene mayorización, si no ACTIVO.
			periodo.setEstado(Long.valueOf(periodo.getIdMayorizacion() != null ? EstadoPeriodos.MAYORIZADO : EstadoPeriodos.ACTIVO));
		}
		em.merge(periodo);
		registrarBitacora(HistorialCierrePeriodo.TIPO_CONTABLE, HistorialCierrePeriodo.ACCION_REAPERTURA, idEmpresa, anio,
				mes, usuario, ip, motivo, periodo.getCodigo(), null, fueraDeOrden);
		em.flush();
		return estadoCierreService.estadoPeriodo(idEmpresa, anio, mes);
	}

	// =====================================================================
	// Bitácora
	// =====================================================================

	@Override
	public void registrarBitacora(String tipo, String accion, Long idEmpresa, Long anio, Long mes, String usuario,
			String ip, String motivo, Long idPeriodo, Long idCorrida, boolean fueraDeOrden) throws Throwable {
		HistorialCierrePeriodo h = new HistorialCierrePeriodo();
		h.setTipo(tipo);
		h.setAccion(accion);
		h.setIdEmpresa(idEmpresa);
		h.setAnio(anio);
		h.setMes(mes);
		h.setFecha(LocalDateTime.now());
		h.setUsuario(usuario != null && !usuario.trim().isEmpty() ? usuario.trim() : "SISTEMA");
		h.setIp(ip);
		if (motivo != null && !motivo.trim().isEmpty()) {
			String m = motivo.trim();
			h.setMotivo(m.length() > 1000 ? m.substring(0, 1000) : m);
		}
		h.setIdPeriodo(idPeriodo);
		h.setIdCorrida(idCorrida);
		h.setFueraDeOrden(fueraDeOrden ? 1L : 0L);
		em.persist(h);
	}

	@Override
	public List<HistorialCierrePeriodo> listarBitacora(Long idEmpresa, String tipo) throws Throwable {
		if (idEmpresa == null) {
			throw new IncomeException("idEmpresa es obligatorio.");
		}
		boolean conTipo = tipo != null && !tipo.trim().isEmpty();
		jakarta.persistence.TypedQuery<HistorialCierrePeriodo> q = em.createQuery(
				"select h from HistorialCierrePeriodo h where h.idEmpresa = :e"
						+ (conTipo ? " and h.tipo = :t" : "") + " order by h.fecha desc, h.codigo desc",
				HistorialCierrePeriodo.class).setParameter("e", idEmpresa);
		if (conTipo) {
			q.setParameter("t", tipo.trim().toUpperCase());
		}
		return q.getResultList();
	}
}
