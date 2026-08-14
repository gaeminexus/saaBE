package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.EstadoPagoFactura;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoDocPagoAplicacion;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class AplicacionPagoCxpServiceImpl implements AplicacionPagoCxpService {

	/** Tolerancia para comparar valores monetarios. */
	private static final double TOLERANCIA = 0.01;

	/** Forma de pago transferencia (rubro TipoFormaPago del sistema). */
	private static final long FORMA_PAGO_TRANSFERENCIA = 2L;

	/** Forma de pago débito automático: el banco debita la cuenta por convenio. */
	private static final long FORMA_PAGO_DEBITO_AUTOMATICO = 4L;

	@EJB
	private AplicacionPagoCxpDaoService aplicacionPagoCxpDaoService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@EJB
	private MovimientoBancoService movimientoBancoService;

	@EJB
	private PersonaCuentaContableDaoService personaCuentaContableDaoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public AplicacionPagoCxp selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById AplicacionPagoCxp con id: " + id);
		return aplicacionPagoCxpDaoService.selectById(id, NombreEntidadesCompra.APLICACION_PAGO_CXP);
	}

	@Override
	public List<AplicacionPagoCxp> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll AplicacionPagoCxpService");
		List<AplicacionPagoCxp> result =
				aplicacionPagoCxpDaoService.selectAll(NombreEntidadesCompra.APLICACION_PAGO_CXP);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total AplicacionPagoCxp no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<AplicacionPagoCxp> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria AplicacionPagoCxpService");
		List<AplicacionPagoCxp> result = aplicacionPagoCxpDaoService.selectByCriteria(datos,
				NombreEntidadesCompra.APLICACION_PAGO_CXP);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio AplicacionPagoCxp no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public AplicacionPagoCxp saveSingle(AplicacionPagoCxp aplicacion) throws Throwable {
		System.out.println("saveSingle - AplicacionPagoCxp");
		if (aplicacion.getId() == null) {
			if (aplicacion.getEstado() == null) {
				aplicacion.setEstado(Long.valueOf(EstadoAplicacionPago.ACTIVO));
			}
			if (aplicacion.getFechaRegistro() == null) {
				aplicacion.setFechaRegistro(LocalDateTime.now());
			}
		}
		aplicacion = aplicacionPagoCxpDaoService.save(aplicacion, aplicacion.getId());
		em.flush();

		// El estado de pago de la factura lo calcula el backend, no la base de datos.
		if (aplicacion.getFacturaCompra() != null) {
			recalcularEstadoPago(aplicacion.getFacturaCompra().getId());
		}
		return aplicacion;
	}

	@Override
	public void save(List<AplicacionPagoCxp> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de AplicacionPagoCxpService");
		for (AplicacionPagoCxp registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de AplicacionPagoCxpService");
		AplicacionPagoCxp entidad = new AplicacionPagoCxp();
		for (Long registro : id) {
			aplicacionPagoCxpDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Aplicaciones automáticas (misma transacción que el asiento)
	// =====================================================================

	@Override
	public AplicacionPagoCxp aplicarRetencionEmitida(RetencionV2 retencion, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarRetencionEmitida | retencion=" + retencion.getId()
				+ " | empresa=" + idEmpresa + " ===");

		Long idProveedor = (retencion.getProveedor() != null)
				? retencion.getProveedor().getCodigo() : null;

		// El documento sustento de la retención vive en sus detalles.
		String numeroFactura = obtenerNumeroDocSustento(retencion.getId());
		FacturaCompra factura = resolverFacturaCompraPorNumero(numeroFactura, idProveedor, idEmpresa);

		AplicacionPagoCxp aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.RETENCION, retencion.getTotal(),
				fechaDe(retencion.getFecha()),
				"Retención V2 N° " + retencion.getNumero(), usuario);
		aplicacion.setRetencionV2(retencion);
		aplicacion.setAsiento(asiento);

		validaMontoContraSaldo(factura, aplicacion.getMontoAplicado(), null);

		aplicacion = saveSingle(aplicacion);
		System.out.println("✓ Aplicación por retención creada: id=" + aplicacion.getId()
				+ " | factura=" + factura.getId() + " | monto=" + aplicacion.getMontoAplicado());
		return aplicacion;
	}

	@Override
	public AplicacionPagoCxp aplicarNotaCredito(NotaCreditoCompra notaCredito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarNotaCredito | notaCredito=" + notaCredito.getId()
				+ " | empresa=" + idEmpresa + " ===");

		Long idProveedor = (notaCredito.getTitular() != null)
				? notaCredito.getTitular().getCodigo() : null;

		FacturaCompra factura = resolverFacturaCompraPorNumero(
				notaCredito.getNumDocModificado(), idProveedor, idEmpresa);

		AplicacionPagoCxp aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.NOTA_CREDITO, notaCredito.getTotal(),
				fechaDe(notaCredito.getFecha()),
				"Nota de Crédito N° " + notaCredito.getNumero(), usuario);
		aplicacion.setNotaCredito(notaCredito);
		aplicacion.setAsiento(asiento);

		validaMontoContraSaldo(factura, aplicacion.getMontoAplicado(), null);

		aplicacion = saveSingle(aplicacion);
		System.out.println("✓ Aplicación por nota de crédito creada: id=" + aplicacion.getId()
				+ " | factura=" + factura.getId() + " | monto=" + aplicacion.getMontoAplicado());
		return aplicacion;
	}

	@Override
	public AplicacionPagoCxp aplicarNotaDebito(NotaDebitoCompra notaDebito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarNotaDebito | notaDebito=" + notaDebito.getId()
				+ " | empresa=" + idEmpresa + " ===");

		Long idProveedor = (notaDebito.getTitular() != null)
				? notaDebito.getTitular().getCodigo() : null;

		FacturaCompra factura = resolverFacturaCompraPorNumero(
				notaDebito.getNumDocModificado(), idProveedor, idEmpresa);

		// La nota de débito AUMENTA el saldo de la factura: monto negativo.
		Double monto = (notaDebito.getTotal() != null) ? -Math.abs(notaDebito.getTotal()) : null;

		AplicacionPagoCxp aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.NOTA_DEBITO, monto,
				fechaDe(notaDebito.getFecha()),
				"Nota de Débito N° " + notaDebito.getNumero(), usuario);
		aplicacion.setNotaDebito(notaDebito);
		aplicacion.setAsiento(asiento);

		aplicacion = saveSingle(aplicacion);
		System.out.println("✓ Aplicación por nota de débito creada: id=" + aplicacion.getId()
				+ " | factura=" + factura.getId() + " | monto=" + aplicacion.getMontoAplicado());
		return aplicacion;
	}

	// =====================================================================
	// Aplicaciones desde la pantalla de tesorería
	// =====================================================================

	@Override
	public Map<String, Object> aplicarAnticipo(Long idFacturaCompra, Double valor,
			String fechaAplicacion, Long idEmpresa, Long idUsuario, String observacion)
			throws Throwable {

		System.out.println("=== aplicarAnticipo | factura=" + idFacturaCompra
				+ " | valor=" + valor + " | empresa=" + idEmpresa + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor a cruzar debe ser mayor a cero.");
		}

		FacturaCompra factura = em.find(FacturaCompra.class, idFacturaCompra);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de compra con ID: " + idFacturaCompra);
		}
		if (factura.getTitular() == null || factura.getTitular().getCodigo() == null) {
			throw new IncomeException("La factura de compra " + idFacturaCompra
					+ " no tiene proveedor asignado.");
		}
		Long idProveedor = factura.getTitular().getCodigo();

		// 1. La factura debe tener saldo suficiente
		validaMontoContraSaldo(factura, valor, null);

		// 2. El proveedor debe tener saldo de anticipos suficiente
		PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(idProveedor, idEmpresa);
		double saldoAnticipos = (cuentaAnticipos.getSaldoInicial() != null)
				? cuentaAnticipos.getSaldoInicial() : 0.0;
		if (saldoAnticipos + TOLERANCIA < valor) {
			throw new IncomeException("El proveedor '" + factura.getTitular().getNombre()
					+ "' tiene un saldo de anticipos de $"
					+ String.format(java.util.Locale.US, "%.2f", saldoAnticipos)
					+ " y no alcanza para cruzar $"
					+ String.format(java.util.Locale.US, "%.2f", valor) + ".");
		}

		LocalDate fecha = parseFecha(fechaAplicacion);
		String observacionAsiento = "Cruce de anticipo | Proveedor: " + factura.getTitular().getNombre()
				+ " | Factura: " + factura.getNumero()
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

		// 3. Asiento contable del cruce
		Asiento asiento = asientoContableService.generarAsientoAplicacionAnticipoProveedor(
				idProveedor, valor, idEmpresa, TipoAsientos.APLICACION_ANTICIPO_PROVEEDOR,
				fecha, observacionAsiento, usuarioNombre(idUsuario));

		// 4. Descontar el saldo de anticipos del proveedor
		cuentaAnticipos.setSaldoInicial(saldoAnticipos - valor);
		em.merge(cuentaAnticipos);
		System.out.println("✓ Saldo de anticipos del proveedor " + idProveedor + ": "
				+ saldoAnticipos + " → " + cuentaAnticipos.getSaldoInicial());

		// 5. Movimiento negativo en PGS.ANTP: sin él, el listado de anticipos
		// del proveedor no refleja el cruce y su saldo acumulado queda
		// desactualizado (el saldo de cada fila es el acumulado al momento
		// del movimiento, igual que al confirmar un anticipo).
		AnticipoProveedor movimiento = new AnticipoProveedor();
		movimiento.setTitular(factura.getTitular());
		movimiento.setEmpresa(em.find(Empresa.class, idEmpresa));
		movimiento.setUsuario(em.find(Usuario.class, idUsuario));
		movimiento.setFechaAnticipo(fecha);
		movimiento.setFechaRecepcion(fecha);
		movimiento.setValor(-valor);
		movimiento.setSaldo(cuentaAnticipos.getSaldoInicial());
		movimiento.setNumeroDoc("Factura N° " + factura.getNumero());
		movimiento.setObservacion("Cruce con factura N° " + factura.getNumero()
				+ ((observacion != null && !observacion.trim().isEmpty())
						? " | " + observacion.trim() : ""));
		movimiento.setEstado(Long.valueOf(EstadoAnticipoProveedor.CONFIRMADO));
		movimiento.setAsiento(asiento);
		movimiento.setFechaRegistro(LocalDateTime.now());
		em.persist(movimiento);
		System.out.println("✓ Movimiento de anticipo registrado: valor=" + (-valor)
				+ " | saldo=" + movimiento.getSaldo());

		// 6. Aplicación, enlazada al movimiento para poder reversarlo
		AplicacionPagoCxp aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.ANTICIPO, valor, fecha,
				(observacion != null && !observacion.trim().isEmpty())
						? observacion : "Cruce de saldo de anticipos",
				usuarioNombre(idUsuario));
		aplicacion.setAsiento(asiento);
		aplicacion.setAnticipo(movimiento);
		aplicacion.setUsuario(em.find(Usuario.class, idUsuario));
		aplicacion = saveSingle(aplicacion);

		em.flush();

		resultado.put("exito", true);
		resultado.put("mensaje", "Anticipo cruzado correctamente.");
		resultado.put("aplicacion", aplicacion.getId());
		resultado.put("asiento", asiento.getNumeroAlterno());
		resultado.put("saldoAnticipos", cuentaAnticipos.getSaldoInicial());
		resultado.putAll(saldoFactura(idFacturaCompra));
		return resultado;
	}

	@Override
	public AplicacionPagoCxp aplicarPagoTransferencia(PagoProgramado pago, Long idUsuario)
			throws Throwable {

		boolean debitoAutomatico = esDebitoAutomatico(pago);
		String tipoTexto = debitoAutomatico ? "débito automático" : "transferencia";

		System.out.println("=== aplicarPagoTransferencia | pago=" + pago.getId()
				+ " | valor=" + pago.getValor() + " | tipo=" + tipoTexto + " ===");

		FacturaCompra factura = pago.getFacturaCompra();
		if (factura == null) {
			throw new IncomeException("El pago " + pago.getId() + " no tiene factura asociada.");
		}
		Long idEmpresa = (pago.getEmpresa() != null) ? pago.getEmpresa().getCodigo() : null;
		Long idProveedor = (pago.getTitular() != null) ? pago.getTitular().getCodigo() : null;

		validaMontoContraSaldo(factura, pago.getValor(), null);

		LocalDate fecha = (pago.getFechaRespuesta() != null) ? pago.getFechaRespuesta() : LocalDate.now();
		String nombreProveedor = (pago.getTitular() != null) ? pago.getTitular().getNombre() : "";
		String observacionAsiento = "Pago por " + tipoTexto + " | Proveedor: " + nombreProveedor
				+ " | Factura: " + factura.getNumero()
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), "")
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", pago.getValor());

		// 1. Asiento contable del pago
		// El débito automático mueve las mismas cuentas que la transferencia:
		// DEBE cuenta CxP del proveedor / HABER cuenta contable del banco.
		Long idCuentaBancaria = (pago.getCuentaBancaria() != null)
				? pago.getCuentaBancaria().getCodigo() : null;
		Asiento asiento = asientoContableService.generarAsientoPagoTransferenciaCxp(
				idProveedor, pago.getValor(), idCuentaBancaria, idEmpresa,
				TipoAsientos.PAGO_TRANSFERENCIA_CXP, fecha, observacionAsiento,
				usuarioNombre(idUsuario));

		// 2. Aplicación a la factura
		AplicacionPagoCxp aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.COBRO_DIRECTO, pago.getValor(), fecha,
				"Pago por " + tipoTexto + " | Ref: " + nvl(pago.getReferenciaBanco(), ""),
				usuarioNombre(idUsuario));
		aplicacion.setFormaPago(debitoAutomatico
				? FORMA_PAGO_DEBITO_AUTOMATICO : FORMA_PAGO_TRANSFERENCIA);
		aplicacion.setReferencia(pago.getReferenciaBanco());
		aplicacion.setBanco(nombreBancoPago(pago, debitoAutomatico));
		aplicacion.setAsiento(asiento);
		aplicacion.setUsuario(em.find(Usuario.class, idUsuario));
		aplicacion = saveSingle(aplicacion);

		// 3. Movimiento bancario de egreso
		movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				"Pago proveedor: " + nombreProveedor + " | Factura: " + factura.getNumero()
				+ (debitoAutomatico ? " | Débito automático" : "")
				+ " | Ref: " + nvl(pago.getReferenciaBanco(), ""),
				asiento, pago.getCuentaBancaria(), pago.getValor(),
				TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO,
				OrigenMovimientoConciliacion.PAGOS);

		System.out.println("✓ Pago aplicado: aplicacion=" + aplicacion.getId()
				+ " | asiento=" + asiento.getNumeroAlterno());
		return aplicacion;
	}

	// =====================================================================
	// Reversión
	// =====================================================================

	@Override
	public Map<String, Object> revertirAplicacion(Long idAplicacion, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== revertirAplicacion | aplicacion=" + idAplicacion + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la reversión.");
		}

		AplicacionPagoCxp aplicacion = em.find(AplicacionPagoCxp.class, idAplicacion);
		if (aplicacion == null) {
			throw new IncomeException("No se encontró la aplicación de pago con ID: " + idAplicacion);
		}
		if (aplicacion.getEstado() != null
				&& aplicacion.getEstado().intValue() == EstadoAplicacionPago.REVERSADO) {
			throw new IncomeException("La aplicación " + idAplicacion + " ya está reversada.");
		}

		revierteUnaAplicacion(aplicacion, motivo);
		em.flush();

		resultado.put("exito", true);
		resultado.put("mensaje", "Aplicación reversada correctamente.");
		resultado.put("aplicacion", idAplicacion);
		if (aplicacion.getFacturaCompra() != null) {
			resultado.putAll(saldoFactura(aplicacion.getFacturaCompra().getId()));
		}
		return resultado;
	}

	@Override
	public int revertirAplicacionesDeDocumento(String tipoDocumento, Long idDocumento, String motivo,
			Long idUsuario) throws Throwable {

		System.out.println("=== revertirAplicacionesDeDocumento | tipo=" + tipoDocumento
				+ " | documento=" + idDocumento + " ===");

		List<AplicacionPagoCxp> aplicaciones =
				aplicacionPagoCxpDaoService.selectActivasByDocumento(tipoDocumento, idDocumento);

		int reversadas = 0;
		for (AplicacionPagoCxp aplicacion : aplicaciones) {
			revierteUnaAplicacion(aplicacion,
					(motivo != null && !motivo.trim().isEmpty())
							? motivo : "Anulación del documento de origen");
			reversadas++;
		}
		System.out.println("✓ Aplicaciones reversadas: " + reversadas);
		return reversadas;
	}

	@Override
	public int eliminarAplicacionesDeDocumento(String tipoDocumento, Long idDocumento)
			throws Throwable {

		System.out.println("=== eliminarAplicacionesDeDocumento | tipo=" + tipoDocumento
				+ " | documento=" + idDocumento + " ===");

		List<AplicacionPagoCxp> aplicaciones =
				aplicacionPagoCxpDaoService.selectActivasByDocumento(tipoDocumento, idDocumento);

		int eliminadas = 0;
		for (AplicacionPagoCxp aplicacion : aplicaciones) {
			Long idAsiento = (aplicacion.getAsiento() != null)
					? aplicacion.getAsiento().getCodigo() : null;
			Long idFactura = (aplicacion.getFacturaCompra() != null)
					? aplicacion.getFacturaCompra().getId() : null;

			em.remove(em.contains(aplicacion) ? aplicacion : em.merge(aplicacion));
			em.flush();

			// Con la aplicación ya borrada se recalcula el estado de la factura.
			if (idFactura != null) {
				recalcularEstadoPago(idFactura);
			}
			if (idAsiento != null) {
				anulaAsientoSeguro(idAsiento);
			}
			eliminadas++;
		}
		System.out.println("✓ Aplicaciones eliminadas: " + eliminadas);
		return eliminadas;
	}

	// =====================================================================
	// Consultas
	// =====================================================================

	@Override
	public List<AplicacionPagoCxp> consultarPorFactura(Long idFacturaCompra, boolean soloActivas)
			throws Throwable {
		System.out.println("=== consultarPorFactura | factura=" + idFacturaCompra
				+ " | soloActivas=" + soloActivas + " ===");
		return soloActivas
				? aplicacionPagoCxpDaoService.selectActivasByFactura(idFacturaCompra)
				: aplicacionPagoCxpDaoService.selectByFactura(idFacturaCompra);
	}

	@Override
	public Map<String, Object> saldoFactura(Long idFacturaCompra) throws Throwable {
		System.out.println("=== saldoFactura | factura=" + idFacturaCompra + " ===");

		FacturaCompra factura = em.find(FacturaCompra.class, idFacturaCompra);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de compra con ID: " + idFacturaCompra);
		}

		double total = (factura.getTotal() != null) ? factura.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxpDaoService.sumaAplicadoByFactura(idFacturaCompra);

		Map<String, Object> saldos = new HashMap<>();
		saldos.put("facturaId", idFacturaCompra);
		saldos.put("numeroFactura", factura.getNumero());
		saldos.put("total", total);
		saldos.put("totalAplicado", aplicado);
		saldos.put("saldoPendiente", total - aplicado);
		saldos.put("estadoPago", factura.getEstadoPago());
		return saldos;
	}

	@Override
	public Long recalcularEstadoPago(Long idFacturaCompra) throws Throwable {
		System.out.println("=== recalcularEstadoPago | factura=" + idFacturaCompra + " ===");

		FacturaCompra factura = em.find(FacturaCompra.class, idFacturaCompra);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de compra con ID: " + idFacturaCompra);
		}

		double total = (factura.getTotal() != null) ? factura.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxpDaoService.sumaAplicadoByFactura(idFacturaCompra);
		double saldo = total - aplicado;

		Long estadoPago;
		if (saldo <= TOLERANCIA) {
			estadoPago = Long.valueOf(EstadoPagoFactura.PAGADA_TOTAL);
		} else if (aplicado > 0) {
			estadoPago = Long.valueOf(EstadoPagoFactura.PAGADA_PARCIAL);
		} else {
			estadoPago = Long.valueOf(EstadoPagoFactura.PENDIENTE);
		}

		factura.setEstadoPago(estadoPago);
		em.merge(factura);

		System.out.println("✓ Estado de pago de la factura " + idFacturaCompra + ": " + estadoPago
				+ " | total=" + total + " | aplicado=" + aplicado + " | saldo=" + saldo);
		return estadoPago;
	}

	@Override
	public FacturaCompra resolverFacturaCompraPorNumero(String numeroDocumento, Long idTitular,
			Long idEmpresa) throws Throwable {

		if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
			throw new IncomeException("El documento no indica el número de la factura a la que afecta. "
					+ "No es posible registrar el pago ni generar la contabilidad.");
		}

		List<FacturaCompra> facturas =
				aplicacionPagoCxpDaoService.selectFacturaByNumero(numeroDocumento, idTitular, idEmpresa);

		if (facturas == null || facturas.isEmpty()) {
			throw new IncomeException("No existe en el sistema la factura de compra N° "
					+ numeroDocumento + " del proveedor indicado. Cargue primero la factura "
					+ "para poder registrar el documento que la afecta.");
		}
		if (facturas.size() > 1) {
			throw new IncomeException("Existe más de una factura de compra con el número "
					+ numeroDocumento + " para el mismo proveedor. Revise los documentos duplicados.");
		}
		return facturas.get(0);
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	/**
	 * Construye una aplicación con los datos comunes.
	 * @param factura     : Factura de compra a la que se aplica
	 * @param idEmpresa   : Id de la empresa
	 * @param tipoDocPago : Tipo de documento que paga (TipoDocPagoAplicacion)
	 * @param monto       : Monto aplicado (negativo para notas de débito)
	 * @param fecha       : Fecha de la aplicación
	 * @param observacion : Observación
	 * @param usuario     : Nombre del usuario
	 * @return            : Aplicación sin grabar
	 * @throws Throwable  : Excepcion
	 */
	private AplicacionPagoCxp nuevaAplicacion(FacturaCompra factura, Long idEmpresa, int tipoDocPago,
			Double monto, LocalDate fecha, String observacion, String usuario) throws Throwable {

		if (monto == null || monto == 0) {
			throw new IncomeException("El monto a aplicar no puede ser cero.");
		}

		AplicacionPagoCxp aplicacion = new AplicacionPagoCxp();
		aplicacion.setFacturaCompra(factura);
		aplicacion.setEmpresa(em.find(Empresa.class, idEmpresa));
		aplicacion.setTipoDocPago(Long.valueOf(tipoDocPago));
		aplicacion.setMontoAplicado(monto);
		aplicacion.setFechaAplicacion(fecha != null ? fecha : LocalDate.now());
		aplicacion.setObservacion(observacion);
		aplicacion.setEstado(Long.valueOf(EstadoAplicacionPago.ACTIVO));
		aplicacion.setFechaRegistro(LocalDateTime.now());
		return aplicacion;
	}

	/**
	 * Valida que el monto a aplicar no supere el saldo pendiente de la factura.
	 * @param factura        : Factura de compra
	 * @param monto          : Monto que se pretende aplicar
	 * @param idAplicacionEx : Id de aplicación a excluir del cálculo (null si no aplica)
	 * @throws Throwable     : Excepcion si el monto supera el saldo
	 */
	private void validaMontoContraSaldo(FacturaCompra factura, Double monto, Long idAplicacionEx)
			throws Throwable {
		if (monto == null || monto <= 0) {
			return; // las notas de débito (monto negativo) no consumen saldo
		}
		double total = (factura.getTotal() != null) ? factura.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxpDaoService.sumaAplicadoByFactura(factura.getId());
		double saldo = total - aplicado;

		if (monto > saldo + TOLERANCIA) {
			throw new IncomeException("El valor a aplicar ($"
					+ String.format(java.util.Locale.US, "%.2f", monto)
					+ ") supera el saldo pendiente de la factura N° " + factura.getNumero()
					+ " ($" + String.format(java.util.Locale.US, "%.2f", saldo) + ").");
		}
	}

	/**
	 * Reversa una aplicación: estado reversado, anulación del asiento,
	 * devolución del saldo de anticipos y anulación del movimiento bancario.
	 * @param aplicacion : Aplicación a reversar
	 * @param motivo     : Motivo de la reversión
	 * @throws Throwable : Excepcion
	 */
	private void revierteUnaAplicacion(AplicacionPagoCxp aplicacion, String motivo) throws Throwable {

		Long idAsiento = (aplicacion.getAsiento() != null)
				? aplicacion.getAsiento().getCodigo() : null;

		// 1. Marcar reversada y recalcular el estado de pago de la factura
		aplicacion.setEstado(Long.valueOf(EstadoAplicacionPago.REVERSADO));
		aplicacion.setObservacion(nvl(aplicacion.getObservacion(), "")
				+ " | REVERSADA: " + motivo);
		em.merge(aplicacion);
		em.flush();

		if (aplicacion.getFacturaCompra() != null) {
			recalcularEstadoPago(aplicacion.getFacturaCompra().getId());
		}

		// 2. Devolver el saldo de anticipos si fue un cruce de anticipo
		if (aplicacion.getTipoDocPago() != null
				&& aplicacion.getTipoDocPago().intValue() == TipoDocPagoAplicacion.ANTICIPO) {

			AnticipoProveedor anticipo = (aplicacion.getAnticipo() != null)
					? em.find(AnticipoProveedor.class, aplicacion.getAnticipo().getId()) : null;

			if (anticipo != null && anticipo.getValor() != null && anticipo.getValor() >= 0) {
				// Cruce contra un anticipo concreto: se devuelve su saldo
				double saldoActual = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
				anticipo.setSaldo(saldoActual + aplicacion.getMontoAplicado());
				if (anticipo.getSaldo() > 0) {
					anticipo.setEstado(1L);
				}
				em.merge(anticipo);
				System.out.println("✓ Saldo del anticipo " + anticipo.getId() + " devuelto: "
						+ saldoActual + " → " + anticipo.getSaldo());
			} else {
				// Cruce por valor contra el saldo global del proveedor: se
				// devuelve el saldo global y se anula el movimiento negativo
				// que el cruce dejó en PGS.ANTP (si existe: los cruces
				// anteriores a ese registro no lo tienen).
				FacturaCompra factura = aplicacion.getFacturaCompra();
				Long idEmpresa = (aplicacion.getEmpresa() != null)
						? aplicacion.getEmpresa().getCodigo() : null;
				if (factura != null && factura.getTitular() != null) {
					PersonaCuentaContable cuentaAnticipos =
							obtenerCuentaAnticipos(factura.getTitular().getCodigo(), idEmpresa);
					double saldoActual = (cuentaAnticipos.getSaldoInicial() != null)
							? cuentaAnticipos.getSaldoInicial() : 0.0;
					cuentaAnticipos.setSaldoInicial(saldoActual + aplicacion.getMontoAplicado());
					em.merge(cuentaAnticipos);
					System.out.println("✓ Saldo de anticipos devuelto: " + saldoActual
							+ " → " + cuentaAnticipos.getSaldoInicial());
				}
				if (anticipo != null) {
					anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.ANULADO));
					anticipo.setObservacion(nvl(anticipo.getObservacion(), "")
							+ " | REVERSADO: " + motivo);
					em.merge(anticipo);
					System.out.println("✓ Movimiento de anticipo " + anticipo.getId()
							+ " anulado por reversión del cruce.");
				}
			}
		}

		// 3. Anular el movimiento bancario del asiento, si lo hubiera
		if (idAsiento != null) {
			try {
				movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
						Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
			} catch (Exception e) {
				System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
						+ idAsiento + ": " + e.getMessage());
			}
		}

		// 4. Anular / reversar el asiento contable
		if (idAsiento != null) {
			anulaAsientoSeguro(idAsiento);
		}
	}

	/**
	 * Anula el asiento delegando en AsientoService, que decide entre anular o
	 * generar la reversión según el estado del período. No interrumpe el flujo.
	 * @param idAsiento : Id del asiento a anular
	 */
	private void anulaAsientoSeguro(Long idAsiento) {
		try {
			asientoService.anulaAsiento(idAsiento);
			System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
		} catch (Throwable e) {
			System.err.println("⚠ No se pudo anular el asiento " + idAsiento + ": " + e.getMessage());
		}
	}

	/**
	 * Recupera la cuenta contable de anticipos del proveedor (tipoCuenta = 2).
	 * <p>
	 * Filtra por rol Proveedor: sobre esta cuenta se lee y se ESCRIBE el saldo
	 * de anticipos, así que en un titular que también es cliente tomar la fila
	 * equivocada descontaría el anticipo de la cuenta del cliente.
	 * @param idTitular  : Id del proveedor
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Cuenta contable de anticipos
	 * @throws Throwable : Excepcion si no está configurada
	 */
	private PersonaCuentaContable obtenerCuentaAnticipos(Long idTitular, Long idEmpresa)
			throws Throwable {
		List<PersonaCuentaContable> lista = personaCuentaContableDaoService
				.selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);

		if (lista.isEmpty()) {
			throw new IncomeException("El proveedor no tiene configurada la cuenta contable de "
					+ "anticipos (Tipo 2, Rol: Proveedor) en Tesorería → Persona → Cuentas Contables. "
					+ "Sin ella no es posible cruzar anticipos.");
		}
		return lista.get(0);
	}

	/**
	 * Obtiene el número del documento sustento (la factura) de una retención V2,
	 * tomado de sus detalles.
	 * @param idRetencionV2 : Id de la retención V2
	 * @return              : Número del documento sustento, null si no hay
	 * @throws Throwable    : Excepcion si la retención afecta a varias facturas
	 */
	private String obtenerNumeroDocSustento(Long idRetencionV2) throws Throwable {
		@SuppressWarnings("unchecked")
		List<String> numeros = em.createQuery(
				" select distinct d.numDocReten from DetalleRetencionV2 d " +
				" where  d.retencionV2.id = :idRetencion " +
				" and    d.numDocReten is not null ")
			.setParameter("idRetencion", idRetencionV2)
			.getResultList();

		if (numeros.isEmpty()) {
			return null;
		}
		if (numeros.size() > 1) {
			throw new IncomeException("La retención afecta a " + numeros.size()
					+ " documentos distintos. El registro automático del pago solo está soportado "
					+ "para retenciones de un solo documento sustento.");
		}
		return numeros.get(0);
	}

	/**
	 * Indica si el pago se realizó por débito automático del banco.
	 * @param pago : Pago programado
	 * @return     : true si es débito automático
	 */
	private boolean esDebitoAutomatico(PagoProgramado pago) {
		return pago.getDebitoAutomatico() != null && pago.getDebitoAutomatico().intValue() == 1;
	}

	/**
	 * Devuelve el banco que queda registrado en la aplicación. En la
	 * transferencia interesa el banco al que se envió el dinero (cuenta del
	 * proveedor); en el débito automático no hay cuenta destino, así que se
	 * guarda el banco de la cuenta propia que el banco debitó.
	 * @param pago             : Pago programado
	 * @param debitoAutomatico : true si el pago es por débito automático
	 * @return                 : Nombre del banco o cadena vacía
	 */
	private String nombreBancoPago(PagoProgramado pago, boolean debitoAutomatico) {
		if (debitoAutomatico) {
			if (pago.getCuentaBancaria() != null && pago.getCuentaBancaria().getBanco() != null) {
				return pago.getCuentaBancaria().getBanco().getNombre();
			}
			return "";
		}
		if (pago.getCuentaDestino() != null && pago.getCuentaDestino().getBanco() != null) {
			return pago.getCuentaDestino().getBanco().getNombre();
		}
		return "";
	}

	/**
	 * Recupera el nombre de un usuario para las trazas y observaciones.
	 * @param idUsuario : Id del usuario
	 * @return          : Nombre del usuario o SISTEMA
	 */
	private String usuarioNombre(Long idUsuario) {
		if (idUsuario == null) {
			return "SISTEMA";
		}
		Usuario usuario = em.find(Usuario.class, idUsuario);
		return (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "SISTEMA";
	}

	/**
	 * Convierte un LocalDateTime a LocalDate tolerando nulos.
	 * @param fechaHora : Fecha y hora
	 * @return          : Fecha, o la de hoy si viene nula
	 */
	private LocalDate fechaDe(LocalDateTime fechaHora) {
		return (fechaHora != null) ? fechaHora.toLocalDate() : LocalDate.now();
	}

	/**
	 * Interpreta una fecha en formato yyyy-MM-dd.
	 * @param fecha : Fecha en texto
	 * @return      : Fecha, o la de hoy si viene vacía o mal formada
	 */
	private LocalDate parseFecha(String fecha) {
		if (fecha == null || fecha.trim().isEmpty()) {
			return LocalDate.now();
		}
		try {
			return LocalDate.parse(fecha.trim());
		} catch (Exception e) {
			System.err.println("⚠ Fecha inválida '" + fecha + "', se usa la fecha actual.");
			return LocalDate.now();
		}
	}

	private String nvl(String valor, String porDefecto) {
		return (valor != null) ? valor : porDefecto;
	}
}
