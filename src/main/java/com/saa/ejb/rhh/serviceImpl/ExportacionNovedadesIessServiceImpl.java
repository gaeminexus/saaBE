package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FacturadorDaoService;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.ejb.rhh.dao.NovedadIessDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.ExportacionNovedadesIessService;
import com.saa.model.cxc.Facturador;
import com.saa.model.rhh.ArchivoBatchIess;
import com.saa.model.rhh.ConfiguracionNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.NovedadIess;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.rubros.RhhCausaVariacionIess;
import com.saa.rubros.RhhCodigoSeguroSocialIess;
import com.saa.rubros.RhhEstadoNovedadIess;
import com.saa.rubros.RhhOrigenPagoIess;
import com.saa.rubros.RhhTipoNovedadIess;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de ExportacionNovedadesIessService.</p>
 *
 * <p>El formato sale del anexo oficial del portal y esta transcrito en
 * <code>NORMATIVA-IESS-NOVEDADES.md</code> 2.2. Los codigos de un digito
 * --jornada, seguro social, origen de pago, causa de salida, causa de variacion-- se leen
 * de <code>PDTRVLRV</code> de los rubros 225 a 230: <b>ninguno esta escrito aqui</b>, de
 * modo que un cambio de codificacion del IESS es un UPDATE y no un despliegue.</p>
 */
@Stateless
public class ExportacionNovedadesIessServiceImpl implements ExportacionNovedadesIessService {

	/** Separador de campos del archivo batch. */
	private static final String SEPARADOR = ";";

	/** Fin de linea del archivo batch. */
	private static final String FIN_LINEA = "\r\n";

	/** Marca del catalogo cuando el codigo del anexo del portal todavia no se pudo leer. */
	private static final String SIN_RESOLVER = "?";

	/** Formato de fecha del archivo. */
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

	/** Formato de periodo de fondos de reserva. */
	private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyy-MM");

	/** Tipo de periodo del registro de fondos de reserva mensual. */
	private static final String TIPO_PERIODO_GENERAL = "G";

	/**
	 * Centinela que marca el tipo de empleador como PROVISIONAL.
	 *
	 * <p>No es normativa ni catalogo: el tipo de empleador <b>lo asigna el IESS a cada
	 * empresa</b>. Este valor existe solo para reconocer que el codigo real todavia no se
	 * sabe, y esta elegido para que no pueda confundirse con uno verdadero.</p>
	 *
	 * <p><b>Por que no un codigo plausible como "2".</b> Un centinela que coincide con un
	 * codigo real posible falla en las dos direcciones: si el codigo verdadero resultara ser
	 * ese, el aviso no se apagaria nunca sobre un dato correcto --y un aviso que siempre
	 * grita acaba ignorado--; y mientras tanto produciria un archivo <b>plausible</b>, que
	 * alguien puede subir y el IESS aceptar, dejando a la empresa declarada con un tipo de
	 * empleador equivocado y sin que nada avise. Con este valor el portal rechaza el archivo
	 * de forma obvia, que es mejor fallo que uno silencioso, y el centinela se apaga solo el
	 * dia que entre el codigo real.</p>
	 */
	private static final String TIPO_EMPLEADOR_PROVISIONAL = "PROV";

	/**
	 * Lo que va en una fecha que no aplica.
	 *
	 * <p><b>En este formato el hueco se escribe, no se omite.</b> Un campo opcional vacio
	 * --dos puntos y coma seguidos, o un punto y coma al final de la linea-- hace que el
	 * IESS rechace el archivo <b>entero</b>, no ese registro. La fecha de fallecimiento del
	 * aviso de salida es el caso: solo lleva valor cuando la causa es la muerte del
	 * trabajador, y en todos los demas va con ceros.</p>
	 */
	private static final String FECHA_NO_APLICA = "00000000";

	@EJB
	private NovedadIessDaoService novedadIessDaoService;

