package com.saa.ejb.cnt.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.cnt.estructura.DefinicionEstructura;
import com.saa.ejb.cnt.estructura.ReglaCuentaRenglon;
import com.saa.ejb.cnt.estructura.RenglonEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SaldoCuenta;
import com.saa.ejb.cnt.estructura.SolicitudEstructura;
import com.saa.ejb.cnt.flujo.CalculadorFlujoIndirecto;
import com.saa.ejb.cnt.flujo.ConsultasFlujo;
import com.saa.ejb.cnt.flujo.FuenteDatosFlujo;
import com.saa.ejb.cnt.flujo.GeneradorFlujoEfectivo;
import com.saa.ejb.cnt.flujo.LineaAsientoCuenta;
import com.saa.ejb.cnt.flujo.ResultadoComparativo;
import com.saa.ejb.cnt.flujo.ResultadoFlujoEfectivo;
import com.saa.ejb.cnt.flujo.SolicitudFlujoEfectivo;
import com.saa.ejb.cnt.service.EstadoPeriodoConsulta;
import com.saa.ejb.cnt.service.EstructuraReporteService;
import com.saa.ejb.cnt.service.FlujoEfectivoService;
import com.saa.rubros.CntParametroEstadoFinanciero;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * C38: estado de flujos de efectivo (indirecto), flujo de caja (directo, BIESS) y comparativo con los formatos de
 * ASOPREP. Solo lectura. El calculo esta en {@link GeneradorFlujoEfectivo} (clases puras de com.saa.ejb.cnt.flujo,
 * validadas fuera del servidor contra cuadreSuper 03-trabajo/flujo-efectivo/scripts/flujo-efectivo.js); este servicio
 * solo les da los datos: el B17 por /esrp (EstructuraReporteService, que valida el cierre del mes), las definiciones
 * FLE/FCD/CMP de CNT.RPRT/RNRP/CRRP (semilla C38-01), los saldos y las lineas de asiento.
 */
@Stateless
public class FlujoEfectivoServiceImpl implements FlujoEfectivoService {

	@PersistenceContext
	private EntityManager em;

	@EJB
	private EstructuraReporteService estructuraReporteService;

	@EJB
	private EstadoPeriodoConsulta estadoPeriodoConsulta;

	@Override
	public ResultadoFlujoEfectivo generar(SolicitudFlujoEfectivo s) throws Throwable {
		System.out.println("FlujoEfectivoServiceImpl.generar " + (s == null ? "" : s.getEmpresa() + " " + s.getAnio() + "-"
				+ s.getMes() + " " + s.getPeriodo()));
		Map<Integer, String> p = parametros();
		ResultadoFlujoEfectivo r = GeneradorFlujoEfectivo.flujo(fuente(s.getEmpresa()), s, grupos(p));
		r.setRazonSocial(valor(p, CntParametroEstadoFinanciero.RAZON_SOCIAL, nombreEmpresa(s.getEmpresa())));
		r.setRuc(valor(p, CntParametroEstadoFinanciero.RUC, null));
		r.setFirma1Nombre(valor(p, CntParametroEstadoFinanciero.NOMBRE_REPRESENTANTE, null));
		r.setFirma1Cargo(valor(p, CntParametroEstadoFinanciero.CARGO_REPRESENTANTE, "GERENTE GENERAL"));
		r.setFirma2Nombre(valor(p, CntParametroEstadoFinanciero.NOMBRE_CONTADOR, null));
		r.setFirma2Cargo(valor(p, CntParametroEstadoFinanciero.CARGO_CONTADOR, "CONTADOR GENERAL"));
		r.setEstadoPeriodo(estadoPeriodoConsulta.estadoPeriodo(s.getEmpresa(), s.getAnio(), s.getMes()).getDescripcion());
		return r;
	}

	@Override
	public ResultadoComparativo comparativo(SolicitudFlujoEfectivo s) throws Throwable {
		System.out.println("FlujoEfectivoServiceImpl.comparativo " + (s == null ? "" : s.getEmpresa() + " " + s.getAnio() + "-" + s.getMes()));
		Map<Integer, String> p = parametros();
		ResultadoComparativo r = GeneradorFlujoEfectivo.comparativo(fuente(s.getEmpresa()), s, grupos(p));
		r.setRazonSocial(valor(p, CntParametroEstadoFinanciero.RAZON_SOCIAL, nombreEmpresa(s.getEmpresa())));
		r.setEstadoPeriodo(estadoPeriodoConsulta.estadoPeriodo(s.getEmpresa(), s.getAnio(), s.getMes()).getDescripcion());
		return r;
	}

