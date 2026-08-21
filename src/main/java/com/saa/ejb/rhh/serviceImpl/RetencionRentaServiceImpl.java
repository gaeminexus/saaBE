package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.CargaFamiliarDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.GastoPersonalProyectadoDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService;
import com.saa.ejb.rhh.dao.TablaImpuestoRentaDaoService;
import com.saa.ejb.rhh.dao.TopeGastoPersonalDaoService;
import com.saa.ejb.rhh.service.RetencionRentaService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.ProyeccionImpuestoRenta;
import com.saa.model.rhh.ResultadoProyeccionIr;
import com.saa.model.rhh.TablaImpuestoRenta;
import com.saa.model.rhh.TopeGastoPersonal;
import com.saa.rubros.RhhTipoAcumulado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de RetencionRentaService.</p>
 *
 * <p>Secuencia de la proyeccion, segun la seccion 4.5 del plan de backend:</p>
 *
 * <ol>
 *   <li>Ingresos ya realizados: suma de los acumulados GRAVADO_IR de los meses anteriores.</li>
 *   <li>Ingresos futuros: remuneracion gravada mensual por los meses restantes.
 *       <b>Se excluyen decimo tercero, decimo cuarto y fondos de reserva</b>, exentos por el
 *       articulo 9 de la LRTI; la exclusion la garantiza el catalogo, porque esos conceptos
 *       llevan CPNMIMIR = 'N' y por tanto nunca entraron en el acumulado GRAVADO_IR.</li>
 *   <li>Base imponible = ingresos proyectados menos aporte personal proyectado.</li>
 *   <li>Impuesto causado con la tabla progresiva de RHH.TBIR.</li>
 *   <li>Rebaja = min(gastos declarados, tope) por el porcentaje de RHH.PRNM.</li>
 *   <li>Retencion mensual = (impuesto a pagar menos lo ya retenido) entre meses restantes.</li>
 * </ol>
 */
@Stateless
public class RetencionRentaServiceImpl implements RetencionRentaService {

	/** Marca de "si" de las banderas S/N del esquema. */
	private static final String SI = "S";

	/** Marca de "no" de las banderas S/N del esquema. */
	private static final String NO = "N";

	/** Ultimo mes del anio calendario. */
	private static final int ULTIMO_MES = 12;

	@PersistenceContext
	private EntityManager em;

	@EJB
	private ProyeccionImpuestoRentaDaoService proyeccionImpuestoRentaDaoService;

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@EJB
	private TablaImpuestoRentaDaoService tablaImpuestoRentaDaoService;

	@EJB
	private TopeGastoPersonalDaoService topeGastoPersonalDaoService;

	@EJB
	private AcumuladoNominaDaoService acumuladoNominaDaoService;

	@EJB
	private CargaFamiliarDaoService cargaFamiliarDaoService;

