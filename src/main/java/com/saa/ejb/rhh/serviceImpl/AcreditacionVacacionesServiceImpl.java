package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.ejb.rhh.service.AcreditacionVacacionesService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.SaldoVacaciones;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhTipoAcumulado;
import com.saa.rubros.RhhTipoProvision;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de AcreditacionVacacionesService.</p>
 *
 * <h3>La escala de dias</h3>
 *
 * <p>El Art. 69 del Codigo del Trabajo da quince dias por anio cumplido, mas uno adicional
 * por cada anio a partir del quinto, con tope de treinta. Aqui ninguno de esos numeros esta
 * escrito: la formula es</p>
 *
 * <pre>
 * dias = PRNMDIVC + max(0, min(aniosCumplidos - (PRNMANVC - 1), PRNMMXVC - PRNMDIVC))
 * </pre>
 *
 * <p>Con la parametria de 2026 (<code>PRNMDIVC</code>=15, <code>PRNMANVC</code>=5,
 * <code>PRNMMXVC</code>=30) da 15 dias hasta el cuarto anio, 16 al quinto, y sube de uno en
 * uno hasta 30. Si manana cambia la norma, se corrige con un <code>UPDATE</code>.</p>
 *
 * <h3>Consumo FIFO y caducidad</h3>
 *
 * <p>El consumo va del periodo mas antiguo al mas reciente, para que los dias que estan por
 * caducar se gasten primero. La caducidad es a los <code>PRNMCDVC</code> anios (Art. 75 CT).
 * Al acreditar un periodo nuevo se arrastra lo no gozado del anterior, salvo lo caducado.</p>
 */
@Stateless
public class AcreditacionVacacionesServiceImpl implements AcreditacionVacacionesService {

	/** Marca de "si" de las banderas S/N del esquema. */
	private static final String SI = "S";

	/** Marca de "no" de las banderas S/N del esquema. */
	private static final String NO = "N";

	/** Meses de la ventana movil con la que se valora el dia de vacaciones. */
	private static final int MESES_VENTANA_VALOR = 12;

	@PersistenceContext
	private EntityManager em;

	@EJB
	private SaldoVacacionesDaoService saldoVacacionesDaoService;

