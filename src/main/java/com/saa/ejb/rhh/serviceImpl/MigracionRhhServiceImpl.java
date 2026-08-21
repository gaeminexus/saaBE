package com.saa.ejb.rhh.serviceImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.ejb.rhh.dao.CuotaDescuentoDaoService;
import com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService;
import com.saa.ejb.rhh.dao.EmpleadoDaoService;
import com.saa.ejb.rhh.dao.SaldoAperturaDaoService;
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.ejb.rhh.service.MigracionRhhService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.AcumuladoNomina;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.CuotaDescuento;
import com.saa.model.rhh.DescuentoRecurrente;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SaldoApertura;
import com.saa.model.rhh.SaldoVacaciones;
import com.saa.model.scp.Empresa;
import com.saa.rubros.RhhEstadoCuotaDescuento;
import com.saa.rubros.RhhEstadoDescuentoRecurrente;
import com.saa.rubros.RhhRolConceptoMotor;
import com.saa.rubros.RhhTipoAcumulado;
import com.saa.rubros.RhhTipoDescuentoRecurrente;
import com.saa.rubros.RhhTipoSaldoApertura;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de MigracionRhhService.</p>
 *
 * <p><b>Formato del archivo</b> (CSV delimitado por punto y coma, UTF-8, una linea de
 * cabecera que se salta):</p>
 *
 * <pre>
 * identificacion;tipoSaldo;valor;dias;fecha;anio;numeroCuotas;numeroReferencia;observacion
 * </pre>
 *
 * <p>Las fechas van en <code>dd/MM/yyyy</code> y los decimales con punto.</p>
 *
 * <p>El concepto de nomina con el que se descuenta un prestamo migrado <b>ya no viaja en
 * el archivo</b>: se resuelve por <code>CPNMROLM</code>, el rol que el catalogo declara
 * para cada concepto (rubro RHH_ROL_CONCEPTO_MOTOR). Antes se pedia una columna
 * <code>codigoConcepto</code> y se guardaba en <code>SLAPOBSR</code> con el prefijo
 * <code>CPNM=</code>, un rodeo que el rol vuelve innecesario.</p>
 *
 * <p>Que materializa cada tipo de saldo (rubro RHH_TIPO_SALDO_APERTURA):</p>
 *
 * <table border="1">
 *   <tr><th>Tipo</th><th>Destino</th></tr>
 *   <tr><td>ANTIGUEDAD</td><td>MPLD.MPLDFCIN</td></tr>
 *   <tr><td>VACACIONES_PENDIENTES</td><td>Un SLDV por periodo con SLDVAPRT='S'</td></tr>
 *   <tr><td>DECIMO_TERCERO_ACUMULADO</td><td>ACMN tipo BASE_DECIMO_TERCERO</td></tr>
 *   <tr><td>DECIMO_CUARTO_ACUMULADO</td><td>ACMN tipo BASE_DECIMO_CUARTO</td></tr>
 *   <tr><td>FONDOS_DE_RESERVA_ACUMULADOS</td><td>ACMN tipo BASE_FONDOS_DE_RESERVA</td></tr>
 *   <tr><td>PRESTAMO_IESS / PRESTAMO_INTERNO</td><td>Un DSRC con DSRCAPRT='S' y sus CTDS</td></tr>
 *   <tr><td>IR_RETENIDO_EN_EL_ANIO</td><td>ACMN tipo RETENCION_IR</td></tr>
 * </table>
 *
 * <p>Cada materializacion graba SLAPRFTB y SLAPRFID para que la reversion sea exacta.</p>
 */
@Stateless
public class MigracionRhhServiceImpl implements MigracionRhhService {

	/** Delimitador del archivo CSV de migracion. */
	private static final String DELIMITADOR = ";";

	/** Patron de fecha del archivo de migracion. */
	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	/** Marca de "si" de las banderas S/N del esquema. */
	private static final String SI = "S";

	/** Marca de "no" de las banderas S/N del esquema. */
	private static final String NO = "N";

	/** Nombres de tabla que se graban en SLAPRFTB para poder revertir. */
	private static final String TABLA_MPLD = "RHH.MPLD";
	private static final String TABLA_SLDV = "RHH.SLDV";
	private static final String TABLA_ACMN = "RHH.ACMN";
	private static final String TABLA_DSRC = "RHH.DSRC";

	/** Longitud de <code>DSRC.DSRCNMRO</code>, el destino del numero de referencia. */
	private static final int LARGO_NUMERO_DESCUENTO = 50;

	/** Meses del anio, para convertir el importe acumulado de un decimo en su base. */
	private static final double MESES_ANIO = 12D;

	@PersistenceContext
	private EntityManager em;

	@EJB
	private SaldoAperturaDaoService saldoAperturaDaoService;

	@EJB
	private EmpleadoDaoService empleadoDaoService;

	@EJB
	private ConceptoNominaDaoService conceptoNominaDaoService;

	@EJB
	private AcumuladoNominaDaoService acumuladoNominaDaoService;

	@EJB
	private SaldoVacacionesDaoService saldoVacacionesDaoService;

	@EJB
	private DescuentoRecurrenteDaoService descuentoRecurrenteDaoService;

