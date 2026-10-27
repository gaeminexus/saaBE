package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.AcreditacionVacacionesService;
import com.saa.ejb.rhh.service.CierreNominaVacacionesService;
import com.saa.ejb.rhh.service.CorridaAcreditacionVacacionesService;
import com.saa.model.rhh.CorridaAcreditacionVacaciones;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.scp.Empresa;
import com.saa.rubros.RhhEstadoCorridaAcreditacion;
import com.saa.rubros.RhhModalidadVacaciones;
import com.saa.rubros.RhhOrigenCorridaAcreditacion;
import com.saa.rubros.RhhTipoPeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de CierreNominaVacacionesService.</p>
 *
 * <h3>Por que NO acredita en modalidad por aniversario</h3>
 *
 * <p>En esa modalidad <code>acreditar</code> crea el saldo del anio de corte con los quince
 * dias COMPLETOS en cuanto el empleado cumple su anio de servicio, y no descuenta el saldo de
 * apertura de migracion que ya cubre ese mismo anio de servicio: <code>diasDisponibles</code>
 * suma los dos. Una corrida manual lo hace cuando una persona decide; una corrida automatica
 * mensual lo haria sola, el mes del aniversario de cada empleado, sin que nadie lo vea. Ver
 * docs/logica-negocio/ESTADO-EQUIPO-OMEN-3.md (hallazgo de la apertura duplicada). En
 * devengo mensual no aplica: el saldo es por anio calendario y la corrida solo recalcula.</p>
 */
@Stateless
public class CierreNominaVacacionesServiceImpl implements CierreNominaVacacionesService {

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@EJB
	private CorridaAcreditacionVacacionesDaoService corridaAcreditacionVacacionesDaoService;

	@EJB
	private CorridaAcreditacionVacacionesService corridaAcreditacionVacacionesService;

	@EJB
	private AcreditacionVacacionesService acreditacionVacacionesService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.CierreNominaVacacionesService#acreditarAlCerrarPeriodo(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public String acreditarAlCerrarPeriodo(Long idPeriodoNomina, String usuario) {
		System.out.println("Ingresa al metodo acreditarAlCerrarPeriodo, periodo: " + idPeriodoNomina);
		try {
			PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
					NombreEntidadesRhh.PERIODO_NOMINA);
			if (periodo == null || periodo.getEmpresa() == null || periodo.getAnio() == null
					|| periodo.getMes() == null) {
				System.out.println("acreditarAlCerrarPeriodo: el periodo " + idPeriodoNomina
						+ " no existe o esta incompleto; no se acredita.");
				return null;
			}
			// Los periodos de decimos, utilidades y liquidacion tambien se cierran, pero no
			// dicen hasta donde llego la nomina del mes.
			if (!Long.valueOf(RhhTipoPeriodoNomina.MENSUAL).equals(periodo.getTipoPeriodo())) {
				System.out.println("acreditarAlCerrarPeriodo: el periodo " + idPeriodoNomina
						+ " no es MENSUAL; no se acredita.");
				return null;
			}

			Long idEmpresa = periodo.getEmpresa().getCodigo();
			LocalDate fechaCorte = YearMonth.of(periodo.getAnio().intValue(), periodo.getMes().intValue())
					.atEndOfMonth();
			Integer anio = Integer.valueOf(fechaCorte.getYear());

			ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
			// Sin parametros del anio no se decide nada aqui: acreditar lanza la
			// IncomeException con el mensaje claro y la deja como ERROR en la bitacora.
			if (prnm != null && (prnm.getModalidadVacaciones() == null
					|| prnm.getModalidadVacaciones().intValue() == RhhModalidadVacaciones.POR_ANIVERSARIO)) {
				registraOmitida(idEmpresa, fechaCorte, Long.valueOf(RhhModalidadVacaciones.POR_ANIVERSARIO),
						"El anio " + anio + " esta en modalidad por aniversario: la corrida automatica del cierre"
								+ " del periodo " + periodo.getAnio() + "-" + periodo.getMes() + " no acredita, porque"
								+ " acreditar el mes del aniversario duplicaria la apertura de migracion. Acredite"
								+ " manualmente desde Acreditar vacaciones.", usuario);
				return null;
			}

			// acreditar recalcula los dias asignados AL CORTE: correrla con un corte anterior
			// al de una corrida ya hecha le bajaria los dias a todos. Cerrar un mes atrasado
			// (un historico) no debe deshacer lo acreditado hasta un mes posterior.
			if (corridaAcreditacionVacacionesDaoService.existeOkConCortePosterior(idEmpresa, anio, fechaCorte)) {
				registraOmitida(idEmpresa, fechaCorte, prnm != null ? prnm.getModalidadVacaciones() : null,
						"Ya hay una corrida OK del anio " + anio + " con corte posterior al " + fechaCorte
								+ ": no se reacredita para no bajar los dias ya acreditados.", usuario);
				return null;
			}

			acreditacionVacacionesService.acreditar(idEmpresa, fechaCorte, usuario,
					RhhOrigenCorridaAcreditacion.CIERRE_NOMINA);
			return null;
		} catch (Throwable e) {
			// El cierre ya esta confirmado: aqui solo se avisa. La fila ERROR la dejo acreditar()
			// en la bitacora; si el fallo fue antes de llegar a ella, queda en el log.
			System.out.println("acreditarAlCerrarPeriodo fallo, el cierre NO se revierte: " + e.getMessage());
			return "El periodo se cerro, pero no se pudo acreditar las vacaciones: "
					+ (e.getMessage() != null ? e.getMessage() : e.getClass().getName())
					+ ". Acredite manualmente desde Acreditar vacaciones.";
		}
	}

	/**
	 * Deja asentado en la bitacora que la corrida automatica no acredito y por que.
	 */
	private void registraOmitida(Long idEmpresa, LocalDate fechaCorte, Long modalidad, String mensaje,
			String usuario) throws Throwable {
		System.out.println("acreditarAlCerrarPeriodo OMITIDA: " + mensaje);
		CorridaAcreditacionVacaciones corrida = new CorridaAcreditacionVacaciones();
		Empresa empresa = new Empresa();
		empresa.setCodigo(idEmpresa);
		corrida.setEmpresa(empresa);
		corrida.setAnio(Integer.valueOf(fechaCorte.getYear()));
		corrida.setFechaCorte(fechaCorte);
		corrida.setModalidad(modalidad);
		corrida.setEmpleadosAcreditados(Integer.valueOf(0));
		corrida.setOrigen(RhhOrigenCorridaAcreditacion.CIERRE_NOMINA);
		corrida.setEstado(RhhEstadoCorridaAcreditacion.OMITIDA);
		corrida.setMensaje(mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje);
		corrida.setUsuarioRegistro(usuario);
		corrida.setFechaRegistro(LocalDateTime.now());
		corridaAcreditacionVacacionesService.registrarEnTransaccionAparte(corrida);
	}
}
