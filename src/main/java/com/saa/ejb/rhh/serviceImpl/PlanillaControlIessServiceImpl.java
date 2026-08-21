package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.NovedadIessDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.PlanillaControlIessService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.LineaPlanillaControlIess;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.NovedadIess;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.PlanillaControlIess;
import com.saa.rubros.RhhEstadoNomina;
import com.saa.rubros.RhhEstadoNovedadIess;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de PlanillaControlIessService.</p>
 *
 * <p>Todas las tasas salen de <code>PRNM</code> del anio del periodo: aporte personal,
 * patronal y contribucion CCC. <b>Ningun porcentaje esta escrito
 * en este codigo</b>, que es lo que permite que un cambio de ley sea un UPDATE.</p>
 */
@Stateless
public class PlanillaControlIessServiceImpl implements PlanillaControlIessService {

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private NominaDaoService nominaDaoService;

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	@EJB
	private NovedadIessDaoService novedadIessDaoService;

	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.PlanillaControlIessService#generar(java.lang.Long)
	 */
	@Override
	public PlanillaControlIess generar(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo generar de planillaControlIess, periodo: " + idPeriodo);

		PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodo,
				NombreEntidadesRhh.PERIODO_NOMINA);
		if (periodo == null) {
			throw new IncomeException("No existe el periodo de nomina " + idPeriodo + ".");
		}

		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(
				periodo.getEmpresa().getCodigo(), periodo.getAnio());
		if (prnm == null) {
			throw new IncomeException("No hay parametros de nomina del anio " + periodo.getAnio()
					+ ", y de ahi salen las tasas del IESS.");
		}

		PlanillaControlIess planilla = new PlanillaControlIess();
		planilla.setIdPeriodo(idPeriodo);
		planilla.setAnio(periodo.getAnio());
		planilla.setMes(periodo.getMes());
		planilla.setFechaInicio(periodo.getFechaInicio());
		planilla.setFechaFin(periodo.getFechaFin());

		Double masa = Double.valueOf(0D);
		Double totalPersonal = Double.valueOf(0D);
		Double totalPatronal = Double.valueOf(0D);
		Double totalSeguroParcial = Double.valueOf(0D);
		int afiliados = 0;

		for (Nomina nomina : nominaDaoService.selectByPeriodo(idPeriodo)) {
			if (Long.valueOf(RhhEstadoNomina.EXCLUIDA).equals(nomina.getEstado())
					|| Long.valueOf(RhhEstadoNomina.ANULADA).equals(nomina.getEstado())) {
				continue;
			}
			// Quien no aporta no va en la planilla: los servicios profesionales tienen
			// nomina pero no relacion de dependencia.
			if (nomina.getBaseIess() == null || nomina.getBaseIess().doubleValue() == 0D) {
				continue;
			}

			LineaPlanillaControlIess linea = armaLinea(nomina, periodo, prnm);
			planilla.getLineas().add(linea);
			afiliados++;

			masa = RedondeoNomina.suma(masa, linea.getSueldo());
			totalPersonal = RedondeoNomina.suma(totalPersonal, linea.getAportePersonal());
			totalPatronal = RedondeoNomina.suma(totalPatronal, linea.getAportePatronal());
			totalSeguroParcial = RedondeoNomina.suma(totalSeguroParcial, linea.getSeguroTiempoParcial());
		}

		planilla.setNumeroAfiliados(Integer.valueOf(afiliados));
		planilla.setMasaSalarial(masa);
		planilla.setTotalAportePersonal(totalPersonal);
		planilla.setTotalAportePatronal(totalPatronal);
		planilla.setTotalAportes(RedondeoNomina.suma(totalPersonal, totalPatronal));

		// LA CCC SE CALCULA SOBRE LA MASA, NO POR PERSONA. Es la unica linea del
		// comprobante que no se puede repartir por afiliado, y por eso no aparece en
		// ninguna fila: sumarla renglon a renglon daria un centavo distinto.
		planilla.setContribucionCcc(RedondeoNomina.porcentaje(masa, prnm.getContribucionCcc()));
		planilla.setTotalSeguroTiempoParcial(totalSeguroParcial);
		planilla.setTotalComprobante(RedondeoNomina.suma(planilla.getTotalAportes(),
				planilla.getContribucionCcc(), totalSeguroParcial));

