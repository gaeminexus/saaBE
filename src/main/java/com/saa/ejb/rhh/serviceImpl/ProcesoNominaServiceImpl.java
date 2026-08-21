package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ConceptoFijoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.CuotaDescuentoDaoService;
import com.saa.ejb.rhh.dao.HoraExtraDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.NovedadNominaDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.ejb.rhh.dao.NovedadIessDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.rhh.dao.ResumenNominaDaoService;
import com.saa.ejb.rhh.service.GeneracionRolPagoService;
import com.saa.ejb.rhh.service.NovedadIessService;
import com.saa.ejb.rhh.service.ProcesoNominaService;
import com.saa.ejb.rhh.service.RetencionRentaService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.AcumuladoNomina;
import com.saa.model.rhh.ConceptoFijoEmpleado;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.CuotaDescuento;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.HoraExtra;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.NovedadIess;
import com.saa.model.rhh.NovedadNomina;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ProvisionNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.model.rhh.RenglonCalculado;
import com.saa.model.rhh.ResultadoCalculoNomina;
import com.saa.model.rhh.ResultadoCalculoPeriodo;
import com.saa.rubros.RhhBaseCalculo;
import com.saa.rubros.RhhEstadoNomina;
import com.saa.rubros.RhhEstadoNovedadIess;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhModoPeriodoNomina;
import com.saa.rubros.RhhModalidadDecimoCuarto;
import com.saa.rubros.RhhModalidadDecimoTercero;
import com.saa.rubros.RhhModalidadFondosReserva;
import com.saa.rubros.RhhOrigenRenglon;
import com.saa.rubros.RhhRolConceptoMotor;
import com.saa.rubros.RhhTipoAcumulado;
import com.saa.rubros.RhhTipoAusencia;
import com.saa.rubros.RhhTipoCalculoConcepto;
import com.saa.rubros.RhhTipoConceptoNomina;
import com.saa.rubros.RhhTipoHoraExtra;
import com.saa.rubros.RhhTipoProvision;
import com.saa.rubros.RhhTipoRelacionLaboral;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion del motor de calculo de nomina.</p>
 *
 * <h3>De donde sale cada numero</h3>
 *
 * <p>El motor no contiene ningun valor normativo. El sueldo sale del contrato; los dias y
 * horas base, el SBU, los topes y los plazos de <code>RHH.PRNM</code>; y el porcentaje de
 * cada concepto de <code>RHH.CPNM.CPNMPRCN</code>.</p>
 *
 * <p><b>Regla de precedencia del porcentaje</b> — el catalogo manda. Varios porcentajes
 * viven duplicados en <code>PRNM</code> y en <code>CPNM</code>: el aporte personal
 * (<code>PRNMAPPR</code> y el concepto de aporte personal), el patronal, el IECE, el SECAP y
 * los fondos de reserva. El motor usa <code>CPNMPRCN</code> y solo cae a <code>PRNM</code>
 * cuando el concepto no lo trae informado. La razon es que <code>PRNM</code> no puede
 * distinguir el IECE del SECAP —ambos son 0,50 % sobre la misma base— mientras que el
 * catalogo si, porque son dos filas distintas con su propia cuenta contable.
 * <code>validarPeriodo</code> avisa cuando las dos fuentes divergen.</p>
 *
 * <h3>Que concepto calcula que</h3>
 *
 * <p>El despacho es por <code>CPNMTPCL</code> (rubro RHH_TIPO_CALCULO_CONCEPTO):</p>
 *
 * <ul>
 *   <li><b>VALOR_FIJO</b> y <b>PORCENTAJE_SOBRE_BASE</b>: los resuelve el bucle generico
 *       de conceptos obligatorios.</li>
 *   <li><b>POR_CANTIDAD</b>: se alimenta de las horas extra aprobadas.</li>
 *   <li><b>TABLA_PROGRESIVA</b> y <b>DESDE_ACUMULADO</b>: los resuelven los pasos
 *       dedicados de impuesto a la renta y beneficios.</li>
 *   <li><b>FORMULA</b>: los decimos mensualizados, que tienen su propio paso.</li>
 *   <li><b>MANUAL</b>: solo entran por novedad del periodo o concepto fijo.</li>
 * </ul>
 */
@Stateless
public class ProcesoNominaServiceImpl implements ProcesoNominaService {

	/** Marca de "si" de las banderas S/N del esquema. */
	private static final String SI = "S";

	/** Marca de "no" de las banderas S/N del esquema. */
	private static final String NO = "N";

	/** Meses del anio, divisor de los decimos mensualizados. */
	private static final double MESES_ANIO = 12D;

	/**
	 * Roles del motor que generan un aporte sobre la base imponible del IESS, en el
	 * orden en que se presentan en el rol de pagos. El IECE y el SECAP comparten tipo,
	 * base y porcentaje: solo el rol los distingue.
	 */
	private static final int[] ROLES_APORTE = {
			RhhRolConceptoMotor.APORTE_PERSONAL,
			RhhRolConceptoMotor.APORTE_PATRONAL,
			RhhRolConceptoMotor.IECE,
			RhhRolConceptoMotor.SECAP };

	/**
	 * Parejas rol del motor y campo equivalente de PRNM, para la comprobacion de
	 * divergencia. Manda el catalogo; PRNM es el respaldo y el contraste.
	 */
	private static final int[] ROLES_CON_PORCENTAJE_EN_PRNM = {
			RhhRolConceptoMotor.APORTE_PERSONAL,
			RhhRolConceptoMotor.APORTE_PATRONAL,
			RhhRolConceptoMotor.IECE,
			RhhRolConceptoMotor.SECAP,
			RhhRolConceptoMotor.FONDOS_DE_RESERVA };

	@PersistenceContext
	private EntityManager em;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@EJB
	private ConceptoNominaDaoService conceptoNominaDaoService;

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	@EJB
	private NominaDaoService nominaDaoService;

	@EJB
	private ReglonNominaDaoService reglonNominaDaoService;

	@EJB
	private ProvisionNominaDaoService provisionNominaDaoService;

	@EJB
	private NovedadNominaDaoService novedadNominaDaoService;

	@EJB
	private HoraExtraDaoService horaExtraDaoService;

	@EJB
	private ConceptoFijoEmpleadoDaoService conceptoFijoEmpleadoDaoService;

	@EJB
	private CuotaDescuentoDaoService cuotaDescuentoDaoService;

	@EJB
	private AcumuladoNominaDaoService acumuladoNominaDaoService;

	@EJB
	private ResumenNominaDaoService resumenNominaDaoService;

	@EJB
	private RetencionRentaService retencionRentaService;

	/** Fase 5: emision del rol de pago al final de aprobarPeriodo. */
	@EJB
	private GeneracionRolPagoService generacionRolPagoService;

	@EJB
	private NovedadIessService novedadIessService;

	@EJB
	private NovedadIessDaoService novedadIessDaoService;

	// =====================================================================
	// Validacion
	// =====================================================================

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#validarPeriodo(java.lang.Long)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> validarPeriodo(Long idPeriodoNomina) throws Throwable {
		System.out.println("Ingresa al metodo validarPeriodo de procesoNomina service, periodo: " + idPeriodoNomina);
		List<String> mensajes = new ArrayList<String>();

		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		Long idEmpresa = periodo.getEmpresa() != null ? periodo.getEmpresa().getCodigo() : null;
		if (idEmpresa == null) {
			mensajes.add("El periodo no tiene empresa asignada (PRDNCDGO.PJRQCDGO).");
			return mensajes;
		}
		if (periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
			mensajes.add("El periodo no tiene fechas de inicio y fin.");
			return mensajes;
		}

		Integer anio = Integer.valueOf(periodo.getFechaFin().getYear());
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		if (prnm == null) {
			mensajes.add("No existen parametros de nomina (RHH.PRNM) para el anio " + anio + ".");
		} else {
			if (prnm.getDiasMes() == null || prnm.getDiasMes().intValue() <= 0) {
				mensajes.add("El parametro de dias base del mes (PRNMDIAS) esta vacio o es cero.");
			}
			if (prnm.getSbu() == null) {
				mensajes.add("El parametro del salario basico unificado (PRNMSBUU) esta vacio.");
			}
		}

