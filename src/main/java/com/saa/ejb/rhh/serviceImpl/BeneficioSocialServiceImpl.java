package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.service.BeneficioSocialService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.LiquidacionBeneficioSocial;
import com.saa.model.rhh.ParametroNomina;
import com.saa.rubros.RhhModalidadFondosReserva;
import com.saa.rubros.RhhRegionDecimoCuarto;
import com.saa.rubros.RhhTipoAcumulado;
import com.saa.rubros.RhhTipoBeneficioSocial;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de BeneficioSocialService.</p>
 *
 * <h3>Los tres periodos</h3>
 *
 * <p>Cada beneficio se acumula en una ventana distinta, y ninguna coincide con el anio
 * calendario salvo la de fondos de reserva:</p>
 *
 * <table border="1">
 *   <tr><th>Beneficio</th><th>Ventana</th><th>Base legal</th></tr>
 *   <tr><td>Decimo tercero</td><td>1-dic del anio anterior al 30-nov</td><td>Art. 111 CT</td></tr>
 *   <tr><td>Decimo cuarto Sierra y Amazonia</td><td>1-ago del anio anterior al 31-jul</td><td>Art. 113 CT</td></tr>
 *   <tr><td>Decimo cuarto Costa e Insular</td><td>1-mar al ultimo dia de febrero</td><td>Art. 113 CT</td></tr>
 *   <tr><td>Fondos de reserva</td><td>Anio calendario</td><td>Codigo del Trabajo</td></tr>
 * </table>
 *
 * <p>Por eso los acumulados se suman con <code>sumaValorRango</code>, que compara
 * <code>anio * 100 + mes</code> y cubre las ventanas que cruzan el cambio de anio.</p>
 *
 * <h3>Idempotencia</h3>
 *
 * <p>El unique <code>UQ_LQBS_BNF (MPLDCDGO, LQBSTPBN, LQBSANOO)</code> impide duplicar. Los
 * generadores buscan el beneficio del anio y lo actualizan si existe, de modo que volver a
 * ejecutarlos tras corregir un acumulado da el valor correcto sin dejar filas huerfanas.
 * <b>Lo que ya se pago (<code>LQBSVLPG</code>) nunca se toca al regenerar.</b></p>
 */
@Stateless
public class BeneficioSocialServiceImpl implements BeneficioSocialService {

	/** Meses del anio, divisor del decimo tercero. */
	private static final double MESES_ANIO = 12D;

	/** Primer mes de la ventana del decimo tercero: diciembre del anio anterior. */
	private static final int MES_INICIO_DECIMO_TERCERO = 12;

	/** Ultimo mes de la ventana del decimo tercero: noviembre. */
	private static final int MES_FIN_DECIMO_TERCERO = 11;

	/** Primer mes de la ventana del decimo cuarto de Sierra y Amazonia: agosto. */
	private static final int MES_INICIO_SIERRA = 8;

	/** Ultimo mes de la ventana del decimo cuarto de Sierra y Amazonia: julio. */
	private static final int MES_FIN_SIERRA = 7;

	/** Primer mes de la ventana del decimo cuarto de Costa e Insular: marzo. */
	private static final int MES_INICIO_COSTA = 3;

	/** Ultimo mes de la ventana del decimo cuarto de Costa e Insular: febrero. */
	private static final int MES_FIN_COSTA = 2;

	@PersistenceContext
	private EntityManager em;

	@EJB
	private LiquidacionBeneficioSocialDaoService liquidacionBeneficioSocialDaoService;