	@EJB
	private GastoPersonalProyectadoDaoService gastoPersonalProyectadoDaoService;

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.RetencionRentaService#proyectar(java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ResultadoProyeccionIr proyectar(Long idEmpleado, Integer anio, Integer mesDesde,
			String usuario) throws Throwable {
		System.out.println("Ingresa al metodo proyectar de retencionRenta service, empleado: " + idEmpleado
				+ ", anio: " + anio + ", mes desde: " + mesDesde);
		return proyecta(idEmpleado, anio, mesDesde, usuario, false);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.RetencionRentaService#liquidarAnio(java.lang.Long, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ResultadoProyeccionIr liquidarAnio(Long idEmpleado, Integer anio, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo liquidarAnio de retencionRenta service, empleado: " + idEmpleado
				+ ", anio: " + anio);
		// En la liquidacion anual no hay meses futuros: todo lo del anio ya se percibio.
		return proyecta(idEmpleado, anio, Integer.valueOf(ULTIMO_MES), usuario, true);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.RetencionRentaService#proyectarTodos(java.lang.Long, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int proyectarTodos(Long idEmpresa, Integer anio, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo proyectarTodos de retencionRenta service, empresa: " + idEmpresa
				+ ", anio: " + anio);
		LocalDate desde = LocalDate.of(anio.intValue(), 1, 1);
		LocalDate hasta = LocalDate.of(anio.intValue(), ULTIMO_MES, 31);
		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				idEmpresa, desde, hasta);
		int proyectados = 0;
		for (ContratoEmpleado contrato : contratos) {
			// Los servicios profesionales sin dependencia no se proyectan: se les retiene
			// puntualmente sobre el honorario y su comprobante sale por CXC.
			if (SI.equals(contrato.getRetieneFuente())) {
				continue;
			}
			proyectar(contrato.getEmpleado().getCodigo(), anio, Integer.valueOf(1), usuario);
			proyectados++;
		}
		System.out.println("proyectarTodos termino: " + proyectados + " empleados proyectados.");
		return proyectados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.RetencionRentaService#obtenerRetencionMensual(java.lang.Long, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Double obtenerRetencionMensual(Long idEmpleado, Integer anio, Integer mes) throws Throwable {
		System.out.println("Ingresa al metodo obtenerRetencionMensual de retencionRenta service, empleado: "
				+ idEmpleado + ", " + mes + "/" + anio);
		ProyeccionImpuestoRenta vigente = proyeccionImpuestoRentaDaoService.selectVigente(idEmpleado, anio);
		if (vigente == null) {
			// Sin proyeccion no se puede retener: se genera en linea, que es lo que
			// pide el paso 11 del algoritmo de calcularPeriodo.
			System.out.println("No hay proyeccion vigente para el empleado " + idEmpleado
					+ " en " + anio + ": se genera en linea.");
			ResultadoProyeccionIr resultado = proyectar(idEmpleado, anio, mes, "SISTEMA");
			return RedondeoNomina.redondea(resultado.getRetencionMensual());
		}
		return RedondeoNomina.redondea(vigente.getRetencionMensual());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.RetencionRentaService#calcularImpuestoSegunTabla(java.lang.Long, java.lang.Double, java.lang.Integer)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Double calcularImpuestoSegunTabla(Long idEmpresa, Double baseImponible, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo calcularImpuestoSegunTabla, base: " + baseImponible
				+ ", anio: " + anio);
		if (baseImponible == null || baseImponible.doubleValue() <= 0D) {
			return Double.valueOf(0D);
		}
		TablaImpuestoRenta tramo = tablaImpuestoRentaDaoService.selectTramo(idEmpresa, anio, baseImponible);
		if (tramo == null) {
			throw new IncomeException("No existe tabla de impuesto a la renta cargada para el anio " + anio
					+ " en RHH.TBIR, o la base " + baseImponible + " no cae en ningun tramo."
					+ " Ejecute el script 07 o revise los tramos del anio.");
		}
		// impuesto = impuesto sobre la fraccion basica + excedente por el porcentaje del tramo
		Double excedente = Double.valueOf(baseImponible.doubleValue() - tramo.getFraccionBasica().doubleValue());
		Double sobreExcedente = RedondeoNomina.porcentaje(excedente, tramo.getPorcentaje());
		return RedondeoNomina.suma(tramo.getImpuestoFraccionBasica(), sobreExcedente);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.RetencionRentaService#calcularTopeGastosPersonales(java.lang.Long, java.lang.Integer)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Double calcularTopeGastosPersonales(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo calcularTopeGastosPersonales, empleado: " + idEmpleado
				+ ", anio: " + anio);
		Empleado empleado = em.find(Empleado.class, idEmpleado);
		if (empleado == null) {
			throw new IncomeException("No existe el empleado " + idEmpleado + ".");
		}
		Long idEmpresa = empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null;
		ParametroNomina prnm = recuperaParametros(idEmpresa, anio);

		// Enfermedad catastrofica: tope ampliado en numero de canastas, tambien de PRNM.
		if (SI.equals(empleado.getEnfermedadCatastrofica())) {
			if (prnm.getCanastasCatastrofica() == null) {
				throw new IncomeException("El parametro de canastas por enfermedad catastrofica (PRNMCNCT)"
						+ " esta vacio para el anio " + anio + ".");
			}
			return RedondeoNomina.redondea(Double.valueOf(
					prnm.getCanastasCatastrofica().doubleValue() * prnm.getCanastaBasica().doubleValue()));
		}

		Integer cargas = cargaFamiliarDaoService.contarVigentesParaIr(idEmpleado,
				LocalDate.of(anio.intValue(), ULTIMO_MES, 31));
		TopeGastoPersonal tope = topeGastoPersonalDaoService.selectByCargas(idEmpresa, anio, cargas);
		if (tope == null) {
			throw new IncomeException("No existe tope de gastos personales cargado para el anio " + anio
					+ " en RHH.TPGP. Ejecute el script 07.");
		}
		return RedondeoNomina.redondea(Double.valueOf(
				tope.getNumeroCanastas().doubleValue() * prnm.getCanastaBasica().doubleValue()));
	}

	// =====================================================================
	// Nucleo de la proyeccion
	// =====================================================================

	/**
	 * Cuerpo comun de proyectar y liquidarAnio.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @param mesDesde		: Mes desde el que rige
	 * @param usuario		: Usuario que registra
	 * @param liquidacion	: true si es la liquidacion anual, sin meses futuros
	 * @return				: El resultado de la proyeccion
	 * @throws Throwable	: Excepcion
	 */
	private ResultadoProyeccionIr proyecta(Long idEmpleado, Integer anio, Integer mesDesde,
			String usuario, boolean liquidacion) throws Throwable {

		Empleado empleado = em.find(Empleado.class, idEmpleado);
		if (empleado == null) {
			throw new IncomeException("No existe el empleado " + idEmpleado + ".");
		}
		Long idEmpresa = empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null;
		ParametroNomina prnm = recuperaParametros(idEmpresa, anio);

		int mesInicio = mesDesde == null ? 1 : mesDesde.intValue();
		int mesesRestantes = liquidacion ? 0 : (ULTIMO_MES - mesInicio + 1);

		// 1. Ingresos gravados ya percibidos en el anio.
		Double ingresosRealizados = acumuladoNominaDaoService.sumaValor(idEmpleado, anio,
				Long.valueOf(RhhTipoAcumulado.GRAVADO_IR), Integer.valueOf(1),
				Integer.valueOf(liquidacion ? ULTIMO_MES : mesInicio - 1));

		// 2. Ingresos gravados futuros: remuneracion mensual por los meses que faltan.
		Double gravadoMensual = estimaGravadoMensual(idEmpleado, anio, mesInicio);
		Double ingresosFuturos = RedondeoNomina.redondea(Double.valueOf(
				gravadoMensual.doubleValue() * mesesRestantes));

		// 3. Total proyectado.
		Double ingresosProyectados = RedondeoNomina.suma(ingresosRealizados, ingresosFuturos);

		// 4. Aporte personal proyectado: el ya descontado mas el de los meses futuros.
		Double aporteRealizado = acumuladoNominaDaoService.sumaValor(idEmpleado, anio,
				Long.valueOf(RhhTipoAcumulado.APORTE_PERSONAL), Integer.valueOf(1),
				Integer.valueOf(liquidacion ? ULTIMO_MES : mesInicio - 1));
		Double imponibleMensual = estimaImponibleIessMensual(idEmpleado, anio, mesInicio);
		Double aporteFuturo = RedondeoNomina.redondea(Double.valueOf(
				RedondeoNomina.porcentaje(imponibleMensual, prnm.getAportePersonal()).doubleValue()
						* mesesRestantes));
		Double aportePersonalProyectado = RedondeoNomina.suma(aporteRealizado, aporteFuturo);

		// 5. Base imponible.
		Double baseImponible = RedondeoNomina.redondea(Double.valueOf(
				ingresosProyectados.doubleValue() - aportePersonalProyectado.doubleValue()));
		if (baseImponible.doubleValue() < 0D) {
			baseImponible = Double.valueOf(0D);
		}

		// 6. Impuesto causado segun la tabla progresiva.
		Double impuestoCausado = calcularImpuestoSegunTabla(idEmpresa, baseImponible, anio);

		// 7. Tope de gastos personales y 8. rebaja.
		Double tope = calcularTopeGastosPersonales(idEmpleado, anio);
		Double gastosDeclarados = gastoPersonalProyectadoDaoService.sumaVigentes(idEmpleado, anio);
		Double gastoDeducible = Double.valueOf(Math.min(
				gastosDeclarados != null ? gastosDeclarados.doubleValue() : 0D,
				tope != null ? tope.doubleValue() : 0D));
		Double rebaja = RedondeoNomina.porcentaje(gastoDeducible, prnm.getPorcentajeGastosPersonales());

		// 9. Impuesto a pagar, con piso en cero.
		Double impuestoAPagar = RedondeoNomina.redondea(Double.valueOf(
				impuestoCausado.doubleValue() - rebaja.doubleValue()));
		if (impuestoAPagar.doubleValue() < 0D) {
			impuestoAPagar = Double.valueOf(0D);
		}

		// 10. Retencion mensual: lo que falta por retener, repartido en los meses restantes.
		Double retenido = acumuladoNominaDaoService.sumaValor(idEmpleado, anio,
				Long.valueOf(RhhTipoAcumulado.RETENCION_IR), Integer.valueOf(1),
				Integer.valueOf(ULTIMO_MES));
		Double porRetener = RedondeoNomina.redondea(Double.valueOf(
				impuestoAPagar.doubleValue() - (retenido != null ? retenido.doubleValue() : 0D)));
		Double retencionMensual = Double.valueOf(0D);
		if (mesesRestantes > 0 && porRetener.doubleValue() > 0D) {
			retencionMensual = RedondeoNomina.divide(porRetener,
					Double.valueOf((double) mesesRestantes));
		}

		// 11. Desmarcar la anterior e insertar la nueva.
		proyeccionImpuestoRentaDaoService.desmarcaVigentes(idEmpleado, anio);

		ProyeccionImpuestoRenta pyir = new ProyeccionImpuestoRenta();
		pyir.setEmpleado(empleado);
		pyir.setAnio(anio);
		pyir.setMesDesde(Integer.valueOf(mesInicio));
		pyir.setIngresosRealizados(ingresosRealizados);
		pyir.setIngresosFuturos(ingresosFuturos);
		pyir.setIngresosProyectados(ingresosProyectados);
		pyir.setAportePersonalProyectado(aportePersonalProyectado);
		pyir.setBaseImponible(baseImponible);
		pyir.setImpuestoCausado(impuestoCausado);
		pyir.setGastosDeclarados(gastosDeclarados);
		pyir.setTopeGastos(tope);
		pyir.setRebaja(rebaja);
		pyir.setImpuestoAPagar(impuestoAPagar);
		pyir.setRetencionesEfectuadas(retenido);
		pyir.setMesesRestantes(Integer.valueOf(mesesRestantes));
		pyir.setRetencionMensual(retencionMensual);
		pyir.setNumeroCargas(cargaFamiliarDaoService.contarVigentesParaIr(idEmpleado,
				LocalDate.of(anio.intValue(), ULTIMO_MES, 31)));
		pyir.setEnfermedadCatastrofica(SI.equals(empleado.getEnfermedadCatastrofica()) ? SI : NO);
		pyir.setVigente(SI);
		pyir.setMotivo(liquidacion ? "Liquidacion anual" : "Proyeccion desde el mes " + mesInicio);
		pyir.setEstado(Long.valueOf(1L));
		pyir.setFechaRegistro(LocalDateTime.now());
		pyir.setUsuarioRegistro(usuario);
		proyeccionImpuestoRentaDaoService.save(pyir, pyir.getCodigo());

		ResultadoProyeccionIr resultado = new ResultadoProyeccionIr();
		resultado.setIdEmpleado(idEmpleado);
		resultado.setAnio(anio);
		resultado.setIngresosProyectados(ingresosProyectados);
		resultado.setBaseImponible(baseImponible);
		resultado.setImpuestoCausado(impuestoCausado);
		resultado.setGastosDeclarados(gastosDeclarados);
		resultado.setTope(tope);
		resultado.setRebaja(rebaja);
		resultado.setImpuestoAPagar(impuestoAPagar);
		resultado.setMesesRestantes(Integer.valueOf(mesesRestantes));
		resultado.setRetencionMensual(retencionMensual);
		return resultado;
	}

	/**
	 * Estima la remuneracion gravada mensual del empleado. Prefiere el ultimo valor
	 * real calculado y cae al sueldo del contrato cuando aun no hay nomina del anio.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @param mes			: Mes de referencia
	 * @return				: La base gravada mensual estimada
	 * @throws Throwable	: Excepcion
	 */
	private Double estimaGravadoMensual(Long idEmpleado, Integer anio, int mes) throws Throwable {
		Double acumulado = acumuladoNominaDaoService.sumaValor(idEmpleado, anio,
				Long.valueOf(RhhTipoAcumulado.GRAVADO_IR), Integer.valueOf(1), Integer.valueOf(mes - 1));
		if (acumulado != null && acumulado.doubleValue() > 0D && mes > 1) {
			// Promedio de lo ya percibido, que absorbe variaciones de horas extra y bonos.
			return RedondeoNomina.divide(acumulado, Double.valueOf((double) (mes - 1)));
		}
		return sueldoDelContrato(idEmpleado, anio);
	}

	/**
	 * Estima la base imponible del IESS mensual, con el mismo criterio.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @param mes			: Mes de referencia
	 * @return				: La base imponible mensual estimada
	 * @throws Throwable	: Excepcion
	 */
	private Double estimaImponibleIessMensual(Long idEmpleado, Integer anio, int mes) throws Throwable {
		Double acumulado = acumuladoNominaDaoService.sumaValor(idEmpleado, anio,
				Long.valueOf(RhhTipoAcumulado.IMPONIBLE_IESS), Integer.valueOf(1), Integer.valueOf(mes - 1));
		if (acumulado != null && acumulado.doubleValue() > 0D && mes > 1) {
			return RedondeoNomina.divide(acumulado, Double.valueOf((double) (mes - 1)));
		}
		return sueldoDelContrato(idEmpleado, anio);
	}

	/**
	 * Recupera el sueldo base del contrato vigente del empleado.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @return				: El sueldo base, o cero si no hay contrato
	 * @throws Throwable	: Excepcion
	 */
	private Double sueldoDelContrato(Long idEmpleado, Integer anio) throws Throwable {
		Empleado empleado = em.find(Empleado.class, idEmpleado);
		Long idEmpresa = empleado != null && empleado.getEmpresa() != null
				? empleado.getEmpresa().getCodigo() : null;
		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(idEmpresa,
				LocalDate.of(anio.intValue(), 1, 1), LocalDate.of(anio.intValue(), ULTIMO_MES, 31));
		for (ContratoEmpleado contrato : contratos) {
			if (contrato.getEmpleado() != null && idEmpleado.equals(contrato.getEmpleado().getCodigo())) {
				return contrato.getSalarioBase() != null
						? RedondeoNomina.redondea(contrato.getSalarioBase()) : Double.valueOf(0D);
			}
		}
		return Double.valueOf(0D);
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
					+ " y la empresa " + idEmpresa + ". Cargue el anio antes de proyectar.");
		}
		return prnm;
	}
}