	@EJB
	private ConfiguracionNominaDaoService configuracionNominaDaoService;

	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private FacturadorDaoService facturadorDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ExportacionNovedadesIessService#generarArchivo(java.lang.Long, java.lang.Long, java.time.LocalDate, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ArchivoBatchIess generarArchivo(Long idPeriodo, Long tipoNovedad, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarArchivo de exportacionNovedadesIess, tipo: "
				+ tipoNovedad + ", periodo: " + idPeriodo);

		PeriodoNomina periodo = recuperaPeriodo(idPeriodo);
		Long idEmpresa = periodo.getEmpresa().getCodigo();
		LocalDate desde = periodo.getFechaInicio();
		LocalDate hasta = periodo.getFechaFin();

		String codigoBatch = codigoBatchDelTipo(tipoNovedad);

		// Solo PENDIENTE: una novedad ya enviada no se vuelve a mandar. El portal admite un
		// envio por tipo y mes, asi que reexportar lo ya enviado seria duplicarlo.
		List<NovedadIess> novedades = novedadIessDaoService.selectByTipoEnVentana(idEmpresa,
				tipoNovedad, desde, hasta, List.of(Long.valueOf(RhhEstadoNovedadIess.PENDIENTE)));
		if (novedades == null || novedades.isEmpty()) {
			throw new IncomeException("No hay novedades PENDIENTE del tipo " + tipoNovedad
					+ " con fecha de hecho entre " + desde + " y " + hasta
					+ ". No se genera un archivo vacio.");
		}

		ConfiguracionNomina configuracion = configuracionNominaDaoService.selectByEmpresa(idEmpresa);
		Cabecera cabecera = leeCabecera(configuracion, idEmpresa, desde);

		// SE VALIDA TODO ANTES DE ESCRIBIR NADA. Un archivo a medias es peor que ninguno:
		// se sube, el IESS lo rechaza entero, y el rechazo llega dias despues.
		List<String> problemas = new ArrayList<String>();
		List<String> lineas = new ArrayList<String>();
		List<NovedadIess> selladas = new ArrayList<NovedadIess>();

		for (NovedadIess novedad : novedades) {
			try {
				lineas.add(armaLinea(novedad, tipoNovedad, codigoBatch, cabecera));
				selladas.add(novedad);
			} catch (Throwable e) {
				problemas.add(quien(novedad) + ": " + e.getMessage());
			}
		}

		if (!problemas.isEmpty()) {
			throw new IncomeException("No se genera el archivo " + codigoBatch + ": hay "
					+ problemas.size() + " novedad(es) con datos incompletos. Corrijalas y vuelva a"
					+ " intentarlo. " + String.join(" | ", problemas));
		}

		// Sellado: el codigo que quedo en el archivo es el que viajo, asi que se graba ahora
		// y no al marcar enviada. Ver la nota de reasignaCausaIess en NovedadIessServiceImpl.
		for (NovedadIess novedad : selladas) {
			novedadIessDaoService.save(novedad, novedad.getCodigo());
		}

		StringBuilder contenido = new StringBuilder();
		for (String linea : lineas) {
			contenido.append(linea).append(FIN_LINEA);
		}

		ArchivoBatchIess archivo = new ArchivoBatchIess();
		archivo.setContenido(contenido.toString());
		archivo.setRegistros(Integer.valueOf(lineas.size()));
		archivo.setNombre(codigoBatch + "_" + desde.format(PERIODO) + ".txt");

		// LA PROTECCION SE QUEDA EN EL SOFTWARE, NO SE MUDA AL PROCESO.
		//
		// Mientras el tipo de empleador sea el provisional del ejemplo oficial y no el que
		// el IESS le asigno a esta empresa, el archivo se genera --hace falta para probar--
		// pero no se puede subir. Si aqui no quedara constancia, lo unico que impediria
		// subirlo seria que alguien se acuerde, y eso no es una proteccion.
		//
		// El aviso viaja en el resultado Y en el nombre del archivo, porque el cliente lo
		// descarga como blob y el nombre es lo unico que el usuario ve con seguridad.
		if (TIPO_EMPLEADOR_PROVISIONAL.equals(cabecera.tipoEmpleador)) {
			archivo.setNoSubir(true);
			archivo.setAviso("TIPO DE EMPLEADOR PROVISIONAL - NO SUBIR AL PORTAL. CFNMTPEM lleva el"
					+ " centinela '" + TIPO_EMPLEADOR_PROVISIONAL + "' porque el codigo que el IESS"
					+ " asigno a esta empresa todavia no se sabe. El archivo sirve para probar el"
					+ " formato y el portal lo rechazara si se sube. Complete CFNMTPEM en RHH.CFNM"
					+ " con el codigo real y este aviso desaparece solo.");
			archivo.setNombre("NO-SUBIR_" + archivo.getNombre());
			System.out.println("Aviso: archivo generado con tipo de empleador PROVISIONAL; no subir.");
		}

		System.out.println("generarArchivo termino: " + lineas.size() + " registros de tipo " + codigoBatch);
		return archivo;
	}

	/**
	 * Recupera el periodo y falla con mensaje explicito si no existe o esta incompleto.
	 *
	 * @param idPeriodo		: Id del periodo
	 * @return				: El periodo
	 * @throws Throwable	: IncomeException si no existe o no tiene empresa o fechas
	 */
	private PeriodoNomina recuperaPeriodo(Long idPeriodo) throws Throwable {
		PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodo,
				NombreEntidadesRhh.PERIODO_NOMINA);
		if (periodo == null) {
			throw new IncomeException("No existe el periodo de nomina " + idPeriodo + ".");
		}
		if (periodo.getEmpresa() == null || periodo.getFechaInicio() == null
				|| periodo.getFechaFin() == null) {
			throw new IncomeException("El periodo " + idPeriodo + " no tiene empresa o fechas, y de"
					+ " ahi sale la ventana de novedades del archivo.");
		}
		return periodo;
	}

	// =====================================================================
	// Armado de cada tipo de registro
	// =====================================================================

	/**
	 * Arma la linea del archivo segun el tipo de novedad.
	 *
	 * @param novedad		: Novedad a exportar
	 * @param tipoNovedad	: Codigo alterno del rubro 204
	 * @param codigoBatch	: Codigo de tres letras del archivo
	 * @param cabecera		: Datos comunes del empleador
	 * @return				: La linea, sin fin de linea
	 * @throws Throwable	: IncomeException si falta un dato obligatorio
	 */
	private String armaLinea(NovedadIess novedad, Long tipoNovedad, String codigoBatch,
			Cabecera cabecera) throws Throwable {
		StringBuilder linea = new StringBuilder();
		linea.append(cabecera.ruc).append(SEPARADOR)
				.append(cabecera.sucursal).append(SEPARADOR)
				.append(cabecera.anio).append(SEPARADOR)
				.append(cabecera.mes).append(SEPARADOR)
				.append(codigoBatch).append(SEPARADOR)
				.append(exige(cedula(novedad), "la cedula del empleado"));

		int tipo = tipoNovedad.intValue();
		switch (tipo) {
			case RhhTipoNovedadIess.AVISO_DE_ENTRADA:
				armaEntrada(linea, novedad, cabecera);
				break;
			case RhhTipoNovedadIess.AVISO_DE_SALIDA:
				armaSalida(linea, novedad);
				break;
			case RhhTipoNovedadIess.MODIFICACION_DE_SUELDO:
			case RhhTipoNovedadIess.CAMBIO_DE_JORNADA:
				linea.append(SEPARADOR).append(importe(exige(novedad.getSueldoNuevo(), "el sueldo nuevo")));
				break;
			case RhhTipoNovedadIess.VARIACION_POR_EXTRAS:
				armaVariacion(linea, novedad);
				break;
			case RhhTipoNovedadIess.NOVEDAD_FONDOS_DE_RESERVA:
				armaFondosReserva(linea, novedad);
				break;
			default:
				throw new IncomeException("El tipo de novedad " + tipoNovedad
						+ " no tiene formato de archivo batch definido.");
		}
		String armada = linea.toString();
		exigeSinHuecos(armada);
		return armada;
	}

	/**
	 * Comprueba que ningun campo de la linea haya quedado vacio.
	 *
	 * <p>Es una guarda estructural, no una validacion mas: <b>un solo campo vacio hace que
	 * el IESS rechace el archivo entero</b>, y el rechazo llega dias despues sin decir cual
	 * era el registro. Cada campo opcional que se anada en el futuro tiene que decidir con
	 * que se rellena --ceros en las fechas, y lo que diga el anexo en los demas--; si
	 * alguien olvida hacerlo, esto lo para aqui en vez de alla.</p>
	 *
	 * @param linea			: Linea ya armada
	 * @throws Throwable	: IncomeException si algun campo esta vacio
	 */
	private void exigeSinHuecos(String linea) throws Throwable {
		String[] campos = linea.split(SEPARADOR, -1);
		for (int i = 0; i < campos.length; i++) {
			if (campos[i] == null || campos[i].trim().isEmpty()) {
				throw new IncomeException("el campo " + (i + 1) + " del registro quedo vacio, y en"
						+ " este formato un hueco hace que el IESS rechace el archivo entero."
						+ " Lo que no aplica se escribe --las fechas con ceros--, no se omite");
			}
		}
	}

	/**
	 * Aviso de entrada. Es el registro con mas campos y el que mas datos exige del
	 * contrato: sin codigo sectorial o sin relacion de trabajo, el IESS lo rechaza.
	 *
	 * @param linea			: Linea en construccion
	 * @param novedad		: Novedad
	 * @param cabecera		: Datos del empleador
	 * @throws Throwable	: IncomeException si falta un dato
	 */
	private void armaEntrada(StringBuilder linea, NovedadIess novedad, Cabecera cabecera) throws Throwable {
		ContratoEmpleado contrato = exige(novedad.getContrato(), "el contrato");
		linea.append(SEPARADOR).append(fecha(exige(contrato.getFechaInicio(), "la fecha de ingreso")))
				.append(SEPARADOR).append(fecha(LocalDate.now()))
				.append(SEPARADOR).append(codigo(Rubros.RHH_JORNADA_IESS,
						exige(contrato.getJornada(), "la jornada del contrato").intValue(), "jornada"))
				.append(SEPARADOR).append(codigo(Rubros.RHH_CODIGO_SEGURO_SOCIAL_IESS,
						cabecera.codigoSeguroSocial, "codigo de seguro social"))
				.append(SEPARADOR).append(exigeTipoEmpleador(cabecera.tipoEmpleador))
				.append(SEPARADOR).append(codigo(Rubros.RHH_RELACION_TRABAJO_IESS,
						exige(contrato.getTipoRelacionLaboral(), "la relacion de trabajo del contrato").intValue(),
						"relacion de trabajo"))
				.append(SEPARADOR).append(exige(contrato.getOcupacionMdt(), "la denominacion del cargo"))
				.append(SEPARADOR).append(exige(contrato.getCodigoSectorialIess(), "el codigo sectorial del IESS"))
				.append(SEPARADOR).append(importe(exige(contrato.getSalarioBase(), "el sueldo del contrato")))
				.append(SEPARADOR).append(codigo(Rubros.RHH_ORIGEN_PAGO_IESS, cabecera.origenPago, "origen de pago"));
	}

	/**
	 * Aviso de salida. La causa se sella en la novedad con el mismo valor que se escribe.
	 *
	 * @param linea			: Linea en construccion
	 * @param novedad		: Novedad
	 * @throws Throwable	: IncomeException si falta un dato
	 */
	private void armaSalida(StringBuilder linea, NovedadIess novedad) throws Throwable {
		if (novedad.getCausalTerminacion() == null
				|| novedad.getCausalTerminacion().getCodigoAlterno() == null) {
			throw new IncomeException("la novedad de salida no tiene causal de terminacion, y el"
					+ " IESS exige la causa");
		}
		String causa = codigo(Rubros.RHH_CAUSA_SALIDA_IESS,
				novedad.getCausalTerminacion().getCodigoAlterno().intValue(), "causa de salida");
		novedad.setCausaIess(causa);
		// LA FECHA DE FALLECIMIENTO NO SE DEJA VACIA: VA CON CEROS.
		//
		// El formato lo dice literal --«si la causa es diferente a Muerte del trabajador
		// llene este campo con ceros»--, y no es un detalle cosmetico: un campo vacio al
		// final de la linea hace que el IESS rechace el archivo entero. En este formato
		// "no aplica" se escribe, no se omite.
		linea.append(SEPARADOR).append(fecha(exige(novedad.getFechaHecho(), "la fecha de salida")))
				.append(SEPARADOR).append(causa)
				.append(SEPARADOR).append(novedad.getFechaFallecimiento() != null
						? fecha(novedad.getFechaFallecimiento()) : FECHA_NO_APLICA);
	}

	/**
	 * Variacion de sueldo por extras.
	 *
	 * @param linea			: Linea en construccion
	 * @param novedad		: Novedad
	 * @throws Throwable	: IncomeException si falta un dato
	 */
	private void armaVariacion(StringBuilder linea, NovedadIess novedad) throws Throwable {
		String causa = codigo(Rubros.RHH_CAUSA_VARIACION_IESS,
				RhhCausaVariacionIess.OTROS_INGRESOS_IMPONIBLES_NO_PERMANENTES, "causa de variacion");
		novedad.setCausaIess(causa);
		linea.append(SEPARADOR).append(importe(exige(novedad.getValorVariacion(), "el valor de la variacion")))
				.append(SEPARADOR).append(causa);
	}

	/**
	 * Fondos de reserva mensual.
	 *
	 * @param linea			: Linea en construccion
	 * @param novedad		: Novedad
	 * @throws Throwable	: IncomeException si falta un dato
	 */
	private void armaFondosReserva(StringBuilder linea, NovedadIess novedad) throws Throwable {
		linea.append(SEPARADOR).append(importe(exige(novedad.getSueldoNuevo(), "el sueldo total del periodo")))
				.append(SEPARADOR).append(exige(novedad.getPeriodoDesde(), "el periodo desde"))
				.append(" A ").append(exige(novedad.getPeriodoHasta(), "el periodo hasta"))
				.append(SEPARADOR).append(exige(novedad.getMesesLaborados(), "los meses laborados"))
				.append(SEPARADOR).append(TIPO_PERIODO_GENERAL);
	}

	// =====================================================================
	// Apoyo
	// =====================================================================

	/** Datos del empleador que se repiten en la cabecera de cada registro. */
	private static class Cabecera {
		private String ruc;
		private String sucursal;
		private String anio;
		private String mes;
		private String tipoEmpleador;
		private int codigoSeguroSocial;
		private int origenPago;
	}

	/**
	 * Lee la cabecera comun del archivo desde la configuracion de nomina.
	 *
	 * @param configuracion	: Configuracion de nomina de la empresa
	 * @param idEmpresa		: Id de la empresa, para localizar su facturador
	 * @param desde			: Fecha de inicio del periodo, de la que salen anio y mes
	 * @return				: La cabecera
	 * @throws Throwable	: IncomeException si falta el RUC o la sucursal
	 */
	private Cabecera leeCabecera(ConfiguracionNomina configuracion, Long idEmpresa, LocalDate desde) throws Throwable {
		if (configuracion == null) {
			throw new IncomeException("La empresa no tiene configuracion de nomina, y de ahi salen"
					+ " el RUC, la sucursal y el tipo de empleador que van en cada registro del"
					+ " archivo.");
		}
		Cabecera cabecera = new Cabecera();
		// EL RUC SALE DEL FACTURADOR, NO DE AQUI. CBR.FCDR es el unico sitio del sistema
		// donde vive el RUC de la empresa --SCP.PJRQ guarda nombre y jerarquia, y nada mas--,
		// asi que se asume la dependencia del modulo de facturacion en vez de duplicar el
		// dato. Se busca por la FK a la empresa: en una instalacion multiempresa, la primera
		// fila de la tabla seria la de otra.
		cabecera.ruc = leeRucDelFacturador(idEmpresa);
		cabecera.sucursal = exigeConfiguracion(configuracion.getSucursalIess(), "CFNMSCIE",
				"el codigo de sucursal del IESS");
		cabecera.tipoEmpleador = configuracion.getTipoEmpleadorIess();
		cabecera.anio = String.valueOf(desde.getYear());
		cabecera.mes = String.format(Locale.ROOT, "%02d", Integer.valueOf(desde.getMonthValue()));
		// SIMPLIFICACION CONSCIENTE, Y ESTA ES SU CONDICION DE CADUCIDAD.
		//
		// El codigo de seguro social y el origen de pago son de empresa, no de contrato: en
		// ASOPREP son iguales para todos. El seguro social ya esta parametrizado en CFNMSGSC;
		// el origen de pago se queda en el regimen general porque no hay caso que lo mueva.
		// El dia que una instalacion tenga plantilla mixta --parte con fondos publicos, o
		// alguien en seguro mixto-- los dos suben a CNTE como columnas propias y se leen del
		// contrato, igual que la jornada y la relacion de trabajo.
		cabecera.codigoSeguroSocial = configuracion.getSeguroSocialIess() != null
				? configuracion.getSeguroSocialIess().intValue()
				: RhhCodigoSeguroSocialIess.LEY_DE_SEGURO_SOCIAL_VIGENTE;
		cabecera.origenPago = RhhOrigenPagoIess.FONDOS_PRIVADOS;
		return cabecera;
	}

	/**
	 * Lee el RUC del empleador desde el facturador de la empresa.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: El RUC
	 * @throws Throwable	: IncomeException si no hay facturador o no tiene numero
	 */
	private String leeRucDelFacturador(Long idEmpresa) throws Throwable {
		Facturador facturador = facturadorDaoService.selectByEmpresa(idEmpresa);
		if (facturador == null || facturador.getNumDoc() == null
				|| facturador.getNumDoc().trim().isEmpty()) {
			throw new IncomeException("La empresa " + idEmpresa + " no tiene facturador con RUC en"
					+ " CBR.FCDR, y de ahi sale el RUC del empleador que va en la cabecera de todos"
					+ " los registros del archivo batch.");
		}
		return facturador.getNumDoc().trim();
	}

	/**
	 * Exige el codigo de tipo de empleador y explica de donde se saca.
	 *
	 * <p>No es un catalogo: <b>el IESS se lo asigna a cada empresa</b> y el formato dice
	 * literalmente «tomar el codigo asignado». No se puede deducir ni poner por defecto, asi
	 * que se aplica la misma regla que a los <code>'?'</code>: sin el no se genera archivo.</p>
	 *
	 * @param tipoEmpleador	: Valor configurado
	 * @return				: El codigo
	 * @throws Throwable	: IncomeException si esta vacio
	 */
	private String exigeTipoEmpleador(String tipoEmpleador) throws Throwable {
		if (tipoEmpleador == null || tipoEmpleador.trim().isEmpty()) {
			throw new IncomeException("no esta configurado el codigo de tipo de empleador"
					+ " (CFNMTPEM en RHH.CFNM). No es un catalogo: lo asigna el IESS a cada empresa"
					+ " y se lee del portal. Sin el, el aviso de entrada no se puede generar");
		}
		return tipoEmpleador.trim();
	}

	/**
	 * Lee un codigo del IESS del catalogo y <b>se niega si sigue sin resolver</b>.
	 *
	 * <p>Es la defensa que pide la normativa 7: dos codigos --jornada parcial y LOSEP--
	 * siguen en <code>'?'</code> porque sus anexos exigen login de empleador. Mandar un
	 * <code>'?'</code> al IESS no falla aqui, falla alla y dias despues.</p>
	 *
	 * @param rubro			: Codigo alterno del rubro
	 * @param detalle		: Codigo alterno del detalle
	 * @param que			: Nombre del dato, para el mensaje
	 * @return				: El codigo IESS
	 * @throws Throwable	: IncomeException si no existe o sigue en '?'
	 */
	private String codigo(int rubro, int detalle, String que) throws Throwable {
		String valor;
		try {
			valor = detalleRubroDaoService.selectValorStringByRubAltDetAlt(rubro, detalle);
		} catch (Throwable e) {
			throw new IncomeException("no se pudo leer el codigo IESS de " + que + " (rubro " + rubro
					+ ", detalle " + detalle + "): " + e.getMessage());
		}
		if (valor == null || valor.trim().isEmpty()) {
			throw new IncomeException("el codigo IESS de " + que + " esta vacio en el rubro " + rubro
					+ ", detalle " + detalle);
		}
		if (SIN_RESOLVER.equals(valor.trim())) {
			throw new IncomeException("el codigo IESS de " + que + " sigue en '?' en el rubro " + rubro
					+ ", detalle " + detalle + ". Se completa leyendo el anexo del portal del IESS"
					+ " (Tramites virtuales / Empleadores / Cargas batch / Formatos y anexos). No se"
					+ " genera el archivo con un '?' dentro");
		}
		return valor.trim();
	}

	/**
	 * Traduce el tipo de novedad a su codigo de archivo, leido del catalogo.
	 *
	 * @param tipoNovedad	: Codigo alterno del rubro 204
	 * @return				: Codigo de tres letras
	 * @throws Throwable	: IncomeException si el tipo no se envia por archivo
	 */
	private String codigoBatchDelTipo(Long tipoNovedad) throws Throwable {
		if (tipoNovedad == null) {
			throw new IncomeException("Hay que indicar el tipo de novedad a exportar.");
		}
		String codigo;
		try {
			codigo = detalleRubroDaoService.selectValorStringByRubAltDetAlt(
					Rubros.RHH_TIPO_NOVEDAD_IESS, tipoNovedad.intValue());
		} catch (Throwable e) {
			throw new IncomeException("No se pudo leer el codigo de archivo del tipo de novedad "
					+ tipoNovedad + ": " + e.getMessage());
		}
		if (codigo == null || codigo.trim().isEmpty()) {
			throw new IncomeException("El tipo de novedad " + tipoNovedad + " no se envia por archivo"
					+ " batch: no tiene codigo en PDTRVLRV del rubro " + Rubros.RHH_TIPO_NOVEDAD_IESS
					+ ". Se registra en el portal una por una.");
		}
		return codigo.trim();
	}

	/**
	 * Comprueba que el dato obligatorio este informado.
	 *
	 * @param <T>			: Tipo del dato
	 * @param valor			: Valor
	 * @param que			: Nombre del dato, para el mensaje
	 * @return				: El propio valor
	 * @throws Throwable	: IncomeException si falta
	 */
	private <T> T exige(T valor, String que) throws Throwable {
		if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) {
			throw new IncomeException("falta " + que);
		}
		return valor;
	}

	/**
	 * Igual que <code>exige</code>, pero nombrando la columna que hay que configurar.
	 *
	 * @param valor			: Valor
	 * @param columna		: Columna de la configuracion
	 * @param que			: Nombre del dato
	 * @return				: El propio valor
	 * @throws Throwable	: IncomeException si falta
	 */
	private String exigeConfiguracion(String valor, String columna, String que) throws Throwable {
		if (valor == null || valor.trim().isEmpty()) {
			throw new IncomeException("No esta configurado " + que + " (" + columna + " en"
					+ " RHH.CFNM). Es un campo obligatorio de la cabecera de todos los registros"
					+ " del archivo batch.");
		}
		return valor.trim();
	}

	/**
	 * Cedula del empleado de la novedad.
	 *
	 * @param novedad	: Novedad
	 * @return			: La cedula, o null
	 */
	private String cedula(NovedadIess novedad) {
		return novedad.getEmpleado() != null ? novedad.getEmpleado().getIdentificacion() : null;
	}

	/**
	 * Nombre del empleado para los mensajes de error.
	 *
	 * @param novedad	: Novedad
	 * @return			: Nombre y apellidos, o el id de la novedad
	 */
	private String quien(NovedadIess novedad) {
		if (novedad.getEmpleado() == null) {
			return "novedad " + novedad.getCodigo();
		}
		return novedad.getEmpleado().getApellidos() + " " + novedad.getEmpleado().getNombres();
	}

	/**
	 * Fecha en el formato del archivo.
	 *
	 * @param fecha	: Fecha
	 * @return		: yyyyMMdd
	 */
	private String fecha(LocalDate fecha) {
		return fecha.format(FECHA);
	}

	/**
	 * Importe con dos decimales y punto, sin separador de miles.
	 *
	 * <p><code>Locale.ROOT</code> es deliberado: con la configuracion regional de Ecuador
	 * el separador decimal seria la coma y el archivo saldria mal sin que nada avisara.</p>
	 *
	 * @param valor	: Importe
	 * @return		: El importe formateado
	 */
	private String importe(Double valor) {
		return String.format(Locale.ROOT, "%.2f", valor);
	}

}
