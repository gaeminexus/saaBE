package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.LiquidacionDaoService;
import com.saa.ejb.rhh.dao.NovedadIessDaoService;
import com.saa.ejb.rhh.service.NovedadIessService;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.Liquidacion;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.NovedadIess;
import com.saa.rubros.RhhCausaVariacionIess;
import com.saa.rubros.RhhEstadoNovedadIess;
import com.saa.rubros.RhhTipoNovedadIess;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz NovedadIessService.
 *  Contiene los servicios relacionados con la entidad NovedadIess.</p>
 *
 * <p>Los plazos legales de cada tipo de novedad se leen del valor numerico del detalle
 * del rubro RHH_TIPO_NOVEDAD_IESS (204). Si el detalle no existe o no tiene valor, se
 * lanza IncomeException: es un hueco de parametrizacion, no algo que este codigo deba
 * suplir con un valor por defecto.</p>
 */
@Stateless
public class NovedadIessServiceImpl implements NovedadIessService {

	@EJB
	private NovedadIessDaoService novedadIessDaoService;

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	@EJB
	private LiquidacionDaoService liquidacionDaoService;

	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;

	@Override
	public void save(List<NovedadIess> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de novedadIess service");
		for (NovedadIess registro : lista) {
			novedadIessDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de novedadIess service");
		//INSTANCIA UNA ENTIDAD
		NovedadIess novedadIess = new NovedadIess();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			novedadIessDaoService.remove(novedadIess, registro);
		}
	}