		List<ConceptoNomina> conceptos = conceptoNominaDaoService.selectActivosByEmpresa(idEmpresa);
		if (conceptos == null || conceptos.isEmpty()) {
			mensajes.add("No hay conceptos de nomina activos (RHH.CPNM) para la empresa. Ejecute el script 08.");
		}

		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				idEmpresa, periodo.getFechaInicio(), periodo.getFechaFin());
		if (contratos == null || contratos.isEmpty()) {
			mensajes.add("No hay contratos activos que se solapen con el periodo.");
		}

		// Divergencia entre el porcentaje del catalogo y el de la parametria del anio.
		// Aqui son informativas, con el prefijo "Aviso:", para que se pueda simular y
		// recalcular mientras se decide cual de las dos fuentes corregir. El bloqueo
		// real esta en aprobarPeriodo: calcular con una tasa vieja es recuperable,
		// aprobarla y contabilizarla no.
		if (prnm != null && conceptos != null) {
			for (String divergencia : comparaPorcentajes(conceptos, prnm)) {
				mensajes.add("Aviso: " + divergencia);
			}
		}

		System.out.println("validarPeriodo termino con " + mensajes.size() + " mensaje(s).");
		return mensajes;
	}

	/**
	 * Avisa cuando el porcentaje de un concepto difiere del equivalente de PRNM. No es un
	 * error: el catalogo manda. Es una alerta de que una de las dos fuentes quedo sin
	 * actualizar tras un cambio de normativa.
	 *
	 * @param conceptos	: Conceptos activos de la empresa
	 * @param prnm		: Parametros del anio
	 * @return			: Mensajes de divergencia; vacio si no hay
	 */
	private List<String> comparaPorcentajes(List<ConceptoNomina> conceptos, ParametroNomina prnm) {
		List<String> mensajes = new ArrayList<String>();
		for (int rol : ROLES_CON_PORCENTAJE_EN_PRNM) {
			ConceptoNomina concepto = conceptoPorRol(conceptos, rol);
			if (concepto == null || concepto.getPorcentaje() == null) {
				continue;
			}
			Double referencia = porcentajeEnParametria(Long.valueOf(rol), prnm);
			if (referencia != null && !RedondeoNomina.sonIguales(concepto.getPorcentaje(), referencia)) {
				mensajes.add("El concepto '" + concepto.getNombre() + "' tiene "
						+ concepto.getPorcentaje() + " % y la parametria del anio " + prnm.getAnio()
						+ " dice " + referencia + " %. El calculo usa el del catalogo."
						+ " Revise cual de las dos fuentes quedo sin actualizar.");
			}
		}
		return mensajes;
	}

	/**
	 * Devuelve las divergencias de porcentaje entre el catalogo y la parametria del
	 * anio de un periodo. Se usa dos veces con distinto rigor: como aviso informativo
	 * en validarPeriodo, y como bloqueo en aprobarPeriodo.
	 *
	 * @param periodo		: Periodo de nomina
	 * @return				: Divergencias encontradas; vacio si las dos fuentes coinciden
	 * @throws Throwable	: Excepcion
	 */
	private List<String> divergenciasDePorcentaje(PeriodoNomina periodo) throws Throwable {
		if (periodo.getEmpresa() == null || periodo.getFechaFin() == null) {
			return new ArrayList<String>();
		}
		Long idEmpresa = periodo.getEmpresa().getCodigo();
		Integer anio = Integer.valueOf(periodo.getFechaFin().getYear());
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		List<ConceptoNomina> conceptos = conceptoNominaDaoService.selectActivosByEmpresa(idEmpresa);
		if (prnm == null || conceptos == null) {
			return new ArrayList<String>();
		}
		return comparaPorcentajes(conceptos, prnm);
	}

	// =====================================================================
	// Calculo del periodo
	// =====================================================================

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#calcularPeriodo(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ResultadoCalculoPeriodo calcularPeriodo(Long idPeriodoNomina, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo calcularPeriodo de procesoNomina service, periodo: " + idPeriodoNomina);

		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		exigeEstadoCalculable(periodo);

		List<String> problemas = validarPeriodo(idPeriodoNomina);
		List<String> errores = new ArrayList<String>();
		for (String problema : problemas) {
			// Los avisos de divergencia no bloquean el calculo; el resto si.
			if (problema.startsWith("Aviso:")) {
				errores.add(problema);
			} else {
				throw new IncomeException("No se puede calcular el periodo: " + problema);
			}
		}

		Long idEmpresa = periodo.getEmpresa().getCodigo();
		Integer anio = Integer.valueOf(periodo.getFechaFin().getYear());
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		List<ConceptoNomina> conceptos = conceptoNominaDaoService.selectActivosByEmpresa(idEmpresa);

		periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.EN_CALCULO));
		periodoNominaDaoService.save(periodo, periodo.getCodigo());

		// Idempotencia: se borran las provisiones del periodo completo antes de regenerar.
		provisionNominaDaoService.eliminaByPeriodo(idPeriodoNomina);

		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				idEmpresa, periodo.getFechaInicio(), periodo.getFechaFin());

		Double totalIngresos = Double.valueOf(0D);
		Double totalDescuentos = Double.valueOf(0D);
		Double totalNeto = Double.valueOf(0D);
		Double totalPatronal = Double.valueOf(0D);
		int procesados = 0;
		int conError = 0;

		for (ContratoEmpleado contrato : contratos) {
			try {
				Nomina nomina = calculaContrato(periodo, contrato, prnm, conceptos, usuario, true, true, null);
				totalIngresos = RedondeoNomina.suma(totalIngresos, nomina.getTotalIngresos());
				totalDescuentos = RedondeoNomina.suma(totalDescuentos, nomina.getTotalDescuentos());
				totalNeto = RedondeoNomina.suma(totalNeto, nomina.getNetoPagar());
				totalPatronal = RedondeoNomina.suma(totalPatronal, nomina.getTotalPatronal());
				generaVariacionIess(periodo, contrato, nomina, prnm, usuario, errores);
				procesados++;
			} catch (Throwable e) {
				conError++;
				String nombre = contrato.getEmpleado() != null
						? contrato.getEmpleado().getApellidos() + " " + contrato.getEmpleado().getNombres()
						: "contrato " + contrato.getCodigo();
				errores.add(nombre + ": " + e.getMessage());
				System.out.println("Error calculando " + nombre + ": " + e.getMessage());
			}
		}

		periodo.setTotalIngresos(totalIngresos);
		periodo.setTotalDescuentos(totalDescuentos);
		periodo.setTotalNeto(totalNeto);
		periodo.setTotalPatronal(totalPatronal);
		periodo.setNumeroEmpleados(Integer.valueOf(procesados));
		periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO));
		periodoNominaDaoService.save(periodo, periodo.getCodigo());

		ResultadoCalculoPeriodo resultado = new ResultadoCalculoPeriodo();
		resultado.setIdPeriodo(idPeriodoNomina);
		resultado.setEmpleadosProcesados(Integer.valueOf(procesados));
		resultado.setEmpleadosConError(Integer.valueOf(conError));
		resultado.setTotalIngresos(totalIngresos);
		resultado.setTotalDescuentos(totalDescuentos);
		resultado.setTotalNeto(totalNeto);
		resultado.setTotalPatronal(totalPatronal);
		resultado.setErrores(errores);

		System.out.println("calcularPeriodo termino: " + procesados + " procesados, " + conError + " con error.");
		return resultado;
	}

	/**
	 * Crea la novedad de variacion de sueldo por extras cuando el imponible del mes
	 * supera al sueldo declarado al IESS.
	 *
	 * <p><b>La variacion no se deduce del catalogo de conceptos, se deduce de la
	 * diferencia</b>, y por eso no hace falta marcar cada concepto como permanente o no.
	 * Para el IESS es variacion todo imponible por encima del sueldo declarado, venga de
	 * horas extras, de una subrogacion, de un encargo o de un bono ocasional. El sueldo
	 * declarado es <code>CNTESLRB x NVL(CNTEDIAD,30) / 30</code>: el referencial de
	 * treinta dias prorrateado a los dias que se declaran, que en jornada completa son
	 * los treinta y el prorrateo no hace nada.</p>
	 *
	 * <p><b>Esto no toca el calculo.</b> Se ejecuta despues de que la nomina esta hecha,
	 * lee sus totales y no los modifica. Si falla, el fallo va a la lista de avisos del
	 * resultado y el periodo se sigue calculando: una novedad que no se pudo crear no
	 * puede tumbar la nomina de nadie. Quien no deja pasar el hueco es
	 * <code>cerrarPeriodo</code>, que se niega a cerrar con novedades pendientes.</p>
	 *
	 * <p>Prueba negativa disponible: en los siete meses de 2026 de ASOPREP el imponible
	 * de todos coincide con su sueldo, asi que <b>este metodo no debe generar ni una
	 * sola novedad</b> al recalcular enero a julio. Si aparece alguna, o el imponible o
	 * el sueldo declarado del contrato estan mal.</p>
	 *
	 * @param periodo	: Periodo que se esta calculando
	 * @param contrato	: Contrato del empleado
	 * @param nomina	: Nomina ya calculada
	 * @param usuario	: Usuario que ejecuta
	 * @param errores	: Lista de avisos del resultado, donde se deja constancia si falla
	 */
	private void generaVariacionIess(PeriodoNomina periodo, ContratoEmpleado contrato,
			Nomina nomina, ParametroNomina prnm, String usuario, List<String> errores) {
		try {
			if (nomina == null || contrato == null || nomina.getBaseIess() == null
					|| contrato.getSalarioBase() == null || prnm == null
					|| prnm.getDiasMes() == null || prnm.getDiasMes().doubleValue() == 0D) {
				return;
			}
			// Los dias del mes salen de PRNMDMES, no de un 30 escrito aqui: es la misma
			// regla que gobierna el resto de la parametria.
			double diasMes = prnm.getDiasMes().doubleValue();
			double dias = contrato.getDiasDeclaradosIess() != null
					? contrato.getDiasDeclaradosIess().doubleValue()
					: diasMes;
			Double sueldoDeclarado = RedondeoNomina.redondea(Double.valueOf(
					contrato.getSalarioBase().doubleValue() * dias / diasMes));
			Double variacion = RedondeoNomina.redondea(Double.valueOf(
					nomina.getBaseIess().doubleValue() - sueldoDeclarado.doubleValue()));
			if (variacion.doubleValue() <= 0D) {
				return;
			}
			novedadIessService.generarVariacionPorExtras(contrato.getCodigo(), periodo.getFechaFin(),
					variacion, periodo.getFechaInicio(), periodo.getFechaFin(), usuario);
		} catch (Throwable e) {
			String nombre = contrato != null && contrato.getEmpleado() != null
					? contrato.getEmpleado().getApellidos() + " " + contrato.getEmpleado().getNombres()
					: "contrato " + (contrato != null ? contrato.getCodigo() : null);
			errores.add("Aviso: no se pudo generar la novedad de variacion al IESS de " + nombre
					+ ": " + e.getMessage());
			System.out.println("Aviso: fallo la novedad de variacion al IESS de " + nombre
					+ ": " + e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#recalcularEmpleado(java.lang.Long, java.lang.Long, boolean, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public ResultadoCalculoNomina recalcularEmpleado(Long idPeriodoNomina, Long idEmpleado,
			boolean preservarManuales, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo recalcularEmpleado, periodo: " + idPeriodoNomina
				+ ", empleado: " + idEmpleado);

		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		exigeEstadoCalculable(periodo);

		Long idEmpresa = periodo.getEmpresa().getCodigo();
		Integer anio = Integer.valueOf(periodo.getFechaFin().getYear());
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		List<ConceptoNomina> conceptos = conceptoNominaDaoService.selectActivosByEmpresa(idEmpresa);

		ContratoEmpleado contrato = localizaContrato(periodo, idEmpresa, idEmpleado);
		provisionNominaDaoService.eliminaByPeriodoYEmpleado(idPeriodoNomina, idEmpleado);

		List<ReglonNomina> renglones = new ArrayList<ReglonNomina>();
		Nomina nomina = calculaContrato(periodo, contrato, prnm, conceptos, usuario, true, preservarManuales, renglones);
		return armaResultado(nomina, renglones);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#simular(java.lang.Long, java.lang.Long)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ResultadoCalculoNomina simular(Long idContrato, Long idPeriodoNomina) throws Throwable {
		System.out.println("Ingresa al metodo simular, contrato: " + idContrato
				+ ", periodo: " + idPeriodoNomina);

		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		ContratoEmpleado contrato = em.find(ContratoEmpleado.class, idContrato);
		if (contrato == null) {
			throw new IncomeException("No existe el contrato " + idContrato + ".");
		}
		Long idEmpresa = periodo.getEmpresa() != null ? periodo.getEmpresa().getCodigo() : null;
		Integer anio = Integer.valueOf(periodo.getFechaFin().getYear());
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		List<ConceptoNomina> conceptos = conceptoNominaDaoService.selectActivosByEmpresa(idEmpresa);

		// persistir = false: no se toca la base, es solo previsualizacion.
		List<ReglonNomina> renglones = new ArrayList<ReglonNomina>();
		Nomina nomina = calculaContrato(periodo, contrato, prnm, conceptos, "SIMULACION", false, false, renglones);
		return armaResultado(nomina, renglones);
	}

	// =====================================================================
	// Maquina de estados del periodo
	// =====================================================================

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#aprobarPeriodo(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void aprobarPeriodo(Long idPeriodoNomina, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo aprobarPeriodo, periodo: " + idPeriodoNomina);
		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		if (!Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO).equals(periodo.getEstado())) {
			throw new IncomeException("El periodo debe estar CALCULADO para aprobarse. Estado actual: "
					+ periodo.getEstado());
		}

		// Aqui la divergencia de porcentajes SI bloquea. En validarPeriodo es solo un
		// aviso, porque calcular con una tasa desactualizada se corrige recalculando;
		// aprobar y contabilizar con ella, no. Este es el ultimo punto reversible.
		List<String> divergencias = divergenciasDePorcentaje(periodo);
		if (!divergencias.isEmpty()) {
			StringBuilder mensaje = new StringBuilder();
			mensaje.append("No se puede aprobar el periodo: el porcentaje de ")
					.append(divergencias.size())
					.append(" concepto(s) no coincide con la parametria del anio.")
					.append(" Corrija RHH.CPNM o RHH.PRNM y recalcule antes de aprobar:\n");
			for (String divergencia : divergencias) {
				mensaje.append("  ").append(divergencia).append("\n");
			}
			throw new IncomeException(mensaje.toString());
		}

		periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.APROBADO));
		periodo.setFechaAprobacion(LocalDate.now());
		periodo.setUsuarioAprueba(usuario);
		periodoNominaDaoService.save(periodo, periodo.getCodigo());

		// Fase 5: el rol de pago se emite AQUI, no antes. Es el documento que el empleado
		// firma, y no debe existir mientras el calculo todavia se puede recalcular. La
		// generacion es idempotente, asi que reabrir, recalcular y volver a aprobar
		// actualiza el rol en vez de duplicarlo.
		int roles = generacionRolPagoService.generarRoles(idPeriodoNomina, usuario);
		System.out.println("aprobarPeriodo emitio " + roles + " rol(es) de pago.");
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#reabrirPeriodo(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void reabrirPeriodo(Long idPeriodoNomina, String motivo, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo reabrirPeriodo, periodo: " + idPeriodoNomina);
		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("La reapertura de un periodo exige un motivo.");
		}
		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		if (Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(periodo.getEstado())) {
			throw new IncomeException("Un periodo PAGADO no se puede reabrir.");
		}
		// El cierre escribio acumulados: hay que retirarlos o el recalculo los duplica.
		int borrados = acumuladoNominaDaoService.eliminaByPeriodo(idPeriodoNomina);
		System.out.println("reabrirPeriodo retiro " + borrados + " acumulado(s) del periodo.");

		periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO));
		periodo.setFechaCierre(null);
		periodo.setUsuarioCierra(null);
		periodo.setObservaciones("Reabierto por " + usuario + ": " + motivo);
		periodoNominaDaoService.save(periodo, periodo.getCodigo());
	}

	/**
	 * Se niega a cerrar el periodo si quedan novedades del IESS sin reportar.
	 *
	 * <p><b>Es la regla que habria evitado marzo.</b> La planilla del 2026-03 declaro a
	 * Castro Arce y a Cevallos Aleman con sueldo entero y treinta dias porque nadie
	 * registro el aviso de salida dentro del plazo; el IESS siguio usando el sueldo que
	 * tenia y ASOPREP pago 208,22 de mas. La novedad existia en el sistema, en estado
	 * PENDIENTE, y nada impidio cerrar el mes por encima de ella.</p>
	 *
	 * <p><b>PENDIENTE y RECHAZADA cuentan las dos.</b> Una novedad que el IESS devolvio
	 * rechazada no esta reportada: esta peor que pendiente, porque alguien ya creyo
	 * haberla mandado. Dejarla fuera del filtro seria dar por bueno justo el caso que mas
	 * vigilancia necesita.</p>
	 *
	 * <p>La ventana es <code>NVISFCHC entre la fecha de inicio y la de fin del periodo</code>,
	 * la misma que usan la pantalla de novedades del mes y el exportador batch. Si aqui se
	 * definiera de otro modo, esta regla dejaria pasar lo que aquella pantalla muestra.</p>
	 *
	 * <h3>En modo historico NO bloquea: avisa</h3>
	 *
	 * <p><b>La regla existe para impedir cerrar un mes mientras al IESS todavia se le puede
	 * informar.</b> En un periodo historico eso ya no se cumple: enero de 2026 mirado desde
	 * agosto tiene el plazo vencido hace siete meses, y bloquear no informa a nadie — solo
	 * impide registrar lo que paso.</p>
	 *
	 * <p><b>En un periodo historico una novedad sin reportar es un hecho registrado, no una
	 * tarea pendiente</b>, y el regimen historico guarda lo que ocurrio. Las cuatro novedades
	 * vencidas de 2026 se quedan PENDIENTE y sin fecha de reporte para siempre: son la
	 * evidencia de los 208,22 que ASOPREP pago de mas y de los dos avisos de enero que
	 * tampoco se presentaron. Marcarlas enviadas falsearia una fecha ante el IESS; anularlas
	 * diria que no correspondian, y correspondian.</p>
	 *
	 * <p>Se deja constancia por dos vias: el log enumera cada una, y las observaciones del
	 * periodo quedan con el numero. Un cierre que ignora algo en silencio es lo que se estaba
	 * evitando; uno que lo deja escrito, no.</p>
	 *
	 * @param periodo		: Periodo que se intenta cerrar
	 * @return				: Aviso para el cierre si es historico y quedan novedades, o null
	 * @throws Throwable	: IncomeException con cuantas son y de quien, solo en modo productivo
	 */
	private String exigeNovedadesIessReportadas(PeriodoNomina periodo) throws Throwable {
		List<Long> sinReportar = List.of(Long.valueOf(RhhEstadoNovedadIess.PENDIENTE),
				Long.valueOf(RhhEstadoNovedadIess.RECHAZADA));
		List<NovedadIess> novedades = novedadIessDaoService.selectByVentana(
				periodo.getEmpresa().getCodigo(), periodo.getFechaInicio(), periodo.getFechaFin(),
				sinReportar);
		if (novedades == null || novedades.isEmpty()) {
			return null;
		}
		// El mensaje dice cuantas y de quien: un "hay novedades pendientes" obliga a ir a
		// buscarlas y es la clase de aviso que se termina ignorando.
		StringBuilder detalle = new StringBuilder();
		for (NovedadIess novedad : novedades) {
			if (detalle.length() > 0) {
				detalle.append("; ");
			}
			String nombre = novedad.getEmpleado() != null
					? novedad.getEmpleado().getApellidos() + " " + novedad.getEmpleado().getNombres()
					: "empleado sin identificar";
			detalle.append(nombre).append(" (tipo ").append(novedad.getTipoNovedad());
			if (novedad.getFechaLimite() != null) {
				detalle.append(", limite ").append(novedad.getFechaLimite());
			}
			detalle.append(", estado ").append(novedad.getEstado()).append(")");
		}
		// MODO HISTORICO: NO BLOQUEA, AVISA. El plazo legal ya vencio; impedir el cierre no
		// informa al IESS, solo impide registrar lo que paso.
		if (esHistorico(periodo)) {
			System.out.println("Aviso: el periodo " + periodo.getCodigo() + " se cierra en modo"
					+ " HISTORICO con " + novedades.size() + " novedad(es) del IESS sin reportar."
					+ " El plazo legal ya vencio: quedan como evidencia de lo que no se declaro."
					+ " Son: " + detalle);
			return "Cerrado con " + novedades.size() + " novedad(es) del IESS sin declarar"
					+ " (periodo historico, plazo vencido).";
		}

		throw new IncomeException("No se puede cerrar el periodo: quedan " + novedades.size()
				+ " novedades del IESS sin reportar (PENDIENTE o RECHAZADA) con fecha de hecho"
				+ " dentro del periodo. Reportelas en el portal del IESS y marquelas como"
				+ " enviadas o aceptadas, o anulelas si no corresponden. Son: " + detalle);
	}

	/**
	 * El periodo no genera contabilidad: es historico.
	 *
	 * <p>Misma definicion que usa <code>ContabilizacionNominaServiceImpl</code>, y con el
	 * mismo criterio para el nulo: los periodos creados antes de que existiera la columna se
	 * tratan como historicos.</p>
	 *
	 * @param periodo	: Periodo de nomina
	 * @return			: true si es historico
	 */
	private boolean esHistorico(PeriodoNomina periodo) {
		return periodo.getModo() == null
				|| Long.valueOf(RhhModoPeriodoNomina.HISTORICO_SIN_CONTABILIZAR).equals(periodo.getModo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#cerrarPeriodo(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cerrarPeriodo(Long idPeriodoNomina, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo cerrarPeriodo, periodo: " + idPeriodoNomina);
		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);

		Long estado = periodo.getEstado();
		boolean contabilizadoOPagado = Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO).equals(estado)
				|| Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(estado);
		if (!contabilizadoOPagado) {
			throw new IncomeException("El periodo debe estar CONTABILIZADO o PAGADO para cerrarse."
					+ " Estado actual: " + estado);
		}

		String avisoNovedades = exigeNovedadesIessReportadas(periodo);

		// Este es el UNICO punto donde se escriben los acumulados. Se retiran primero los
		// que hubiera dejado un cierre anterior, para que cerrar dos veces no duplique.
		acumuladoNominaDaoService.eliminaByPeriodo(idPeriodoNomina);

		List<Nomina> nominas = nominaDaoService.selectByPeriodo(idPeriodoNomina);
		Integer anio = periodo.getAnio();
		Integer mes = periodo.getMes();
		for (Nomina nomina : nominas) {
			if (Long.valueOf(RhhEstadoNomina.EXCLUIDA).equals(nomina.getEstado())
					|| Long.valueOf(RhhEstadoNomina.ANULADA).equals(nomina.getEstado())) {
				continue;
			}
			Long idEmpleado = nomina.getEmpleado().getCodigo();
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.IMPONIBLE_IESS,
					nomina.getBaseIess(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.GRAVADO_IR,
					nomina.getBaseImpuestoRenta(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.BASE_DECIMO_TERCERO,
					nomina.getBaseDecimoTercero(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.BASE_DECIMO_CUARTO,
					nomina.getBaseDecimoCuarto(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.BASE_FONDOS_DE_RESERVA,
					nomina.getBaseFondosReserva(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.APORTE_PERSONAL,
					nomina.getAportePersonal(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.RETENCION_IR,
					nomina.getRetencionImpuestoRenta(), null, usuario);
			escribeAcumulado(periodo, idEmpleado, anio, mes, RhhTipoAcumulado.DIAS_TRABAJADOS,
					Double.valueOf(0D), nomina.getDiasTrabajados(), usuario);
		}

		periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CERRADO));
		periodo.setFechaCierre(LocalDate.now());
		periodo.setUsuarioCierra(usuario);
		if (avisoNovedades != null) {
			// Que quede en el periodo, no solo en el log: un cierre que ignora algo en
			// silencio es lo que la regla venia a evitar.
			periodo.setObservaciones(avisoNovedades);
		}
		periodoNominaDaoService.save(periodo, periodo.getCodigo());
		System.out.println("cerrarPeriodo termino: " + nominas.size() + " nomina(s) acumuladas.");
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProcesoNominaService#excluirEmpleado(java.lang.Long, java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void excluirEmpleado(Long idPeriodoNomina, Long idEmpleado, String motivo,
			String usuario) throws Throwable {
		System.out.println("Ingresa al metodo excluirEmpleado, periodo: " + idPeriodoNomina
				+ ", empleado: " + idEmpleado);
		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		exigeEstadoCalculable(periodo);

		Nomina nomina = nominaDaoService.selectByPeriodoYEmpleado(idPeriodoNomina, idEmpleado);
		if (nomina == null) {
			throw new IncomeException("El empleado " + idEmpleado + " no tiene nomina en el periodo "
					+ idPeriodoNomina + ".");
		}
		reglonNominaDaoService.eliminaGeneradosByNomina(nomina.getCodigo());
		provisionNominaDaoService.eliminaByPeriodoYEmpleado(idPeriodoNomina, idEmpleado);

		nomina.setEstado(Long.valueOf(RhhEstadoNomina.EXCLUIDA));
		nomina.setTotalIngresos(Double.valueOf(0D));
		nomina.setTotalDescuentos(Double.valueOf(0D));
		nomina.setNetoPagar(Double.valueOf(0D));
		nomina.setTotalPatronal(Double.valueOf(0D));
		nomina.setObservacion("Excluido por " + usuario + ": " + motivo);
		nominaDaoService.save(nomina, nomina.getCodigo());
	}

	// =====================================================================
	// Nucleo: calculo de un contrato
	// =====================================================================

	/**
	 * Calcula la nomina de un contrato en un periodo. Es el corazon del motor.
	 *
	 * @param periodo			: Periodo de nomina
	 * @param contrato			: Contrato a calcular
	 * @param prnm				: Parametros normativos del anio
	 * @param conceptos			: Catalogo de conceptos activos de la empresa
	 * @param usuario			: Usuario que ejecuta
	 * @param persistir			: false para simular sin tocar la base
	 * @param preservarManuales	: Conservar los renglones editados a mano
	 * @return					: La nomina calculada, con sus renglones en memoria
	 * @throws Throwable		: Excepcion
	 */
	private Nomina calculaContrato(PeriodoNomina periodo, ContratoEmpleado contrato, ParametroNomina prnm,
			List<ConceptoNomina> conceptos, String usuario, boolean persistir,
			boolean preservarManuales, List<ReglonNomina> salida) throws Throwable {

		Empleado empleado = contrato.getEmpleado();
		Long idEmpleado = empleado.getCodigo();
		LocalDate desde = periodo.getFechaInicio();
		LocalDate hasta = periodo.getFechaFin();

		// --- Paso 3: dias trabajados -------------------------------------------------
		Double diasBase = Double.valueOf(prnm.getDiasMes().doubleValue());
		Double diasTrabajados = calculaDiasTrabajados(contrato, periodo, prnm);

		// --- Paso 4: sueldo del periodo ----------------------------------------------
		Double sueldoPeriodo = calculaSueldoPeriodo(contrato, prnm, diasTrabajados, diasBase);

		// --- Nomina cabecera ---------------------------------------------------------
		Nomina nomina = persistir
				? nominaDaoService.selectByPeriodoYEmpleado(periodo.getCodigo(), idEmpleado) : null;
		if (nomina == null) {
			nomina = new Nomina();
			nomina.setPeriodoNomina(periodo);
			nomina.setEmpleado(empleado);
			nomina.setContrato(contrato);
			nomina.setFechaRegistro(LocalDate.now());
			nomina.setUsuarioRegistro(usuario);
		}
		nomina.setSalarioBase(RedondeoNomina.redondea(contrato.getSalarioBase()));
		nomina.setDiasTrabajados(RedondeoNomina.redondeaCantidad(diasTrabajados));
		nomina.setEstado(Long.valueOf(RhhEstadoNomina.CALCULADA));

		// La cabecera se persiste ANTES de calcular los renglones, porque los renglones
		// necesitan el codigo generado de la nomina. En ese momento los totales todavia
		// no existen, y RHH.NMNA declara NMNATING, NMNATDSC y NMNANETO como NOT NULL, asi
		// que hay que sembrarlos en cero: al final del metodo se sobreescriben con los
		// valores reales. Sin esto el INSERT falla con ORA-02290.
		ceroSiNulo(nomina);

		if (persistir) {
			nomina = nominaDaoService.save(nomina, nomina.getCodigo());
			em.flush();
			// Idempotencia: se borran los renglones generados, nunca los manuales.
			reglonNominaDaoService.eliminaGeneradosByNomina(nomina.getCodigo());
		}

		// Si el llamador paso una lista, se llena ahi para que pueda armar el DTO sin releer.
		List<ReglonNomina> renglones = salida != null ? salida : new ArrayList<ReglonNomina>();

		// --- Paso 5: renglones de ingreso --------------------------------------------
		// 5.a Conceptos obligatorios del catalogo, evaluados genericamente.
		for (ConceptoNomina concepto : conceptos) {
			if (!SI.equals(concepto.getObligatorio())) {
				continue;
			}
			if (!aplicaARelacionLaboral(concepto, contrato)) {
				continue;
			}
			if (!esEvaluableEnBucleGenerico(concepto)) {
				continue;
			}
			Double valor = evaluaConcepto(concepto, contrato, prnm, sueldoPeriodo, diasTrabajados,
					diasBase, renglones);
			if (valor == null) {
				continue;
			}
			renglones.add(nuevoRenglon(nomina, concepto, null, valor,
					baseDelConcepto(concepto, prnm, sueldoPeriodo, renglones), concepto.getPorcentaje(),
					RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
		}

		// 5.b Conceptos fijos del empleado vigentes en el rango.
		List<ConceptoFijoEmpleado> fijos = conceptoFijoEmpleadoDaoService.selectVigentes(
				idEmpleado, desde, hasta);
		for (ConceptoFijoEmpleado fijo : fijos) {
			Double valor = fijo.getValor();
			if (valor == null && fijo.getPorcentaje() != null) {
				valor = RedondeoNomina.porcentaje(sueldoPeriodo, fijo.getPorcentaje());
			}
			if (valor == null) {
				continue;
			}
			renglones.add(nuevoRenglon(nomina, fijo.getConcepto(), fijo.getCantidad(),
					RedondeoNomina.redondea(valor), sueldoPeriodo, fijo.getPorcentaje(),
					RhhOrigenRenglon.CONCEPTO_FIJO_DEL_EMPLEADO, "RHH.CPXM", fijo.getCodigo()));
		}

		// 5.c Novedades aprobadas del periodo.
		List<NovedadNomina> novedades = novedadNominaDaoService.selectAprobadas(
				periodo.getCodigo(), idEmpleado);
		for (NovedadNomina novedad : novedades) {
			renglones.add(nuevoRenglon(nomina, novedad.getConceptoNomina(), novedad.getCantidad(),
					RedondeoNomina.redondea(novedad.getValor()), null, null,
					RhhOrigenRenglon.NOVEDAD_DEL_PERIODO, "RHH.NVNM", novedad.getCodigo()));
		}

		// 5.d Horas extra aprobadas y aun no pagadas.
		Double valorHora = calculaValorHora(contrato, prnm);
		List<HoraExtra> horasExtra = horaExtraDaoService.selectAprobadasPendientes(idEmpleado, desde, hasta);
		for (HoraExtra hora : horasExtra) {
			// El recargo sale de la propia hora extra, que lo copio del rubro al registrarse.
			Double recargo = hora.getRecargo();
			Double valorBase = hora.getValorHora() != null ? hora.getValorHora() : valorHora;
			Double valor = RedondeoNomina.redondea(Double.valueOf(
					hora.getHoras().doubleValue() * valorBase.doubleValue()
							* (1D + (recargo != null ? recargo.doubleValue() : 0D) / 100D)));
			ConceptoNomina conceptoHora = conceptoDeHoraExtra(conceptos, hora);
			renglones.add(nuevoRenglon(nomina, conceptoHora, hora.getHoras(), valor, valorBase, recargo,
					RhhOrigenRenglon.HORA_EXTRA, "RHH.HREX", hora.getCodigo()));
		}

		// --- Paso 6: bases, en una sola pasada sobre los renglones ya generados -------
		Double baseIess = sumaPorBandera(renglones, "IMIE");
		Double baseIr = sumaPorBandera(renglones, "IMIR");
		Double baseFr = sumaPorBandera(renglones, "APFR");
		Double baseDec3 = sumaPorBandera(renglones, "BSDT");
		Double baseDec4 = sumaPorBandera(renglones, "BSDC");
		Double baseVac = sumaPorBandera(renglones, "BSVC");

		// --- Paso 7: aportes ----------------------------------------------------------
		// Los cuatro aportes se localizan por su rol, no por la terna: el IECE y el SECAP
		// comparten tipo, tipo de calculo, base y porcentaje, y solo el rol los separa.
		if (SI.equals(contrato.getAportaIess())) {
			for (int rolAporte : ROLES_APORTE) {
				ConceptoNomina concepto = conceptoPorRol(conceptos, rolAporte);
				if (concepto == null || yaTieneRenglon(renglones, concepto)) {
					continue;
				}
				Double porcentaje = porcentajeVigente(concepto, prnm);
				if (porcentaje == null) {
					continue;
				}
				Double valor = RedondeoNomina.porcentaje(baseIess, porcentaje);
				renglones.add(nuevoRenglon(nomina, concepto, null, valor, baseIess, porcentaje,
						RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
			}
		}

		// --- Paso 8: fondos de reserva ------------------------------------------------
		Double fondosReserva = Double.valueOf(0D);
		if (Long.valueOf(RhhModalidadFondosReserva.MENSUALIZADO).equals(contrato.getModalidadFondosReserva())
				&& superaUnAnio(empleado, contrato, hasta)) {
			ConceptoNomina conceptoFr = conceptoPorRol(conceptos, RhhRolConceptoMotor.FONDOS_DE_RESERVA);
			if (conceptoFr != null && !yaTieneRenglon(renglones, conceptoFr)) {
				fondosReserva = RedondeoNomina.porcentaje(baseFr, porcentajeVigente(conceptoFr, prnm));
				renglones.add(nuevoRenglon(nomina, conceptoFr, null, fondosReserva, baseFr,
						porcentajeVigente(conceptoFr, prnm),
						RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
			}
		} else if (Long.valueOf(RhhModalidadFondosReserva.ACUMULADO_EN_EL_IESS)
				.equals(contrato.getModalidadFondosReserva())) {
			ConceptoNomina conceptoFr = conceptoPorRol(conceptos, RhhRolConceptoMotor.FONDOS_DE_RESERVA);
			Double provision = RedondeoNomina.porcentaje(baseFr,
					conceptoFr != null ? porcentajeVigente(conceptoFr, prnm) : prnm.getFondosReserva());
			generaProvision(periodo, empleado,
					conceptoPorRol(conceptos, RhhRolConceptoMotor.PROVISION_FONDOS_DE_RESERVA),
					RhhTipoProvision.FONDOS_DE_RESERVA,
					baseFr, provision, usuario, persistir);
		}

		// --- Paso 9: decimo tercero ---------------------------------------------------
		Double decimoTercero = RedondeoNomina.divide(baseDec3, Double.valueOf(MESES_ANIO));
		if (Long.valueOf(RhhModalidadDecimoTercero.MENSUALIZADO).equals(contrato.getModalidadDecimoTercero())) {
			ConceptoNomina conceptoD3 = conceptoPorRol(conceptos, RhhRolConceptoMotor.DECIMO_TERCERO);
			if (conceptoD3 != null) {
				renglones.add(nuevoRenglon(nomina, conceptoD3, null, decimoTercero, baseDec3, null,
						RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
			}
		} else {
			generaProvision(periodo, empleado,
					conceptoPorRol(conceptos, RhhRolConceptoMotor.PROVISION_DECIMO_TERCERO),
					RhhTipoProvision.DECIMO_TERCERO, baseDec3, decimoTercero, usuario, persistir);
		}

		// --- Paso 10: decimo cuarto ---------------------------------------------------
		if (SI.equals(contrato.getDerechoDecimoCuarto())) {
			Double decimoCuarto = RedondeoNomina.redondea(Double.valueOf(
					(prnm.getSbu().doubleValue() / MESES_ANIO)
							* (diasTrabajados.doubleValue() / diasBase.doubleValue())));
			if (Long.valueOf(RhhModalidadDecimoCuarto.MENSUALIZADO)
					.equals(contrato.getModalidadDecimoCuarto())) {
				ConceptoNomina conceptoD4 = conceptoPorRol(conceptos, RhhRolConceptoMotor.DECIMO_CUARTO);
				if (conceptoD4 != null) {
					renglones.add(nuevoRenglon(nomina, conceptoD4, null, decimoCuarto, prnm.getSbu(), null,
							RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
				}
			} else {
				generaProvision(periodo, empleado,
						conceptoPorRol(conceptos, RhhRolConceptoMotor.PROVISION_DECIMO_CUARTO),
						RhhTipoProvision.DECIMO_CUARTO, prnm.getSbu(), decimoCuarto, usuario, persistir);
			}
		}

		// --- Paso 10b: provision de vacaciones ----------------------------------------
		// Las vacaciones NO admiten mensualizacion, a diferencia de los decimos y los
		// fondos de reserva: el trabajador las goza en dias o se le liquidan al salir.
		// Por eso esta provision se genera SIEMPRE, sin depender de ninguna modalidad
		// del contrato, y es la unica que no tiene un renglon equivalente en el rol.
		//
		// La formula sale entera de PRNM, sin ninguna constante en el codigo:
		//   provision mensual = baseVacaciones x PRNMDIVC / PRNMDANO
		// Con la parametria 2026 (15 dias sobre 360) eso es la base entre 24, que es la
		// cifra que suele citarse. Se deja derivada y no escrita como 24 porque si la
		// escala base cambia, el divisor cambia con ella.
		Double provisionVacaciones = RedondeoNomina.redondea(Double.valueOf(
				baseVac.doubleValue() * prnm.getDiasVacaciones().doubleValue()
						/ prnm.getDiasAnio().doubleValue()));
		generaProvision(periodo, empleado,
				conceptoPorRol(conceptos, RhhRolConceptoMotor.PROVISION_VACACIONES),
				RhhTipoProvision.VACACIONES, baseVac, provisionVacaciones, usuario, persistir);

		// --- Paso 11: retencion de impuesto a la renta --------------------------------
		Double retencionIr = Double.valueOf(0D);
		// ESTE EMPLEADOR NO LE RETIENE A ESTE TRABAJADOR.
		//
		// Art. 43 LRTI: con varios empleadores, el trabajador presenta su proyeccion al que
		// mas le paga --ese retiene sobre el total-- y entrega a los demas copia certificada
		// para que se abstengan. La proyeccion NO se toca: lo que al trabajador le corresponde
		// sigue siendo lo que dice PYIR, y agosto lo necesita intacto para calcular el
		// alcance. Lo que cambia es que este empleador no lo descuenta.
		if (SI.equals(contrato.getNoRetieneImpuestoRenta())) {
			System.out.println("Contrato " + contrato.getCodigo() + ": este empleador no retiene IR"
					+ " (art. 43 LRTI). Motivo: " + contrato.getMotivoNoRetencion());
		} else if (!SI.equals(contrato.getRetieneFuente())) {
			retencionIr = retencionRentaService.obtenerRetencionMensual(idEmpleado,
					periodo.getAnio(), periodo.getMes());
			ConceptoNomina conceptoIr = conceptoPorRol(conceptos, RhhRolConceptoMotor.IMPUESTO_A_LA_RENTA);
			if (conceptoIr != null && retencionIr != null && retencionIr.doubleValue() > 0D) {
				renglones.add(nuevoRenglon(nomina, conceptoIr, null, retencionIr, baseIr, null,
						RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
			}
		} else if (contrato.getPorcentajeRetencionFuente() != null) {
			// Servicios profesionales sin dependencia: retencion puntual sobre el honorario.
			ConceptoNomina conceptoRet = conceptoRetencionServicios(conceptos);
			if (conceptoRet != null) {
				retencionIr = RedondeoNomina.porcentaje(sueldoPeriodo,
						contrato.getPorcentajeRetencionFuente());
				renglones.add(nuevoRenglon(nomina, conceptoRet, null, retencionIr, sueldoPeriodo,
						contrato.getPorcentajeRetencionFuente(),
						RhhOrigenRenglon.CALCULO_AUTOMATICO, null, null));
			}
		}

		// --- Paso 12: descuentos recurrentes ------------------------------------------
		List<CuotaDescuento> cuotas = cuotaDescuentoDaoService.selectPendientesPorVencer(
				idEmpleado, desde, hasta);
		Double netoPreliminar = calculaNeto(renglones);
		for (CuotaDescuento cuota : cuotas) {
			Double valor;
			if (cuota.getDescuentoRecurrente().getPorcentaje() != null) {
				// Los porcentuales (retencion judicial) van sobre el neto preliminar.
				valor = RedondeoNomina.porcentaje(netoPreliminar,
						cuota.getDescuentoRecurrente().getPorcentaje());
			} else {
				valor = RedondeoNomina.redondea(cuota.getTotal());
			}
			renglones.add(nuevoRenglon(nomina, cuota.getDescuentoRecurrente().getConceptoNomina(),
					null, valor, null, cuota.getDescuentoRecurrente().getPorcentaje(),
					RhhOrigenRenglon.DESCUENTO_RECURRENTE, "RHH.CTDS", cuota.getCodigo()));
		}

		// --- Pasos 13 y 14: neto y proteccion de neto negativo ------------------------
		Double neto = calculaNeto(renglones);
		if (neto.doubleValue() < 0D) {
			neto = recortaDescuentos(renglones, neto, empleado);
		}

		// --- Paso 15: persistir --------------------------------------------------------
		Double ingresos = sumaPorTipo(renglones, RhhTipoConceptoNomina.INGRESO);
		Double descuentos = sumaPorTipo(renglones, RhhTipoConceptoNomina.EGRESO);
		Double patronal = sumaPorTipo(renglones, RhhTipoConceptoNomina.APORTE_PATRONAL);

		nomina.setTotalIngresos(ingresos);
		nomina.setTotalDescuentos(descuentos);
		nomina.setNetoPagar(neto);
		nomina.setTotalPatronal(patronal);
		nomina.setBaseIess(baseIess);
		nomina.setBaseImpuestoRenta(baseIr);
		nomina.setBaseFondosReserva(baseFr);
		nomina.setBaseDecimoTercero(baseDec3);
		nomina.setBaseDecimoCuarto(baseDec4);
		// Los tres campos de aporte de la cabecera son distintos entre si y se reparten
		// por rol, no por la terna: NMNAAPPT lleva SOLO el aporte patronal al IESS y
		// NMNAIESC el IECE mas el SECAP. NMNATTPT, que es todo el costo patronal, se
		// mantiene con la suma de los renglones patronales.
		nomina.setAportePersonal(sumaPorRol(renglones, RhhRolConceptoMotor.APORTE_PERSONAL));
		nomina.setAportePatronal(sumaPorRol(renglones, RhhRolConceptoMotor.APORTE_PATRONAL));
		nomina.setAporteIeceSecap(RedondeoNomina.suma(
				sumaPorRol(renglones, RhhRolConceptoMotor.IECE),
				sumaPorRol(renglones, RhhRolConceptoMotor.SECAP)));
		nomina.setFondosReserva(fondosReserva);
		nomina.setRetencionImpuestoRenta(retencionIr);

		if (persistir) {
			nominaDaoService.save(nomina, nomina.getCodigo());
			int orden = 1;
			for (ReglonNomina renglon : renglones) {
				renglon.setNomina(nomina);
				renglon.setOrden(Integer.valueOf(orden++));
				renglon.setFechaRegistro(LocalDate.now());
				renglon.setUsuarioRegistro(usuario);
				reglonNominaDaoService.save(renglon, renglon.getCodigo());
			}
			if (preservarManuales) {
				// Los manuales ya estaban en base y no se tocaron; solo se informa.
				List<ReglonNomina> manuales = reglonNominaDaoService.selectManualesByNomina(nomina.getCodigo());
				System.out.println("Se preservaron " + manuales.size() + " renglon(es) manual(es).");
			}
		}

		return nomina;
	}

	// =====================================================================
	// Piezas del calculo
	// =====================================================================

	/**
	 * Dias trabajados: los dias base del mes menos las ausencias no remuneradas,
	 * ajustados por ingreso o salida a mitad de periodo.
	 *
	 * @param contrato		: Contrato
	 * @param periodo		: Periodo de nomina
	 * @param prnm			: Parametros del anio
	 * @return				: Dias trabajados
	 * @throws Throwable	: Excepcion
	 */
	private Double calculaDiasTrabajados(ContratoEmpleado contrato, PeriodoNomina periodo,
			ParametroNomina prnm) throws Throwable {
		double diasBase = prnm.getDiasMes().doubleValue();
		LocalDate desde = periodo.getFechaInicio();
		LocalDate hasta = periodo.getFechaFin();

		// Ajuste por ingreso o salida a mitad de periodo.
		LocalDate inicioReal = contrato.getFechaInicio() != null && contrato.getFechaInicio().isAfter(desde)
				? contrato.getFechaInicio() : desde;
		LocalDate finReal = hasta;
		if (contrato.getFechaTerminacion() != null && contrato.getFechaTerminacion().isBefore(hasta)) {
			finReal = contrato.getFechaTerminacion();
		} else if (contrato.getFechaFin() != null && contrato.getFechaFin().isBefore(hasta)) {
			finReal = contrato.getFechaFin();
		}

		// EL MES COMERCIAL SE CUENTA POR EL DIA DEL MES, NO POR FRACCION DEL CALENDARIO.
		//
		// Antes se hacia diasBase x diasEfectivos / diasCalendario, que en enero es x/31 y
		// devuelve decimales: quien entra el 15 de enero cobraba 16,4516 dias. El cliente y
		// el IESS cuentan igual que la ley, sobre un mes de treinta: del dia de ingreso al
		// 30, ambos inclusive, que son 30 - 15 + 1 = 16 dias justos.
		//
		// No es que nuestro motor afinara mas: es que pagaba 44,59 al anio que nadie cobro,
		// y esos 44,59 son la mayor de las diferencias de enero. Un mes completo sigue dando
		// 1..30 = 30, asi que no mueve a nadie que no entre o salga a mitad de mes.
		//
		// Radio verificado antes de tocarlo: solo enero, y solo dos personas -- Bravo Caiza
		// (ingreso 15-01) de 16,4516 a 16, y Cevallos Montenegro (19-01) de 12,5806 a 12.
		// Nadie mas tiene dias distintos de 30 en ningun mes del ejercicio.
		double diaInicio = inicioReal.isAfter(desde) ? inicioReal.getDayOfMonth() : 1D;
		double diaFin = finReal.isBefore(hasta) ? finReal.getDayOfMonth() : diasBase;
		double dias = Math.min(diaFin, diasBase) - Math.min(diaInicio, diasBase) + 1D;
		if (dias < 0D) {
			dias = 0D;
		}
		if (dias > diasBase) {
			dias = diasBase;
		}

		// Ausencias no remuneradas del periodo.
		List<Long> tiposDescuentan = Arrays.asList(
				Long.valueOf(RhhTipoAusencia.FALTA_INJUSTIFICADA),
				Long.valueOf(RhhTipoAusencia.PERMISO_SIN_GOCE));
		Double ausencias = resumenNominaDaoService.contarDiasAusenciaNoRemunerada(
				contrato.getEmpleado().getCodigo(), desde, hasta, tiposDescuentan);
		if (ausencias != null) {
			dias = dias - ausencias.doubleValue();
		}
		if (dias < 0D) {
			dias = 0D;
		}
		return RedondeoNomina.redondeaCantidad(Double.valueOf(dias));
	}

	/**
	 * Sueldo del periodo segun el tipo de relacion laboral.
	 *
	 * @param contrato			: Contrato
	 * @param prnm				: Parametros del anio
	 * @param diasTrabajados	: Dias trabajados
	 * @param diasBase			: Dias base del mes
	 * @return					: El sueldo del periodo
	 * @throws Throwable		: Excepcion
	 */
	private Double calculaSueldoPeriodo(ContratoEmpleado contrato, ParametroNomina prnm,
			Double diasTrabajados, Double diasBase) throws Throwable {
		Double salario = contrato.getSalarioBase() != null ? contrato.getSalarioBase() : Double.valueOf(0D);

		// Servicios profesionales: el honorario pactado, sin prorrateo.
		if (Long.valueOf(RhhTipoRelacionLaboral.SERVICIOS_PROFESIONALES_SIN_DEPENDENCIA)
				.equals(contrato.getTipoRelacionLaboral())) {
			return RedondeoNomina.redondea(salario);
		}

		// Por horas o jornada parcial: horas efectivas por el valor de la hora pactado.
		boolean porHoras = Long.valueOf(RhhTipoRelacionLaboral.POR_HORAS)
				.equals(contrato.getTipoRelacionLaboral());
		if (porHoras && contrato.getValorHora() != null && contrato.getHorasSemanales() != null) {
			double semanasMes = diasBase.doubleValue() / 7D;
			double horas = contrato.getHorasSemanales().doubleValue() * semanasMes;
			return RedondeoNomina.redondea(Double.valueOf(horas * contrato.getValorHora().doubleValue()));
		}

		// Jornada completa: prorrateo por dias trabajados.
		return RedondeoNomina.redondea(Double.valueOf(
				salario.doubleValue() * diasTrabajados.doubleValue() / diasBase.doubleValue()));
	}

	/**
	 * Valor de la hora ordinaria: sueldo del contrato entre las horas base del mes.
	 *
	 * @param contrato	: Contrato
	 * @param prnm		: Parametros del anio
	 * @return			: El valor de la hora
	 */
	private Double calculaValorHora(ContratoEmpleado contrato, ParametroNomina prnm) {
		if (contrato.getValorHora() != null) {
			return contrato.getValorHora();
		}
		if (prnm.getHorasMes() == null || prnm.getHorasMes().intValue() == 0) {
			return Double.valueOf(0D);
		}
		return RedondeoNomina.divide(contrato.getSalarioBase(),
				Double.valueOf(prnm.getHorasMes().doubleValue()));
	}

	/**
	 * Evalua un concepto del catalogo segun su tipo de calculo.
	 *
	 * @param concepto			: Concepto del catalogo
	 * @param contrato			: Contrato
	 * @param prnm				: Parametros del anio
	 * @param sueldoPeriodo		: Sueldo ya prorrateado
	 * @param diasTrabajados	: Dias trabajados
	 * @param diasBase			: Dias base del mes
	 * @param renglones			: Renglones ya generados, para las bases acumulativas
	 * @return					: El valor del concepto, o null si no aplica
	 */
	private Double evaluaConcepto(ConceptoNomina concepto, ContratoEmpleado contrato, ParametroNomina prnm,
			Double sueldoPeriodo, Double diasTrabajados, Double diasBase, List<ReglonNomina> renglones) {
		Long tipoCalculo = concepto.getTipoCalculo();
		Double base = baseDelConcepto(concepto, prnm, sueldoPeriodo, renglones);

		if (Long.valueOf(RhhTipoCalculoConcepto.VALOR_FIJO).equals(tipoCalculo)) {
			// Con base SUELDO_CONTRATO el valor es el sueldo ya prorrateado.
			if (Long.valueOf(RhhBaseCalculo.SUELDO_DEL_CONTRATO).equals(concepto.getBaseCalculo())) {
				return sueldoPeriodo;
			}
			return concepto.getValor() != null ? RedondeoNomina.redondea(concepto.getValor()) : null;
		}
		if (Long.valueOf(RhhTipoCalculoConcepto.PORCENTAJE_SOBRE_BASE).equals(tipoCalculo)) {
			Double porcentaje = porcentajeVigente(concepto, prnm);
			if (porcentaje == null || base == null) {
				return null;
			}
			return RedondeoNomina.porcentaje(base, porcentaje);
		}
		// Los demas tipos de calculo tienen su propio paso en el algoritmo.
		return null;
	}

	/**
	 * Resuelve la base de calculo de un concepto segun el rubro RHH_BASE_CALCULO.
	 *
	 * @param concepto		: Concepto del catalogo
	 * @param prnm			: Parametros del anio
	 * @param sueldoPeriodo	: Sueldo ya prorrateado
	 * @param renglones		: Renglones ya generados
	 * @return				: La base, o null si no se puede resolver
	 */
	private Double baseDelConcepto(ConceptoNomina concepto, ParametroNomina prnm, Double sueldoPeriodo,
			List<ReglonNomina> renglones) {
		Long base = concepto.getBaseCalculo();
		if (base == null) {
			return null;
		}
		if (Long.valueOf(RhhBaseCalculo.SUELDO_DEL_CONTRATO).equals(base)) {
			return sueldoPeriodo;
		}
		if (Long.valueOf(RhhBaseCalculo.SBU).equals(base)) {
			return prnm.getSbu();
		}
		if (Long.valueOf(RhhBaseCalculo.IMPONIBLE_IESS).equals(base)) {
			return sumaPorBandera(renglones, "IMIE");
		}
		if (Long.valueOf(RhhBaseCalculo.GRAVADO_IR).equals(base)) {
			return sumaPorBandera(renglones, "IMIR");
		}
		if (Long.valueOf(RhhBaseCalculo.TOTAL_INGRESOS).equals(base)) {
			return sumaPorTipo(renglones, RhhTipoConceptoNomina.INGRESO);
		}
		if (Long.valueOf(RhhBaseCalculo.NETO).equals(base)) {
			return calculaNeto(renglones);
		}
		return null;
	}

	/**
	 * Porcentaje vigente de un concepto: manda el catalogo, con caida a la parametria
	 * del anio cuando el concepto no lo trae informado.
	 *
	 * @param concepto	: Concepto del catalogo
	 * @param prnm		: Parametros del anio
	 * @return			: El porcentaje aplicable
	 */
	private Double porcentajeVigente(ConceptoNomina concepto, ParametroNomina prnm) {
		if (concepto.getPorcentaje() != null) {
			return concepto.getPorcentaje();
		}
		return porcentajeEnParametria(concepto.getRolMotor(), prnm);
	}

	/**
	 * Devuelve el porcentaje que la parametria del anio declara para un rol del motor.
	 * Es el respaldo cuando el concepto no trae CPNMPRCN, y el contraste con el que
	 * validarPeriodo detecta divergencias.
	 *
	 * @param rolMotor	: Codigo alterno del detalle del rubro RHH_ROL_CONCEPTO_MOTOR
	 * @param prnm		: Parametros del anio
	 * @return			: El porcentaje, o null si el rol no tiene equivalente en PRNM
	 */
	private Double porcentajeEnParametria(Long rolMotor, ParametroNomina prnm) {
		if (rolMotor == null || prnm == null) {
			return null;
		}
		if (Long.valueOf(RhhRolConceptoMotor.APORTE_PERSONAL).equals(rolMotor)) {
			return prnm.getAportePersonal();
		}
		if (Long.valueOf(RhhRolConceptoMotor.APORTE_PATRONAL).equals(rolMotor)) {
			return prnm.getAportePatronal();
		}
		if (Long.valueOf(RhhRolConceptoMotor.IECE).equals(rolMotor)) {
			return prnm.getIece();
		}
		if (Long.valueOf(RhhRolConceptoMotor.SECAP).equals(rolMotor)) {
			return prnm.getSecap();
		}
		if (Long.valueOf(RhhRolConceptoMotor.FONDOS_DE_RESERVA).equals(rolMotor)) {
			return prnm.getFondosReserva();
		}
		return null;
	}

	/**
	 * Indica si el bucle generico debe evaluar el concepto, o si lo resuelve un paso propio.
	 *
	 * @param concepto	: Concepto del catalogo
	 * @return			: true si lo evalua el bucle generico
	 */
	private boolean esEvaluableEnBucleGenerico(ConceptoNomina concepto) {
		Long tipoCalculo = concepto.getTipoCalculo();
		if (tipoCalculo == null) {
			return false;
		}
		boolean valorFijo = Long.valueOf(RhhTipoCalculoConcepto.VALOR_FIJO).equals(tipoCalculo);
		boolean porcentaje = Long.valueOf(RhhTipoCalculoConcepto.PORCENTAJE_SOBRE_BASE).equals(tipoCalculo);
		if (!valorFijo && !porcentaje) {
			return false;
		}
		// Todo concepto con rol tiene su propio paso en el algoritmo, que ademas respeta
		// las banderas del contrato (CNTEAPRT para los aportes, las modalidades para los
		// decimos y los fondos de reserva). Dejarlo tambien en el bucle generico lo
		// duplicaria.
		if (concepto.getRolMotor() != null) {
			return false;
		}
		return true;
	}

	/**
	 * Indica si el concepto aplica al tipo de relacion laboral del contrato.
	 *
	 * @param concepto	: Concepto del catalogo
	 * @param contrato	: Contrato
	 * @return			: true si aplica
	 */
	private boolean aplicaARelacionLaboral(ConceptoNomina concepto, ContratoEmpleado contrato) {
		if (concepto.getTipoRelacionLaboral() == null) {
			return true;
		}
		return concepto.getTipoRelacionLaboral().equals(contrato.getTipoRelacionLaboral());
	}

	/**
	 * Suma los renglones que llevan activa una bandera de base.
	 *
	 * @param renglones	: Renglones generados
	 * @param bandera	: IMIE, IMIR, APFR, BSDT o BSDC
	 * @return			: La suma de renglones redondeados
	 */
	private Double sumaPorBandera(List<ReglonNomina> renglones, String bandera) {
		Double total = Double.valueOf(0D);
		for (ReglonNomina renglon : renglones) {
			ConceptoNomina concepto = renglon.getConceptoNomina();
			if (concepto == null) {
				continue;
			}
			String valor;
			if ("IMIE".equals(bandera)) {
				valor = concepto.getImponibleIess();
			} else if ("IMIR".equals(bandera)) {
				valor = concepto.getImponibleIr();
			} else if ("APFR".equals(bandera)) {
				valor = concepto.getAportaFondosReserva();
			} else if ("BSDT".equals(bandera)) {
				valor = concepto.getBaseDecimoTercero();
			} else if ("BSDC".equals(bandera)) {
				valor = concepto.getBaseDecimoCuarto();
			} else if ("BSVC".equals(bandera)) {
				valor = concepto.getBaseVacaciones();
			} else if ("BSUT".equals(bandera)) {
				valor = concepto.getBaseUtilidades();
			} else {
				throw new IllegalArgumentException("Bandera de base desconocida: " + bandera
						+ ". Las validas son IMIE, IMIR, APFR, BSDT, BSDC, BSVC y BSUT.");
			}
			if (SI.equals(valor)) {
				total = RedondeoNomina.suma(total, renglon.getValor());
			}
		}
		return total;
	}

	/**
	 * Suma los renglones de un tipo de concepto.
	 *
	 * @param renglones	: Renglones generados
	 * @param tipo		: Codigo alterno del detalle del rubro RHH_TIPO_CONCEPTO_NOMINA
	 * @return			: La suma de renglones redondeados
	 */
	private Double sumaPorTipo(List<ReglonNomina> renglones, int tipo) {
		Double total = Double.valueOf(0D);
		for (ReglonNomina renglon : renglones) {
			if (Long.valueOf(tipo).equals(renglon.getTipoConcepto())) {
				total = RedondeoNomina.suma(total, renglon.getValor());
			}
		}
		return total;
	}

	/**
	 * Suma los renglones cuyo concepto cumple un rol del motor.
	 *
	 * Sustituye al antiguo sumaAporte, que localizaba los aportes por la terna
	 * baseCalculo = IMPONIBLE_IESS mas tipoConcepto. Eso contradecia la decision de que
	 * el motor localiza SIEMPRE por CPNMROLM: la terna discrimina en el catalogo del
	 * script 08, pero deja de hacerlo en cuanto el cliente agrega un egreso propio sobre
	 * la base imponible del IESS, y ese egreso se sumaria al aporte personal sin que
	 * nadie lo note.
	 *
	 * @param renglones		: Renglones generados
	 * @param rolConcepto	: Codigo alterno del detalle del rubro RHH_ROL_CONCEPTO_MOTOR
	 * @return				: La suma de renglones redondeados
	 */
	private Double sumaPorRol(List<ReglonNomina> renglones, int rolConcepto) {
		Double total = Double.valueOf(0D);
		for (ReglonNomina renglon : renglones) {
			ConceptoNomina concepto = renglon.getConceptoNomina();
			if (concepto == null || concepto.getRolMotor() == null) {
				continue;
			}
			if (Long.valueOf(rolConcepto).equals(concepto.getRolMotor())) {
				total = RedondeoNomina.suma(total, renglon.getValor());
			}
		}
		return total;
	}

	/**
	 * Neto = ingresos menos egresos. Los patronales y las provisiones no lo afectan.
	 *
	 * @param renglones	: Renglones generados
	 * @return			: El neto
	 */
	private Double calculaNeto(List<ReglonNomina> renglones) {
		Double ingresos = sumaPorTipo(renglones, RhhTipoConceptoNomina.INGRESO);
		Double egresos = sumaPorTipo(renglones, RhhTipoConceptoNomina.EGRESO);
		return RedondeoNomina.redondea(Double.valueOf(
				ingresos.doubleValue() - egresos.doubleValue()));
	}

	/**
	 * Proteccion de neto negativo: recorta los descuentos recortables en orden
	 * descendente de prelacion hasta que el neto deje de ser negativo. Los de ley
	 * nunca se recortan.
	 *
	 * @param renglones		: Renglones generados
	 * @param neto			: Neto negativo actual
	 * @param empleado		: Empleado, para el mensaje de error
	 * @return				: El neto corregido
	 * @throws Throwable	: IncomeException si aun asi queda negativo
	 */
	private Double recortaDescuentos(List<ReglonNomina> renglones, Double neto,
			Empleado empleado) throws Throwable {
		System.out.println("Neto negativo (" + neto + ") para " + empleado.getIdentificacion()
				+ ": se recortan descuentos.");

		// Orden descendente de CPNMORDN: se recorta primero el de menor prelacion.
		List<ReglonNomina> recortables = new ArrayList<ReglonNomina>();
		for (ReglonNomina renglon : renglones) {
			ConceptoNomina concepto = renglon.getConceptoNomina();
			if (concepto != null && SI.equals(concepto.getRecortable())
					&& Long.valueOf(RhhTipoConceptoNomina.EGRESO).equals(renglon.getTipoConcepto())) {
				recortables.add(renglon);
			}
		}
		recortables.sort((uno, otro) -> {
			Integer ordenUno = uno.getConceptoNomina().getOrden();
			Integer ordenOtro = otro.getConceptoNomina().getOrden();
			int a = ordenUno == null ? 0 : ordenUno.intValue();
			int b = ordenOtro == null ? 0 : ordenOtro.intValue();
			return Integer.compare(b, a);
		});

		Double faltante = Double.valueOf(-neto.doubleValue());
		for (ReglonNomina renglon : recortables) {
			if (faltante.doubleValue() <= 0D) {
				break;
			}
			Double valor = renglon.getValor();
			Double recorte = Double.valueOf(Math.min(valor.doubleValue(), faltante.doubleValue()));
			renglon.setValor(RedondeoNomina.redondea(Double.valueOf(
					valor.doubleValue() - recorte.doubleValue())));
			faltante = RedondeoNomina.redondea(Double.valueOf(
					faltante.doubleValue() - recorte.doubleValue()));
		}

		Double netoFinal = calculaNeto(renglones);
		if (netoFinal.doubleValue() < 0D) {
			throw new IncomeException("El neto de " + empleado.getApellidos() + " " + empleado.getNombres()
					+ " (" + empleado.getIdentificacion() + ") queda en " + netoFinal
					+ " aun tras recortar todos los descuentos recortables. Los descuentos de ley"
					+ " —aporte IESS, impuesto a la renta, retencion judicial y prestamos IESS—"
					+ " no se pueden recortar. Revise las novedades y los descuentos del empleado.");
		}
		return netoFinal;
	}

	// =====================================================================
	// Apoyo
	// =====================================================================

	/**
	 * Construye un renglon con el snapshot completo del concepto.
	 *
	 * @param nomina		: Nomina a la que pertenece
	 * @param concepto		: Concepto del catalogo
	 * @param cantidad		: Cantidad, si aplica
	 * @param valor			: Valor ya redondeado
	 * @param base			: Base de calculo usada
	 * @param porcentaje	: Porcentaje aplicado
	 * @param origen		: Codigo alterno del detalle del rubro RHH_ORIGEN_RENGLON
	 * @param tablaOrigen	: Tabla de la que proviene, para trazabilidad
	 * @param idOrigen		: Id del registro de origen
	 * @return				: El renglon sin persistir
	 */
	private ReglonNomina nuevoRenglon(Nomina nomina, ConceptoNomina concepto, Double cantidad, Double valor,
			Double base, Double porcentaje, int origen, String tablaOrigen, Long idOrigen) {
		ReglonNomina renglon = new ReglonNomina();
		renglon.setNomina(nomina);
		renglon.setConceptoNomina(concepto);
		// RNGLCANT y RNGLIMPN son NOT NULL en la tabla original. La mayoria de los
		// renglones no tienen cantidad —un sueldo o un aporte no se miden en unidades—,
		// asi que se graba cero, que es lo que la columna admite para decir "no aplica".
		renglon.setCantidad(cantidad != null ? cantidad : Double.valueOf(0D));
		renglon.setImponible(NO);
		renglon.setValor(RedondeoNomina.redondea(valor));
		renglon.setBaseCalculo(base);
		renglon.setPorcentaje(porcentaje);
		renglon.setOrigen(Long.valueOf(origen));
		renglon.setManual(NO);
		renglon.setTablaReferencia(tablaOrigen);
		renglon.setIdReferencia(idOrigen);
		if (concepto != null) {
			renglon.setDescripcion(concepto.getNombre());
			renglon.setTipoConcepto(concepto.getTipoConcepto());
			// Snapshot de las banderas: si manana cambia el catalogo, el rol historico
			// sigue explicando por que ese renglon entro en cada base.
			renglon.setImponibleIess(concepto.getImponibleIess());
			renglon.setGravadoIr(concepto.getImponibleIr());
			renglon.setPatronal(concepto.getPatronal());
			renglon.setImponible(concepto.getImponibleIess());
		}
		return renglon;
	}

	/**
	 * Genera y opcionalmente persiste una provision del periodo.
	 *
	 * @param periodo		: Periodo de nomina
	 * @param empleado		: Empleado
	 * @param concepto		: Concepto asociado, que aporta la cuenta contable
	 * @param tipoProvision	: Codigo alterno del detalle del rubro RHH_TIPO_PROVISION
	 * @param base			: Base de calculo
	 * @param valor			: Valor provisionado
	 * @param usuario		: Usuario que ejecuta
	 * @param persistir		: false para simular
	 * @throws Throwable	: Excepcion
	 */
	private void generaProvision(PeriodoNomina periodo, Empleado empleado, ConceptoNomina concepto,
			int tipoProvision, Double base, Double valor, String usuario, boolean persistir) throws Throwable {
		if (!persistir || valor == null || valor.doubleValue() == 0D) {
			return;
		}
		ProvisionNomina provision = new ProvisionNomina();
		provision.setPeriodoNomina(periodo);
		provision.setEmpleado(empleado);
		provision.setConceptoNomina(concepto);
		provision.setTipoProvision(Long.valueOf(tipoProvision));
		provision.setBaseCalculo(RedondeoNomina.redondea(base));
		provision.setValor(RedondeoNomina.redondea(valor));
		provision.setEstado(Long.valueOf(1L));
		provision.setFechaRegistro(LocalDateTime.now());
		provision.setUsuarioRegistro(usuario);
		provisionNominaDaoService.save(provision, provision.getCodigo());
	}

	/**
	 * Escribe un acumulado del periodo. Solo se invoca desde cerrarPeriodo.
	 *
	 * @param periodo		: Periodo de nomina
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio
	 * @param mes			: Mes
	 * @param tipo			: Codigo alterno del detalle del rubro RHH_TIPO_ACUMULADO
	 * @param valor			: Valor acumulado
	 * @param dias			: Dias, cuando el tipo es DIAS_TRABAJADOS
	 * @param usuario		: Usuario que ejecuta
	 * @throws Throwable	: Excepcion
	 */
	private void escribeAcumulado(PeriodoNomina periodo, Long idEmpleado, Integer anio, Integer mes,
			int tipo, Double valor, Double dias, String usuario) throws Throwable {
		if ((valor == null || valor.doubleValue() == 0D) && (dias == null || dias.doubleValue() == 0D)) {
			return;
		}
		AcumuladoNomina acumulado = acumuladoNominaDaoService.selectByClave(idEmpleado, anio, mes,
				Long.valueOf(tipo));
		if (acumulado == null) {
			acumulado = new AcumuladoNomina();
			acumulado.setEmpleado(em.find(Empleado.class, idEmpleado));
			acumulado.setAnio(anio);
			acumulado.setMes(mes);
			acumulado.setTipoAcumulado(Long.valueOf(tipo));
			acumulado.setAperturaMigracion(NO);
			acumulado.setEstado(Long.valueOf(1L));
			acumulado.setFechaRegistro(LocalDateTime.now());
		}
		acumulado.setPeriodoNomina(periodo);
		acumulado.setValor(RedondeoNomina.redondea(valor));
		acumulado.setDias(RedondeoNomina.redondeaCantidad(dias));
		acumulado.setUsuarioRegistro(usuario);
		acumuladoNominaDaoService.save(acumulado, acumulado.getCodigo());
	}

	/**
	 * Indica si el empleado supera el anio de servicio a la fecha indicada, que es la
	 * condicion para que los fondos de reserva se paguen.
	 *
	 * @param empleado	: Empleado
	 * @param contrato	: Contrato
	 * @param fecha		: Fecha de corte
	 * @return			: true si supera el anio
	 */
	private boolean superaUnAnio(Empleado empleado, ContratoEmpleado contrato, LocalDate fecha) {
		LocalDate ingreso = empleado.getFechaIngreso() != null
				? empleado.getFechaIngreso() : contrato.getFechaInicio();
		if (ingreso == null) {
			return false;
		}
		return !ingreso.plusYears(1).isAfter(fecha);
	}

	/**
	 * Indica si ya existe un renglon de ese concepto, para no duplicarlo.
	 *
	 * @param renglones	: Renglones generados
	 * @param concepto	: Concepto a buscar
	 * @return			: true si ya existe
	 */
	private boolean yaTieneRenglon(List<ReglonNomina> renglones, ConceptoNomina concepto) {
		if (concepto == null) {
			return false;
		}
		for (ReglonNomina renglon : renglones) {
			if (renglon.getConceptoNomina() != null
					&& concepto.getCodigo().equals(renglon.getConceptoNomina().getCodigo())) {
				return true;
			}
		}
		return false;
	}

	/**
	/**
	/**
	 * Localiza en el catalogo el concepto que cumple un rol del motor.
	 *
	 * <p>Antes esto se resolvia por la terna (tipo de concepto, tipo de calculo, base de
	 * calculo), que era discriminante en el catalogo inicial pero dejaba de serlo en
	 * cuanto el cliente agregaba un concepto propio: un "Bono navideno" definido como
	 * INGRESO / FORMULA / SBU se habria confundido con el decimo cuarto. Con
	 * <code>CPNMROLM</code> la identificacion es explicita y el indice unico
	 * <code>UQ_CPNM_ROLM</code> impide que dos conceptos reclamen el mismo rol.</p>
	 *
	 * @param conceptos	: Catalogo activo de la empresa
	 * @param rolMotor	: Codigo alterno del detalle del rubro RHH_ROL_CONCEPTO_MOTOR
	 * @return			: El concepto, o null si el rol no esta asignado en el catalogo
	 */
	private ConceptoNomina conceptoPorRol(List<ConceptoNomina> conceptos, int rolMotor) {
		for (ConceptoNomina concepto : conceptos) {
			if (Long.valueOf(rolMotor).equals(concepto.getRolMotor())) {
				return concepto;
			}
		}
		return null;
	}

	/**
	 * Localiza el concepto de hora extra que corresponde al tipo de la hora.
	 *
	 * <p>El tipo de la <code>HREX</code> es un detalle del rubro RHH_TIPO_HORA_EXTRA
	 * (1 suplementaria, 2 extraordinaria, 3 recargo nocturno) y se traduce al rol
	 * equivalente del motor. Ya no se empareja por coincidencia de porcentaje, que
	 * fallaba si dos conceptos compartian recargo.</p>
	 *
	 * @param conceptos	: Catalogo activo de la empresa
	 * @param hora		: Hora extra
	 * @return			: El concepto, o null si el rol no esta asignado
	 */
	private ConceptoNomina conceptoDeHoraExtra(List<ConceptoNomina> conceptos, HoraExtra hora) {
		Long tipo = hora.getTipoHoraExtra();
		if (tipo == null) {
			return null;
		}
		if (Long.valueOf(RhhTipoHoraExtra.SUPLEMENTARIA_50).equals(tipo)) {
			return conceptoPorRol(conceptos, RhhRolConceptoMotor.HORA_SUPLEMENTARIA);
		}
		if (Long.valueOf(RhhTipoHoraExtra.EXTRAORDINARIA_100).equals(tipo)) {
			return conceptoPorRol(conceptos, RhhRolConceptoMotor.HORA_EXTRAORDINARIA);
		}
		if (Long.valueOf(RhhTipoHoraExtra.RECARGO_NOCTURNO_25).equals(tipo)) {
			return conceptoPorRol(conceptos, RhhRolConceptoMotor.RECARGO_NOCTURNO);
		}
		return null;
	}

	/**
	 * Localiza el concepto de retencion en la fuente por servicios profesionales.
	 *
	 * <p><b>Este es el unico concepto que sigue localizandose por la terna</b>: el rubro
	 * 221 no incluye un rol para el, porque no forma parte del calculo ordinario de
	 * nomina sino de la via de servicios profesionales sin dependencia. Si el catalogo
	 * llegara a tener dos egresos porcentuales sobre el total de ingresos, habria que
	 * agregarle su propio rol.</p>
	 *
	 * @param conceptos	: Catalogo activo de la empresa
	 * @return			: El concepto, o null
	 */
	private ConceptoNomina conceptoRetencionServicios(List<ConceptoNomina> conceptos) {
		for (ConceptoNomina concepto : conceptos) {
			if (Long.valueOf(RhhTipoConceptoNomina.EGRESO).equals(concepto.getTipoConcepto())
					&& Long.valueOf(RhhTipoCalculoConcepto.PORCENTAJE_SOBRE_BASE)
							.equals(concepto.getTipoCalculo())
					&& Long.valueOf(RhhBaseCalculo.TOTAL_INGRESOS).equals(concepto.getBaseCalculo())) {
				return concepto;
			}
		}
		return null;
	}

	/**
	 * Siembra en cero los totales monetarios de la cabecera que RHH.NMNA declara
	 * NOT NULL, para que el INSERT previo al calculo de los renglones no los viole.
	 *
	 * @param nomina	: Nomina recien armada
	 */
	private void ceroSiNulo(Nomina nomina) {
		if (nomina.getSalarioBase() == null) {
			nomina.setSalarioBase(Double.valueOf(0D));
		}
		if (nomina.getTotalIngresos() == null) {
			nomina.setTotalIngresos(Double.valueOf(0D));
		}
		if (nomina.getTotalDescuentos() == null) {
			nomina.setTotalDescuentos(Double.valueOf(0D));
		}
		if (nomina.getNetoPagar() == null) {
			nomina.setNetoPagar(Double.valueOf(0D));
		}
	}

	/**
	 * Recupera el periodo y falla con mensaje explicito si no existe.
	 *
	 * @param idPeriodoNomina	: Id del periodo
	 * @return					: El periodo
	 * @throws Throwable		: IncomeException si no existe
	 */
	private PeriodoNomina recuperaPeriodo(Long idPeriodoNomina) throws Throwable {
		PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
				NombreEntidadesRhh.PERIODO_NOMINA);
		if (periodo == null) {
			throw new IncomeException("No existe el periodo de nomina " + idPeriodoNomina + ".");
		}
		return periodo;
	}

	/**
	 * Exige que el periodo admita calculo.
	 *
	 * @param periodo		: Periodo de nomina
	 * @throws Throwable	: IncomeException si el estado no lo permite
	 */
	private void exigeEstadoCalculable(PeriodoNomina periodo) throws Throwable {
		Long estado = periodo.getEstado();
		boolean calculable = estado == null
				|| Long.valueOf(RhhEstadoPeriodoNomina.ABIERTO).equals(estado)
				|| Long.valueOf(RhhEstadoPeriodoNomina.EN_CALCULO).equals(estado)
				|| Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO).equals(estado);
		if (!calculable) {
			throw new IncomeException("El periodo esta en estado " + estado
					+ " y ya no admite calculo. Reabralo primero si necesita recalcularlo.");
		}
	}

	/**
	 * Localiza el contrato de un empleado dentro del periodo.
	 *
	 * @param periodo		: Periodo de nomina
	 * @param idEmpresa		: Id de la empresa
	 * @param idEmpleado	: Id del empleado
	 * @return				: El contrato
	 * @throws Throwable	: IncomeException si no lo encuentra
	 */
	private ContratoEmpleado localizaContrato(PeriodoNomina periodo, Long idEmpresa,
			Long idEmpleado) throws Throwable {
		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				idEmpresa, periodo.getFechaInicio(), periodo.getFechaFin());
		for (ContratoEmpleado contrato : contratos) {
			if (contrato.getEmpleado() != null && idEmpleado.equals(contrato.getEmpleado().getCodigo())) {
				return contrato;
			}
		}
		throw new IncomeException("El empleado " + idEmpleado
				+ " no tiene contrato activo en el periodo " + periodo.getCodigo() + ".");
	}

	/**
	 * Arma el DTO de respuesta a partir de la nomina calculada.
	 *
	 * @param nomina		: Nomina calculada
	 * @param renglones	: Renglones generados en el calculo
	 * @return				: El DTO
	 * @throws Throwable	: Excepcion
	 */
	private ResultadoCalculoNomina armaResultado(Nomina nomina, List<ReglonNomina> renglones) throws Throwable {
		ResultadoCalculoNomina resultado = new ResultadoCalculoNomina();
		resultado.setIdEmpleado(nomina.getEmpleado().getCodigo());
		resultado.setNombreEmpleado(nomina.getEmpleado().getApellidos() + " "
				+ nomina.getEmpleado().getNombres());
		resultado.setDiasTrabajados(nomina.getDiasTrabajados());
		resultado.setTotalIngresos(nomina.getTotalIngresos());
		resultado.setTotalDescuentos(nomina.getTotalDescuentos());
		resultado.setNeto(nomina.getNetoPagar());

		if (renglones == null || renglones.isEmpty()) {
			renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());
		}
		List<RenglonCalculado> detalle = new ArrayList<RenglonCalculado>();
		int orden = 1;
		for (ReglonNomina renglon : renglones) {
			RenglonCalculado item = new RenglonCalculado();
			if (renglon.getConceptoNomina() != null) {
				item.setCodigoConcepto(renglon.getConceptoNomina().getCodigoAlterno());
				item.setNombreConcepto(renglon.getConceptoNomina().getNombre());
			}
			item.setTipoConcepto(renglon.getTipoConcepto());
			item.setCantidad(renglon.getCantidad());
			item.setBase(renglon.getBaseCalculo());
			item.setPorcentaje(renglon.getPorcentaje());
			item.setValor(renglon.getValor());
			item.setOrden(renglon.getOrden() != null ? renglon.getOrden() : Integer.valueOf(orden));
			detalle.add(item);
			orden++;
		}
		resultado.setRenglones(detalle);
		return resultado;
	}
}