	/** Datos de la empresa para el generador. */
	private FuenteDatosFlujo fuente(Long empresa) {
		return new FuenteDatosFlujo() {
			@Override
			public DefinicionEstructura definicion(String codigoEstructura) throws Throwable {
				return estructuraReporteService.definicionEstructura(empresa, codigoEstructura);
			}

			@Override
			public List<RenglonEstructura> renglones(DefinicionEstructura def, LocalDate corte) {
				return estructuraReporteService.renglonesVigentes(def.getCodigoReporte(), corte);
			}

			@Override
			public List<ReglaCuentaRenglon> reglas(DefinicionEstructura def, LocalDate corte) {
				return estructuraReporteService.reglasVigentes(def.getCodigoReporte(), corte);
			}

			@Override
			public ResultadoEstructura b17(LocalDate corte, LocalDate fechaComparacion, boolean preliminar) throws Throwable {
				SolicitudEstructura se = new SolicitudEstructura();
				se.setEmpresa(empresa);
				se.setCodigoEstructura(GeneradorFlujoEfectivo.ESTRUCTURA_B17);
				se.setFechaCorte(corte);
				se.setFechaComparacion(fechaComparacion);
				se.setPreliminar(preliminar);
				return estructuraReporteService.generar(se);
			}

			@Override
			public List<SaldoCuenta> saldosPorCuenta(LocalDate desde, LocalDate hasta) throws Throwable {
				return estructuraReporteService.saldosPorCuenta(empresa, desde, hasta);
			}

			@Override
			@SuppressWarnings("unchecked")
			public List<LineaAsientoCuenta> lineasPorAsiento(LocalDate desde, LocalDate hasta) {
				Query q = em.createNativeQuery(ConsultasFlujo.LINEAS_POR_ASIENTO);
				q.setParameter(1, empresa);
				q.setParameter(2, EstadoAsiento.ACTIVO);
				q.setParameter(3, EstadoAsiento.REVERSADO);
				q.setParameter(4, java.sql.Date.valueOf(desde));
				q.setParameter(5, java.sql.Date.valueOf(hasta.plusDays(1)));
				List<LineaAsientoCuenta> out = new ArrayList<>();
				for (Object[] f : (List<Object[]>) q.getResultList()) {
					out.add(new LineaAsientoCuenta(((Number) f[0]).longValue(), f[1].toString(), decimal(f[2])));
				}
				return out;
			}

			@Override
			public LocalDate primerAsiento() {
				Query q = em.createNativeQuery(ConsultasFlujo.PRIMER_ASIENTO);
				q.setParameter(1, empresa);
				q.setParameter(2, EstadoAsiento.ACTIVO);
				q.setParameter(3, EstadoAsiento.REVERSADO);
				Object o = q.getSingleResult();
				if (o == null) {
					return null;
				}
				if (o instanceof java.sql.Timestamp) {
					return ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
				}
				if (o instanceof java.sql.Date) {
					return ((java.sql.Date) o).toLocalDate();
				}
				if (o instanceof java.time.LocalDateTime) {
					return ((java.time.LocalDateTime) o).toLocalDate();
				}
				if (o instanceof LocalDate) {
					return (LocalDate) o;
				}
				return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
			}
		};
	}

	private static CalculadorFlujoIndirecto.Grupos grupos(Map<Integer, String> p) {
		return new CalculadorFlujoIndirecto.Grupos(valor(p, CntParametroEstadoFinanciero.GRUPO_ACTIVO, "1"),
				valor(p, CntParametroEstadoFinanciero.GRUPO_PASIVO, "2"),
				valor(p, CntParametroEstadoFinanciero.GRUPO_PATRIMONIO, "3"),
				valor(p, CntParametroEstadoFinanciero.GRUPO_GASTOS, "4"),
				valor(p, CntParametroEstadoFinanciero.GRUPO_INGRESOS, "5"));
	}

	/**
	 * Parametros del rubro 250 por codigo alterno de detalle; vacio si el rubro no existe. Misma consulta que
	 * EstadoFinancieroOficialServiceImpl (razon social, RUC, firmas y grupos de nivel 1).
	 */
	@SuppressWarnings("unchecked")
	private Map<Integer, String> parametros() {
		Map<Integer, String> m = new HashMap<>();
		try {
			Query q = em.createNativeQuery("SELECT d.PDTRALTR, d.PDTRVLRV FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO"
					+ " WHERE r.PRBRALTR = ?1 AND d.PDTRESTD = 1");
			q.setParameter(1, Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS);
			for (Object[] f : (List<Object[]>) q.getResultList()) {
				if (f[0] != null && f[1] != null && !f[1].toString().trim().isEmpty()) {
					m.put(((Number) f[0]).intValue(), f[1].toString().trim());
				}
			}
		} catch (Exception e) {
			// sin rubro 250: valores por defecto
		}
		return m;
	}

	private static String valor(Map<Integer, String> p, int clave, String porDefecto) {
		String v = p.get(clave);
		return v != null ? v : porDefecto;
	}

	private String nombreEmpresa(Long empresa) {
		try {
			Query q = em.createNativeQuery("SELECT PJRQNMBR FROM SCP.PJRQ WHERE PJRQCDGO = ?1");
			q.setParameter(1, empresa);
			Object o = q.getSingleResult();
			return o == null ? "" : o.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static BigDecimal decimal(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		return o instanceof BigDecimal ? (BigDecimal) o : new BigDecimal(o.toString());
	}
}