		planilla.getAvisos().add("El seguro de salud de tiempo parcial NO esta incluido: hoy no se"
				+ " guarda en la nomina, asi que para un periodo cerrado no hay de donde sacarlo y"
				+ " calcularlo con el contrato actual daria un numero falso. Si en este periodo hubo"
				+ " alguien a jornada parcial, el comprobante del portal traera ese importe de mas"
				+ " que este. Lo resuelve el punto 11 de la lista de fin de calibracion.");

		agregaAvisoDeNovedadesPendientes(planilla, periodo);

		System.out.println("generar planilla de control termino: " + afiliados + " afiliados, masa "
				+ masa + ", comprobante " + planilla.getTotalComprobante());
		return planilla;
	}

	/**
	 * Arma la fila de un afiliado.
	 *
	 * <p>Los aportes se toman de la nomina y no se recalculan: si el motor los tiene mal,
	 * este control tiene que <b>mostrarlo</b>, no taparlo recalculando por su cuenta. Es la
	 * regla 6 aplicada al control -- un instrumento que recalcula lo que verifica solo
	 * consigue confirmarse a si mismo.</p>
	 *
	 * @param nomina		: Nomina del afiliado
	 * @param periodo		: Periodo
	 * @param prnm			: Parametros del anio
	 * @return				: La fila
	 * @throws Throwable	: Excepcion
	 */
	private LineaPlanillaControlIess armaLinea(Nomina nomina, PeriodoNomina periodo,
			ParametroNomina prnm) throws Throwable {
		LineaPlanillaControlIess linea = new LineaPlanillaControlIess();
		Empleado empleado = nomina.getEmpleado();
		if (empleado != null) {
			linea.setIdentificacion(empleado.getIdentificacion());
			linea.setNombre((empleado.getApellidos() != null ? empleado.getApellidos() : "")
					+ " " + (empleado.getNombres() != null ? empleado.getNombres() : ""));
		}
		linea.setSueldo(nomina.getBaseIess());
		linea.setAportePersonal(nomina.getAportePersonal());
		linea.setAportePatronal(nomina.getAportePatronal());
		linea.setTotalIess(RedondeoNomina.suma(nomina.getAportePersonal(), nomina.getAportePatronal()));

		// LOS DIAS SALEN DE LA NOMINA, NO DEL CONTRATO.
		//
		// CNTE se actualiza en sitio: cuando alguien pasa de jornada parcial a completa, la
		// fila del contrato deja de decir lo que decia el mes pasado. Pedir la planilla de
		// un mes cerrado y leer el contrato de hoy imprimiria los dias de la jornada actual
		// sobre unos aportes de la anterior --a Mendez Torres, 30 dias en marzo cuando
		// declaro 15--. NMNADITR es la foto de ese periodo y no cambia.
		linea.setDias(nomina.getDiasTrabajados() != null
				? Long.valueOf(nomina.getDiasTrabajados().longValue())
				: diasDelMes(prnm));

		// La relacion de trabajo si se lee del contrato: es una clasificacion estable que no
		// tiene copia en la nomina. Si algun dia cambia de forma retroactiva, entra en la
		// misma regla que los dias.
		ContratoEmpleado contrato = contratoDelAfiliado(empleado, periodo);
		if (contrato != null) {
			linea.setRelacionTrabajo(relacionTrabajo(contrato));
		}

		// EL SEGURO DE TIEMPO PARCIAL NO SE INVENTA DESDE EL CONTRATO ACTUAL.
		//
		// Hoy no se persiste en ninguna parte: ni la nomina ni los renglones lo guardan, asi
		// que para un mes ya cerrado no hay de donde sacarlo. Calcularlo con el contrato de
		// hoy daria cero para todo el mundo --porque todos estan en jornada completa ahora--
		// y ese cero pareceria un dato. Se deja nulo y la planilla lo dice en sus avisos.
		// Lo resuelve el punto 11, que es quien lo calculara y lo guardara.
		linea.setSeguroTiempoParcial(null);
		return linea;
	}

	/**
	 * Codigo de relacion de trabajo del IESS del contrato, leido del catalogo.
	 *
	 * <p>Si no se puede resolver devuelve null en vez de fallar: esta planilla es un
	 * instrumento de lectura, y una relacion sin codigo no puede impedir mirar los numeros.
	 * Quien se niega ante un hueco de catalogo es el exportador batch.</p>
	 *
	 * @param contrato	: Contrato
	 * @return			: El codigo, o null
	 */
	private String relacionTrabajo(ContratoEmpleado contrato) {
		if (contrato.getTipoRelacionLaboral() == null) {
			return null;
		}
		try {
			return detalleRubroDaoService.selectValorStringByRubAltDetAlt(
					Rubros.RHH_RELACION_TRABAJO_IESS, contrato.getTipoRelacionLaboral().intValue());
		} catch (Throwable e) {
			System.out.println("Aviso: no se pudo leer la relacion de trabajo del contrato "
					+ contrato.getCodigo() + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Contrato vigente del afiliado dentro del periodo.
	 *
	 * @param empleado		: Empleado
	 * @param periodo		: Periodo
	 * @return				: El contrato, o null
	 * @throws Throwable	: Excepcion
	 */
	private ContratoEmpleado contratoDelAfiliado(Empleado empleado, PeriodoNomina periodo) throws Throwable {
		if (empleado == null) {
			return null;
		}
		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				periodo.getEmpresa().getCodigo(), periodo.getFechaInicio(), periodo.getFechaFin());
		for (ContratoEmpleado contrato : contratos) {
			if (contrato.getEmpleado() != null
					&& empleado.getCodigo().equals(contrato.getEmpleado().getCodigo())) {
				return contrato;
			}
		}
		return null;
	}

	/**
	 * Anade el aviso de las novedades que siguen sin reportar.
	 *
	 * <p><b>Es lo que convierte esta planilla en un control y no en un listado.</b> Si
	 * quedan novedades pendientes, la planilla del portal <b>no</b> va a coincidir con
	 * esta, y la diferencia sera justamente la que esas novedades habrian corregido. Sin
	 * este aviso, quien cuadre las dos vera un descuadre sin causa aparente y lo mas
	 * probable es que ajuste el lado equivocado.</p>
	 *
	 * @param planilla	: Planilla en construccion
	 * @param periodo	: Periodo
	 */
	private void agregaAvisoDeNovedadesPendientes(PlanillaControlIess planilla, PeriodoNomina periodo) {
		try {
			List<NovedadIess> pendientes = novedadIessDaoService.selectByVentana(
					periodo.getEmpresa().getCodigo(), periodo.getFechaInicio(), periodo.getFechaFin(),
					List.of(Long.valueOf(RhhEstadoNovedadIess.PENDIENTE),
							Long.valueOf(RhhEstadoNovedadIess.RECHAZADA)));
			if (pendientes == null || pendientes.isEmpty()) {
				return;
			}
			planilla.getAvisos().add("Hay " + pendientes.size() + " novedad(es) del IESS sin reportar"
					+ " en este periodo. Mientras sigan asi, la planilla del portal puede no coincidir"
					+ " con esta, y la diferencia sera la que esas novedades habrian corregido.");
			for (NovedadIess novedad : pendientes) {
				String nombre = novedad.getEmpleado() != null
						? novedad.getEmpleado().getApellidos() + " " + novedad.getEmpleado().getNombres()
						: "empleado sin identificar";
				planilla.getAvisos().add("Sin reportar: " + nombre + ", tipo " + novedad.getTipoNovedad()
						+ ", fecha del hecho " + novedad.getFechaHecho()
						+ ", limite " + novedad.getFechaLimite());
			}
		} catch (Throwable e) {
			planilla.getAvisos().add("Aviso: no se pudieron leer las novedades pendientes del periodo: "
					+ e.getMessage());
		}
	}

	/**
	 * Dias del mes segun la parametria.
	 *
	 * @param prnm	: Parametros del anio
	 * @return		: Los dias, o null
	 */
	private Long diasDelMes(ParametroNomina prnm) {
		return prnm.getDiasMes() != null ? Long.valueOf(prnm.getDiasMes().longValue()) : null;
	}

}