	@EJB
	private CuotaDescuentoDaoService cuotaDescuentoDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.MigracionRhhService#cargarSaldosApertura(java.io.InputStream, java.lang.Long, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int cargarSaldosApertura(InputStream archivo, Long idEmpresa, LocalDate fechaCorte,
			String usuario) throws Throwable {
		System.out.println("Ingresa al metodo cargarSaldosApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);

		if (archivo == null) {
			throw new IncomeException("No se recibio ningun archivo de saldos de apertura.");
		}
		if (idEmpresa == null || fechaCorte == null) {
			throw new IncomeException("La empresa y la fecha de corte son obligatorias.");
		}

		Empresa empresa = em.find(Empresa.class, idEmpresa);
		if (empresa == null) {
			throw new IncomeException("No existe la empresa " + idEmpresa + ".");
		}

		int cargados = 0;
		int numeroLinea = 0;
		List<String> errores = new ArrayList<String>();

		BufferedReader lector = new BufferedReader(new InputStreamReader(archivo, StandardCharsets.UTF_8));
		String linea;
		while ((linea = lector.readLine()) != null) {
			numeroLinea++;
			// Primera linea de cabecera y lineas en blanco: se saltan.
			if (numeroLinea == 1 || linea.trim().isEmpty()) {
				continue;
			}
			try {
				SaldoApertura saldo = armaSaldoDesdeLinea(linea, empresa, fechaCorte, usuario);
				saldoAperturaDaoService.save(saldo, saldo.getCodigo());
				cargados++;
			} catch (Throwable e) {
				// Una linea mal formada no aborta la carga: se acumula y se reporta al final,
				// que es el mismo criterio de los importadores del resto del sistema.
				errores.add("Linea " + numeroLinea + ": " + e.getMessage());
			}
		}

		if (!errores.isEmpty()) {
			StringBuilder mensaje = new StringBuilder();
			mensaje.append("El archivo tiene ").append(errores.size())
					.append(" linea(s) con error y no se cargo ninguna:\n");
			for (String error : errores) {
				mensaje.append("  ").append(error).append("\n");
			}
			// Se lanza para que la transaccion revierta: entra el archivo entero o no entra nada.
			throw new IncomeException(mensaje.toString());
		}

		System.out.println("cargarSaldosApertura termino: " + cargados + " saldos cargados.");
		return cargados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.MigracionRhhService#validarSaldosApertura(java.lang.Long, java.time.LocalDate)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> validarSaldosApertura(Long idEmpresa, LocalDate fechaCorte) throws Throwable {
		System.out.println("Ingresa al metodo validarSaldosApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);

		List<String> inconsistencias = new ArrayList<String>();
		List<SaldoApertura> saldos = saldoAperturaDaoService.selectByEmpresaYCorte(idEmpresa, fechaCorte);

		if (saldos == null || saldos.isEmpty()) {
			inconsistencias.add("No hay saldos de apertura cargados para el corte " + fechaCorte + ".");
			return inconsistencias;
		}

		for (SaldoApertura saldo : saldos) {
			String referencia = "Saldo " + saldo.getCodigo() + " (" + saldo.getIdentificacion() + ")";

			// 1. El empleado debe existir y ser unico.
			Empleado empleado = saldo.getEmpleado();
			if (empleado == null) {
				empleado = empleadoDaoService.selectByIdentificacion(saldo.getIdentificacion(), idEmpresa);
			}
			if (empleado == null) {
				inconsistencias.add(referencia + ": no existe un empleado unico con la identificacion "
						+ saldo.getIdentificacion() + ".");
				continue;
			}

			// 2. El tipo de saldo debe estar informado.
			if (saldo.getTipoSaldo() == null) {
				inconsistencias.add(referencia + ": el tipo de saldo esta vacio.");
				continue;
			}
			int tipo = saldo.getTipoSaldo().intValue();

			// 3. Los montos nunca son negativos.
			if (saldo.getValor() != null && saldo.getValor().doubleValue() < 0D) {
				inconsistencias.add(referencia + ": el valor es negativo (" + saldo.getValor() + ").");
			}
			if (saldo.getDias() != null && saldo.getDias().doubleValue() < 0D) {
				inconsistencias.add(referencia + ": los dias son negativos (" + saldo.getDias() + ").");
			}

			// 4. Campos obligatorios segun el tipo de saldo.
			inconsistencias.addAll(validaCamposPorTipo(saldo, tipo, referencia, idEmpresa));
		}

		// 5. Duplicados dentro del mismo corte.
		List<Object[]> duplicados = saldoAperturaDaoService.selectDuplicados(idEmpresa, fechaCorte);
		if (duplicados != null) {
			for (Object[] fila : duplicados) {
				inconsistencias.add("Duplicado: la identificacion " + fila[0] + " tiene " + fila[3]
						+ " filas del tipo de saldo " + fila[1] + " para el anio " + fila[2] + ".");
			}
		}

		System.out.println("validarSaldosApertura termino con " + inconsistencias.size() + " inconsistencia(s).");
		return inconsistencias;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.MigracionRhhService#aplicarSaldosApertura(java.lang.Long, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int aplicarSaldosApertura(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo aplicarSaldosApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);

		// No se materializa nada mientras haya inconsistencias: es el control que
		// justifica que validar y aplicar sean dos pasos separados.
		List<String> inconsistencias = validarSaldosApertura(idEmpresa, fechaCorte);
		if (!inconsistencias.isEmpty()) {
			StringBuilder mensaje = new StringBuilder();
			mensaje.append("No se puede aplicar la migracion: hay ").append(inconsistencias.size())
					.append(" inconsistencia(s) pendiente(s):\n");
			for (String inconsistencia : inconsistencias) {
				mensaje.append("  ").append(inconsistencia).append("\n");
			}
			throw new IncomeException(mensaje.toString());
		}

		List<SaldoApertura> pendientes = saldoAperturaDaoService.selectPendientesPorAplicar(idEmpresa, fechaCorte);
		int aplicados = 0;

		for (SaldoApertura saldo : pendientes) {
			// Idempotencia: un saldo ya aplicado nunca se vuelve a materializar.
			if (SI.equals(saldo.getAplicado())) {
				continue;
			}
			if (saldo.getEmpleado() == null) {
				saldo.setEmpleado(empleadoDaoService.selectByIdentificacion(
						saldo.getIdentificacion(), idEmpresa));
			}

			int tipo = saldo.getTipoSaldo().intValue();
			switch (tipo) {
				case RhhTipoSaldoApertura.ANTIGUEDAD:
					aplicaAntiguedad(saldo);
					break;
				case RhhTipoSaldoApertura.VACACIONES_PENDIENTES:
					aplicaVacaciones(saldo, usuario);
					break;
				case RhhTipoSaldoApertura.DECIMO_TERCERO_ACUMULADO:
					aplicaAcumulado(saldo, RhhTipoAcumulado.BASE_DECIMO_TERCERO, usuario);
					break;
				case RhhTipoSaldoApertura.DECIMO_CUARTO_ACUMULADO:
					aplicaAcumulado(saldo, RhhTipoAcumulado.BASE_DECIMO_CUARTO, usuario);
					break;
				case RhhTipoSaldoApertura.FONDOS_DE_RESERVA_ACUMULADOS:
					aplicaAcumulado(saldo, RhhTipoAcumulado.BASE_FONDOS_DE_RESERVA, usuario);
					break;
				case RhhTipoSaldoApertura.IR_RETENIDO_EN_EL_ANIO:
					aplicaAcumulado(saldo, RhhTipoAcumulado.RETENCION_IR, usuario);
					break;
				case RhhTipoSaldoApertura.PRESTAMO_IESS:
				case RhhTipoSaldoApertura.PRESTAMO_HIPOTECARIO_IESS:
				case RhhTipoSaldoApertura.PRESTAMO_INTERNO:
				case RhhTipoSaldoApertura.ANTICIPO:
					// El tipo de descuento sale de tipoDescuentoDelSaldo, que es la MISMA
					// funcion que usa la validacion: antes cada lado tenia su propia suposicion
					// y las dos daban quirografario para cualquier saldo del IESS.
					aplicaDescuento(saldo, tipoDescuentoDelSaldo(tipo), usuario);
					break;
				default:
					throw new IncomeException("El tipo de saldo de apertura " + tipo
							+ " no tiene materializacion definida (saldo " + saldo.getCodigo() + ").");
			}

			saldo.setAplicado(SI);
			saldo.setFechaAplicacion(LocalDate.now());
			saldoAperturaDaoService.save(saldo, saldo.getCodigo());
			aplicados++;
		}

		System.out.println("aplicarSaldosApertura termino: " + aplicados + " saldos materializados.");
		return aplicados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.MigracionRhhService#revertirSaldosApertura(java.lang.Long, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int revertirSaldosApertura(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo revertirSaldosApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);

		List<SaldoApertura> aplicados = saldoAperturaDaoService.selectAplicados(idEmpresa, fechaCorte);
		int revertidos = 0;

		for (SaldoApertura saldo : aplicados) {
			String tabla = saldo.getTablaReferencia();
			Long idReferencia = saldo.getIdReferencia();

			if (tabla == null || idReferencia == null) {
				// Sin rastro no se puede revertir con exactitud, y adivinar seria peor:
				// se deja marcado para revision manual.
				System.out.println("Saldo " + saldo.getCodigo()
						+ " aplicado sin SLAPRFTB/SLAPRFID: requiere reversion manual.");
				continue;
			}

			if (TABLA_MPLD.equals(tabla)) {
				// La antiguedad es el unico saldo que sobreescribe el maestro en vez de crear
				// una fila, asi que se revierte devolviendo MPLDFCIN a lo que habia, no
				// poniendolo en nulo: limpiarlo dejaba a toda la plantilla sin fecha de ingreso
				// en un ciclo normal de aplicar -> revisar -> revertir, sin un solo error.
				//
				// Un SLAPFCAN nulo significa que el empleado no tenia fecha antes, y entonces
				// nulo es exactamente el valor correcto a restaurar.
				Empleado empleado = em.find(Empleado.class, idReferencia);
				if (empleado != null) {
					LocalDate anterior = saldo.getFechaAnterior();
					System.out.println("Saldo " + saldo.getCodigo() + ": la antiguedad de "
							+ empleado.getIdentificacion() + " vuelve de "
							+ empleado.getFechaIngreso() + " a " + anterior + ".");
					empleado.setFechaIngreso(anterior);
					em.merge(empleado);
				}
			} else if (TABLA_SLDV.equals(tabla)) {
				SaldoVacaciones registro = em.find(SaldoVacaciones.class, idReferencia);
				if (registro != null) {
					em.remove(registro);
				}
			} else if (TABLA_ACMN.equals(tabla)) {
				AcumuladoNomina registro = em.find(AcumuladoNomina.class, idReferencia);
				if (registro != null) {
					em.remove(registro);
				}
			} else if (TABLA_DSRC.equals(tabla)) {
				// Primero las cuotas hijas, despues la cabecera, o la FK lo impide.
				DescuentoRecurrente registro = em.find(DescuentoRecurrente.class, idReferencia);
				if (registro != null) {
					em.createQuery("delete from CuotaDescuento c where c.descuentoRecurrente.codigo = :id")
							.setParameter("id", idReferencia)
							.executeUpdate();
					em.remove(registro);
				}
			} else {
				throw new IncomeException("El saldo " + saldo.getCodigo() + " apunta a la tabla '"
						+ tabla + "', que no tiene reversion definida.");
			}

			saldo.setAplicado(NO);
			saldo.setFechaAplicacion(null);
			saldo.setTablaReferencia(null);
			saldo.setIdReferencia(null);
			// El rastro de la reversion se limpia entero, incluida la fecha anterior: el saldo
			// vuelve a estar pendiente y un aplicar posterior volvera a capturar la que haya
			// en ese momento. Conservarla haria que una segunda reversion restaurase un valor
			// de dos aplicaciones atras.
			saldo.setFechaAnterior(null);
			saldo.setUsuarioRegistro(usuario);
			saldoAperturaDaoService.save(saldo, saldo.getCodigo());
			revertidos++;
		}

		System.out.println("revertirSaldosApertura termino: " + revertidos + " saldos revertidos.");
		return revertidos;
	}

	// =====================================================================
	// Materializacion por tipo de saldo
	// =====================================================================

	/**
	 * ANTIGUEDAD: escribe la fecha de ingreso en el maestro de empleados.
	 *
	 * @param saldo			: Saldo a materializar
	 * @throws Throwable	: Excepcion
	 */
	private void aplicaAntiguedad(SaldoApertura saldo) throws Throwable {
		Empleado empleado = saldo.getEmpleado();
		// El valor previo se guarda ANTES de pisarlo: es lo unico que hace reversible este
		// tipo de saldo. Los otros siete crean una fila propia y se revierten borrandola;
		// este escribe sobre el maestro y no hay nada que borrar. Un nulo aqui significa que
		// el empleado no tenia fecha de ingreso, y revertir se la volvera a dejar en nulo.
		saldo.setFechaAnterior(empleado.getFechaIngreso());
		empleado.setFechaIngreso(saldo.getFecha());
		em.merge(empleado);
		saldo.setTablaReferencia(TABLA_MPLD);
		saldo.setIdReferencia(empleado.getCodigo());
	}

	/**
	 * VACACIONES_PENDIENTES: crea un SLDV del anio indicado, marcado como saldo de
	 * apertura para que se distinga de los acreditados por el proceso normal.
	 *
	 * @param saldo			: Saldo a materializar
	 * @param usuario		: Usuario que registra
	 * @throws Throwable	: Excepcion
	 */
	private void aplicaVacaciones(SaldoApertura saldo, String usuario) throws Throwable {
		SaldoVacaciones registro = new SaldoVacaciones();
		registro.setEmpleado(saldo.getEmpleado());
		registro.setAnio(saldo.getAnio());
		registro.setDiasAsignados(RedondeoNomina.redondeaCantidad(saldo.getDias()));
		registro.setDiasUsados(Double.valueOf(0D));
		registro.setDiasPendientes(RedondeoNomina.redondeaCantidad(saldo.getDias()));
		registro.setDiasAdicionales(Double.valueOf(0D));
		registro.setDiasArrastrados(Double.valueOf(0D));
		registro.setDiasPagados(Double.valueOf(0D));

		// Valor del dia de vacaciones: SALE DEL PROPIO SALDO, valor entre dias, y no del
		// sueldo del contrato.
		//
		// Para la mayoria las dos vias coinciden --Barcenas da 23,33, que es 700/30-- pero en
		// los cuatro que tienen adenda anterior al corte NO: su saldo abarca dos tramos de
		// sueldo, asi que la tarifa correcta es una mezcla de los dos. Torres Chavez da 65,27
		// y no los 66,67 que saldrian de dividir su sueldo actual de 2.000 entre 30. Calcularlo
		// desde el contrato daria el valor equivocado justo en las cuatro personas donde
		// importa, y ademas dejaria de reproducir el importe de apertura.
		//
		// Sin esto valorDia quedaba en nulo en las 22 filas y el valor de las vacaciones de la
		// apertura --3.637,61-- se quedaba en SLAP sin materializarse en ninguna parte.
		// CUATRO decimales, no dos, y no es cosmetica: valorDia es un FACTOR que despues se
		// multiplica por los dias, y a dos decimales el producto no devuelve el importe
		// original. Torres Chavez: 505,83 / 7,75 da 65,2684; guardado como 65,27 y vuelto a
		// multiplicar da 505,84 -- un centavo de mas, en el rubro que el acta del Ministerio
		// del Trabajo fija en 547,50. El script 30 amplio SLDVVLDI a NUMBER(20,4) para esto;
		// usar aqui RedondeoNomina.divide, que redondea a dos, volveria a escribir 65,27 en la
		// siguiente reaplicacion y desharia el script sin que nadie lo notara.
		// El valor admite nulo: validaCamposPorTipo exige dias y anio a un saldo de vacaciones,
		// pero no el importe. Sin importe no hay tarifa que calcular.
		registro.setValorDia(saldo.getValor() != null && saldo.getDias() != null
				&& saldo.getDias().doubleValue() != 0D
				? RedondeoNomina.redondea(Double.valueOf(
						saldo.getValor().doubleValue() / saldo.getDias().doubleValue()),
						RedondeoNomina.DECIMALES_CANTIDAD)
				: Double.valueOf(0D));

		registro.setCaducado(NO);
		registro.setAperturaMigracion(SI);
		registro.setEstado(Long.valueOf(1L));
		registro.setFechaRegistro(LocalDate.now());
		registro.setUsuarioRegistro(usuario);
		registro = saldoVacacionesDaoService.save(registro, registro.getCodigo());
		em.flush();
		saldo.setTablaReferencia(TABLA_SLDV);
		saldo.setIdReferencia(registro.getCodigo());
	}

	/**
	 * Acumulados de decimos, fondos de reserva e impuesto retenido: crea un ACMN del
	 * tipo indicado, sin periodo de nomina (los saldos de apertura no lo tienen).
	 *
	 * @param saldo			: Saldo a materializar
	 * @param tipoAcumulado	: Codigo alterno del detalle del rubro RHH_TIPO_ACUMULADO
	 * @param usuario		: Usuario que registra
	 * @throws Throwable	: Excepcion
	 */
	private void aplicaAcumulado(SaldoApertura saldo, int tipoAcumulado, String usuario) throws Throwable {
		AcumuladoNomina registro = new AcumuladoNomina();
		registro.setEmpleado(saldo.getEmpleado());
		registro.setPeriodoNomina(null);
		registro.setAnio(saldo.getAnio());
		// El acumulado de apertura se ancla al mes de la fecha de corte.
		registro.setMes(Integer.valueOf(saldo.getFechaCorte().getMonthValue()));
		registro.setTipoAcumulado(Long.valueOf(tipoAcumulado));
		// EL SALDO DE APERTURA DEL DECIMO TERCERO ES EL IMPORTE, Y ACMN GUARDA LA BASE.
		//
		// SLAP tipo 3 trae lo que el trabajador tiene acumulado de decimo tercero --Viteri
		// 183,33, que es 2.200/12--, mientras que las filas mensuales de ACMN
		// BASE_DECIMO_TERCERO llevan la REMUNERACION del mes --2.200--. Son unidades
		// distintas en el mismo tipo de acumulado, y quien las suma --calcularDecimoTercero,
		// que divide la suma entre doce-- se come la diferencia sin avisar.
		//
		// Se multiplica por los meses del anio para dejar la base equivalente. Corregido el
		// 2026-08-21; las filas ya cargadas las endereza el script 37.
		//
		// El decimo CUARTO no lleva esta conversion: no se calcula sobre una base de
		// remuneraciones sino sobre el SBU por dias trabajados, y su fila de apertura aporta
		// los dias, no el valor.
		Double valorAcumulado = saldo.getValor();
		if (tipoAcumulado == RhhTipoAcumulado.BASE_DECIMO_TERCERO && valorAcumulado != null) {
			valorAcumulado = Double.valueOf(valorAcumulado.doubleValue() * MESES_ANIO);
		}
		registro.setValor(RedondeoNomina.redondea(valorAcumulado));
		registro.setDias(RedondeoNomina.redondeaCantidad(saldo.getDias()));
		registro.setAperturaMigracion(SI);
		registro.setEstado(Long.valueOf(1L));
		registro.setFechaRegistro(LocalDateTime.now());
		registro.setUsuarioRegistro(usuario);
		registro = acumuladoNominaDaoService.save(registro, registro.getCodigo());
		em.flush();
		saldo.setTablaReferencia(TABLA_ACMN);
		saldo.setIdReferencia(registro.getCodigo());
	}

	/**
	 * Prestamos: crea el DSRC y su tabla de cuotas. El concepto con el que se descuenta
	 * viene del archivo (codigo alterno de CPNM), no de una constante en Java.
	 *
	 * @param saldo			: Saldo a materializar
	 * @param tipoDescuento	: Codigo alterno del detalle del rubro RHH_TIPO_DESCUENTO_RECURRENTE
	 * @param usuario		: Usuario que registra
	 * @throws Throwable	: Excepcion
	 */
	private void aplicaDescuento(SaldoApertura saldo, int tipoDescuento, String usuario) throws Throwable {
		int rolMotor = rolDelDescuento(tipoDescuento);
		ConceptoNomina concepto = conceptoNominaDaoService.selectByRolMotor(
				Integer.valueOf(rolMotor), saldo.getEmpresa().getCodigo());
		if (concepto == null) {
			throw new IncomeException("El saldo " + saldo.getCodigo() + " es un prestamo, pero ningun"
					+ " concepto de RHH.CPNM tiene asignado el rol " + rolMotor
					+ " del rubro RHH_ROL_CONCEPTO_MOTOR para esta empresa."
					+ " Ejecute los UPDATE de rol del script 08.");
		}

		Integer cuotas = saldo.getNumeroCuotas();
		Double saldoPendiente = RedondeoNomina.redondea(saldo.getValor());

		DescuentoRecurrente descuento = new DescuentoRecurrente();
		descuento.setEmpleado(saldo.getEmpleado());
		descuento.setConceptoNomina(concepto);
		descuento.setTipoDescuento(Long.valueOf(tipoDescuento));
		descuento.setNumero(numeroReferencia(saldo));
		descuento.setValor(saldoPendiente);
		descuento.setSaldo(saldoPendiente);
		descuento.setNumeroCuotas(cuotas);
		descuento.setCuotasPagadas(Integer.valueOf(0));
		descuento.setValorCuota(RedondeoNomina.divide(saldoPendiente,
				cuotas != null ? Double.valueOf(cuotas.doubleValue()) : null));
		descuento.setFechaInicio(saldo.getFechaCorte());
		descuento.setAperturaMigracion(SI);
		descuento.setEstado(Long.valueOf(RhhEstadoDescuentoRecurrente.VIGENTE));
		descuento.setFechaRegistro(LocalDateTime.now());
		descuento.setUsuarioRegistro(usuario);
		descuento = descuentoRecurrenteDaoService.save(descuento, descuento.getCodigo());
		em.flush();

		generaCuotas(descuento, saldo, usuario);

		saldo.setTablaReferencia(TABLA_DSRC);
		saldo.setIdReferencia(descuento.getCodigo());
	}

	/**
	 * Numero de referencia del prestamo migrado -- el NUT, en los del IESS.
	 *
	 * <p><b>Sale de <code>SLAPOBSR</code>, que es el campo equivocado</b>, y esto es una red,
	 * no la solucion. <code>SLAPOBSR</code> son «observaciones del saldo», texto libre de 500
	 * caracteres; <code>DSRCNMRO</code> es «numero de referencia del prestamo», estructurado y
	 * de 50. Meter uno en otro es un dato estructurado viajando en el campo de otro, y la
	 * colision no es accidental sino inevitable: 500 no cabe en 50. Una observacion de 65
	 * caracteres tumbo con <code>ORA-12899</code> la aplicacion de los 57 saldos, porque
	 * aplicar es una sola transaccion -- y debe serlo.</p>
	 *
	 * <p><b>Truncar corrompe un NUT</b>, que es la clave con la que el prestamo se concilia
	 * contra el detalle del IESS: un NUT recortado no casa con nada el mes siguiente y no
	 * levanta ningun error. Por eso se trunca <b>con traza</b> y nombrando el valor entero,
	 * para que quede en el log lo que se perdio. Lo que corresponde es que <code>SLAP</code>
	 * tenga su propia columna de referencia; mientras no exista, esto evita que un dato largo
	 * vuelva a tumbar la carga completa.</p>
	 *
	 * @param saldo	: Saldo de apertura del prestamo
	 * @return		: El numero de referencia, recortado a lo que admite la columna destino
	 */
	private String numeroReferencia(SaldoApertura saldo) {
		String referencia = saldo.getObservacion();
		if (referencia == null) {
			return null;
		}
		referencia = referencia.trim();
		if (referencia.length() <= LARGO_NUMERO_DESCUENTO) {
			return referencia;
		}
		System.out.println("AVISO: el saldo " + saldo.getCodigo() + " de "
				+ saldo.getIdentificacion() + " trae una referencia de " + referencia.length()
				+ " caracteres y DSRCNMRO admite " + LARGO_NUMERO_DESCUENTO
				+ ". Se recorta. Si es un NUT del IESS, la conciliacion del mes siguiente NO lo"
				+ " va a encontrar: corrijalo a mano. Valor completo: [" + referencia + "]");
		return referencia.substring(0, LARGO_NUMERO_DESCUENTO);
	}

	/**
	 * Genera la tabla de cuotas de un descuento de apertura. La ultima cuota absorbe la
	 * diferencia de redondeo, de modo que la suma de cuotas iguale exactamente el saldo.
	 *
	 * @param descuento		: Descuento recien creado
	 * @param saldo			: Saldo de apertura que lo origino
	 * @param usuario		: Usuario que registra
	 * @throws Throwable	: Excepcion
	 */
	private void generaCuotas(DescuentoRecurrente descuento, SaldoApertura saldo,
			String usuario) throws Throwable {
		Integer numeroCuotas = descuento.getNumeroCuotas();
		if (numeroCuotas == null || numeroCuotas.intValue() <= 0) {
			return;
		}
		int total = numeroCuotas.intValue();
		Double valorCuota = descuento.getValorCuota();
		Double acumulado = Double.valueOf(0D);
		LocalDate vencimiento = saldo.getFechaCorte();

		for (int numero = 1; numero <= total; numero++) {
			vencimiento = vencimiento.plusMonths(1);
			Double valor;
			if (numero < total) {
				valor = valorCuota;
				acumulado = RedondeoNomina.suma(acumulado, valor);
			} else {
				// Cuota de cierre: lo que falte para completar el saldo exacto.
				valor = RedondeoNomina.redondea(Double.valueOf(
						descuento.getSaldo().doubleValue() - acumulado.doubleValue()));
				acumulado = RedondeoNomina.suma(acumulado, valor);
			}

			CuotaDescuento cuota = new CuotaDescuento();
			cuota.setDescuentoRecurrente(descuento);
			cuota.setNumeroCuota(Integer.valueOf(numero));
			cuota.setFechaVencimiento(vencimiento);
			cuota.setTotal(valor);
			cuota.setCapital(valor);
			cuota.setInteres(Double.valueOf(0D));
			cuota.setValorDescontado(Double.valueOf(0D));
			cuota.setSaldo(RedondeoNomina.redondea(Double.valueOf(
					descuento.getSaldo().doubleValue() - acumulado.doubleValue())));
			cuota.setPeriodoNomina(null);
			cuota.setEstado(Long.valueOf(RhhEstadoCuotaDescuento.PENDIENTE));
			cuota.setFechaRegistro(LocalDateTime.now());
			cuota.setUsuarioRegistro(usuario);
			cuotaDescuentoDaoService.save(cuota, cuota.getCodigo());
		}
	}

	// =====================================================================
	// Apoyo
	// =====================================================================

	/**
	 * Arma un SaldoApertura a partir de una linea del CSV.
	 *
	 * @param linea			: Linea del archivo
	 * @param empresa		: Empresa de la migracion
	 * @param fechaCorte	: Fecha de corte
	 * @param usuario		: Usuario que registra
	 * @return				: El saldo sin persistir
	 * @throws Throwable	: IncomeException si la linea esta mal formada
	 */
	private SaldoApertura armaSaldoDesdeLinea(String linea, Empresa empresa, LocalDate fechaCorte,
			String usuario) throws Throwable {
		String[] campos = linea.split(DELIMITADOR, -1);
		if (campos.length < 7) {
			throw new IncomeException("se esperaban al menos 7 columnas separadas por '"
					+ DELIMITADOR + "' y llegaron " + campos.length);
		}

		SaldoApertura saldo = new SaldoApertura();
		saldo.setEmpresa(empresa);
		saldo.setFechaCorte(fechaCorte);
		saldo.setIdentificacion(texto(campos, 0));
		saldo.setTipoSaldo(entero(campos, 1, "tipoSaldo") != null
				? Long.valueOf(entero(campos, 1, "tipoSaldo").longValue()) : null);
		saldo.setValor(decimal(campos, 2, "valor"));
		saldo.setDias(decimal(campos, 3, "dias"));
		saldo.setFecha(fecha(campos, 4));
		saldo.setAnio(entero(campos, 5, "anio"));
		saldo.setNumeroCuotas(entero(campos, 6, "numeroCuotas"));
		// El codigo de concepto viaja en la observacion cuando no hay columna propia;
		// se guarda tal cual para que aplicaDescuento lo resuelva contra CPNM.
		saldo.setObservacion(armaObservacion(campos));
		saldo.setAplicado(NO);
		saldo.setEstado(Long.valueOf(1L));
		saldo.setFechaRegistro(LocalDateTime.now());
		saldo.setUsuarioRegistro(usuario);

		if (saldo.getIdentificacion() == null || saldo.getIdentificacion().isEmpty()) {
			throw new IncomeException("la identificacion es obligatoria");
		}
		if (saldo.getTipoSaldo() == null) {
			throw new IncomeException("el tipo de saldo es obligatorio");
		}
		return saldo;
	}

	/**
	 * Compone la observacion conservando el numero de referencia del prestamo que
	 * viene en el archivo. El concepto de nomina ya no viaja aqui: se resuelve por el
	 * rol del catalogo.
	 *
	 * @param campos	: Campos de la linea
	 * @return			: Texto de la observacion
	 */
	private String armaObservacion(String[] campos) {
		StringBuilder observacion = new StringBuilder();
		String numeroReferencia = texto(campos, 7);
		String libre = texto(campos, 8);
		if (numeroReferencia != null && !numeroReferencia.isEmpty()) {
			observacion.append("REF=").append(numeroReferencia).append(";");
		}
		if (libre != null && !libre.isEmpty()) {
			observacion.append(libre);
		}
		return observacion.length() == 0 ? null : observacion.toString();
	}

	/**
	 * Traduce el tipo de descuento recurrente al rol del motor con el que se localiza
	 * el concepto de nomina en RHH.CPNM.
	 *
	 * <p>Antes el concepto venia en una columna del archivo y se guardaba en SLAPOBSR
	 * con el prefijo CPNM=. Con el rol del rubro 221 esa columna deja de hacer falta:
	 * el catalogo declara que concepto cumple cada papel.</p>
	 *
	 * @param tipoDescuento	: Codigo alterno del detalle del rubro RHH_TIPO_DESCUENTO_RECURRENTE
	 * @return				: Codigo alterno del detalle del rubro RHH_ROL_CONCEPTO_MOTOR
	 * @throws Throwable	: IncomeException si el tipo no tiene rol equivalente
	 */
	private int rolDelDescuento(int tipoDescuento) throws Throwable {
		switch (tipoDescuento) {
			case RhhTipoDescuentoRecurrente.PRESTAMO_QUIROGRAFARIO_IESS:
				return RhhRolConceptoMotor.PRESTAMO_QUIROGRAFARIO;
			case RhhTipoDescuentoRecurrente.PRESTAMO_HIPOTECARIO_IESS:
				return RhhRolConceptoMotor.PRESTAMO_HIPOTECARIO;
			case RhhTipoDescuentoRecurrente.ANTICIPO_DE_SUELDO:
				return RhhRolConceptoMotor.ANTICIPO_DE_SUELDO;
			case RhhTipoDescuentoRecurrente.PRESTAMO_INTERNO:
				return RhhRolConceptoMotor.PRESTAMO_INTERNO;
			case RhhTipoDescuentoRecurrente.RETENCION_JUDICIAL:
				return RhhRolConceptoMotor.RETENCION_JUDICIAL;
			default:
				throw new IncomeException("El tipo de descuento recurrente " + tipoDescuento
						+ " no tiene un rol equivalente en el rubro RHH_ROL_CONCEPTO_MOTOR.");
		}
	}

	/**
	 * Valida los campos obligatorios de cada tipo de saldo.
	 *
	 * @param saldo			: Saldo a validar
	 * @param tipo			: Codigo alterno del tipo de saldo
	 * @param referencia	: Texto identificador del saldo para los mensajes
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Inconsistencias encontradas; vacio si esta correcto
	 * @throws Throwable	: Excepcion
	 */
	private List<String> validaCamposPorTipo(SaldoApertura saldo, int tipo, String referencia,
			Long idEmpresa) throws Throwable {
		List<String> errores = new ArrayList<String>();

		switch (tipo) {
			case RhhTipoSaldoApertura.ANTIGUEDAD:
				if (saldo.getFecha() == null) {
					errores.add(referencia + ": la antiguedad necesita la fecha de ingreso en la columna 'fecha'.");
				} else if (saldo.getEmpleado() != null
						&& saldo.getEmpleado().getFechaIngreso() != null
						&& !saldo.getEmpleado().getFechaIngreso().equals(saldo.getFecha())) {
					// Aplicar SOBREESCRIBE MPLDFCIN. Si el maestro ya trae una fecha distinta,
					// se avisa antes en vez de pisarla en silencio: de la antiguedad salen los
					// fondos de reserva, el decimo cuarto proporcional y los anios de servicio
					// del finiquito, asi que un cambio inadvertido no se manifiesta como error
					// sino como cuatro calculos distintos.
					errores.add(referencia + ": la antiguedad cargada (" + saldo.getFecha()
							+ ") no coincide con la fecha de ingreso que ya tiene el empleado ("
							+ saldo.getEmpleado().getFechaIngreso() + "). Aplicar la sustituira."
							+ " Confirme cual es la correcta antes de continuar.");
				}
				break;
			case RhhTipoSaldoApertura.VACACIONES_PENDIENTES:
				if (saldo.getDias() == null) {
					errores.add(referencia + ": las vacaciones pendientes necesitan los dias.");
				}
				if (saldo.getAnio() == null) {
					errores.add(referencia + ": las vacaciones pendientes necesitan el anio del periodo.");
				}
				break;
			case RhhTipoSaldoApertura.DECIMO_TERCERO_ACUMULADO:
			case RhhTipoSaldoApertura.DECIMO_CUARTO_ACUMULADO:
			case RhhTipoSaldoApertura.FONDOS_DE_RESERVA_ACUMULADOS:
			case RhhTipoSaldoApertura.IR_RETENIDO_EN_EL_ANIO:
				if (saldo.getValor() == null) {
					errores.add(referencia + ": el acumulado necesita el valor.");
				}
				if (saldo.getAnio() == null) {
					errores.add(referencia + ": el acumulado necesita el anio.");
				}
				break;
			case RhhTipoSaldoApertura.PRESTAMO_IESS:
			case RhhTipoSaldoApertura.PRESTAMO_HIPOTECARIO_IESS:
			case RhhTipoSaldoApertura.PRESTAMO_INTERNO:
			case RhhTipoSaldoApertura.ANTICIPO:
				if (saldo.getValor() == null || saldo.getValor().doubleValue() == 0D) {
					errores.add(referencia + ": el prestamo necesita el saldo pendiente.");
				}
				if (saldo.getNumeroCuotas() == null || saldo.getNumeroCuotas().intValue() <= 0) {
					errores.add(referencia + ": el prestamo necesita el numero de cuotas pendientes.");
				}
				errores.addAll(validaConceptoDelPrestamo(saldo, tipo, referencia, idEmpresa));
				break;
			default:
				errores.add(referencia + ": el tipo de saldo " + tipo
						+ " no corresponde a ningun detalle del rubro RHH_TIPO_SALDO_APERTURA.");
				break;
		}
		return errores;
	}

	/**
	 * Comprueba que el prestamo indique un concepto de nomina existente.
	 *
	 * @param saldo			: Saldo a validar
	 * @param referencia	: Texto identificador del saldo para los mensajes
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Inconsistencias encontradas; vacio si esta correcto
	 */
	/**
	 * Traduce un tipo de saldo de apertura al tipo de descuento recurrente con el que se
	 * materializa.
	 *
	 * <p><b>Es el unico sitio donde vive esa correspondencia</b>, y ese es el punto. Antes
	 * estaba duplicada: el switch de aplicar y <code>validaConceptoDelPrestamo</code> tenian
	 * cada uno su version, las dos suponian quirografario para cualquier saldo del IESS, y un
	 * hipotecario migrado acababa en el concepto del quirografario <b>sin que la validacion
	 * avisara</b> --porque validaba contra la misma suposicion equivocada--. Dos copias de una
	 * regla no se contradicen entre si: se equivocan juntas.</p>
	 *
	 * <p>El catalogo de saldos era ademas mas grueso que el de descuentos --dos clases de
	 * prestamo contra cinco de descuento--, asi que el anticipo no tenia tipo propio y habia que
	 * migrarlo como prestamo interno. El script 29 los iguala.</p>
	 *
	 * @param tipoSaldo	: Codigo alterno del detalle del rubro RHH_TIPO_SALDO_APERTURA
	 * @return			: Codigo alterno del detalle del rubro RHH_TIPO_DESCUENTO_RECURRENTE
	 */
	private int tipoDescuentoDelSaldo(int tipoSaldo) {
		switch (tipoSaldo) {
			case RhhTipoSaldoApertura.PRESTAMO_IESS:
				return RhhTipoDescuentoRecurrente.PRESTAMO_QUIROGRAFARIO_IESS;
			case RhhTipoSaldoApertura.PRESTAMO_HIPOTECARIO_IESS:
				return RhhTipoDescuentoRecurrente.PRESTAMO_HIPOTECARIO_IESS;
			case RhhTipoSaldoApertura.ANTICIPO:
				return RhhTipoDescuentoRecurrente.ANTICIPO_DE_SUELDO;
			default:
				return RhhTipoDescuentoRecurrente.PRESTAMO_INTERNO;
		}
	}

	private List<String> validaConceptoDelPrestamo(SaldoApertura saldo, int tipo, String referencia,
			Long idEmpresa) {
		List<String> errores = new ArrayList<String>();
		try {
			int rolMotor = rolDelDescuento(tipoDescuentoDelSaldo(tipo));
			ConceptoNomina concepto = conceptoNominaDaoService.selectByRolMotor(
					Integer.valueOf(rolMotor), idEmpresa);
			if (concepto == null) {
				errores.add(referencia + ": ningun concepto de RHH.CPNM tiene el rol " + rolMotor
						+ " del rubro RHH_ROL_CONCEPTO_MOTOR para esta empresa."
						+ " Ejecute los UPDATE de rol del script 08.");
			}
		} catch (Throwable e) {
			errores.add(referencia + ": " + e.getMessage());
		}
		return errores;
	}

	/**
	 * Devuelve el campo de la posicion indicada, o null si no existe o esta vacio.
	 *
	 * @param campos	: Campos de la linea
	 * @param posicion	: Posicion base cero
	 * @return			: El texto, o null
	 */
	private String texto(String[] campos, int posicion) {
		if (posicion >= campos.length) {
			return null;
		}
		String valor = campos[posicion].trim();
		return valor.isEmpty() ? null : valor;
	}

	/**
	 * Convierte el campo a entero.
	 *
	 * @param campos		: Campos de la linea
	 * @param posicion		: Posicion base cero
	 * @param nombre		: Nombre de la columna, para el mensaje de error
	 * @return				: El entero, o null si el campo viene vacio
	 * @throws Throwable	: IncomeException si no es un numero
	 */
	private Integer entero(String[] campos, int posicion, String nombre) throws Throwable {
		String valor = texto(campos, posicion);
		if (valor == null) {
			return null;
		}
		try {
			return Integer.valueOf(valor);
		} catch (NumberFormatException e) {
			throw new IncomeException("la columna '" + nombre + "' no es un numero entero: '" + valor + "'");
		}
	}

	/**
	 * Convierte el campo a decimal, aceptando coma o punto como separador.
	 *
	 * @param campos		: Campos de la linea
	 * @param posicion		: Posicion base cero
	 * @param nombre		: Nombre de la columna, para el mensaje de error
	 * @return				: El decimal, o null si el campo viene vacio
	 * @throws Throwable	: IncomeException si no es un numero
	 */
	private Double decimal(String[] campos, int posicion, String nombre) throws Throwable {
		String valor = texto(campos, posicion);
		if (valor == null) {
			return null;
		}
		try {
			return Double.valueOf(valor.replace(",", "."));
		} catch (NumberFormatException e) {
			throw new IncomeException("la columna '" + nombre + "' no es un numero: '" + valor + "'");
		}
	}

	/**
	 * Convierte el campo a fecha con el patron dd/MM/yyyy.
	 *
	 * @param campos		: Campos de la linea
	 * @param posicion		: Posicion base cero
	 * @return				: La fecha, o null si el campo viene vacio
	 * @throws Throwable	: IncomeException si no respeta el patron
	 */
	private LocalDate fecha(String[] campos, int posicion) throws Throwable {
		String valor = texto(campos, posicion);
		if (valor == null) {
			return null;
		}
		try {
			return LocalDate.parse(valor, FORMATO_FECHA);
		} catch (Throwable e) {
			throw new IncomeException("la fecha '" + valor + "' no respeta el formato dd/MM/yyyy");
		}
	}
}
