package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.tsr.dao.CierreCajaChicaDaoService;
import com.saa.ejb.tsr.dao.MovimientoCajaChicaDaoService;
import com.saa.ejb.tsr.service.CajaChicaService;
import com.saa.ejb.tsr.service.MovimientoCajaChicaService;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CajaChica;
import com.saa.model.tsr.CierreCajaChica;
import com.saa.model.tsr.MovimientoCajaChica;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoCajaChica;
import com.saa.rubros.EstadoMovimientoCajaChica;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.TipoMovimientoCajaChica;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementación de MovimientoCajaChicaService.</p>
 */
@Stateless
public class MovimientoCajaChicaServiceImpl implements MovimientoCajaChicaService {

	private static final double TOLERANCIA = 0.01;

	@EJB
	private MovimientoCajaChicaDaoService movimientoCajaChicaDaoService;

	@EJB
	private CierreCajaChicaDaoService cierreCajaChicaDaoService;

	@EJB
	private CajaChicaService cajaChicaService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@EJB
	private PagoProgramadoService pagoProgramadoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public MovimientoCajaChica selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById MovimientoCajaChica con id: " + id);
		return movimientoCajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.MOVIMIENTO_CAJA_CHICA);
	}

	@Override
	public List<MovimientoCajaChica> selectAll() throws Throwable {
		List<MovimientoCajaChica> result =
				movimientoCajaChicaDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total MovimientoCajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<MovimientoCajaChica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<MovimientoCajaChica> result = movimientoCajaChicaDaoService.selectByCriteria(datos,
				NombreEntidadesTesoreria.MOVIMIENTO_CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio MovimientoCajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public MovimientoCajaChica saveSingle(MovimientoCajaChica movimiento) throws Throwable {
		if (movimiento.getCodigo() == null) {
			if (movimiento.getEstado() == null) {
				movimiento.setEstado(Long.valueOf(EstadoMovimientoCajaChica.ACTIVO));
			}
			if (movimiento.getFechaRegistro() == null) {
				movimiento.setFechaRegistro(LocalDateTime.now());
			}
		}
		return movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());
	}

	@Override
	public void save(List<MovimientoCajaChica> lista) throws Throwable {
		for (MovimientoCajaChica registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		MovimientoCajaChica entidad = new MovimientoCajaChica();
		for (Long registro : id) {
			movimientoCajaChicaDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Gasto
	// =====================================================================

	@Override
	public MovimientoCajaChica registrarGasto(Long idCaja, LocalDate fecha, Double valor, String descripcion,
			String observacion, Long idProducto, Long idTitular, String numeroDocumento, Long idUsuario)
			throws Throwable {

		System.out.println("=== registrarGasto caja chica | caja=" + idCaja + " | valor=" + valor + " ===");

		CajaChica caja = cajaChicaService.selectById(idCaja);
		if (caja.getEstado() == null || caja.getEstado().intValue() != EstadoCajaChica.ACTIVA) {
			throw new IncomeException("La caja chica '" + caja.getNombre() + "' no está activa.");
		}
		if (observacion == null || observacion.trim().isEmpty()) {
			throw new IncomeException("Debe indicar la observación del gasto.");
		}
		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del gasto debe ser mayor a cero.");
		}
		if (descripcion == null || descripcion.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el concepto del gasto.");
		}
		ProductoPago producto = validaProducto(idProducto);

		double saldoActual = ((Number) cajaChicaService.saldo(idCaja).get("saldo")).doubleValue();
		if (valor > saldoActual + TOLERANCIA) {
			throw new IncomeException("El valor del gasto ($"
					+ String.format(java.util.Locale.US, "%.2f", valor)
					+ ") supera el saldo disponible de la caja '" + caja.getNombre() + "' ($"
					+ String.format(java.util.Locale.US, "%.2f", saldoActual) + ").");
		}
		if (caja.getMontoMaximoGasto() != null && valor > caja.getMontoMaximoGasto()) {
			throw new IncomeException("El valor del gasto ($"
					+ String.format(java.util.Locale.US, "%.2f", valor)
					+ ") supera el tope permitido por gasto de la caja '" + caja.getNombre() + "' ($"
					+ String.format(java.util.Locale.US, "%.2f", caja.getMontoMaximoGasto()) + ").");
		}

		LocalDate fechaGasto = (fecha != null) ? fecha : LocalDate.now();
		CierreCajaChica ultimoCierre = cierreCajaChicaDaoService.selectUltimoCerrado(idCaja);
		if (ultimoCierre != null && ultimoCierre.getFechaFin() != null
				&& !fechaGasto.isAfter(ultimoCierre.getFechaFin())) {
			throw new IncomeException("La fecha del gasto (" + fechaGasto + ") debe ser posterior al "
					+ "último cierre de la caja (" + ultimoCierre.getFechaFin() + ").");
		}
		rechazaSiEnBorrador(idCaja, fechaGasto, "el gasto");

		MovimientoCajaChica movimiento = new MovimientoCajaChica();
		movimiento.setCajaChica(caja);
		movimiento.setTipo(Long.valueOf(TipoMovimientoCajaChica.GASTO));
		movimiento.setFecha(fechaGasto);
		movimiento.setValor(valor);
		movimiento.setDescripcion(descripcion.trim());
		movimiento.setObservacion(observacion.trim());
		movimiento.setProducto(producto);
		if (idTitular != null) {
			movimiento.setTitular(em.find(Titular.class, idTitular));
		}
		movimiento.setNumeroDocumento(numeroDocumento);
		movimiento.setUsuario(idUsuario);
		movimiento = saveSingle(movimiento);
		em.flush();

		String observacionAsiento = "Gasto caja chica " + caja.getNombre()
				+ " | " + descripcion.trim()
				+ " | Doc: " + nvl(numeroDocumento, "")
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

		com.saa.model.cnt.Asiento asientoGenerado = asientoContableService.generarAsientoGastoCajaChica(
				idProducto, caja.getNombre(), descripcion.trim(), valor, caja.getPlanCuenta().getCodigo(),
				caja.getEmpresa().getCodigo(), fechaGasto, observacionAsiento, usuarioNombre(idUsuario));

		movimiento.setAsiento(asientoGenerado);
		movimiento = movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());
		em.flush();

		System.out.println("✓ Gasto de caja chica registrado: id=" + movimiento.getCodigo()
				+ " | asiento=" + asientoGenerado.getNumeroAlterno());
		return movimiento;
	}

	/**
	 * Rechaza si la caja tiene un cierre en BORRADOR cuyo periodo
	 * [fechaInicio, fechaFin] contiene la fecha indicada. Un BORRADOR ya
	 * congeló sus totales en {@code prepararCierre}; registrar o anular un
	 * movimiento dentro de su rango los desactualiza en silencio hasta que
	 * {@code confirmarCierre} los recalcula — pero mientras tanto el arqueo
	 * en pantalla estaría mostrando números que ya no son ciertos.
	 * @param idCaja      : Id de la caja chica
	 * @param fecha       : Fecha del movimiento a registrar/anular
	 * @param descripcion : Qué se está intentando hacer, para el mensaje ("el gasto", "el movimiento N")
	 * @throws Throwable  : IncomeException si hay un BORRADOR que cubre esa fecha
	 */
	private void rechazaSiEnBorrador(Long idCaja, LocalDate fecha, String descripcion) throws Throwable {
		if (fecha == null) {
			return;
		}
		CierreCajaChica borrador = cierreCajaChicaDaoService.selectBorrador(idCaja);
		if (borrador == null || borrador.getFechaInicio() == null || borrador.getFechaFin() == null) {
			return;
		}
		if (!fecha.isBefore(borrador.getFechaInicio()) && !fecha.isAfter(borrador.getFechaFin())) {
			throw new IncomeException("Hay un cierre en preparación (BORRADOR N° " + borrador.getCodigo()
					+ ") que cubre del " + borrador.getFechaInicio() + " al " + borrador.getFechaFin()
					+ ": no se puede registrar ni anular " + descripcion + " en ese rango hasta "
					+ "confirmar o anular el cierre.");
		}
	}

	private ProductoPago validaProducto(Long idProducto) throws Throwable {
		if (idProducto == null) {
			throw new IncomeException("Debe indicar el producto que clasifica el gasto.");
		}
		ProductoPago producto = em.find(ProductoPago.class, idProducto);
		if (producto == null) {
			throw new IncomeException("No se encontró el producto CXP con ID: " + idProducto);
		}
		if (producto.getGrupoProducto() == null) {
			throw new IncomeException("El producto '" + producto.getNombre()
					+ "' no tiene grupo asignado. Clasifíquelo en CXP → Productos antes de usarlo.");
		}
		if (producto.getGrupoProducto().getPlanCuenta() == null) {
			throw new IncomeException("El grupo '" + producto.getGrupoProducto().getNombre()
					+ "' del producto '" + producto.getNombre()
					+ "' no tiene cuenta contable configurada (Contabilidad → Grupos de Producto).");
		}
		return producto;
	}

	@Override
	public void anularGasto(Long idMovimiento, String motivo, Long idUsuario) throws Throwable {

		System.out.println("=== anularGasto caja chica | movimiento=" + idMovimiento + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}
		MovimientoCajaChica movimiento = selectById(idMovimiento);

		int tipo = (movimiento.getTipo() != null) ? movimiento.getTipo().intValue() : 0;
		if (tipo != TipoMovimientoCajaChica.GASTO) {
			String refPago = (movimiento.getPagoProgramado() != null)
					? String.valueOf(movimiento.getPagoProgramado().getId()) : "desconocido";
			throw new IncomeException("El movimiento " + idMovimiento + " no es un gasto. "
					+ "Reverse el pago programado N° " + refPago + " (pgtr/revertirConfirmado).");
		}
		if (movimiento.getEstado() != null
				&& movimiento.getEstado().intValue() == EstadoMovimientoCajaChica.ANULADO) {
			throw new IncomeException("El movimiento " + idMovimiento + " ya está anulado.");
		}
		if (movimiento.getCierre() != null) {
			throw new IncomeException("El movimiento " + idMovimiento + " ya quedó incluido en el "
					+ "cierre N° " + movimiento.getCierre().getCodigo() + ": no se puede anular.");
		}
		rechazaSiEnBorrador(movimiento.getCajaChica().getCodigo(), movimiento.getFecha(),
				"el movimiento " + idMovimiento);

		Long idAsiento = (movimiento.getAsiento() != null) ? movimiento.getAsiento().getCodigo() : null;
		if (idAsiento != null) {
			try {
				asientoService.anulaAsiento(idAsiento);
				System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
			} catch (Throwable e) {
				System.err.println("⚠ No se pudo anular el asiento " + idAsiento + ": " + e.getMessage());
			}
		}

		movimiento.setEstado(Long.valueOf(EstadoMovimientoCajaChica.ANULADO));
		movimiento.setMotivoAnulacion(motivo.trim());
		movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());

		System.out.println("✓ Gasto de caja chica " + idMovimiento + " anulado. Motivo: " + motivo);
	}

	// =====================================================================
	// Apertura / reposición desde banco
	// =====================================================================

	@Override
	public Map<String, Object> registrarReposicion(Long idCaja, Double valor, Long idCuentaBancariaOrigen,
			Long formaPago, boolean debitoAutomatico, String referencia, LocalDate fecha,
			String descripcion, Long idUsuario) throws Throwable {
		return registrarPagoBanco(idCaja, valor, idCuentaBancariaOrigen, formaPago, debitoAutomatico,
				referencia, fecha, descripcion, idUsuario, TipoMovimientoCajaChica.REPOSICION);
	}

	@Override
	public Map<String, Object> registrarApertura(Long idCaja, Double valor, Long idCuentaBancariaOrigen,
			Long formaPago, boolean debitoAutomatico, String referencia, LocalDate fecha,
			String descripcion, Long idUsuario) throws Throwable {
		return registrarPagoBanco(idCaja, valor, idCuentaBancariaOrigen, formaPago, debitoAutomatico,
				referencia, fecha, descripcion, idUsuario, TipoMovimientoCajaChica.APERTURA);
	}

	private Map<String, Object> registrarPagoBanco(Long idCaja, Double valor, Long idCuentaBancariaOrigen,
			Long formaPago, boolean debitoAutomatico, String referencia, LocalDate fecha,
			String descripcion, Long idUsuario, int tipo) throws Throwable {

		String etiqueta = (tipo == TipoMovimientoCajaChica.APERTURA) ? "apertura" : "reposición";
		System.out.println("=== registrar " + etiqueta + " caja chica | caja=" + idCaja
				+ " | valor=" + valor + " | formaPago=" + formaPago + " ===");

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor de la " + etiqueta + " debe ser mayor a cero.");
		}
		// idCuentaBancariaOrigen es OPCIONAL (2026-08-30): con cuenta nula el pago nace
		// POR_APROBAR y tesorería elige cheque o débito automático al aprobar, así que
		// aquí ya no se valida forma de pago — la restricción se trasladó a
		// PagoProgramadoServiceImpl.aprobar (rechaza Transferencia para este origen).
		if (idCuentaBancariaOrigen != null) {
			// La caja chica no tiene cuenta bancaria externa de destino: la
			// transferencia (formaPago=2) la exige y falla con un mensaje que
			// habla del archivo del banco, sin decir nada de caja chica. Cheque y
			// débito automático además contabilizan en el acto, así que el saldo
			// nunca sube antes de que el dinero realmente entre.
			long fp = (formaPago != null) ? formaPago.longValue()
					: (debitoAutomatico ? com.saa.rubros.FormaPagoProgramado.DEBITO_AUTOMATICO
							: com.saa.rubros.FormaPagoProgramado.TRANSFERENCIA);
			if (fp != com.saa.rubros.FormaPagoProgramado.CHEQUE
					&& fp != com.saa.rubros.FormaPagoProgramado.DEBITO_AUTOMATICO) {
				throw new IncomeException("La reposición de caja chica debe pagarse con cheque o "
						+ "débito automático: la caja no tiene cuenta bancaria de destino.");
			}
		}

		CajaChica caja = cajaChicaService.selectById(idCaja);
		if (caja.getEstado() == null || caja.getEstado().intValue() != EstadoCajaChica.ACTIVA) {
			throw new IncomeException("La caja chica '" + caja.getNombre() + "' no está activa.");
		}

		double saldoActual = ((Number) cajaChicaService.saldo(idCaja).get("saldo")).doubleValue();
		double fondo = (caja.getMontoFondo() != null) ? caja.getMontoFondo() : 0.0;
		if (tipo == TipoMovimientoCajaChica.APERTURA) {
			if (saldoActual > TOLERANCIA) {
				throw new IncomeException("La caja '" + caja.getNombre() + "' ya tiene saldo ($"
						+ String.format(java.util.Locale.US, "%.2f", saldoActual)
						+ "): no se puede registrar una apertura nueva.");
			}
		} else {
			double disponible = fondo - saldoActual;
			if (valor > disponible + TOLERANCIA) {
				throw new IncomeException("El valor de la reposición ($"
						+ String.format(java.util.Locale.US, "%.2f", valor)
						+ ") supera lo que falta para llegar al fondo de la caja '" + caja.getNombre()
						+ "' ($" + String.format(java.util.Locale.US, "%.2f", disponible) + ").");
			}
		}

		LocalDate fechaMovimiento = (fecha != null) ? fecha : LocalDate.now();
		CierreCajaChica ultimoCierre = cierreCajaChicaDaoService.selectUltimoCerrado(idCaja);
		if (ultimoCierre != null && ultimoCierre.getFechaFin() != null
				&& !fechaMovimiento.isAfter(ultimoCierre.getFechaFin())) {
			throw new IncomeException("La fecha de la " + etiqueta + " (" + fechaMovimiento
					+ ") debe ser posterior al último cierre de la caja (" + ultimoCierre.getFechaFin() + ").");
		}
		rechazaSiEnBorrador(idCaja, fechaMovimiento, "la " + etiqueta);

		MovimientoCajaChica movimiento = new MovimientoCajaChica();
		movimiento.setCajaChica(caja);
		movimiento.setTipo(Long.valueOf(tipo));
		movimiento.setFecha(fechaMovimiento);
		movimiento.setValor(valor);
		movimiento.setDescripcion((descripcion != null && !descripcion.trim().isEmpty())
				? descripcion.trim() : (tipo == TipoMovimientoCajaChica.APERTURA
						? "Apertura de caja chica" : "Reposición de caja chica"));
		movimiento.setUsuario(idUsuario);
		movimiento = saveSingle(movimiento);
		em.flush();

		BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
		beneficiario.setNombre(caja.getNombre());
		beneficiario.setIdentificacion("CAJACHICA-" + caja.getCodigo());

		Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeOrigenExterno(
				OrigenPagoExterno.TSR_CAJA_CHICA, movimiento.getCodigo(), caja.getEmpresa().getCodigo(),
				idCuentaBancariaOrigen, valor, fechaMovimiento.toString(), beneficiario, null,
				movimiento.getDescripcion(), idUsuario,
				idCuentaBancariaOrigen != null && debitoAutomatico, referencia,
				idCuentaBancariaOrigen != null ? formaPago : null);

		Long idPago = (Long) resultadoPago.get("pago");
		if (idPago != null) {
			PagoProgramado pago = em.find(PagoProgramado.class, idPago);
			movimiento.setPagoProgramado(pago);
			movimiento = movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());

			Map<String, Object> resultado = new HashMap<>();
			resultado.put("idMovimiento", movimiento.getCodigo());
			resultado.put("idPago", idPago);
			resultado.put("estadoPago", pago.getEstado());
			resultado.put("numeroCheque", resultadoPago.get("numeroCheque"));
			resultado.put("mensaje", resultadoPago.get("mensaje"));
			System.out.println("✓ " + etiqueta + " de caja chica registrada: movimiento="
					+ movimiento.getCodigo() + " | pago=" + idPago);
			return resultado;
		}

		// No debería pasar (registrarPagoDeOrigenExterno siempre devuelve "pago"),
		// pero se deja explícito en vez de un NPE silencioso.
		throw new IncomeException("El circuito de pagos no devolvió el pago generado para la "
				+ etiqueta + " de la caja '" + caja.getNombre() + "'.");
	}

	// =====================================================================
	// Consultas
	// =====================================================================

	@Override
	public List<MovimientoCajaChica> listar(Long idCaja, LocalDate desde, LocalDate hasta, Long tipo,
			Long estado) throws Throwable {
		System.out.println("=== listar movimientos caja chica | caja=" + idCaja + " ===");
		return movimientoCajaChicaDaoService.selectByCaja(idCaja, desde, hasta, tipo, estado);
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	private String usuarioNombre(Long idUsuario) {
		if (idUsuario == null) {
			return "SISTEMA";
		}
		Usuario usuario = em.find(Usuario.class, idUsuario);
		return (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "SISTEMA";
	}

	private String nvl(String valor, String porDefecto) {
		return (valor != null) ? valor : porDefecto;
	}

}
