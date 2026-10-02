package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.cxp.service.dto.LineaContablePago;
import com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService;
import com.saa.ejb.rhh.dao.LiquidacionExternaDaoService;
import com.saa.ejb.rhh.service.LiquidacionExternaService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.rhh.DetalleLiquidacionExterna;
import com.saa.model.rhh.LiquidacionExterna;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhConceptoLiquidacionExterna;
import com.saa.rubros.RhhEstadoLiquidacionExterna;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de LiquidacionExternaService. Diseno:
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md.</p>
 */
@Stateless
public class LiquidacionExternaServiceImpl implements LiquidacionExternaService {

	/** Fecha de salida limite (D7): solo salidas anteriores a este dia. */
	private static final LocalDate LIMITE_FECHA_SALIDA = LocalDate.of(2026, 1, 1);

	/** Cedula: exactamente 10 digitos. */
	private static final Pattern CEDULA = Pattern.compile("\\d{10}");

	/** Pasaporte: de 5 a 15 caracteres alfanumericos. */
	private static final Pattern PASAPORTE = Pattern.compile("[A-Za-z0-9]{5,15}");

	@EJB
	private LiquidacionExternaDaoService liquidacionExternaDaoService;

	@EJB
	private DetalleLiquidacionExternaDaoService detalleLiquidacionExternaDaoService;

	@EJB
	private PagoProgramadoService pagoProgramadoService;

	@EJB
	private PagoProgramadoDaoService pagoProgramadoDaoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService — los seis de la casa
	// =====================================================================

