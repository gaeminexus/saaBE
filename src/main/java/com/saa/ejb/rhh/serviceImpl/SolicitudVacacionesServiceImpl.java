/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial").
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.ejb.rhh.dao.DetalleConsumoVacacionesDaoService;
import com.saa.ejb.rhh.dao.NovedadNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.ejb.rhh.dao.SolicitudVacacionesDaoService;
import com.saa.ejb.rhh.service.AcreditacionVacacionesService;
import com.saa.ejb.rhh.service.SolicitudVacacionesService;
import com.saa.ejb.rhh.util.PeriodoModificableNomina;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.DetalleConsumoVacaciones;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.NovedadNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.SaldoVacaciones;
import com.saa.model.rhh.SolicitudVacaciones;
import com.saa.model.scp.Usuario;
import com.saa.rubros.RhhEstadoPeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz SolicitudVacacionesService.
 *  Contiene los servicios relacionados con la entidad SolicitudVacaciones</p>
 *
 * <p>El CRUD generico (save/remove/selectAll/selectByCriteria/saveSingle) se mantiene
 * para el listado y la creacion de la solicitud. <code>aprobar</code>, <code>rechazar</code>
 * y <code>anularAprobacion</code> son el ciclo real: hasta el 2026-08-27 aprobar solo
 * cambiaba SLCTESTD sin tocar el saldo ni generar la novedad de nomina. Ver
 * docs/logica-negocio/rhh/CICLO-APROBACION-VACACIONES.md.</p>
 */
@Stateless
public class SolicitudVacacionesServiceImpl implements SolicitudVacacionesService {

	/** Estado de la solicitud, texto libre en SLCTESTD (no es un rubro). */
	private static final String ESTADO_APROBADA = "APROBADA";
	private static final String ESTADO_RECHAZADA = "RECHAZADA";
	private static final String ESTADO_ANULADA = "ANULADA";

	/**
	 * Codigo alterno (CPNMALTR) del concepto "Vacaciones pagadas" en el catalogo de
	 * conceptos de nomina. No tiene rol del motor (CPNMROLM) porque el motor nunca lo
	 * genera solo: las vacaciones no admiten mensualizacion y el unico renglon que
	 * produce este concepto es el que crea esta clase via NovedadNomina.
	 */
	private static final Long CODIGO_ALTERNO_VACACIONES_PAGADAS = Long.valueOf(12L);

	@PersistenceContext
	private EntityManager em;

	@EJB
	private SolicitudVacacionesDaoService solicitudVacaciones;

	@EJB
	private AcreditacionVacacionesService acreditacionVacacionesService;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private ConceptoNominaDaoService conceptoNominaDaoService;

	@EJB
	private NovedadNominaDaoService novedadNominaDaoService;

	@EJB
	private SaldoVacacionesDaoService saldoVacacionesDaoService;