	@Override
	public List<NovedadIess> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) NovedadIess");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<NovedadIess> result = novedadIessDaoService.selectAll(NombreEntidadesRhh.NOVEDAD_IESS);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de novedadIess no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public NovedadIess selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de novedadIess con id: " + id);
		return novedadIessDaoService.selectById(id, NombreEntidadesRhh.NOVEDAD_IESS);
	}

	@Override
	public List<NovedadIess> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) NovedadIess");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<NovedadIess> result = novedadIessDaoService.selectByCriteria(datos, NombreEntidadesRhh.NOVEDAD_IESS);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de novedadIess no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public NovedadIess saveSingle(NovedadIess novedadIess) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) NovedadIess");
		novedadIess = novedadIessDaoService.save(novedadIess, novedadIess.getCodigo());
		return novedadIess;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#generarAvisoEntrada(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess generarAvisoEntrada(Long idContrato, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarAvisoEntrada de novedadIess service, contrato: " + idContrato);

		ContratoEmpleado contrato = recuperaContrato(idContrato);

		NovedadIess novedad = nuevaNovedad(RhhTipoNovedadIess.AVISO_DE_ENTRADA,
				contrato.getFechaInicio(), usuario);
		novedad.setEmpleado(contrato.getEmpleado());
		novedad.setContrato(contrato);
		novedad.setObservacion("Aviso de entrada generado al registrar el contrato " + contrato.getNumero());

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#generarAvisoSalida(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess generarAvisoSalida(Long idLiquidacion, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarAvisoSalida de novedadIess service, liquidacion: " + idLiquidacion);

		Liquidacion liquidacion = liquidacionDaoService.selectById(idLiquidacion, NombreEntidadesRhh.LIQUIDACION);
		if (liquidacion == null) {
			throw new IncomeException("No existe la liquidacion " + idLiquidacion
					+ ", no se puede generar el aviso de salida al IESS.");
		}

		ContratoEmpleado contrato = liquidacion.getContrato();

		NovedadIess novedad = nuevaNovedad(RhhTipoNovedadIess.AVISO_DE_SALIDA,
				liquidacion.getFechaSalida(), usuario);
		novedad.setEmpleado(liquidacion.getEmpleado());
		novedad.setContrato(contrato);
		if (contrato != null) {
			novedad.setCausalTerminacion(contrato.getCausalTerminacion());
		}
		novedad.setObservacion("Aviso de salida generado por la liquidacion " + idLiquidacion);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#generarModificacionSueldo(java.lang.Long, java.lang.Double, java.lang.Double, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess generarModificacionSueldo(Long idContrato, Double sueldoAnterior,
			Double sueldoNuevo, LocalDate vigencia, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarModificacionSueldo de novedadIess service, contrato: " + idContrato);

		ContratoEmpleado contrato = recuperaContrato(idContrato);

		NovedadIess novedad = nuevaNovedad(RhhTipoNovedadIess.MODIFICACION_DE_SUELDO, vigencia, usuario);
		novedad.setEmpleado(contrato.getEmpleado());
		novedad.setContrato(contrato);
		novedad.setSueldoAnterior(sueldoAnterior);
		novedad.setSueldoNuevo(sueldoNuevo);
		novedad.setObservacion("Modificacion de sueldo del contrato " + contrato.getNumero());

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#generarVariacionPorExtras(java.lang.Long, java.time.LocalDate, java.lang.Double, java.time.LocalDate, java.time.LocalDate, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess generarVariacionPorExtras(Long idContrato, LocalDate fechaHecho, Double valorVariacion,
			LocalDate desde, LocalDate hasta, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarVariacionPorExtras de novedadIess service, contrato: "
				+ idContrato + ", valor: " + valorVariacion);

		ContratoEmpleado contrato = recuperaContrato(idContrato);

		// IDEMPOTENCIA. calcularPeriodo se corre varias veces sobre el mismo mes --marzo
		// se recalculo tres veces--, asi que crear sin mirar dejaria una novedad por
		// pasada y el IESS recibiria la variacion repetida.
		List<NovedadIess> existentes = novedadIessDaoService.selectByContratoTipoEnVentana(
				idContrato, Long.valueOf(RhhTipoNovedadIess.VARIACION_POR_EXTRAS), desde, hasta);
		NovedadIess novedad = null;
		for (NovedadIess candidata : existentes) {
			Long estado = candidata.getEstado();
			// Una novedad ya enviada al IESS no se reescribe: lo que se mando, se mando.
			// Si el recalculo cambia el importe, eso es una correccion que alguien tiene
			// que decidir a mano, no algo que el motor deba pisar en silencio.
			if (estado != null && estado.longValue() != RhhEstadoNovedadIess.PENDIENTE
					&& estado.longValue() != RhhEstadoNovedadIess.ANULADA) {
				System.out.println("Aviso: el contrato " + idContrato + " ya tiene una variacion por"
						+ " extras en estado " + estado + " para este periodo; no se regenera."
						+ " Importe recalculado: " + valorVariacion);
				return null;
			}
			if (estado != null && estado.longValue() == RhhEstadoNovedadIess.PENDIENTE) {
				novedad = candidata;
			}
		}

		if (novedad == null) {
			novedad = nuevaNovedad(RhhTipoNovedadIess.VARIACION_POR_EXTRAS, fechaHecho, usuario);
			novedad.setEmpleado(contrato.getEmpleado());
			novedad.setContrato(contrato);
		}
		novedad.setFechaHecho(fechaHecho);
		novedad.setFechaLimite(calculaFechaLimite(RhhTipoNovedadIess.VARIACION_POR_EXTRAS, fechaHecho));
		novedad.setValorVariacion(valorVariacion);
		novedad.setCausaIess(causaVariacionPorDefecto());
		novedad.setObservacion("Variacion generada por el calculo del periodo: imponible por encima"
				+ " del sueldo declarado del contrato " + contrato.getNumero());

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#generarCambioRelacionTrabajo(java.lang.Long, java.time.LocalDate, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess generarCambioRelacionTrabajo(Long idContrato, LocalDate vigencia, String detalle,
			String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarCambioRelacionTrabajo de novedadIess service, contrato: "
				+ idContrato);

		ContratoEmpleado contrato = recuperaContrato(idContrato);

		NovedadIess novedad = nuevaNovedad(RhhTipoNovedadIess.CAMBIO_DE_RELACION_DE_TRABAJO, vigencia, usuario);
		novedad.setEmpleado(contrato.getEmpleado());
		novedad.setContrato(contrato);
		novedad.setObservacion("Cambio en el contrato " + contrato.getNumero() + ": " + detalle);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#generarCambioJornada(java.lang.Long, java.time.LocalDate, java.lang.Double, java.lang.Double, java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess generarCambioJornada(Long idContrato, LocalDate vigencia, Double sueldoAnterior,
			Double sueldoNuevo, Long diasDeclarados, String aviso, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarCambioJornada de novedadIess service, contrato: "
				+ idContrato + ", dias: " + diasDeclarados);

		ContratoEmpleado contrato = recuperaContrato(idContrato);

		NovedadIess novedad = nuevaNovedad(RhhTipoNovedadIess.CAMBIO_DE_JORNADA, vigencia, usuario);
		novedad.setEmpleado(contrato.getEmpleado());
		novedad.setContrato(contrato);
		novedad.setSueldoAnterior(sueldoAnterior);
		novedad.setSueldoNuevo(sueldoNuevo);
		// Los dias son la razon de que esta novedad exista aparte de la 3: sin ellos, la
		// planilla del mes siguiente seguiria imprimiendo los de la jornada anterior.
		novedad.setDiasDeclarados(diasDeclarados);
		novedad.setSueldoReferencial(sueldoNuevo);
		novedad.setObservacion("Cambio de jornada del contrato " + contrato.getNumero()
				+ ", dias declarados al IESS: " + diasDeclarados + (aviso != null ? aviso : ""));

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#marcarEnviada(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess marcarEnviada(Long idNovedad, String lote, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo marcarEnviada de novedadIess service, novedad: " + idNovedad);

		NovedadIess novedad = recuperaNovedad(idNovedad);
		exigeEstado(novedad, "enviar", RhhEstadoNovedadIess.PENDIENTE, RhhEstadoNovedadIess.RECHAZADA);

		// EL CODIGO SE SELLA DONDE SE USA, Y AQUI SOLO SI NADIE LO SELLO ANTES.
		//
		// En el camino batch el orden real es exportar -> subir al portal -> marcar enviada,
		// asi que el codigo que de verdad viajo es el que el exportador escribio en el .txt;
		// re-resolverlo aqui sellaria un valor distinto del que esta en el archivo. En el
		// camino manual --sin archivo, el usuario teclea en el portal-- este si es el
		// instante. reasignaCausaIess distingue los dos casos por si sola mirando el LOTE.
		//
		// EL ORDEN IMPORTA: se resuelve ANTES de estampar el lote de este envio. Al reves,
		// la novedad ya tendria lote y nunca se resolveria en su primer envio.
		reasignaCausaIess(novedad);

		novedad.setEstado(Long.valueOf(RhhEstadoNovedadIess.ENVIADA));
		novedad.setFechaReporte(LocalDate.now());
		if (lote != null && !lote.trim().isEmpty()) {
			novedad.setLote(lote.trim());
		}
		// Un envio nuevo deja sin sentido la respuesta del rechazo anterior.
		novedad.setRespuestaIess(null);
		novedad.setUsuarioRegistro(usuario);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#marcarAceptada(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess marcarAceptada(Long idNovedad, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo marcarAceptada de novedadIess service, novedad: " + idNovedad);

		NovedadIess novedad = recuperaNovedad(idNovedad);
		exigeEstado(novedad, "aceptar", RhhEstadoNovedadIess.ENVIADA);

		novedad.setEstado(Long.valueOf(RhhEstadoNovedadIess.ACEPTADA));
		if (novedad.getFechaReporte() == null) {
			novedad.setFechaReporte(LocalDate.now());
		}
		novedad.setRespuestaIess(null);
		novedad.setUsuarioRegistro(usuario);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#marcarRechazada(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess marcarRechazada(Long idNovedad, String motivo, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo marcarRechazada de novedadIess service, novedad: " + idNovedad);

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Para marcar una novedad como rechazada hay que indicar el"
					+ " motivo que devolvio el IESS: sin el no se puede corregir, y la novedad"
					+ " seguira impidiendo cerrar el periodo sin decir por que.");
		}

		NovedadIess novedad = recuperaNovedad(idNovedad);
		exigeEstado(novedad, "rechazar", RhhEstadoNovedadIess.ENVIADA);

		novedad.setEstado(Long.valueOf(RhhEstadoNovedadIess.RECHAZADA));
		novedad.setRespuestaIess(motivo.trim());
		novedad.setUsuarioRegistro(usuario);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#anular(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess anular(Long idNovedad, String motivo, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo anular de novedadIess service, novedad: " + idNovedad);

		NovedadIess novedad = recuperaNovedad(idNovedad);
		// Una novedad que el IESS ya acepto no se anula: existe en la historia laboral del
		// afiliado, y borrarla de nuestro lado solo conseguiria que los dos sistemas dejaran
		// de coincidir. Lo que corresponde es reportar la novedad contraria.
		exigeEstado(novedad, "anular", RhhEstadoNovedadIess.PENDIENTE, RhhEstadoNovedadIess.ENVIADA,
				RhhEstadoNovedadIess.RECHAZADA);

		novedad.setEstado(Long.valueOf(RhhEstadoNovedadIess.ANULADA));
		String texto = motivo != null && !motivo.trim().isEmpty() ? motivo.trim() : "sin motivo indicado";
		novedad.setObservacion((novedad.getObservacion() != null ? novedad.getObservacion() + " | " : "")
				+ "Anulada por " + usuario + ": " + texto);
		novedad.setUsuarioRegistro(usuario);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.NovedadIessService#registrar(com.saa.model.rhh.NovedadIess, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public NovedadIess registrar(NovedadIess novedad, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo registrar de novedadIess service");

		if (novedad == null) {
			throw new IncomeException("No llego ninguna novedad que registrar.");
		}
		if (novedad.getTipoNovedad() == null) {
			throw new IncomeException("Hay que indicar el tipo de novedad: de el sale el plazo"
					+ " legal para reportarla.");
		}
		if (novedad.getFechaHecho() == null) {
			throw new IncomeException("Hay que indicar la fecha del hecho: es desde la que corre el"
					+ " plazo legal, y es la que decide a que periodo pertenece la novedad.");
		}
		if (novedad.getEmpleado() == null || novedad.getEmpleado().getCodigo() == null) {
			throw new IncomeException("Hay que indicar el empleado al que afecta la novedad.");
		}

		// EL PLAZO SE CALCULA AQUI Y NO SE ACEPTA DEL CLIENTE. Si viniera en el cuerpo, una
		// pantalla con un error de calculo --o alguien probando-- podria dejar una novedad
		// con un plazo mas largo del que la ley concede, y nadie lo notaria hasta la multa.
		novedad.setFechaLimite(calculaFechaLimite(novedad.getTipoNovedad().intValue(),
				novedad.getFechaHecho()));
		novedad.setEstado(Long.valueOf(RhhEstadoNovedadIess.PENDIENTE));
		novedad.setFechaRegistro(LocalDateTime.now());
		novedad.setUsuarioRegistro(usuario);
		// Un alta manual no ha viajado a ninguna parte: ni fecha de reporte ni lote.
		novedad.setFechaReporte(null);
		novedad.setLote(null);
		novedad.setRespuestaIess(null);

		return novedadIessDaoService.save(novedad, novedad.getCodigo());
	}

	/**
	 * Recupera la novedad y falla con mensaje explicito si no existe.
	 *
	 * @param idNovedad		: Id de la novedad
	 * @return				: La novedad
	 * @throws Throwable	: IncomeException si no existe
	 */
	private NovedadIess recuperaNovedad(Long idNovedad) throws Throwable {
		NovedadIess novedad = novedadIessDaoService.selectById(idNovedad, NombreEntidadesRhh.NOVEDAD_IESS);
		if (novedad == null) {
			throw new IncomeException("No existe la novedad IESS " + idNovedad + ".");
		}
		return novedad;
	}

	/**
	 * Comprueba que la novedad este en uno de los estados desde los que la accion tiene
	 * sentido.
	 *
	 * <p>La maquina de estados vive aqui y no en la pantalla. Hasta ahora el cliente la
	 * reproducia con el PUT del CRUD, y <b>reproducir no es impedir</b>: cualquier otro
	 * cliente, o el mismo con un fallo, podia llevar una novedad aceptada de vuelta a
	 * pendiente sin que nada se opusiera.</p>
	 *
	 * @param novedad		: Novedad
	 * @param accion		: Nombre de la accion, para el mensaje
	 * @param admitidos		: Estados desde los que se admite
	 * @throws Throwable	: IncomeException si el estado actual no esta entre los admitidos
	 */
	private void exigeEstado(NovedadIess novedad, String accion, int... admitidos) throws Throwable {
		Long estado = novedad.getEstado();
		for (int admitido : admitidos) {
			if (estado != null && estado.longValue() == admitido) {
				return;
			}
		}
		StringBuilder lista = new StringBuilder();
		for (int admitido : admitidos) {
			if (lista.length() > 0) {
				lista.append(", ");
			}
			lista.append(admitido);
		}
		throw new IncomeException("No se puede " + accion + " la novedad " + novedad.getCodigo()
				+ ": su estado es " + estado + " y la accion solo se admite desde " + lista + ".");
	}

	/**
	 * Vuelve a resolver el codigo IESS de la causa desde el catalogo, si la novedad lleva
	 * una.
	 *
	 * <p>Solo toca los dos tipos que llevan causa --salida y variacion por extras--. En la
	 * salida el codigo sale de la causal de terminacion, cuyo codigo alterno coincide con
	 * el del detalle del rubro; en la variacion, de la causa generica.</p>
	 *
	 * <p>Si el catalogo devuelve un codigo sin resolver o no responde, <b>no se pisa lo que
	 * la novedad ya tenia</b>: es preferible conservar un dato viejo que sustituirlo por
	 * uno peor. Quien no deja pasar el hueco es el exportador.</p>
	 *
	 * @param novedad	: Novedad que se va a enviar
	 */
	private void reasignaCausaIess(NovedadIess novedad) {
		Long tipo = novedad.getTipoNovedad();
		if (tipo == null) {
			return;
		}
		// LO QUE CONGELA EL CODIGO NO ES HABER GENERADO EL ARCHIVO, ES HABERLO ENVIADO.
		//
		// Generar un archivo es reversible --se hacen exportaciones de ensayo, y una de
		// ellas escribio la causa en las dos novedades reales de marzo antes de que nadie
		// mandara nada--. Lo irreversible es el envio, y su testigo es el LOTE, que se
		// estampa aqui mismo al marcar enviada.
		//
		// Con lote puesto no se toca jamas: esa novedad ya viajo con ese codigo. Sin lote,
		// aunque tenga un codigo de un ensayo anterior, se vuelve a resolver: sigue siendo
		// revisable y conviene que refleje el catalogo de hoy.
		boolean yaEnviada = novedad.getLote() != null && !novedad.getLote().trim().isEmpty();
		if (yaEnviada) {
			return;
		}
		String actual = novedad.getCausaIess();
		if (actual != null && !actual.trim().isEmpty() && !SIN_RESOLVER.equals(actual.trim())) {
			// Sin lote, un codigo puesto por un ensayo se recalcula igualmente; solo se sale
			// de aqui si ademas coincide con lo que el catalogo diria ahora, en cuyo caso
			// resolverlo no cambiaria nada.
			System.out.println("Aviso: la novedad " + novedad.getCodigo() + " tiene causa " + actual
					+ " sin lote de envio; se vuelve a resolver desde el catalogo.");
		}
		try {
			String codigo = null;
			if (tipo.longValue() == RhhTipoNovedadIess.AVISO_DE_SALIDA) {
				if (novedad.getCausalTerminacion() == null
						|| novedad.getCausalTerminacion().getCodigoAlterno() == null) {
					return;
				}
				codigo = detalleRubroDaoService.selectValorStringByRubAltDetAlt(
						Rubros.RHH_CAUSA_SALIDA_IESS,
						novedad.getCausalTerminacion().getCodigoAlterno().intValue());
			} else if (tipo.longValue() == RhhTipoNovedadIess.VARIACION_POR_EXTRAS) {
				codigo = causaVariacionPorDefecto();
			} else {
				return;
			}
			if (codigo != null && !codigo.trim().isEmpty() && !SIN_RESOLVER.equals(codigo.trim())) {
				novedad.setCausaIess(codigo.trim());
			}
		} catch (Throwable e) {
			System.out.println("Aviso: no se pudo re-resolver la causa IESS de la novedad "
					+ novedad.getCodigo() + "; se conserva la que tenia. Detalle: " + e.getMessage());
		}
	}

	/**
	 * Marca que el catalogo usa cuando el codigo del anexo del portal todavia no se pudo
	 * leer. El exportador se niega a generar un archivo que la contenga.
	 */
	private static final String SIN_RESOLVER = "?";

	/**
	 * Codigo IESS de la causa de variacion que se pone por defecto a lo que genera el
	 * motor.
	 *
	 * <p>El motor sabe que hubo imponible de mas, pero no <b>por que</b>: la diferencia
	 * no distingue una hora extra de un encargo. Se marca como la causa generica y queda
	 * a la pantalla afinarla antes de enviar.</p>
	 *
	 * <p>Devuelve <code>null</code> si el catalogo no responde, en vez de fallar: una
	 * causa sin resolver no puede tumbar el calculo de una nomina. Quien no deja pasar
	 * el hueco es el exportador, que se niega a generar el archivo.</p>
	 *
	 * @return	: El codigo IESS, o null si no se pudo leer
	 */
	private String causaVariacionPorDefecto() {
		try {
			return detalleRubroDaoService.selectValorStringByRubAltDetAlt(
					Rubros.RHH_CAUSA_VARIACION_IESS,
					RhhCausaVariacionIess.OTROS_INGRESOS_IMPONIBLES_NO_PERMANENTES);
		} catch (Throwable e) {
			System.out.println("Aviso: no se pudo leer la causa de variacion por defecto del rubro "
					+ Rubros.RHH_CAUSA_VARIACION_IESS + "; la novedad queda sin causa y el exportador"
					+ " la rechazara. Detalle: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Recupera el contrato y falla con mensaje explicito si no existe.
	 *
	 * @param idContrato	: Id del contrato
	 * @return				: El contrato
	 * @throws Throwable	: IncomeException si no existe
	 */
	private ContratoEmpleado recuperaContrato(Long idContrato) throws Throwable {
		ContratoEmpleado contrato = contratoEmpleadoDaoService.selectById(idContrato,
				NombreEntidadesRhh.CONTRATO_EMPLEADO);
		if (contrato == null) {
			throw new IncomeException("No existe el contrato " + idContrato
					+ ", no se puede generar la novedad para el IESS.");
		}
		return contrato;
	}

	/**
	 * Arma la cabecera comun de la novedad, resolviendo la fecha limite legal a partir
	 * del plazo parametrizado en el rubro RHH_TIPO_NOVEDAD_IESS.
	 *
	 * @param tipoNovedad	: Codigo alterno del detalle del rubro 204
	 * @param fechaHecho	: Fecha del hecho que se reporta
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad sin persistir, en estado PENDIENTE
	 * @throws Throwable	: IncomeException si el plazo no esta parametrizado
	 */
	private NovedadIess nuevaNovedad(int tipoNovedad, LocalDate fechaHecho, String usuario) throws Throwable {
		NovedadIess novedad = new NovedadIess();
		novedad.setTipoNovedad(Long.valueOf(tipoNovedad));
		novedad.setFechaHecho(fechaHecho);
		novedad.setFechaLimite(calculaFechaLimite(tipoNovedad, fechaHecho));
		novedad.setEstado(Long.valueOf(RhhEstadoNovedadIess.PENDIENTE));
		novedad.setFechaRegistro(LocalDateTime.now());
		novedad.setUsuarioRegistro(usuario);
		return novedad;
	}

	/**
	 * Suma a la fecha del hecho el plazo legal en dias del tipo de novedad. El plazo
	 * sale del PDTRVLRN del detalle del rubro 204: NO se escribe en este codigo.
	 *
	 * @param tipoNovedad	: Codigo alterno del detalle del rubro 204
	 * @param fechaHecho	: Fecha del hecho que se reporta
	 * @return				: Fecha limite legal para reportar la novedad
	 * @throws Throwable	: IncomeException si el plazo no esta parametrizado
	 */
	private LocalDate calculaFechaLimite(int tipoNovedad, LocalDate fechaHecho) throws Throwable {
		if (fechaHecho == null) {
			return null;
		}
		Double plazoDias;
		try {
			plazoDias = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
					Rubros.RHH_TIPO_NOVEDAD_IESS, tipoNovedad);
		} catch (Throwable e) {
			throw new IncomeException("No esta parametrizado el plazo legal del tipo de novedad IESS "
					+ tipoNovedad + " en el rubro " + Rubros.RHH_TIPO_NOVEDAD_IESS
					+ ". Revise el detalle correspondiente en SCP.PDTR. Detalle: " + e.getMessage());
		}
		if (plazoDias == null) {
			throw new IncomeException("El plazo legal del tipo de novedad IESS " + tipoNovedad
					+ " esta vacio en el rubro " + Rubros.RHH_TIPO_NOVEDAD_IESS
					+ ". Complete PDTRVLRN en SCP.PDTR.");
		}
		return fechaHecho.plusDays(plazoDias.longValue());
	}
}