	@Override
	public LiquidacionExterna selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de liquidacionExterna con id: " + id);
		return liquidacionExternaDaoService.selectById(id, NombreEntidadesRhh.LIQUIDACION_EXTERNA);
	}

	@Override
	public List<LiquidacionExterna> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) LiquidacionExterna");
		List<LiquidacionExterna> result =
				liquidacionExternaDaoService.selectAll(NombreEntidadesRhh.LIQUIDACION_EXTERNA);
		// Decision del arbitro (2026-09-30): getAll tambien devuelve [] con 200 en vacio --
		// la primera vez que se abra la pantalla no va a haber ninguna, y un 500 ahi se lee
		// como error. A diferencia del resto de la casa, que lanza IncomeException.
		sincronizaLista(result);
		return result;
	}

	@Override
	public List<LiquidacionExterna> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) LiquidacionExterna");
		List<LiquidacionExterna> result =
				liquidacionExternaDaoService.selectByCriteria(datos, NombreEntidadesRhh.LIQUIDACION_EXTERNA);
		// §6, explicito en el contrato: sin resultados devuelve [] con 200, no 500 -- a
		// diferencia del resto de la casa (selectAll incluido), que lanza IncomeException.
		sincronizaLista(result);
		return result;
	}

	@Override
	public LiquidacionExterna saveSingle(LiquidacionExterna liquidacionExterna) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) LiquidacionExterna");
		return liquidacionExternaDaoService.save(liquidacionExterna, liquidacionExterna.getCodigo());
	}

	@Override
	public void save(List<LiquidacionExterna> lista) throws Throwable {
		for (LiquidacionExterna registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		LiquidacionExterna entidad = new LiquidacionExterna();
		for (Long registro : id) {
			liquidacionExternaDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// §6 — Detalle, registrar, actualizar
	// =====================================================================

	@Override
	public List<DetalleLiquidacionExterna> detalle(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo detalle de liquidacionExterna service, liquidacion: " + idLiquidacion);
		return detalleLiquidacionExternaDaoService.selectByLiquidacion(idLiquidacion);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public LiquidacionExterna registrar(LiquidacionExterna liquidacion, List<DetalleLiquidacionExterna> detalles)
			throws Throwable {
		System.out.println("=== registrar liquidacion externa ===");
		if (liquidacion == null) {
			throw new IncomeException("No llegaron los datos de la liquidación.");
		}
		liquidacion.setCodigo(null);
		normalizaYValida(liquidacion, detalles, null);

		liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacionExterna.REGISTRADA));
		liquidacion.setIdPago(null);
		liquidacion.setIdAsiento(null);
		liquidacion.setFechaPago(null);
		liquidacion.setMotivoAnulacion(null);
		liquidacion = liquidacionExternaDaoService.save(liquidacion, null);

		for (DetalleLiquidacionExterna detalle : detalles) {
			detalle.setCodigo(null);
			detalle.setLiquidacion(liquidacion);
			if (detalle.getUsuarioRegistro() == null) {
				detalle.setUsuarioRegistro(liquidacion.getUsuarioRegistro());
			}
			detalleLiquidacionExternaDaoService.save(detalle, null);
		}

		System.out.println("✓ Liquidación externa registrada: id=" + liquidacion.getCodigo());
		return liquidacion;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public LiquidacionExterna actualizar(LiquidacionExterna liquidacion, List<DetalleLiquidacionExterna> detalles)
			throws Throwable {
		System.out.println("=== actualizar liquidacion externa ===");
		if (liquidacion == null || liquidacion.getCodigo() == null) {
			throw new IncomeException("Debe indicar el código de la liquidación a actualizar.");
		}
		LiquidacionExterna previa = recuperaLiquidacion(liquidacion.getCodigo());
		if (previa.getEstado() == null || previa.getEstado().intValue() != RhhEstadoLiquidacionExterna.REGISTRADA) {
			throw new IncomeException("La liquidación " + liquidacion.getCodigo() + " está en estado "
					+ textoEstado(previa.getEstado()) + ": solo se puede actualizar una liquidación REGISTRADA.");
		}

		normalizaYValida(liquidacion, detalles, liquidacion.getCodigo());

		// Campos internos que el PUT de edicion no manda: se conservan del previo. El merge
		// desnudo de EntityDaoImpl.save los grabaria en NULL (REGISTRO-RESERVAS-EQUIPOS.md
		// §8.2) -- mismo precedente que ContratoEmpleadoServiceImpl.saveSingle.
		liquidacion.setEstado(previa.getEstado());
		liquidacion.setIdPago(previa.getIdPago());
		liquidacion.setIdAsiento(previa.getIdAsiento());
		liquidacion.setFechaPago(previa.getFechaPago());
		liquidacion.setMotivoAnulacion(previa.getMotivoAnulacion());
		if (liquidacion.getFechaRegistro() == null) {
			liquidacion.setFechaRegistro(previa.getFechaRegistro());
		}

		liquidacion = liquidacionExternaDaoService.save(liquidacion, liquidacion.getCodigo());

		detalleLiquidacionExternaDaoService.eliminaByLiquidacion(liquidacion.getCodigo());
		for (DetalleLiquidacionExterna detalle : detalles) {
			detalle.setCodigo(null);
			detalle.setLiquidacion(liquidacion);
			if (detalle.getUsuarioRegistro() == null) {
				detalle.setUsuarioRegistro(liquidacion.getUsuarioRegistro());
			}
			detalleLiquidacionExternaDaoService.save(detalle, null);
		}

		System.out.println("✓ Liquidación externa actualizada: id=" + liquidacion.getCodigo());
		return liquidacion;
	}

	/**
	 * Normaliza y valida la cabecera y el detalle segun §6, y calcula totalIngresos,
	 * totalDescuentos y neto (ignora lo que haya llegado del cliente en esos tres campos).
	 *
	 * @param liquidacion	: Cabecera a normalizar en el sitio
	 * @param detalles		: Detalle a validar
	 * @param codigoActual	: Codigo de la liquidacion en edicion, para excluirse a si misma
	 *						  del chequeo de duplicados; null en el alta
	 * @throws Throwable	: IncomeException con el primer problema encontrado
	 */
	private void normalizaYValida(LiquidacionExterna liquidacion, List<DetalleLiquidacionExterna> detalles,
			Long codigoActual) throws Throwable {

		if (liquidacion.getEmpresa() == null || liquidacion.getEmpresa().getCodigo() == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		if (liquidacion.getApellidos() == null || liquidacion.getApellidos().trim().isEmpty()) {
			throw new IncomeException("Los apellidos son obligatorios.");
		}
		if (liquidacion.getNombres() == null || liquidacion.getNombres().trim().isEmpty()) {
			throw new IncomeException("Los nombres son obligatorios.");
		}
		if (liquidacion.getFechaSalida() == null) {
			throw new IncomeException("La fecha de salida es obligatoria.");
		}
		if (liquidacion.getProductoPago() == null || liquidacion.getProductoPago().getId() == null) {
			throw new IncomeException("El producto de pago es obligatorio.");
		}
		if (!liquidacion.getFechaSalida().isBefore(LIMITE_FECHA_SALIDA)) {
			throw new IncomeException(
					"Esta pantalla es sólo para salidas anteriores a 2026. Use la liquidación de haberes.");
		}

		String identificacion =
				liquidacion.getIdentificacion() != null ? liquidacion.getIdentificacion().trim() : null;
		if (identificacion == null || identificacion.isEmpty()) {
			throw new IncomeException("La identificación es obligatoria.");
		}
		boolean formatoValido;
		if ("C".equals(liquidacion.getTipoIdentificacion())) {
			formatoValido = CEDULA.matcher(identificacion).matches();
		} else if ("P".equals(liquidacion.getTipoIdentificacion())) {
			formatoValido = PASAPORTE.matcher(identificacion).matches();
		} else {
			formatoValido = false;
		}
		if (!formatoValido) {
			throw new IncomeException("La identificación no es válida para el tipo indicado.");
		}
		liquidacion.setIdentificacion(identificacion);
		liquidacion.setApellidos(liquidacion.getApellidos().trim().toUpperCase());
		liquidacion.setNombres(liquidacion.getNombres().trim().toUpperCase());

		if (detalles == null || detalles.isEmpty()) {
			throw new IncomeException("La liquidación debe tener al menos un concepto.");
		}
		boolean hayIngreso = false;
		List<Double> ingresos = new ArrayList<>();
		List<Double> descuentos = new ArrayList<>();
		for (DetalleLiquidacionExterna detalleActual : detalles) {
			Long tipo = detalleActual.getTipoConcepto();
			if (tipo == null || !RhhConceptoLiquidacionExterna.esValido(tipo.longValue())) {
				throw new IncomeException("El tipo de concepto " + tipo + " no es válido.");
			}
			if (detalleActual.getValor() == null || detalleActual.getValor().doubleValue() <= 0D) {
				throw new IncomeException("El valor del concepto " + tipo + " debe ser mayor que cero.");
			}
			if (RhhConceptoLiquidacionExterna.esIngreso(tipo.longValue())) {
				hayIngreso = true;
				ingresos.add(detalleActual.getValor());
			} else {
				descuentos.add(detalleActual.getValor());
			}
		}
		if (!hayIngreso) {
			throw new IncomeException("La liquidación debe tener al menos un ingreso.");
		}

		Double totalIngresos = RedondeoNomina.suma(ingresos.toArray(new Double[0]));
		Double totalDescuentos = RedondeoNomina.suma(descuentos.toArray(new Double[0]));
		Double neto = RedondeoNomina
				.redondea(Double.valueOf(totalIngresos.doubleValue() - totalDescuentos.doubleValue()));
		if (neto.doubleValue() <= 0D) {
			throw new IncomeException("El neto a pagar debe ser mayor que cero.");
		}
		liquidacion.setTotalIngresos(totalIngresos);
		liquidacion.setTotalDescuentos(totalDescuentos);
		liquidacion.setNeto(neto);

		List<LiquidacionExterna> vigentes =
				liquidacionExternaDaoService.selectVigentesByIdentificacion(identificacion, codigoActual);
		if (!vigentes.isEmpty()) {
			throw new IncomeException(
					"Ya existe la liquidación N.º " + vigentes.get(0).getCodigo() + " para esta identificación.");
		}

		boolean tieneBanco = liquidacion.getBanco() != null && liquidacion.getBanco().getCodigo() != null;
		boolean tieneTipoCuenta = liquidacion.getTipoCuenta() != null;
		boolean tieneNumeroCuenta =
				liquidacion.getNumeroCuenta() != null && !liquidacion.getNumeroCuenta().trim().isEmpty();
		int cuantos = (tieneBanco ? 1 : 0) + (tieneTipoCuenta ? 1 : 0) + (tieneNumeroCuenta ? 1 : 0);
		if (cuantos != 0 && cuantos != 3) {
			throw new IncomeException(
					"Los datos bancarios deben venir completos (banco, tipo de cuenta y número de cuenta) o todos vacíos.");
		}
		if (cuantos == 0) {
			String nota = "Sin datos bancarios: solo se puede pagar con cheque.";
			String obs = liquidacion.getObservacion();
			liquidacion.setObservacion(
					(obs != null && !obs.trim().isEmpty()) ? obs.trim() + " | " + nota : nota);
		}
	}

	// =====================================================================
	// §4 — Tesorería
	// =====================================================================

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> enviarATesoreria(Long idLiquidacion, Long idUsuario) throws Throwable {
		System.out.println("=== enviarATesoreria liquidacion externa | id=" + idLiquidacion + " ===");

		LiquidacionExterna liquidacion = recuperaLiquidacion(idLiquidacion);
		if (liquidacion.getEstado() == null
				|| liquidacion.getEstado().intValue() != RhhEstadoLiquidacionExterna.REGISTRADA) {
			throw new IncomeException("La liquidación " + idLiquidacion + " está en estado "
					+ textoEstado(liquidacion.getEstado()) + ": solo se puede enviar a tesorería desde REGISTRADA.");
		}
		if (idUsuario == null) {
			throw new IncomeException("Falta idUsuario para registrar el pago en tesorería.");
		}
		Long idEmpresa = liquidacion.getEmpresa() != null ? liquidacion.getEmpresa().getCodigo() : null;
		if (idEmpresa == null) {
			throw new IncomeException("La liquidación " + idLiquidacion + " no tiene empresa: sin ella no se"
					+ " puede registrar el pago en la bandeja de tesorería.");
		}

		// Idempotencia (§4): si ya existe un pago vigente de este origen y este id, no se
		// crea otro -- se reusa.
		List<PagoProgramado> vigentes = pagoProgramadoDaoService
				.selectVigentesByOrigen(OrigenPagoExterno.RHH_LIQUIDACION_EXCOLABORADOR, idLiquidacion);
		Long idPago;
		if (!vigentes.isEmpty()) {
			idPago = vigentes.get(0).getId();
			System.out.println("La liquidación " + idLiquidacion + " ya tenía un pago vigente (" + idPago
					+ "): no se crea otro.");
		} else {
			if (liquidacion.getProductoPago() == null || liquidacion.getProductoPago().getId() == null) {
				throw new IncomeException("La liquidación " + idLiquidacion + " no tiene producto de pago:"
						+ " no se puede enviar a tesorería.");
			}

			String nombreCompleto = ((liquidacion.getApellidos() != null ? liquidacion.getApellidos() : "") + " "
					+ (liquidacion.getNombres() != null ? liquidacion.getNombres() : "")).trim();

			BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
			beneficiario.setNombre(nombreCompleto);
			beneficiario.setIdentificacion(liquidacion.getIdentificacion());
			if (liquidacion.getBanco() != null) {
				beneficiario.setIdBancoExterno(liquidacion.getBanco().getCodigo());
			}
			beneficiario.setTipoCuenta(liquidacion.getTipoCuenta());
			beneficiario.setNumeroCuenta(liquidacion.getNumeroCuenta());

			String concepto = "Liquidación " + nombreCompleto + " - administración anterior";

			LineaContablePago linea = new LineaContablePago();
			linea.setIdProductoPago(liquidacion.getProductoPago().getId());
			linea.setValor(liquidacion.getNeto());
			linea.setConcepto(concepto);
			List<LineaContablePago> desglose = new ArrayList<>();
			desglose.add(linea);

			Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeOrigenExterno(
					OrigenPagoExterno.RHH_LIQUIDACION_EXCOLABORADOR, idLiquidacion, idEmpresa, null,
					liquidacion.getNeto(), LocalDate.now().toString(), beneficiario, desglose, concepto, idUsuario,
					false, null);

			idPago = (Long) resultadoPago.get("pago");
			if (idPago == null) {
				throw new IncomeException("El circuito de pagos no devolvió el pago generado para la liquidación "
						+ idLiquidacion + ".");
			}
		}

		liquidacion.setIdPago(idPago);
		liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacionExterna.EN_TESORERIA));
		liquidacion = liquidacionExternaDaoService.save(liquidacion, liquidacion.getCodigo());

		Map<String, Object> resultado = new LinkedHashMap<>();
		resultado.put("liquidacion", liquidacion);
		resultado.put("idPago", idPago);
		System.out.println("✓ Liquidación externa " + idLiquidacion + " enviada a tesorería | idPago=" + idPago);
		return resultado;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public LiquidacionExterna sincronizarPago(Long idLiquidacion) throws Throwable {
		System.out.println("=== sincronizarPago liquidacion externa | id=" + idLiquidacion + " ===");
		return sincroniza(recuperaLiquidacion(idLiquidacion));
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public LiquidacionExterna anular(Long idLiquidacion, Long idUsuario, String motivo) throws Throwable {
		System.out.println("=== anular liquidacion externa | id=" + idLiquidacion + " ===");
		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}
		LiquidacionExterna liquidacion = recuperaLiquidacion(idLiquidacion);
		int estado = liquidacion.getEstado() != null ? liquidacion.getEstado().intValue() : 0;

		if (estado == RhhEstadoLiquidacionExterna.PAGADA) {
			throw new IncomeException("La liquidación " + idLiquidacion + " ya está PAGADA: no se anula desde"
					+ " aquí. Reverse el pago en Tesorería primero.");
		}
		if (estado == RhhEstadoLiquidacionExterna.ANULADA) {
			throw new IncomeException("La liquidación " + idLiquidacion + " ya está ANULADA.");
		}

		// §4, literal: EN_TESORERIA solo se anula "si el pago sigue POR_APROBAR". Interpretado
		// estricto (no como POR_APROBAR||REGISTRADO de OrdenBeneficioSocialServiceImpl.anular):
		// cualquier otro estado del pago (REGISTRADO en adelante) rechaza, en vez de anular en
		// silencio un pago que ya pudo haber avanzado. Ver el reporte al arbitro.
		if (estado == RhhEstadoLiquidacionExterna.EN_TESORERIA && liquidacion.getIdPago() != null) {
			Long estadoPago = estadoPago(liquidacion.getIdPago());
			if (estadoPago == null) {
				throw new IncomeException("La liquidación " + idLiquidacion + " está EN_TESORERIA pero no se"
						+ " encontró su pago N.º " + liquidacion.getIdPago() + ": revísela antes de anular.");
			}
			if (estadoPago.intValue() != EstadoPagoProgramado.POR_APROBAR) {
				throw new IncomeException("El pago N.º " + liquidacion.getIdPago() + " de la liquidación "
						+ idLiquidacion + " ya no está POR_APROBAR (está " + textoEstadoPago(estadoPago) + "): "
						+ instruccionSegunEstadoPago(estadoPago.intValue()));
			}
			pagoProgramadoService.anularPago(liquidacion.getIdPago(),
					"Anulación de la liquidación de ex-colaborador " + idLiquidacion + ": " + motivo.trim(),
					idUsuario);
			System.out.println(
					"✓ Pago " + liquidacion.getIdPago() + " anulado junto con la liquidación " + idLiquidacion + ".");
		}

		liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacionExterna.ANULADA));
		liquidacion.setMotivoAnulacion(motivo.trim());
		liquidacion = liquidacionExternaDaoService.save(liquidacion, liquidacion.getCodigo());

		System.out.println("✓ Liquidación externa " + idLiquidacion + " anulada. Motivo: " + motivo);
		return liquidacion;
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	/**
	 * Recupera la liquidacion por id. {@code EntityDaoImpl.selectById} usa
	 * {@code getSingleResult()}: una fila faltante lanza {@code NoResultException}, no
	 * devuelve null (CLAUDE.md, "El DAO generico").
	 *
	 * @param idLiquidacion	: Id a buscar
	 * @return				: La liquidacion
	 * @throws Throwable	: IncomeException si no existe
	 */
	private LiquidacionExterna recuperaLiquidacion(Long idLiquidacion) throws Throwable {
		try {
			return liquidacionExternaDaoService.selectById(idLiquidacion, NombreEntidadesRhh.LIQUIDACION_EXTERNA);
		} catch (NoResultException e) {
			throw new IncomeException("No existe la liquidación " + idLiquidacion + ".");
		}
	}

	/**
	 * Sincroniza una liquidacion EN_TESORERIA contra su pago (§4): CONFIRMADO -&gt; PAGADA
	 * (copia fechaPago e idAsiento); RECHAZADO/ANULADO -&gt; REGISTRADA con idPago null. Sin
	 * efecto si la liquidacion no esta EN_TESORERIA o no tiene idPago.
	 *
	 * <p>Solo el escalar del pago -- {@code PagoProgramado} tiene varios {@code @ManyToOne}
	 * EAGER, mismo criterio que {@code PlanillaIessServiceImpl}/{@code CajaChicaServiceImpl}.</p>
	 *
	 * @param liquidacion	: Liquidacion a sincronizar
	 * @return				: La liquidacion, guardada si cambio de estado
	 * @throws Throwable	: Excepcion
	 */
	private LiquidacionExterna sincroniza(LiquidacionExterna liquidacion) throws Throwable {
		if (liquidacion.getEstado() == null
				|| liquidacion.getEstado().intValue() != RhhEstadoLiquidacionExterna.EN_TESORERIA
				|| liquidacion.getIdPago() == null) {
			return liquidacion;
		}
		Object[] fila;
		try {
			fila = (Object[]) em
					.createQuery("select p.estado, p.fechaRespuesta, p.asiento.codigo from PagoProgramado p"
							+ " where p.id = :id")
					.setParameter("id", liquidacion.getIdPago())
					.getSingleResult();
		} catch (NoResultException e) {
			return liquidacion;
		}
		Long estadoPago = (Long) fila[0];
		if (estadoPago == null) {
			return liquidacion;
		}
		int estado = estadoPago.intValue();
		if (estado == EstadoPagoProgramado.CONFIRMADO) {
			liquidacion.setFechaPago((LocalDate) fila[1]);
			liquidacion.setIdAsiento((Long) fila[2]);
			liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacionExterna.PAGADA));
			liquidacion = liquidacionExternaDaoService.save(liquidacion, liquidacion.getCodigo());
			System.out.println("✓ Liquidación externa " + liquidacion.getCodigo() + " sincronizada: PAGADA.");
		} else if (estado == EstadoPagoProgramado.RECHAZADO || estado == EstadoPagoProgramado.ANULADO) {
			liquidacion.setIdPago(null);
			liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacionExterna.REGISTRADA));
			liquidacion = liquidacionExternaDaoService.save(liquidacion, liquidacion.getCodigo());
			System.out
					.println("✓ Liquidación externa " + liquidacion.getCodigo() + " sincronizada: vuelve a REGISTRADA.");
		}
		return liquidacion;
	}

	/**
	 * Sincroniza en el sitio cada liquidacion EN_TESORERIA de la lista (getAll/selectByCriteria, §6).
	 *
	 * @param lista			: Lista a sincronizar
	 * @throws Throwable	: Excepcion
	 */
	private void sincronizaLista(List<LiquidacionExterna> lista) throws Throwable {
		for (int i = 0; i < lista.size(); i++) {
			LiquidacionExterna liquidacion = lista.get(i);
			if (liquidacion.getEstado() != null
					&& liquidacion.getEstado().intValue() == RhhEstadoLiquidacionExterna.EN_TESORERIA) {
				lista.set(i, sincroniza(liquidacion));
			}
		}
	}

	private Long estadoPago(Long idPago) throws Throwable {
		try {
			return (Long) em.createQuery("select p.estado from PagoProgramado p where p.id = :id")
					.setParameter("id", idPago)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	private String textoEstado(Long estado) {
		if (estado == null) {
			return "sin estado";
		}
		switch (estado.intValue()) {
			case RhhEstadoLiquidacionExterna.REGISTRADA:
				return "REGISTRADA";
			case RhhEstadoLiquidacionExterna.EN_TESORERIA:
				return "EN_TESORERIA";
			case RhhEstadoLiquidacionExterna.PAGADA:
				return "PAGADA";
			case RhhEstadoLiquidacionExterna.ANULADA:
				return "ANULADA";
			default:
				return "ESTADO " + estado;
		}
	}

	/**
	 * Instrucción para el usuario cuando el pago de la liquidación ya avanzó más allá de
	 * POR_APROBAR y por eso {@code anular} lo rechaza (decisión del árbitro, 2026-09-30).
	 *
	 * @param estadoPago	: Estado actual del pago, distinto de POR_APROBAR
	 * @return				: Qué hacer antes de poder anular la liquidación
	 */
	private String instruccionSegunEstadoPago(int estadoPago) {
		switch (estadoPago) {
			case EstadoPagoProgramado.REGISTRADO:
			case EstadoPagoProgramado.EN_ARCHIVO:
				return "Tesorería ya lo aprobó: recházelo allá primero y luego anule la liquidación aquí.";
			case EstadoPagoProgramado.CONFIRMADO:
				return "Tesorería ya lo confirmó: sincronice el pago (la liquidación pasará a PAGADA) y,"
						+ " si corresponde deshacerlo, revierta el pago confirmado en Tesorería.";
			default:
				return "Revíselo en Tesorería antes de anular la liquidación.";
		}
	}

	private String textoEstadoPago(Long estado) {
		if (estado == null) {
			return "sin pago";
		}
		switch (estado.intValue()) {
			case EstadoPagoProgramado.POR_APROBAR:
				return "POR_APROBAR";
			case EstadoPagoProgramado.REGISTRADO:
				return "REGISTRADO";
			case EstadoPagoProgramado.EN_ARCHIVO:
				return "EN_ARCHIVO";
			case EstadoPagoProgramado.CONFIRMADO:
				return "CONFIRMADO";
			case EstadoPagoProgramado.RECHAZADO:
				return "RECHAZADO";
			case EstadoPagoProgramado.ANULADO:
				return "ANULADO";
			default:
				return "ESTADO " + estado;
		}
	}
}