	@EJB
	private ParametroNominaDaoService parametroNominaDaoService;

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	@EJB
	private AcumuladoNominaDaoService acumuladoNominaDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#acreditar(java.lang.Long, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int acreditar(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo acreditar de acreditacionVacaciones service, empresa: "
				+ idEmpresa + ", corte: " + fechaCorte);

		if (fechaCorte == null) {
			throw new IncomeException("La fecha de corte es obligatoria para acreditar vacaciones.");
		}
		Integer anio = Integer.valueOf(fechaCorte.getYear());
		ParametroNomina prnm = recuperaParametros(idEmpresa, anio);

		// Primero caducan los saldos vencidos: asi el arrastre no lleva dias muertos.
		caducarSaldos(idEmpresa, fechaCorte, usuario);

		int acreditados = 0;
		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				idEmpresa, LocalDate.of(anio.intValue(), 1, 1), fechaCorte);

		for (ContratoEmpleado contrato : contratos) {
			Empleado empleado = contrato.getEmpleado();
			LocalDate ingreso = empleado.getFechaIngreso() != null
					? empleado.getFechaIngreso() : contrato.getFechaInicio();
			if (ingreso == null) {
				System.out.println("Empleado " + empleado.getIdentificacion()
						+ " sin fecha de ingreso: no se puede acreditar su periodo.");
				continue;
			}

			int aniosCumplidos = aniosDeServicio(ingreso, fechaCorte);
			if (aniosCumplidos < 1) {
				// El derecho nace al cumplir el primer anio de servicio.
				continue;
			}

			Double dias = diasQueLeCorresponden(aniosCumplidos, prnm);
			Double arrastre = arrastreDelPeriodoAnterior(empleado.getCodigo(), anio);

			SaldoVacaciones saldo = saldoVacacionesDaoService.selectByEmpleadoYAnio(
					empleado.getCodigo(), anio);
			if (saldo == null) {
				saldo = new SaldoVacaciones();
				saldo.setEmpleado(empleado);
				saldo.setAnio(anio);
				saldo.setDiasUsados(Double.valueOf(0D));
				saldo.setDiasPagados(Double.valueOf(0D));
				saldo.setCaducado(NO);
				saldo.setAperturaMigracion(NO);
				saldo.setEstado(Long.valueOf(1L));
				saldo.setFechaRegistro(LocalDate.now());
			}

			// Idempotencia: se recalculan los dias asignados sin tocar los ya usados.
			Double usados = saldo.getDiasUsados() != null ? saldo.getDiasUsados() : Double.valueOf(0D);
			Double diasAdicionales = RedondeoNomina.redondeaCantidad(Double.valueOf(
					dias.doubleValue() - prnm.getDiasVacaciones().doubleValue()));

			saldo.setFechaInicio(ingreso.withYear(anio.intValue()));
			saldo.setFechaFin(ingreso.withYear(anio.intValue()).plusYears(1).minusDays(1));
			saldo.setDiasAsignados(RedondeoNomina.redondeaCantidad(dias));
			saldo.setDiasAdicionales(diasAdicionales);
			// CORREGIDO 2026-08-27: diasPendientes NO suma el arrastre. Los dias no
			// gozados del anio anterior siguen viviendo en SU PROPIO saldo (el de ese
			// anio), que sigue apareciendo en selectDisponibles hasta que se consuma o
			// caduque. Sumarlos aqui los duplicaba: diasDisponibles recorre TODOS los
			// anios no caducados y sumaba el mismo dia una vez en el anio de origen y
			// otra vez arrastrado al nuevo. diasArrastrados queda como dato informativo
			// -cuanto viene de atras-, no como parte del saldo de este anio. Ver
			// docs/logica-negocio/rhh/CICLO-ACREDITACION-VACACIONES.md.
			saldo.setDiasArrastrados(RedondeoNomina.redondeaCantidad(arrastre));
			saldo.setDiasPendientes(RedondeoNomina.redondeaCantidad(Double.valueOf(
					dias.doubleValue() - usados.doubleValue())));
			saldo.setValorDia(valorDiaVacaciones(empleado.getCodigo(), fechaCorte));
			saldo.setUsuarioRegistro(usuario);
			saldoVacacionesDaoService.save(saldo, saldo.getCodigo());
			acreditados++;
		}

		System.out.println("acreditar termino: " + acreditados + " periodo(s) acreditados.");
		return acreditados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#revertirAcreditacion(java.lang.Long, java.lang.Integer, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int revertirAcreditacion(Long idEmpresa, Integer anio, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo revertirAcreditacion de acreditacionVacaciones service, empresa: "
				+ idEmpresa + ", anio: " + anio);

		if (idEmpresa == null || anio == null) {
			throw new IncomeException("Debe indicar la empresa y el anio a revertir.");
		}

		List<SaldoVacaciones> saldosDelAnio = saldoVacacionesDaoService.selectByEmpresaYAnio(idEmpresa, anio);
		if (saldosDelAnio.isEmpty()) {
			throw new IncomeException("No hay saldos de vacaciones acreditados para la empresa " + idEmpresa
					+ " en el anio " + anio + ": no hay nada que revertir.");
		}

		// Todo o nada: si un solo empleado ya consumio o le pagaron de ese saldo, o el
		// saldo viene de una apertura de migracion (no lo creo esta acreditacion, no es
		// suyo revertirlo), se rechaza la reversion COMPLETA nombrandolos. Nunca un
		// reverso parcial que deje saldos inconsistentes.
		List<String> bloqueados = new ArrayList<String>();
		for (SaldoVacaciones saldo : saldosDelAnio) {
			double usados = saldo.getDiasUsados() != null ? saldo.getDiasUsados().doubleValue() : 0D;
			double pagados = saldo.getDiasPagados() != null ? saldo.getDiasPagados().doubleValue() : 0D;
			Empleado empleado = saldo.getEmpleado();
			String nombre = empleado != null ? (empleado.getApellidos() + " " + empleado.getNombres()) : "?";
			if (usados > 0D || pagados > 0D) {
				bloqueados.add(nombre + " (usados=" + saldo.getDiasUsados() + ", pagados="
						+ saldo.getDiasPagados() + ")");
			} else if (SI.equals(saldo.getAperturaMigracion())) {
				bloqueados.add(nombre + " (saldo de apertura de migracion, no lo creo esta acreditacion)");
			}
		}
		if (!bloqueados.isEmpty()) {
			throw new IncomeException("No se puede revertir la acreditacion " + anio + " de la empresa "
					+ idEmpresa + ": " + bloqueados.size() + " saldo(s) ya tienen movimiento y no se pueden"
					+ " borrar sin perder informacion: " + bloqueados + ". No se revirtio ningun saldo.");
		}

		// Desmarca la caducidad que ESTA acreditacion provoco. acreditar llama a
		// caducarSaldos(fechaCorte del anio que se acredita) antes de acreditar, con
		// anioLimite = anio - PRNMCDVC: es el unico anio que cruza el umbral por primera
		// vez en esta corrida (los anios anteriores a anioLimite ya estaban caducados de
		// corridas previas, y caducarSaldos es idempotente sobre ellos). Revertir sin
		// desmarcar dejaria caducados unos dias que nadie decidio caducar.
		//
		// No hay columna que diga "que corrida caduco este saldo" (a proposito, no se
		// crea sin el DDL del usuario) asi que esto es una inferencia, no un registro
		// exacto: si alguien llamo a POST /sldv/caducar suelto, por fuera de acreditar,
		// con fechaCorte del mismo anio, tambien se desmarca -- son indistinguibles
		// porque ambos calculan el mismo anioLimite con los mismos parametros, y revertir
		// "lo que esta corrida caduco" es correcto para los dos casos por igual.
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa, anio);
		int caducidadesDesmarcadas = 0;
		if (prnm != null && prnm.getAniosCaducidadVacaciones() != null) {
			int anioLimite = anio.intValue() - prnm.getAniosCaducidadVacaciones().intValue();
			for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectByEmpresaYAnio(idEmpresa,
					Integer.valueOf(anioLimite))) {
				if (SI.equals(saldo.getCaducado())) {
					saldo.setCaducado(NO);
					saldo.setUsuarioRegistro(usuario);
					saldoVacacionesDaoService.save(saldo, saldo.getCodigo());
					caducidadesDesmarcadas++;
				}
			}
		}

		int borrados = 0;
		for (SaldoVacaciones saldo : saldosDelAnio) {
			saldoVacacionesDaoService.remove(saldo, saldo.getCodigo());
			borrados++;
		}

		System.out.println("revertirAcreditacion termino: " + borrados + " saldo(s) del anio " + anio
				+ " borrados, " + caducidadesDesmarcadas + " caducidad(es) desmarcada(s) del anio "
				+ (prnm != null && prnm.getAniosCaducidadVacaciones() != null
						? Integer.valueOf(anio.intValue() - prnm.getAniosCaducidadVacaciones().intValue())
						: "N/A") + ".");
		return borrados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#diasDisponibles(java.lang.Long)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Double diasDisponibles(Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo diasDisponibles, empleado: " + idEmpleado);
		Double total = Double.valueOf(0D);
		for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectDisponibles(idEmpleado)) {
			total = Double.valueOf(total.doubleValue()
					+ (saldo.getDiasPendientes() != null ? saldo.getDiasPendientes().doubleValue() : 0D));
		}
		return RedondeoNomina.redondeaCantidad(total);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#consumir(java.lang.Long, java.lang.Double, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void consumir(Long idEmpleado, Double dias, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo consumir de acreditacionVacaciones service, empleado: "
				+ idEmpleado + ", dias: " + dias);

		if (dias == null || dias.doubleValue() <= 0D) {
			return;
		}
		Double disponibles = diasDisponibles(idEmpleado);
		if (disponibles.doubleValue() < dias.doubleValue()) {
			throw new IncomeException("El empleado " + idEmpleado + " tiene " + disponibles
					+ " dia(s) de vacaciones disponibles y se intentan consumir " + dias + ".");
		}

		// FIFO: el DAO devuelve los saldos del anio mas antiguo al mas reciente, de modo
		// que se gastan primero los dias que estan mas cerca de caducar.
		double porConsumir = dias.doubleValue();
		for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectDisponibles(idEmpleado)) {
			if (porConsumir <= 0D) {
				break;
			}
			double pendientes = saldo.getDiasPendientes() != null
					? saldo.getDiasPendientes().doubleValue() : 0D;
			double consume = Math.min(pendientes, porConsumir);
			double usados = saldo.getDiasUsados() != null ? saldo.getDiasUsados().doubleValue() : 0D;

			saldo.setDiasUsados(RedondeoNomina.redondeaCantidad(Double.valueOf(usados + consume)));
			saldo.setDiasPendientes(RedondeoNomina.redondeaCantidad(
					Double.valueOf(pendientes - consume)));
			saldo.setUsuarioRegistro(usuario);
			saldoVacacionesDaoService.save(saldo, saldo.getCodigo());
			porConsumir = porConsumir - consume;
		}
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#revertirConsumo(java.lang.Long, java.lang.Double, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void revertirConsumo(Long idEmpleado, Double dias, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo revertirConsumo de acreditacionVacaciones service, empleado: "
				+ idEmpleado + ", dias: " + dias);

		if (dias == null || dias.doubleValue() <= 0D) {
			return;
		}
		// Orden inverso al consumo: se devuelve primero al periodo mas reciente, que es
		// del que se gasto al final.
		List<SaldoVacaciones> saldos = saldoVacacionesDaoService.selectDisponibles(idEmpleado);
		List<SaldoVacaciones> todos = new ArrayList<SaldoVacaciones>(saldos);
		double porDevolver = dias.doubleValue();

		for (int i = todos.size() - 1; i >= 0 && porDevolver > 0D; i--) {
			SaldoVacaciones saldo = todos.get(i);
			double usados = saldo.getDiasUsados() != null ? saldo.getDiasUsados().doubleValue() : 0D;
			if (usados <= 0D) {
				continue;
			}
			double devuelve = Math.min(usados, porDevolver);
			double pendientes = saldo.getDiasPendientes() != null
					? saldo.getDiasPendientes().doubleValue() : 0D;

			saldo.setDiasUsados(RedondeoNomina.redondeaCantidad(Double.valueOf(usados - devuelve)));
			saldo.setDiasPendientes(RedondeoNomina.redondeaCantidad(
					Double.valueOf(pendientes + devuelve)));
			saldo.setUsuarioRegistro(usuario);
			saldoVacacionesDaoService.save(saldo, saldo.getCodigo());
			porDevolver = porDevolver - devuelve;
		}

		if (porDevolver > 0D) {
			System.out.println("Quedaron " + porDevolver + " dia(s) sin devolver al empleado "
					+ idEmpleado + ": no habia consumo registrado suficiente.");
		}
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#caducarSaldos(java.lang.Long, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public List<String> caducarSaldos(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo caducarSaldos de acreditacionVacaciones service, corte: "
				+ fechaCorte);

		List<String> avisos = new ArrayList<String>();
		Integer anio = Integer.valueOf(fechaCorte.getYear());
		ParametroNomina prnm = recuperaParametros(idEmpresa, anio);
		if (prnm.getAniosCaducidadVacaciones() == null) {
			// Sin plazo parametrizado no se caduca nada: es preferible acumular de mas
			// que borrar un derecho por una suposicion.
			System.out.println("PRNMCDVC no esta parametrizado: no se caduca ningun saldo.");
			return avisos;
		}
		int anioLimite = anio.intValue() - prnm.getAniosCaducidadVacaciones().intValue();

		List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
				idEmpresa, LocalDate.of(anio.intValue(), 1, 1), fechaCorte);
		for (ContratoEmpleado contrato : contratos) {
			Long idEmpleado = contrato.getEmpleado().getCodigo();
			for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectDisponibles(idEmpleado)) {
				if (saldo.getAnio() == null || saldo.getAnio().intValue() > anioLimite) {
					continue;
				}
				avisos.add("Caduco el saldo de vacaciones " + saldo.getAnio() + " de "
						+ contrato.getEmpleado().getApellidos() + " "
						+ contrato.getEmpleado().getNombres() + ": "
						+ saldo.getDiasPendientes() + " dia(s) no gozados.");
				saldo.setCaducado(SI);
				saldo.setUsuarioRegistro(usuario);
				saldoVacacionesDaoService.save(saldo, saldo.getCodigo());
			}
		}

		System.out.println("caducarSaldos termino: " + avisos.size() + " saldo(s) caducados.");
		return avisos;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.AcreditacionVacacionesService#valorDiaVacaciones(java.lang.Long, java.time.LocalDate)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Double valorDiaVacaciones(Long idEmpleado, LocalDate fechaCorte) throws Throwable {
		Empleado empleado = em.find(Empleado.class, idEmpleado);
		Long idEmpresa = empleado != null && empleado.getEmpresa() != null
				? empleado.getEmpresa().getCodigo() : null;
		ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa,
				Integer.valueOf(fechaCorte.getYear()));
		if (prnm == null || prnm.getDiasAnio() == null) {
			return Double.valueOf(0D);
		}

		// -------------------------------------------------------------------------
		// LA VENTANA SALE DE PVNM, NO DE ACMN, Y ESA ES LA CORRECCION DE FONDO
		// -------------------------------------------------------------------------
		// ACMN tipo 7 BASE_VACACIONES tiene UN LECTOR --este metodo-- y CERO
		// ESCRITORES: cerrarPeriodo acumula ocho tipos y ese no esta. La ventana de
		// doce meses no se llenaba "poco a poco": estaba vacia hoy y lo habria seguido
		// estando en 2030, asi que la tarifa salia 0,00 para todo el mundo.
		//
		// La misma base SI esta persistida: el motor la calcula --baseVac, la suma de
		// los renglones con CPNMBSVC-- y la graba como base de la provision de
		// vacaciones, que se genera SIEMPRE PARA TODOS por decision tomada. Leerla de
		// ahi no toca el motor congelado ni pide columna nueva.
		//
		// SOLO PERIODOS CERRADOS: PVNM se escribe al calcular, y un mes calculado y no
		// cerrado no es historia todavia. Es la misma condicion que daba ACMN, y la que
		// impide que un recalculo a medias mueva la tarifa de alguien.
		LocalDate desde = fechaCorte.minusMonths(MESES_VENTANA_VALOR - 1L);
		int claveDesde = desde.getYear() * 100 + desde.getMonthValue();
		int claveHasta = fechaCorte.getYear() * 100 + fechaCorte.getMonthValue();

		List<Object[]> ventana = em.createQuery(" select p.periodoNomina.anio, p.periodoNomina.mes,"
				+ "        sum(p.baseCalculo) "
				+ " from   ProvisionNomina p "
				+ " where  p.empleado.codigo = :idEmpleado "
				+ "        and p.tipoProvision = :tipoVacaciones "
				+ "        and p.periodoNomina.estado = :cerrado "
				+ "        and (p.periodoNomina.anio * 100 + p.periodoNomina.mes) between :desde and :hasta "
				+ " group by p.periodoNomina.anio, p.periodoNomina.mes ", Object[].class)
				.setParameter("idEmpleado", idEmpleado)
				.setParameter("tipoVacaciones", Long.valueOf(RhhTipoProvision.VACACIONES))
				.setParameter("cerrado", Long.valueOf(RhhEstadoPeriodoNomina.CERRADO))
				.setParameter("desde", Integer.valueOf(claveDesde))
				.setParameter("hasta", Integer.valueOf(claveHasta))
				.getResultList();

		int mesesConDatos = ventana.size();
		double baseVentana = 0D;
		for (Object[] fila : ventana) {
			baseVentana += fila[2] != null ? ((Number) fila[2]).doubleValue() : 0D;
		}

		// El divisor son los DIAS EFECTIVAMENTE TRABAJADOS, no meses por treinta. Un mes
		// incompleto --el primero de quien ingresa a mitad de mes-- tiene una base
		// proporcional, y dividirla entre treinta daria una tarifa diaria mas baja de la
		// que esa persona gana de verdad. Con los dias reales, "la tarifa de ese mes" es
		// literalmente la tarifa de ese mes.
		//
		// Los dias salen de ACMN tipo 10, que SI se escribe --es el centinela del cierre--
		// y que los guarda en ACMNDIAS y no en ACMNVLOR, asi que no sirve sumaValor. El
		// filtro de periodo es el MISMO que el de la base, para que las dos sumas cubran
		// exactamente los mismos meses: una base sin sus dias inflaria la tarifa.
		Double diasQuery = em.createQuery(" select sum(a.dias) "
				+ " from   AcumuladoNomina a "
				+ " where  a.empleado.codigo = :idEmpleado "
				+ "        and a.tipoAcumulado = :tipoDias "
				+ "        and a.periodoNomina.estado = :cerrado "
				+ "        and (a.periodoNomina.anio * 100 + a.periodoNomina.mes) between :desde and :hasta ",
				Double.class)
				.setParameter("idEmpleado", idEmpleado)
				.setParameter("tipoDias", Long.valueOf(RhhTipoAcumulado.DIAS_TRABAJADOS))
				.setParameter("cerrado", Long.valueOf(RhhEstadoPeriodoNomina.CERRADO))
				.setParameter("desde", Integer.valueOf(claveDesde))
				.setParameter("hasta", Integer.valueOf(claveHasta))
				.getSingleResult();
		double diasTrabajadosVentana = diasQuery != null ? diasQuery.doubleValue() : 0D;

		Double tarifaVentana = Double.valueOf(0D);
		double diasVentana = 0D;
		if (diasTrabajadosVentana > 0D) {
			tarifaVentana = RedondeoNomina.redondea(
					Double.valueOf(baseVentana / diasTrabajadosVentana),
					RedondeoNomina.DECIMALES_CANTIDAD);
			// Dias de vacaciones que esa historia devengo, con la misma convencion de
			// siempre: dias trabajados por PRNMDIVC/PRNMDANO. Es el peso con el que la
			// ventana entra en el promedio contra el saldo de apertura.
			if (prnm.getDiasVacaciones() != null) {
				diasVentana = diasTrabajadosVentana * prnm.getDiasVacaciones().doubleValue()
						/ prnm.getDiasAnio().doubleValue();
			}
		}

		// Con doce meses reales la ventana se basta: el saldo de apertura ya no cae
		// dentro de ella y sale solo, sin ninguna condicion que lo eche.
		if (mesesConDatos >= MESES_VENTANA_VALOR) {
			return tarifaVentana;
		}

		// -------------------------------------------------------------------------
		// EL SALDO DE APERTURA ES HISTORIA: la anterior al sistema
		// -------------------------------------------------------------------------
		// Mientras la ventana sea parcial, el saldo pondera junto a ella POR SUS DIAS.
		// Desplazarlo en cuanto aparece una fila tiraria la tarifa mezclada de los que
		// tienen adenda anterior al corte, que es justo la que el acta del Ministerio
		// del Trabajo valido en el finiquito de enero.
		//
		// Se pondera por dias y no se toma la de un saldo cualquiera: con varios
		// periodos abiertos cada uno tiene su tarifa, y el ponderado es el unico que
		// devuelve el importe total al multiplicarlo por los dias.
		double diasSaldo = 0D;
		double valorSaldo = 0D;
		for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectDisponibles(idEmpleado)) {
			if (saldo.getDiasPendientes() == null || saldo.getValorDia() == null) {
				continue;
			}
			diasSaldo += saldo.getDiasPendientes().doubleValue();
			valorSaldo += saldo.getDiasPendientes().doubleValue()
					* saldo.getValorDia().doubleValue();
		}

		double diasTotales = diasSaldo + diasVentana;
		if (diasTotales == 0D) {
			System.out.println("valorDiaVacaciones: el empleado " + idEmpleado + " no tiene ni"
					+ " saldo de apertura ni periodos cerrados antes de " + fechaCorte
					+ ". La tarifa sale en cero.");
			return Double.valueOf(0D);
		}
		Double ponderada = RedondeoNomina.redondea(
				Double.valueOf((valorSaldo + diasVentana * tarifaVentana.doubleValue())
						/ diasTotales),
				RedondeoNomina.DECIMALES_CANTIDAD);
		System.out.println("valorDiaVacaciones del empleado " + idEmpleado + " a " + fechaCorte
				+ ": " + ponderada + " (saldo " + diasSaldo + " dias, ventana de " + mesesConDatos
				+ " mes(es) con " + diasVentana + " dias a " + tarifaVentana + ").");
		return ponderada;
	}

	// =====================================================================
	// Apoyo
	// =====================================================================

	/**
	 * Dias que le corresponden a un empleado segun su antiguedad, con la escala de PRNM.
	 *
	 * @param aniosCumplidos	: Anios de servicio cumplidos
	 * @param prnm				: Parametros del anio
	 * @return					: Dias de vacaciones del periodo
	 * @throws Throwable		: IncomeException si falta la escala
	 */
	private Double diasQueLeCorresponden(int aniosCumplidos, ParametroNomina prnm) throws Throwable {
		if (prnm.getDiasVacaciones() == null || prnm.getAnioVacacionAdicional() == null
				|| prnm.getMaxDiasVacaciones() == null) {
			throw new IncomeException("La escala de vacaciones no esta completa en RHH.PRNM del anio "
					+ prnm.getAnio() + ": revise PRNMDIVC, PRNMANVC y PRNMMXVC.");
		}
		int base = prnm.getDiasVacaciones().intValue();
		int anioAdicional = prnm.getAnioVacacionAdicional().intValue();
		int maximo = prnm.getMaxDiasVacaciones().intValue();

		// Un dia adicional por cada anio a partir de PRNMANVC, con tope PRNMMXVC.
		int adicionales = aniosCumplidos - (anioAdicional - 1);
		if (adicionales < 0) {
			adicionales = 0;
		}
		if (adicionales > maximo - base) {
			adicionales = maximo - base;
		}
		return Double.valueOf((double) (base + adicionales));
	}

	/**
	 * Dias no gozados del periodo anterior que se arrastran al nuevo.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio que se esta acreditando
	 * @return				: Dias arrastrados; cero si no hay periodo anterior
	 * @throws Throwable	: Excepcion
	 */
	private Double arrastreDelPeriodoAnterior(Long idEmpleado, Integer anio) throws Throwable {
		SaldoVacaciones anterior = saldoVacacionesDaoService.selectByEmpleadoYAnio(
				idEmpleado, Integer.valueOf(anio.intValue() - 1));
		if (anterior == null || SI.equals(anterior.getCaducado())) {
			return Double.valueOf(0D);
		}
		return anterior.getDiasPendientes() != null
				? anterior.getDiasPendientes() : Double.valueOf(0D);
	}

	/**
	 * Anios de servicio completos entre el ingreso y la fecha de corte.
	 *
	 * @param ingreso		: Fecha de ingreso
	 * @param fechaCorte	: Fecha de corte
	 * @return				: Anios cumplidos
	 */
	private int aniosDeServicio(LocalDate ingreso, LocalDate fechaCorte) {
		int anios = fechaCorte.getYear() - ingreso.getYear();
		if (fechaCorte.getDayOfYear() < ingreso.getDayOfYear()) {
			anios--;
		}
		return anios < 0 ? 0 : anios;
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
					+ " y la empresa " + idEmpresa + ".");
		}
		return prnm;
	}
}