	@EJB
	private AcumuladoNominaDaoService acumuladoNominaDaoService;

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.BeneficioSocialService#generarDecimoTercero(java.lang.Long, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int generarDecimoTercero(Long idEmpresa, Integer anio, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarDecimoTercero de beneficioSocial service, empresa: "
				+ idEmpresa + ", anio: " + anio);

		int generados = 0;
		for (ContratoEmpleado contrato : contratosDelAnio(idEmpresa, anio)) {
			Long idEmpleado = contrato.getEmpleado().getCodigo();
			LiquidacionBeneficioSocial beneficio = calcularDecimoTercero(idEmpleado, anio);
			persiste(beneficio, usuario);
			generados++;
		}
		System.out.println("generarDecimoTercero termino: " + generados + " beneficio(s).");
		return generados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.BeneficioSocialService#generarDecimoCuarto(java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int generarDecimoCuarto(Long idEmpresa, Integer anio, Integer region,
			String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarDecimoCuarto de beneficioSocial service, empresa: "
				+ idEmpresa + ", anio: " + anio + ", region: " + region);

		if (region == null) {
			throw new IncomeException("La region es obligatoria: el periodo del decimo cuarto depende"
					+ " de ella (rubro RHH_REGION_DECIMO_CUARTO).");
		}

		int generados = 0;
		for (ContratoEmpleado contrato : contratosDelAnio(idEmpresa, anio)) {
			Empleado empleado = contrato.getEmpleado();
			// Solo los empleados de la region pedida. Los que no la tienen asignada se
			// omiten a proposito: sin region no se sabe que ventana aplicarles.
			if (empleado.getRegion() == null
					|| !empleado.getRegion().equals(Long.valueOf(region.longValue()))) {
				continue;
			}
			if (!SI_DECIMO_CUARTO.equals(contrato.getDerechoDecimoCuarto())) {
				continue;
			}
			LiquidacionBeneficioSocial beneficio = calcularDecimoCuarto(empleado.getCodigo(), anio);
			persiste(beneficio, usuario);
			generados++;
		}
		System.out.println("generarDecimoCuarto termino: " + generados + " beneficio(s).");
		return generados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.BeneficioSocialService#generarFondosReserva(java.lang.Long, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int generarFondosReserva(Long idEmpresa, Integer anio, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarFondosReserva de beneficioSocial service, empresa: "
				+ idEmpresa + ", anio: " + anio);

		ParametroNomina prnm = recuperaParametros(idEmpresa, anio);
		int generados = 0;

		for (ContratoEmpleado contrato : contratosDelAnio(idEmpresa, anio)) {
			// Los mensualizados ya cobraron el fondo en cada rol: aqui solo van los
			// acumulados en el IESS.
			if (!Long.valueOf(RhhModalidadFondosReserva.ACUMULADO_EN_EL_IESS)
					.equals(contrato.getModalidadFondosReserva())) {
				continue;
			}
			Empleado empleado = contrato.getEmpleado();
			Long idEmpleado = empleado.getCodigo();

			// Solo los meses posteriores al primer anio de servicio.
			int mesDesde = primerMesConDerecho(empleado, contrato, anio);
			if (mesDesde > MES_INICIO_DECIMO_TERCERO) {
				continue;
			}
			Double base = acumuladoNominaDaoService.sumaValorRango(idEmpleado,
					Long.valueOf(RhhTipoAcumulado.BASE_FONDOS_DE_RESERVA),
					anio, Integer.valueOf(mesDesde), anio, Integer.valueOf(MES_INICIO_DECIMO_TERCERO));
			Double valor = RedondeoNomina.porcentaje(base, prnm.getFondosReserva());

			LiquidacionBeneficioSocial beneficio = nuevoBeneficio(idEmpleado,
					RhhTipoBeneficioSocial.FONDOS_DE_RESERVA, anio);
			beneficio.setFechaInicio(LocalDate.of(anio.intValue(), mesDesde, 1));
			beneficio.setFechaFin(LocalDate.of(anio.intValue(), MES_INICIO_DECIMO_TERCERO, 31));
			beneficio.setBaseCalculo(base);
			beneficio.setValor(valor);
			persiste(beneficio, usuario);
			generados++;
		}
		System.out.println("generarFondosReserva termino: " + generados + " beneficio(s).");
		return generados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.BeneficioSocialService#calcularDecimoTercero(java.lang.Long, java.lang.Integer)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public LiquidacionBeneficioSocial calcularDecimoTercero(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo calcularDecimoTercero, empleado: " + idEmpleado
				+ ", anio: " + anio);

		int anioAnterior = anio.intValue() - 1;
		// Ventana del Art. 111: 1-dic del anio anterior al 30-nov de este.
		Double base = acumuladoNominaDaoService.sumaValorRango(idEmpleado,
				Long.valueOf(RhhTipoAcumulado.BASE_DECIMO_TERCERO),
				Integer.valueOf(anioAnterior), Integer.valueOf(MES_INICIO_DECIMO_TERCERO),
				anio, Integer.valueOf(MES_FIN_DECIMO_TERCERO));

		Double calculado = RedondeoNomina.divide(base, Double.valueOf(MESES_ANIO));

		LiquidacionBeneficioSocial beneficio = nuevoBeneficio(idEmpleado,
				RhhTipoBeneficioSocial.DECIMO_TERCERO, anio);
		beneficio.setFechaInicio(LocalDate.of(anioAnterior, MES_INICIO_DECIMO_TERCERO, 1));
		beneficio.setFechaFin(LocalDate.of(anio.intValue(), MES_FIN_DECIMO_TERCERO, 30));
		beneficio.setBaseCalculo(base);

		// Si el empleado cambio de modalidad a mitad de anio, ya cobro una parte en el
		// rol. Ese valor esta en LQBSVLMN y se descuenta para no pagarlo dos veces.
		Double mensualizado = beneficio.getValorMensualizado() != null
				? beneficio.getValorMensualizado() : Double.valueOf(0D);
		beneficio.setValor(RedondeoNomina.redondea(Double.valueOf(
				calculado.doubleValue() - mensualizado.doubleValue())));
		return beneficio;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.BeneficioSocialService#calcularDecimoCuarto(java.lang.Long, java.lang.Integer)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public LiquidacionBeneficioSocial calcularDecimoCuarto(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo calcularDecimoCuarto, empleado: " + idEmpleado
				+ ", anio: " + anio);

		Empleado empleado = em.find(Empleado.class, idEmpleado);
		if (empleado == null) {
			throw new IncomeException("No existe el empleado " + idEmpleado + ".");
		}
		Long idEmpresa = empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null;
		ParametroNomina prnm = recuperaParametros(idEmpresa, anio);

		boolean sierra = empleado.getRegion() == null
				|| Long.valueOf(RhhRegionDecimoCuarto.SIERRA_Y_AMAZONIA).equals(empleado.getRegion());
		int mesInicio = sierra ? MES_INICIO_SIERRA : MES_INICIO_COSTA;
		int mesFin = sierra ? MES_FIN_SIERRA : MES_FIN_COSTA;
		int anioInicio = anio.intValue() - 1;

		LocalDate desde = LocalDate.of(anioInicio, mesInicio, 1);
		// El ultimo dia del mes de cierre lo resuelve la propia fecha: febrero varia.
		LocalDate hasta = LocalDate.of(anio.intValue(), mesFin, 1)
				.withDayOfMonth(LocalDate.of(anio.intValue(), mesFin, 1).lengthOfMonth());

		// Dias efectivamente trabajados dentro de la ventana, desde los acumulados.
		//
		// sumaDIASRango, no sumaValorRango: el acumulado de dias trabajados guarda los dias
		// en ACMNDIAS y deja ACMNVLOR en cero --lo escribe asi cerrarPeriodo--, de modo que
		// sumar el valor devolvia 0,00 y el decimo cuarto salia en cero PARA TODO EL MUNDO.
		// Corregido el 2026-08-21. Misma familia que el ACMN tipo 7 de vacaciones: un lector
		// apuntando a donde nadie escribe.
		Double dias = acumuladoNominaDaoService.sumaDiasRango(idEmpleado,
				Long.valueOf(RhhTipoAcumulado.DIAS_TRABAJADOS),
				Integer.valueOf(anioInicio), Integer.valueOf(mesInicio), anio, Integer.valueOf(mesFin));

		// SBU prorrateado por dias sobre los dias base del anio, con tope de un SBU.
		Double sbu = prnm.getSbu();
		Double diasAnio = Double.valueOf(prnm.getDiasAnio().doubleValue());
		Double valor = RedondeoNomina.redondea(Double.valueOf(
				sbu.doubleValue() * dias.doubleValue() / diasAnio.doubleValue()));
		if (valor.doubleValue() > sbu.doubleValue()) {
			valor = RedondeoNomina.redondea(sbu);
		}

		LiquidacionBeneficioSocial beneficio = nuevoBeneficio(idEmpleado,
				RhhTipoBeneficioSocial.DECIMO_CUARTO, anio);
		beneficio.setFechaInicio(desde);
		beneficio.setFechaFin(hasta);
		beneficio.setBaseCalculo(sbu);
		beneficio.setDias(RedondeoNomina.redondeaCantidad(dias));

		Double mensualizado = beneficio.getValorMensualizado() != null
				? beneficio.getValorMensualizado() : Double.valueOf(0D);
		beneficio.setValor(RedondeoNomina.redondea(Double.valueOf(
				valor.doubleValue() - mensualizado.doubleValue())));
		return beneficio;
	}

	// =====================================================================
	// Apoyo
	// =====================================================================

	/** Marca de "si" de la bandera de derecho a decimo cuarto del contrato. */
	private static final String SI_DECIMO_CUARTO = "S";

	/**
	 * Recupera el beneficio del anio si ya existe, o crea uno nuevo. Es lo que hace
	 * idempotentes a los generadores.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param tipoBeneficio	: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
	 * @param anio			: Anio del beneficio
	 * @return				: El beneficio, existente o nuevo
	 * @throws Throwable	: Excepcion
	 */
	private LiquidacionBeneficioSocial nuevoBeneficio(Long idEmpleado, int tipoBeneficio,
			Integer anio) throws Throwable {
		LiquidacionBeneficioSocial beneficio = liquidacionBeneficioSocialDaoService
				.selectByEmpleadoTipoAnio(idEmpleado, Long.valueOf(tipoBeneficio), anio);
		if (beneficio != null) {
			return beneficio;
		}
		beneficio = new LiquidacionBeneficioSocial();
		beneficio.setEmpleado(em.find(Empleado.class, idEmpleado));
		beneficio.setTipoBeneficio(Long.valueOf(tipoBeneficio));
		beneficio.setAnio(anio);
		beneficio.setValorMensualizado(Double.valueOf(0D));
		beneficio.setValorPagado(Double.valueOf(0D));
		beneficio.setEstado(Long.valueOf(1L));
		beneficio.setFechaRegistro(LocalDateTime.now());
		return beneficio;
	}

	/**
	 * Persiste el beneficio conservando lo que ya se pago.
	 *
	 * @param beneficio		: Beneficio calculado
	 * @param usuario		: Usuario que ejecuta
	 * @throws Throwable	: Excepcion
	 */
	private void persiste(LiquidacionBeneficioSocial beneficio, String usuario) throws Throwable {
		beneficio.setUsuarioRegistro(usuario);
		liquidacionBeneficioSocialDaoService.save(beneficio, beneficio.getCodigo());
	}

	/**
	 * Contratos activos durante el anio indicado.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio
	 * @return				: Listado de contratos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	private List<ContratoEmpleado> contratosDelAnio(Long idEmpresa, Integer anio) throws Throwable {
		return contratoEmpleadoDaoService.selectActivosEnPeriodo(idEmpresa,
				LocalDate.of(anio.intValue(), 1, 1),
				LocalDate.of(anio.intValue(), MES_INICIO_DECIMO_TERCERO, 31));
	}

	/**
	 * Primer mes del anio en el que el empleado ya cumplio su primer anio de servicio,
	 * que es cuando nace el derecho a fondos de reserva.
	 *
	 * @param empleado	: Empleado
	 * @param contrato	: Contrato
	 * @param anio		: Anio evaluado
	 * @return			: Mes desde el que hay derecho; 13 si no lo hay en todo el anio
	 */
	private int primerMesConDerecho(Empleado empleado, ContratoEmpleado contrato, Integer anio) {
		LocalDate ingreso = empleado.getFechaIngreso() != null
				? empleado.getFechaIngreso() : contrato.getFechaInicio();
		if (ingreso == null) {
			return MES_INICIO_DECIMO_TERCERO + 1;
		}
		LocalDate cumpleUnAnio = ingreso.plusYears(1);
		if (cumpleUnAnio.getYear() < anio.intValue()) {
			return 1;
		}
		if (cumpleUnAnio.getYear() > anio.intValue()) {
			return MES_INICIO_DECIMO_TERCERO + 1;
		}
		return cumpleUnAnio.getMonthValue();
	}

	/**
	 * Recupera los parametros del anio y falla con mensaje explicito si faltan.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio de vigencia
	 * @return				: Los parametros
	 * @throws Throwable	: IncomeException si el anio no esta parametrizado
	 */
	private ParametroNomina recuperaParametros(Long idEmpresa, Integer anio) throws Throwable {
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		if (prnm == null) {
			throw new IncomeException("No existen parametros de nomina (RHH.PRNM) para el anio " + anio
					+ " y la empresa " + idEmpresa + ". Cargue el anio antes de generar beneficios.");
		}
		return prnm;
	}
}