	@EJB
	private DetalleConsumoVacacionesDaoService detalleConsumoVacacionesDaoService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<SolicitudVacaciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de solicituVacaciones service");
		for (SolicitudVacaciones registro:lista) {
			solicitudVacaciones.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de solicituVacaciones service");
		//INSTANCIA UNA ENTIDAD
		SolicitudVacaciones solicituVacaciones = new SolicitudVacaciones();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {

				solicitudVacaciones.remove(solicituVacaciones, registro);
			}

	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<SolicitudVacaciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) SolicitudVacaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SolicitudVacaciones> result = solicitudVacaciones.selectAll(NombreEntidadesRhh.SOLICITUD_VACACIONES);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de solicituVacaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.SolicitudVacacionesService#selectById(java.lang.Long)
	 */
	public SolicitudVacaciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);
		return solicitudVacaciones.selectById(id, NombreEntidadesRhh.SOLICITUD_VACACIONES);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.SolicitudVacacionesService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<SolicitudVacaciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SolicitudVacaciones");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SolicitudVacaciones> result = solicitudVacaciones.selectByCriteria(datos, NombreEntidadesRhh.SOLICITUD_VACACIONES);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de solicituVacaciones no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/**
	 * CRUD generico, para crear la solicitud y editar sus campos antes de que entre al
	 * ciclo de aprobacion. No es la puerta para cambiar el estado.
	 *
	 * <p><b>2026-09-08, hallazgo de produccion:</b> el FE aprobaba solicitudes con
	 * <code>PUT /slct</code> mandando <code>estado: 'APROBADA'</code> directo, y este metodo
	 * lo grababa tal cual -- sin consumir saldo, sin fila en RHH.DVAC y sin
	 * {@code NovedadNomina}. Seis solicitudes de agosto (empresa 1236) quedaron APROBADAS
	 * sin haber pasado por {@link #aprobar}, y el rol de agosto nunca tuvo el renglon de
	 * vacaciones que le correspondia. El FE ya corrigio para llamar a
	 * <code>/slct/aprobar</code>, <code>/rechazar</code> o <code>/anularAprobacion</code>,
	 * pero el guard queda aqui tambien: un cliente no deberia poder saltarse el ciclo real
	 * solo porque el generico lo permite.</p>
	 *
	 * <p>Rechaza el <code>PUT</code> si el <code>estado</code> que llega es distinto del que
	 * esta en la base y alguno de los dos (el actual o el entrante) es APROBADA, RECHAZADA o
	 * ANULADA -- esos tres solo se alcanzan desde el ciclo real. Editar otros campos de una
	 * solicitud sin tocar el estado (p.ej. la observacion de una PENDIENTE) sigue permitido
	 * por este metodo.</p>
	 *
	 * <p>Los campos de auditoria de la aprobacion (<code>usuarioAprobacion</code>,
	 * <code>fechaAprobacion</code>) no se aceptan por este metodo aunque el cliente los
	 * mande: se ignoran en silencio y se conserva lo que ya estaba grabado, porque son
	 * escritura exclusiva de {@link #aprobar}/{@link #rechazar}/{@link #anularAprobacion} y
	 * un PUT generico no deberia poder falsificarlos.</p>
	 */
	@Override
	public SolicitudVacaciones saveSingle(SolicitudVacaciones solicituVacaciones) throws Throwable {
		System.out.println("Ingresa al metodo saveSingle de SolicitudVacaciones, solicitud: "
				+ solicituVacaciones.getCodigo());

		if (solicituVacaciones.getCodigo() != null) {
			SolicitudVacaciones existente = em.find(SolicitudVacaciones.class, solicituVacaciones.getCodigo());
			if (existente != null) {
				String estadoActual = existente.getEstado();
				String estadoEntrante = solicituVacaciones.getEstado();
				boolean cambiaEstado = estadoEntrante != null && !estadoEntrante.equalsIgnoreCase(estadoActual);
				boolean tocaEstadoControlado = esEstadoDelCiclo(estadoActual) || esEstadoDelCiclo(estadoEntrante);
				if (cambiaEstado && tocaEstadoControlado) {
					throw new IncomeException("El estado de la solicitud " + existente.getCodigo()
							+ " es " + estadoActual + " y no se cambia editando el registro: use"
							+ " /slct/aprobar, /slct/rechazar o /slct/anularAprobacion segun corresponda.");
				}
				solicituVacaciones.setUsuarioAprobacion(existente.getUsuarioAprobacion());
				solicituVacaciones.setFechaAprobacion(existente.getFechaAprobacion());
			}
		}

		solicituVacaciones = solicitudVacaciones.save(solicituVacaciones, solicituVacaciones.getCodigo());
		return solicituVacaciones;
	}

	/**
	 * True si el estado es uno de los tres que solo se alcanzan desde el ciclo real de
	 * aprobacion (APROBADA, RECHAZADA, ANULADA); false para PENDIENTE, null, o cualquier
	 * otro valor libre.
	 */
	private boolean esEstadoDelCiclo(String estado) {
		return ESTADO_APROBADA.equalsIgnoreCase(estado) || ESTADO_RECHAZADA.equalsIgnoreCase(estado)
				|| ESTADO_ANULADA.equalsIgnoreCase(estado);
	}

	// =====================================================================
	// Ciclo de aprobacion
	// =====================================================================

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.SolicitudVacacionesService#aprobar(java.lang.Long, java.lang.Long, java.lang.String)
	 */
	@Override
	public SolicitudVacaciones aprobar(Long idSolicitud, Long idUsuario, String observacion) throws Throwable {
		System.out.println("Ingresa al metodo aprobar de SolicitudVacaciones, solicitud: " + idSolicitud);

		SolicitudVacaciones solicitud = em.find(SolicitudVacaciones.class, idSolicitud);
		if (solicitud == null) {
			throw new IncomeException("No existe la solicitud de vacaciones " + idSolicitud + ".");
		}
		if (ESTADO_APROBADA.equalsIgnoreCase(solicitud.getEstado())
				|| ESTADO_ANULADA.equalsIgnoreCase(solicitud.getEstado())) {
			throw new IncomeException("La solicitud " + idSolicitud + " esta en estado "
					+ solicitud.getEstado() + "; solo se puede aprobar una solicitud que no este"
					+ " aprobada ni anulada.");
		}
		if (solicitud.getFechaHasta().isBefore(solicitud.getFechaDesde())) {
			throw new IncomeException("La solicitud " + idSolicitud + " tiene fecha hasta ("
					+ solicitud.getFechaHasta() + ") anterior a la fecha desde ("
					+ solicitud.getFechaDesde() + ").");
		}

		// Los dias del rango se recalculan aqui: SLCTDIAS es lo que se grabo al
		// solicitar y puede no coincidir con lo que corresponde hoy.
		Double dias = RedondeoNomina.redondeaCantidad(Double.valueOf(
				ChronoUnit.DAYS.between(solicitud.getFechaDesde(), solicitud.getFechaHasta()) + 1));

		Long idEmpleado = solicitud.getEmpleado().getCodigo();
		Long idEmpresa = solicitud.getEmpleado().getEmpresa() != null
				? solicitud.getEmpleado().getEmpresa().getCodigo() : null;

		Double disponibles = acreditacionVacacionesService.diasDisponibles(idEmpleado);
		if (disponibles == null || disponibles.doubleValue() < dias.doubleValue()) {
			throw new IncomeException("El empleado " + idEmpleado + " tiene "
					+ (disponibles != null ? disponibles : Double.valueOf(0D))
					+ " dia(s) de vacaciones disponibles y la solicitud " + idSolicitud
					+ " pide " + dias + ".");
		}

		PeriodoNomina periodo = periodoNominaDaoService.selectByFechaEmpresa(idEmpresa,
				solicitud.getFechaDesde());
		if (periodo == null) {
			throw new IncomeException("No existe un periodo de nomina de la empresa " + idEmpresa
					+ " que contenga la fecha " + solicitud.getFechaDesde() + ".");
		}
		// Antes exigia ABIERTO estricto; relajado el 2026-09-08 a ABIERTO-o-CALCULADO (mismo
		// criterio que ValorNoPagadoServiceImpl y OrdenBeneficioSocialServiceImpl, ver
		// PeriodoModificableNomina): reabrirPeriodo deja el periodo en CALCULADO, nunca en
		// ABIERTO, asi que el guard estricto era una pared sin puerta para aprobar una
		// solicitud de vacaciones despues de una reapertura.
		PeriodoModificableNomina.exige(periodo, "generar la novedad de vacaciones de la solicitud "
				+ idSolicitud);

		ConceptoNomina concepto = conceptoNominaDaoService.selectByCodigoAlterno(
				CODIGO_ALTERNO_VACACIONES_PAGADAS, idEmpresa);
		if (concepto == null) {
			throw new IncomeException("No existe en la empresa " + idEmpresa + " el concepto de nomina"
					+ " con codigo alterno " + CODIGO_ALTERNO_VACACIONES_PAGADAS + " (Vacaciones pagadas).");
		}

		String usuario = usuarioNombre(idUsuario);

		consumeSaldoFifo(idEmpleado, dias, solicitud, usuario);

		Double valorDia = acreditacionVacacionesService.valorDiaVacaciones(idEmpleado, solicitud.getFechaDesde());
		Double valorNovedad = RedondeoNomina.redondea(Double.valueOf(
				dias.doubleValue() * (valorDia != null ? valorDia.doubleValue() : 0D)));

		NovedadNomina novedad = new NovedadNomina();
		novedad.setPeriodoNomina(periodo);
		novedad.setEmpleado(solicitud.getEmpleado());
		novedad.setConceptoNomina(concepto);
		novedad.setCantidad(dias);
		novedad.setValor(valorNovedad);
		novedad.setDescripcion(marcadorNovedad(idSolicitud));
		novedad.setAprobada("S");
		novedad.setUsuarioAprueba(usuario);
		novedad.setFechaAprobacion(LocalDate.now());
		novedad.setEstado(Long.valueOf(1L));
		novedad.setFechaRegistro(LocalDateTime.now());
		novedad.setUsuarioRegistro(usuario);
		novedadNominaDaoService.save(novedad, null);

		solicitud.setDiasSolicitados(dias);
		solicitud.setEstado(ESTADO_APROBADA);
		solicitud.setUsuarioAprobacion(usuario);
		solicitud.setFechaAprobacion(LocalDate.now());
		if (observacion != null && !observacion.trim().isEmpty()) {
			solicitud.setObservacion(observacion.trim());
		}
		return solicitudVacaciones.save(solicitud, solicitud.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.SolicitudVacacionesService#rechazar(java.lang.Long, java.lang.Long, java.lang.String)
	 */
	@Override
	public SolicitudVacaciones rechazar(Long idSolicitud, Long idUsuario, String motivo) throws Throwable {
		System.out.println("Ingresa al metodo rechazar de SolicitudVacaciones, solicitud: " + idSolicitud);

		SolicitudVacaciones solicitud = em.find(SolicitudVacaciones.class, idSolicitud);
		if (solicitud == null) {
			throw new IncomeException("No existe la solicitud de vacaciones " + idSolicitud + ".");
		}
		if (ESTADO_APROBADA.equalsIgnoreCase(solicitud.getEstado())
				|| ESTADO_ANULADA.equalsIgnoreCase(solicitud.getEstado())) {
			throw new IncomeException("La solicitud " + idSolicitud + " esta en estado "
					+ solicitud.getEstado() + "; solo se puede rechazar una solicitud que no este"
					+ " aprobada ni anulada. Una solicitud aprobada se anula con anularAprobacion.");
		}

		// No hay saldo ni novedad que tocar: una solicitud rechazada nunca llego a
		// consumirse (eso solo pasa en aprobar).
		String usuario = usuarioNombre(idUsuario);
		solicitud.setEstado(ESTADO_RECHAZADA);
		solicitud.setUsuarioAprobacion(usuario);
		solicitud.setFechaAprobacion(LocalDate.now());
		if (motivo != null && !motivo.trim().isEmpty()) {
			solicitud.setObservacion(motivo.trim());
		}
		return solicitudVacaciones.save(solicitud, solicitud.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.SolicitudVacacionesService#anularAprobacion(java.lang.Long, java.lang.String, java.lang.Long)
	 */
	@Override
	public SolicitudVacaciones anularAprobacion(Long idSolicitud, String motivo, Long idUsuario) throws Throwable {
		System.out.println("Ingresa al metodo anularAprobacion de SolicitudVacaciones, solicitud: " + idSolicitud);

		SolicitudVacaciones solicitud = em.find(SolicitudVacaciones.class, idSolicitud);
		if (solicitud == null) {
			throw new IncomeException("No existe la solicitud de vacaciones " + idSolicitud + ".");
		}
		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulacion.");
		}

		// ===== INICIO anular sin aprobar (equipo omen-saa-2, 2026-09-08) =====
		// El FE no tiene (ni va a tener) un endpoint de proceso para cancelar una solicitud
		// que nunca se aprobo -PENDIENTE/SOLICITADA-: para el usuario es el mismo boton
		// "anular" en los dos casos, y forzarlo a saber en que estado esta la solicitud
		// antes de elegir endpoint es la misma clase de acoplamiento que produjo el PUT
		// directo que motivo el guard de saveSingle. Este metodo admite los dos origenes;
		// RECHAZADA y ANULADA siguen sin admitir "anular" -ya son estados terminales.
		boolean estabaAprobada = ESTADO_APROBADA.equalsIgnoreCase(solicitud.getEstado());
		if (!estabaAprobada && (ESTADO_RECHAZADA.equalsIgnoreCase(solicitud.getEstado())
				|| ESTADO_ANULADA.equalsIgnoreCase(solicitud.getEstado()))) {
			throw new IncomeException("La solicitud " + idSolicitud + " esta en estado "
					+ solicitud.getEstado() + "; no hay una aprobacion que anular ni una solicitud"
					+ " pendiente que cancelar.");
		}

		String usuario = usuarioNombre(idUsuario);

		if (estabaAprobada) {
			Long idEmpleado = solicitud.getEmpleado().getCodigo();
			Long idEmpresa = solicitud.getEmpleado().getEmpresa() != null
					? solicitud.getEmpleado().getEmpresa().getCodigo() : null;

			ConceptoNomina concepto = conceptoNominaDaoService.selectByCodigoAlterno(
					CODIGO_ALTERNO_VACACIONES_PAGADAS, idEmpresa);
			NovedadNomina novedad = concepto != null
					? novedadNominaDaoService.selectPorDescripcion(idEmpleado, concepto.getCodigo(),
							marcadorNovedad(idSolicitud))
					: null;
			if (novedad == null) {
				throw new IncomeException("No se encontro la novedad de nomina que genero la aprobacion de"
						+ " la solicitud " + idSolicitud + "; no se puede anular de forma automatica.");
			}
			PeriodoNomina periodoNovedad = novedad.getPeriodoNomina();
			if (periodoNovedad != null && periodoNovedad.getEstado() != null
					&& periodoNovedad.getEstado().longValue() >= RhhEstadoPeriodoNomina.PAGADO) {
				throw new IncomeException("La novedad de la solicitud " + idSolicitud + " ya entro en un rol"
						+ " del periodo " + periodoNovedad.getMes() + "/" + periodoNovedad.getAnio()
						+ ", que esta en estado " + periodoNovedad.getEstado() + " (pagado o posterior); no"
						+ " se puede anular una novedad que ya se pago.");
			}

			// Devolucion EXACTA via RHH.DVAC: cada fila vigente dice de que anio salieron
			// los dias de ESTA solicitud, asi que se devuelven a exactamente esos anios,
			// sin importar que se haya consumido despues. Si la solicitud se aprobo antes
			// de que existiera esta tabla (no hay backfill, a proposito - ver
			// docs/logica-negocio/rhh/sql/03-detalle-consumo-vacaciones.sql), no hay filas
			// y se cae al heuristico revertirConsumo, con su imprecision documentada.
			List<DetalleConsumoVacaciones> consumos = detalleConsumoVacacionesDaoService
					.selectVigentesPorSolicitud(idSolicitud);
			if (!consumos.isEmpty()) {
				for (DetalleConsumoVacaciones detalle : consumos) {
					SaldoVacaciones saldo = detalle.getSaldo();
					double pendientes = saldo.getDiasPendientes() != null ? saldo.getDiasPendientes().doubleValue() : 0D;
					double usados = saldo.getDiasUsados() != null ? saldo.getDiasUsados().doubleValue() : 0D;
					double dias = detalle.getDias().doubleValue();
					saldo.setDiasPendientes(RedondeoNomina.redondeaCantidad(Double.valueOf(pendientes + dias)));
					saldo.setDiasUsados(RedondeoNomina.redondeaCantidad(Double.valueOf(Math.max(0D, usados - dias))));
					saldo.setUsuarioRegistro(usuario);
					saldoVacacionesDaoService.save(saldo, saldo.getCodigo());

					detalle.setEstado(Long.valueOf(0L));
					detalleConsumoVacacionesDaoService.save(detalle, detalle.getCodigo());
				}
			} else {
				System.out.println("⚠ Solicitud " + idSolicitud + " sin filas en RHH.DVAC (aprobada antes"
						+ " de esa tabla): la devolucion usa el heuristico revertirConsumo, no exacto.");
				acreditacionVacacionesService.revertirConsumo(idEmpleado, solicitud.getDiasSolicitados(), usuario);
			}

			novedad.setAprobada("N");
			novedad.setEstado(Long.valueOf(0L));
			novedad.setDescripcion(nvl(novedad.getDescripcion()) + " | ANULADA: " + motivo.trim());
			novedadNominaDaoService.save(novedad, novedad.getCodigo());
		}
		// Si no estaba aprobada (PENDIENTE/SOLICITADA): nunca se consumio saldo ni se genero
		// novedad, asi que no hay nada que revertir -pasa directo a ANULADA.
		// ===== FIN anular sin aprobar =====

		solicitud.setEstado(ESTADO_ANULADA);
		solicitud.setObservacion(nvl(solicitud.getObservacion()) + " | ANULACION: " + motivo.trim()
				+ " (usuario: " + usuario + ", " + LocalDate.now() + ")");
		return solicitudVacaciones.save(solicitud, solicitud.getCodigo());
	}

	// ===== INICIO consumo FIFO extraido + reparacion (equipo omen-saa-2, 2026-09-08) =====
	/**
	 * Consumo FIFO del saldo de vacaciones: del anio mas antiguo al mas reciente, saltando
	 * caducados ({@link SaldoVacacionesDaoService#selectDisponibles(Long)}). No se hace con
	 * {@code AcreditacionVacacionesService.consumir}, que hace exactamente esto pero no expone
	 * que anios toco, para poder grabar una fila de RHH.DVAC por cada SaldoVacaciones que se
	 * consume -es lo que permite que {@link #anularAprobacion} devuelva los dias exactamente a
	 * esos anios. Ver docs/logica-negocio/rhh/CICLO-APROBACION-VACACIONES.md.
	 *
	 * <p>Extraido el 2026-09-08 de {@link #aprobar} (comportamiento identico, sin cambios) para
	 * que {@link #consumirSaldoSinNovedad} lo pueda reusar sin duplicar la regla de caducidad
	 * ni el redondeo -que es exactamente donde se meten diferencias si se reescribe aparte.</p>
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param dias			: Dias a consumir
	 * @param solicitud		: Solicitud que motiva el consumo, para la fila de RHH.DVAC
	 * @param usuario		: Usuario que ejecuta
	 * @throws Throwable	: IncomeException si el saldo cambio en el medio y ya no alcanza
	 */
	private void consumeSaldoFifo(Long idEmpleado, Double dias, SolicitudVacaciones solicitud, String usuario)
			throws Throwable {
		double porConsumir = dias.doubleValue();
		for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectDisponibles(idEmpleado)) {
			if (porConsumir <= 0D) {
				break;
			}
			double pendientes = saldo.getDiasPendientes() != null ? saldo.getDiasPendientes().doubleValue() : 0D;
			double consume = Math.min(pendientes, porConsumir);
			if (consume <= 0D) {
				continue;
			}
			double usados = saldo.getDiasUsados() != null ? saldo.getDiasUsados().doubleValue() : 0D;
			saldo.setDiasUsados(RedondeoNomina.redondeaCantidad(Double.valueOf(usados + consume)));
			saldo.setDiasPendientes(RedondeoNomina.redondeaCantidad(Double.valueOf(pendientes - consume)));
			saldo.setUsuarioRegistro(usuario);
			saldoVacacionesDaoService.save(saldo, saldo.getCodigo());

			DetalleConsumoVacaciones detalle = new DetalleConsumoVacaciones();
			detalle.setSolicitud(solicitud);
			detalle.setSaldo(saldo);
			detalle.setDias(RedondeoNomina.redondeaCantidad(Double.valueOf(consume)));
			detalle.setEstado(Long.valueOf(1L));
			detalle.setFechaRegistro(LocalDateTime.now());
			detalle.setUsuarioRegistro(usuario);
			detalleConsumoVacacionesDaoService.save(detalle, null);

			porConsumir = porConsumir - consume;
		}
		if (porConsumir > 0D) {
			// No debería pasar: el llamador ya valido que el saldo alcanza. Si pasa, es una
			// condicion de carrera (otra aprobacion consumio el saldo en el medio) y es mejor
			// abortar la transaccion entera que dejar el consumo a medias.
			throw new IncomeException("El saldo del empleado " + idEmpleado + " cambió mientras se "
					+ "procesaba la solicitud " + solicitud.getCodigo() + ": faltan "
					+ RedondeoNomina.redondeaCantidad(Double.valueOf(porConsumir)) + " dia(s) por cubrir."
					+ " Intente de nuevo.");
		}
	}

	/**
	 * <b>Endpoint de REPARACION, no parte del ciclo normal.</b> Existe solo para las
	 * solicitudes que quedaron APROBADA por el camino viejo: hasta el 2026-09-08 el FE
	 * aprobaba con <code>PUT /slct</code> mandando <code>estado: 'APROBADA'</code> directo, y
	 * {@link #saveSingle} lo grababa tal cual -- sin pasar por {@link #aprobar}, sin consumir
	 * saldo, sin fila en RHH.DVAC y sin {@code NovedadNomina}. Caso real: seis solicitudes de
	 * agosto (SLCT 1-6, empresa 1236). El FE ya corrige llamando a <code>/slct/aprobar</code>,
	 * y {@link #saveSingle} ahora rechaza ese PUT (ver su javadoc), asi que este caso no puede
	 * volver a producirse -- el dia que las seis solicitudes viejas esten reparadas, este
	 * endpoint no tiene mas trabajo pendiente.</p>
	 *
	 * <p>Descuenta el saldo (mismo {@link #consumeSaldoFifo} que usa <code>aprobar</code>, RHH.SLDV
	 * + RHH.DVAC) para que esos dias dejen de aparecer como disponibles. <b>No genera
	 * {@code NovedadNomina}</b>: el asiento contable de esos periodos ya se hizo a mano, y
	 * generar la novedad ahora duplicaria el gasto si alguien recalcula.</p>
	 *
	 * <p>Guard de idempotencia: exige que la solicitud este ya APROBADA (no es un atajo para
	 * aprobar sin pasar por {@code aprobar}, es solo para completar el descuento de saldo que
	 * quedo pendiente) y que no tenga ya filas en RHH.DVAC -- si ya las tiene, es que se proceso
	 * bien (por {@code aprobar} o por una corrida anterior de este mismo metodo) y no hay nada
	 * que reparar; correrlo de nuevo por error no duplica el descuento.</p>
	 *
	 * @param idSolicitud	: Id de la solicitud APROBADA a reparar
	 * @param idUsuario		: Usuario que ejecuta la reparacion
	 * @param motivo		: Motivo/referencia de la reparacion, obligatorio, para la observacion
	 * @return				: La solicitud actualizada
	 * @throws Throwable	: IncomeException si la solicitud no existe, no esta APROBADA, ya
	 *						  tiene consumo registrado, o falta el motivo
	 */
	@Override
	public SolicitudVacaciones consumirSaldoSinNovedad(Long idSolicitud, Long idUsuario, String motivo)
			throws Throwable {
		System.out.println("Ingresa al metodo consumirSaldoSinNovedad (REPARACION) de SolicitudVacaciones,"
				+ " solicitud: " + idSolicitud);

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la reparacion.");
		}
		SolicitudVacaciones solicitud = em.find(SolicitudVacaciones.class, idSolicitud);
		if (solicitud == null) {
			throw new IncomeException("No existe la solicitud de vacaciones " + idSolicitud + ".");
		}
		if (!ESTADO_APROBADA.equalsIgnoreCase(solicitud.getEstado())) {
			throw new IncomeException("La solicitud " + idSolicitud + " esta en estado "
					+ solicitud.getEstado() + "; este endpoint solo repara solicitudes ya APROBADA"
					+ " (por el camino viejo, sin consumo de saldo). Use /slct/aprobar para las demas.");
		}
		List<DetalleConsumoVacaciones> consumosPrevios = detalleConsumoVacacionesDaoService
				.selectVigentesPorSolicitud(idSolicitud);
		if (consumosPrevios != null && !consumosPrevios.isEmpty()) {
			throw new IncomeException("La solicitud " + idSolicitud + " ya tiene " + consumosPrevios.size()
					+ " fila(s) en RHH.DVAC: ya consumio saldo (por /slct/aprobar o por una reparacion"
					+ " anterior). No hay nada que reparar.");
		}
		if (solicitud.getDiasSolicitados() == null || solicitud.getDiasSolicitados().doubleValue() <= 0D) {
			throw new IncomeException("La solicitud " + idSolicitud + " no tiene dias solicitados"
					+ " (" + solicitud.getDiasSolicitados() + "); no hay nada que descontar.");
		}

		Long idEmpleado = solicitud.getEmpleado().getCodigo();
		String usuario = usuarioNombre(idUsuario);

		consumeSaldoFifo(idEmpleado, solicitud.getDiasSolicitados(), solicitud, usuario);

		solicitud.setObservacion(nvl(solicitud.getObservacion()) + " | REPARACION 2026-09: saldo descontado"
				+ " sin novedad (aprobada por el camino viejo, sin pasar por /slct/aprobar). Motivo: "
				+ motivo.trim() + " (usuario: " + usuario + ", " + LocalDate.now() + ")");
		return solicitudVacaciones.save(solicitud, solicitud.getCodigo());
	}
	// ===== FIN consumo FIFO extraido + reparacion =====

	// =====================================================================
	// Apoyo
	// =====================================================================

	/**
	 * Marcador exacto que identifica, en NVNM.NVNMDSCR, la novedad que genero la
	 * aprobacion de una solicitud. No hay FK de SLCT a NVNM: este texto es el unico
	 * enlace, por eso el formato no debe cambiar sin migrar las novedades existentes.
	 *
	 * @param idSolicitud	: Id de la solicitud
	 * @return				: El marcador
	 */
	private String marcadorNovedad(Long idSolicitud) {
		return "Solicitud de vacaciones #" + idSolicitud;
	}

	/**
	 * Resuelve idUsuario a su nombre, que es lo que se graba en los campos de texto de
	 * este modulo (SLCTAPRB, NVNMUSAP, NVNMUSRR) -- mismo criterio que
	 * {@code ConciliacionCierreServiceImpl.usuarioNombre}.
	 *
	 * @param idUsuario	: Id del usuario (SCP.PJRQ)
	 * @return			: Su nombre
	 * @throws Throwable	: IncomeException si no existe
	 */
	private String usuarioNombre(Long idUsuario) throws Throwable {
		if (idUsuario == null) {
			throw new IncomeException("Debe indicar el usuario.");
		}
		Usuario usuario = em.find(Usuario.class, idUsuario);
		if (usuario == null) {
			throw new IncomeException("No existe el usuario con id " + idUsuario);
		}
		return usuario.getNombre();
	}

	/**
	 * Concatena texto sobre un valor que puede ser nulo, sin literal "null".
	 *
	 * @param valor	: Valor existente, puede ser nulo o vacio
	 * @return		: El valor, o cadena vacia si era nulo
	 */
	private String nvl(String valor) {
		return valor != null ? valor : "";
	}

}
