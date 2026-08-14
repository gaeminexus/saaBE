package com.saa.ejb.cxc.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxc.dao.AplicacionPagoCxcDaoService;
import com.saa.ejb.cxc.service.AplicacionPagoCxcService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.model.cxc.AplicacionPagoCxc;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.LiquidacionCompra;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.NotaCredito;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxp.RetencionCompra;
import com.saa.model.cxp.RetencionCompraV2;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.PersonaCuentaContable;
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
public class AplicacionPagoCxcServiceImpl implements AplicacionPagoCxcService {

	/** Tolerancia para comparar valores monetarios. */
	private static final double TOLERANCIA = 0.01;

	/** Forma de pago transferencia (rubro TipoFormaPago del sistema). */
	private static final long FORMA_PAGO_TRANSFERENCIA = 2L;

	@EJB
	private AplicacionPagoCxcDaoService aplicacionPagoCxcDaoService;

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
	public AplicacionPagoCxc selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById AplicacionPagoCxc con id: " + id);
		return aplicacionPagoCxcDaoService.selectById(id, NombreEntidadesCobro.APLICACION_PAGO_CXC);
	}

	@Override
	public List<AplicacionPagoCxc> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll AplicacionPagoCxcService");
		List<AplicacionPagoCxc> result =
				aplicacionPagoCxcDaoService.selectAll(NombreEntidadesCobro.APLICACION_PAGO_CXC);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total AplicacionPagoCxc no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<AplicacionPagoCxc> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria AplicacionPagoCxcService");
		List<AplicacionPagoCxc> result = aplicacionPagoCxcDaoService.selectByCriteria(datos,
				NombreEntidadesCobro.APLICACION_PAGO_CXC);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio AplicacionPagoCxc no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public AplicacionPagoCxc saveSingle(AplicacionPagoCxc aplicacion) throws Throwable {
		System.out.println("saveSingle - AplicacionPagoCxc");
		if (aplicacion.getId() == null) {
			if (aplicacion.getEstado() == null) {
				aplicacion.setEstado(Long.valueOf(EstadoAplicacionPago.ACTIVO));
			}
			if (aplicacion.getFechaRegistro() == null) {
				aplicacion.setFechaRegistro(LocalDateTime.now());
			}
		}
		aplicacion = aplicacionPagoCxcDaoService.save(aplicacion, aplicacion.getId());
		em.flush();

		// El estado de pago del documento lo calcula el backend, no la base de datos.
		recalcularEstadoPagoDeAplicacion(aplicacion);
		return aplicacion;
	}

	@Override
	public void save(List<AplicacionPagoCxc> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de AplicacionPagoCxcService");
		for (AplicacionPagoCxc registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de AplicacionPagoCxcService");
		AplicacionPagoCxc entidad = new AplicacionPagoCxc();
		for (Long registro : id) {
			aplicacionPagoCxcDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Aplicaciones automáticas (misma transacción que el asiento)
	// =====================================================================

	@Override
	public AplicacionPagoCxc aplicarRetencionRecibida(RetencionCompra retencion, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarRetencionRecibida | retencion=" + retencion.getId()
				+ " | empresa=" + idEmpresa + " ===");

		String numeroFactura = obtenerNumeroDocSustento(
				"DetalleRetencionCompra", "retencion", retencion.getId());
		Factura factura = resolverFacturaPorNumero(numeroFactura, null, idEmpresa);

		AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.RETENCION, retencion.getTotal(),
				fechaDe(retencion.getFecha()),
				"Retención recibida N° " + retencion.getNumero(), usuario);
		aplicacion.setRetencion(retencion);
		aplicacion.setAsiento(asiento);

		validaMontoContraSaldo(factura, aplicacion.getMontoAplicado());

		aplicacion = saveSingle(aplicacion);
		System.out.println("✓ Aplicación por retención recibida creada: id=" + aplicacion.getId()
				+ " | factura=" + factura.getId() + " | monto=" + aplicacion.getMontoAplicado());
		return aplicacion;
	}

	@Override
	public AplicacionPagoCxc aplicarRetencionRecibidaV2(RetencionCompraV2 retencion, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarRetencionRecibidaV2 | retencion=" + retencion.getId()
				+ " | empresa=" + idEmpresa + " ===");

		String numeroFactura = obtenerNumeroDocSustento(
				"DetalleRetencionCompraV2", "retencionCompraV2", retencion.getId());
		Factura factura = resolverFacturaPorNumero(numeroFactura, null, idEmpresa);

		AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.RETENCION, retencion.getTotal(),
				fechaDe(retencion.getFecha()),
				"Retención V2 recibida N° " + retencion.getNumero(), usuario);
		aplicacion.setRetencionV2(retencion);
		aplicacion.setAsiento(asiento);

		validaMontoContraSaldo(factura, aplicacion.getMontoAplicado());

		aplicacion = saveSingle(aplicacion);
		System.out.println("✓ Aplicación por retención V2 recibida creada: id=" + aplicacion.getId()
				+ " | factura=" + factura.getId() + " | monto=" + aplicacion.getMontoAplicado());
		return aplicacion;
	}

	@Override
	public AplicacionPagoCxc aplicarNotaCredito(NotaCredito notaCredito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarNotaCredito | notaCredito=" + notaCredito.getId()
				+ " | empresa=" + idEmpresa + " ===");

		// En CXC la nota de crédito tiene FK dura a la factura.
		Factura factura = notaCredito.getFactura();
		if (factura == null) {
			Long idCliente = (notaCredito.getTitular() != null)
					? notaCredito.getTitular().getCodigo() : null;
			factura = resolverFacturaPorNumero(notaCredito.getNumDocModificado(), idCliente, idEmpresa);
		}

		AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.NOTA_CREDITO, notaCredito.getTotal(),
				fechaDe(notaCredito.getFecha()),
				"Nota de Crédito N° " + notaCredito.getNumero(), usuario);
		aplicacion.setNotaCredito(notaCredito);
		aplicacion.setAsiento(asiento);

		validaMontoContraSaldo(factura, aplicacion.getMontoAplicado());

		aplicacion = saveSingle(aplicacion);
		System.out.println("✓ Aplicación por nota de crédito creada: id=" + aplicacion.getId()
				+ " | factura=" + factura.getId() + " | monto=" + aplicacion.getMontoAplicado());
		return aplicacion;
	}

	@Override
	public AplicacionPagoCxc aplicarNotaDebito(NotaDebito notaDebito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable {

		System.out.println("=== aplicarNotaDebito | notaDebito=" + notaDebito.getId()
				+ " | empresa=" + idEmpresa + " ===");

		Factura factura = notaDebito.getFactura();
		if (factura == null) {
			Long idCliente = (notaDebito.getTitular() != null)
					? notaDebito.getTitular().getCodigo() : null;
			factura = resolverFacturaPorNumero(notaDebito.getNumDocModificado(), idCliente, idEmpresa);
		}

		// La nota de débito AUMENTA el saldo de la factura: monto negativo.
		Double monto = (notaDebito.getTotal() != null) ? -Math.abs(notaDebito.getTotal()) : null;

		AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
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
	public Map<String, Object> aplicarAnticipo(Long idFactura, Double valor, String fechaAplicacion,
			Long idEmpresa, Long idUsuario, String observacion) throws Throwable {

		System.out.println("=== aplicarAnticipo (CXC) | factura=" + idFactura
				+ " | valor=" + valor + " | empresa=" + idEmpresa + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor a cruzar debe ser mayor a cero.");
		}

		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de venta con ID: " + idFactura);
		}
		if (factura.getTitular() == null || factura.getTitular().getCodigo() == null) {
			throw new IncomeException("La factura de venta " + idFactura
					+ " no tiene cliente asignado.");
		}
		Long idCliente = factura.getTitular().getCodigo();

		// 1. La factura debe tener saldo suficiente
		validaMontoContraSaldo(factura, valor);

		// 2. El cliente debe tener saldo de anticipos suficiente
		PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(idCliente, idEmpresa);
		double saldoAnticipos = (cuentaAnticipos.getSaldoInicial() != null)
				? cuentaAnticipos.getSaldoInicial() : 0.0;
		if (saldoAnticipos + TOLERANCIA < valor) {
			throw new IncomeException("El cliente '" + factura.getTitular().getNombre()
					+ "' tiene un saldo de anticipos de $"
					+ String.format(java.util.Locale.US, "%.2f", saldoAnticipos)
					+ " y no alcanza para cruzar $"
					+ String.format(java.util.Locale.US, "%.2f", valor) + ".");
		}

		LocalDate fecha = parseFecha(fechaAplicacion);
		String observacionAsiento = "Cruce de anticipo | Cliente: " + factura.getTitular().getNombre()
				+ " | Factura: " + factura.getNumero()
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

		// 3. Asiento contable del cruce
		Asiento asiento = asientoContableService.generarAsientoAplicacionAnticipoCliente(
				idCliente, valor, idEmpresa, TipoAsientos.APLICACION_ANTICIPO_CLIENTE,
				fecha, observacionAsiento, usuarioNombre(idUsuario));

		// 4. Descontar el saldo de anticipos del cliente
		cuentaAnticipos.setSaldoInicial(saldoAnticipos - valor);
		em.merge(cuentaAnticipos);
		System.out.println("✓ Saldo de anticipos del cliente " + idCliente + ": "
				+ saldoAnticipos + " → " + cuentaAnticipos.getSaldoInicial());

		// 5. Movimiento negativo en el listado de anticipos del cliente: sin él,
		// el listado no refleja el cruce y su saldo acumulado queda
		// desactualizado (el saldo de cada fila es el acumulado al momento
		// del movimiento, igual que al registrar un anticipo).
		AnticipoCliente movimiento = new AnticipoCliente();
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
		movimiento.setEstado(2L); // Confirmado
		movimiento.setAsiento(asiento);
		movimiento.setFechaRegistro(LocalDateTime.now());
		em.persist(movimiento);
		System.out.println("✓ Movimiento de anticipo registrado: valor=" + (-valor)
				+ " | saldo=" + movimiento.getSaldo());

		// 6. Aplicación, enlazada al movimiento para poder reversarlo
		AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
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
		resultado.putAll(saldoFactura(idFactura));
		return resultado;
	}

	@Override
	public Map<String, Object> aplicarCobroTransferencia(Long idFactura, Double valor,
			String fechaCobro, String numeroTransferencia, Long idCuentaBancaria, Long idEmpresa,
			Long idUsuario, String observacion) throws Throwable {

		System.out.println("=== aplicarCobroTransferencia | factura=" + idFactura
				+ " | valor=" + valor + " | transferencia=" + numeroTransferencia + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor recibido debe ser mayor a cero.");
		}
		if (numeroTransferencia == null || numeroTransferencia.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el número de la transferencia recibida.");
		}

		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de venta con ID: " + idFactura);
		}
		if (factura.getTitular() == null || factura.getTitular().getCodigo() == null) {
			throw new IncomeException("La factura de venta " + idFactura
					+ " no tiene cliente asignado.");
		}
		Long idCliente = factura.getTitular().getCodigo();

		CuentaBancaria cuentaBancaria = em.find(CuentaBancaria.class, idCuentaBancaria);
		if (cuentaBancaria == null) {
			throw new IncomeException("No se encontró la cuenta bancaria con ID: " + idCuentaBancaria);
		}

		validaMontoContraSaldo(factura, valor);

		LocalDate fecha = parseFecha(fechaCobro);
		String observacionAsiento = "Cobro por transferencia | Cliente: "
				+ factura.getTitular().getNombre()
				+ " | Factura: " + factura.getNumero()
				+ " | Ref: " + numeroTransferencia.trim()
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

		// 1. Asiento contable del cobro
		Asiento asiento = asientoContableService.generarAsientoCobroTransferenciaCxc(
				idCliente, valor, idCuentaBancaria, idEmpresa,
				TipoAsientos.COBRO_TRANSFERENCIA_CXC, fecha, observacionAsiento,
				usuarioNombre(idUsuario));

		// 2. Aplicación a la factura
		AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
				TipoDocPagoAplicacion.COBRO_DIRECTO, valor, fecha,
				(observacion != null && !observacion.trim().isEmpty())
						? observacion : "Cobro por transferencia",
				usuarioNombre(idUsuario));
		aplicacion.setFormaPago(FORMA_PAGO_TRANSFERENCIA);
		aplicacion.setReferencia(numeroTransferencia.trim());
		aplicacion.setBanco(nombreBanco(cuentaBancaria));
		aplicacion.setAsiento(asiento);
		aplicacion.setUsuario(em.find(Usuario.class, idUsuario));
		aplicacion = saveSingle(aplicacion);

		// 3. Movimiento bancario de ingreso
		movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				"Cobro cliente: " + factura.getTitular().getNombre()
				+ " | Factura: " + factura.getNumero()
				+ " | Ref: " + numeroTransferencia.trim(),
				asiento, cuentaBancaria, valor,
				TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO,
				OrigenMovimientoConciliacion.COBROS);

		em.flush();

		System.out.println("✓ Cobro aplicado: aplicacion=" + aplicacion.getId()
				+ " | asiento=" + asiento.getNumeroAlterno());

		resultado.put("exito", true);
		resultado.put("mensaje", "Cobro registrado correctamente.");
		resultado.put("aplicacion", aplicacion.getId());
		resultado.put("asiento", asiento.getNumeroAlterno());
		resultado.putAll(saldoFactura(idFactura));
		return resultado;
	}

	// =====================================================================
	// Reversión
	// =====================================================================

	@Override
	public Map<String, Object> revertirAplicacion(Long idAplicacion, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== revertirAplicacion (CXC) | aplicacion=" + idAplicacion + " ===");

		Map<String, Object> resultado = new HashMap<>();

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la reversión.");
		}

		AplicacionPagoCxc aplicacion = em.find(AplicacionPagoCxc.class, idAplicacion);
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
		if (aplicacion.getFactura() != null) {
			resultado.putAll(saldoFactura(aplicacion.getFactura().getId()));
		}
		return resultado;
	}

	@Override
	public int revertirAplicacionesDeDocumento(String tipoDocumento, Long idDocumento, String motivo,
			Long idUsuario) throws Throwable {

		System.out.println("=== revertirAplicacionesDeDocumento (CXC) | tipo=" + tipoDocumento
				+ " | documento=" + idDocumento + " ===");

		List<AplicacionPagoCxc> aplicaciones =
				aplicacionPagoCxcDaoService.selectActivasByDocumento(tipoDocumento, idDocumento);

		int reversadas = 0;
		for (AplicacionPagoCxc aplicacion : aplicaciones) {
			revierteUnaAplicacion(aplicacion,
					(motivo != null && !motivo.trim().isEmpty())
							? motivo : "Anulación del documento de origen");
			reversadas++;
		}
		System.out.println("✓ Aplicaciones reversadas: " + reversadas);
		return reversadas;
	}

	@Override
	public int revertirAplicacionesDeFactura(Long idFactura, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== revertirAplicacionesDeFactura | factura=" + idFactura + " ===");

		List<AplicacionPagoCxc> aplicaciones =
				aplicacionPagoCxcDaoService.selectActivasByFactura(idFactura);

		int reversadas = 0;
		for (AplicacionPagoCxc aplicacion : aplicaciones) {
			revierteUnaAplicacion(aplicacion,
					(motivo != null && !motivo.trim().isEmpty())
							? motivo : "Anulación de la factura");
			reversadas++;
		}
		System.out.println("✓ Aplicaciones de la factura reversadas: " + reversadas);
		return reversadas;
	}

	@Override
	public int eliminarAplicacionesDeDocumento(String tipoDocumento, Long idDocumento)
			throws Throwable {

		System.out.println("=== eliminarAplicacionesDeDocumento (CXC) | tipo=" + tipoDocumento
				+ " | documento=" + idDocumento + " ===");

		List<AplicacionPagoCxc> aplicaciones =
				aplicacionPagoCxcDaoService.selectActivasByDocumento(tipoDocumento, idDocumento);

		int eliminadas = 0;
		for (AplicacionPagoCxc aplicacion : aplicaciones) {
			Long idAsiento = (aplicacion.getAsiento() != null)
					? aplicacion.getAsiento().getCodigo() : null;
			Long idFactura = (aplicacion.getFactura() != null)
					? aplicacion.getFactura().getId() : null;
			Long idLiquidacion = (aplicacion.getLiquidacion() != null)
					? aplicacion.getLiquidacion().getId() : null;

			em.remove(em.contains(aplicacion) ? aplicacion : em.merge(aplicacion));
			em.flush();

			// Con la aplicación ya borrada se recalcula el estado del documento.
			if (idFactura != null) {
				recalcularEstadoPago(idFactura);
			}
			if (idLiquidacion != null) {
				recalcularEstadoPagoLiquidacion(idLiquidacion);
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
	public List<AplicacionPagoCxc> consultarPorFactura(Long idFactura, boolean soloActivas)
			throws Throwable {
		System.out.println("=== consultarPorFactura (CXC) | factura=" + idFactura
				+ " | soloActivas=" + soloActivas + " ===");
		return soloActivas
				? aplicacionPagoCxcDaoService.selectActivasByFactura(idFactura)
				: aplicacionPagoCxcDaoService.selectByFactura(idFactura);
	}

	@Override
	public Map<String, Object> saldoFactura(Long idFactura) throws Throwable {
		System.out.println("=== saldoFactura (CXC) | factura=" + idFactura + " ===");

		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de venta con ID: " + idFactura);
		}

		double total = (factura.getTotal() != null) ? factura.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxcDaoService.sumaAplicadoByFactura(idFactura);

		Map<String, Object> saldos = new HashMap<>();
		saldos.put("facturaId", idFactura);
		saldos.put("numeroFactura", factura.getNumero());
		saldos.put("total", total);
		saldos.put("totalAplicado", aplicado);
		saldos.put("saldoPendiente", total - aplicado);
		saldos.put("estadoPago", factura.getEstadoPago());
		return saldos;
	}

	@Override
	public Long recalcularEstadoPago(Long idFactura) throws Throwable {
		System.out.println("=== recalcularEstadoPago (CXC) | factura=" + idFactura + " ===");

		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de venta con ID: " + idFactura);
		}

		double total = (factura.getTotal() != null) ? factura.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxcDaoService.sumaAplicadoByFactura(idFactura);
		Long estadoPago = calculaEstadoPago(total, aplicado);

		factura.setEstadoPago(estadoPago);
		em.merge(factura);

		System.out.println("✓ Estado de pago de la factura " + idFactura + ": " + estadoPago
				+ " | total=" + total + " | aplicado=" + aplicado);
		return estadoPago;
	}

	@Override
	public Long recalcularEstadoPagoLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("=== recalcularEstadoPagoLiquidacion | liquidacion=" + idLiquidacion + " ===");

		LiquidacionCompra liquidacion = em.find(LiquidacionCompra.class, idLiquidacion);
		if (liquidacion == null) {
			throw new IncomeException("No se encontró la liquidación con ID: " + idLiquidacion);
		}

		double total = (liquidacion.getTotal() != null) ? liquidacion.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxcDaoService.sumaAplicadoByLiquidacion(idLiquidacion);
		Long estadoPago = calculaEstadoPago(total, aplicado);

		liquidacion.setEstadoPago(estadoPago);
		em.merge(liquidacion);

		System.out.println("✓ Estado de pago de la liquidación " + idLiquidacion + ": " + estadoPago);
		return estadoPago;
	}

	@Override
	public Factura resolverFacturaPorNumero(String numeroDocumento, Long idTitular, Long idEmpresa)
			throws Throwable {

		if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
			throw new IncomeException("El documento no indica el número de la factura a la que afecta. "
					+ "No es posible registrar el cobro ni generar la contabilidad.");
		}

		List<Factura> facturas =
				aplicacionPagoCxcDaoService.selectFacturaByNumero(numeroDocumento, idTitular, idEmpresa);

		if (facturas == null || facturas.isEmpty()) {
			throw new IncomeException("No existe en el sistema la factura de venta N° "
					+ numeroDocumento + ". No se puede registrar el documento que la afecta.");
		}
		if (facturas.size() > 1) {
			throw new IncomeException("Existe más de una factura de venta con el número "
					+ numeroDocumento + ". Revise los documentos duplicados.");
		}
		return facturas.get(0);
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	/**
	 * Construye una aplicación sobre una factura de venta con los datos comunes.
	 * @param factura     : Factura de venta a la que se aplica
	 * @param idEmpresa   : Id de la empresa
	 * @param tipoDocPago : Tipo de documento que paga (TipoDocPagoAplicacion)
	 * @param monto       : Monto aplicado (negativo para notas de débito)
	 * @param fecha       : Fecha de la aplicación
	 * @param observacion : Observación
	 * @param usuario     : Nombre del usuario
	 * @return            : Aplicación sin grabar
	 * @throws Throwable  : Excepcion
	 */
	private AplicacionPagoCxc nuevaAplicacion(Factura factura, Long idEmpresa, int tipoDocPago,
			Double monto, LocalDate fecha, String observacion, String usuario) throws Throwable {

		if (monto == null || monto == 0) {
			throw new IncomeException("El monto a aplicar no puede ser cero.");
		}

		AplicacionPagoCxc aplicacion = new AplicacionPagoCxc();
		aplicacion.setFactura(factura);
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
	 * @param factura    : Factura de venta
	 * @param monto      : Monto que se pretende aplicar
	 * @throws Throwable : Excepcion si el monto supera el saldo
	 */
	private void validaMontoContraSaldo(Factura factura, Double monto) throws Throwable {
		if (monto == null || monto <= 0) {
			return; // las notas de débito (monto negativo) no consumen saldo
		}
		double total = (factura.getTotal() != null) ? factura.getTotal() : 0.0;
		double aplicado = aplicacionPagoCxcDaoService.sumaAplicadoByFactura(factura.getId());
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
	private void revierteUnaAplicacion(AplicacionPagoCxc aplicacion, String motivo) throws Throwable {

		Long idAsiento = (aplicacion.getAsiento() != null)
				? aplicacion.getAsiento().getCodigo() : null;

		// 1. Marcar reversada y recalcular el estado de pago del documento
		aplicacion.setEstado(Long.valueOf(EstadoAplicacionPago.REVERSADO));
		aplicacion.setObservacion(nvl(aplicacion.getObservacion(), "")
				+ " | REVERSADA: " + motivo);
		em.merge(aplicacion);
		em.flush();

		recalcularEstadoPagoDeAplicacion(aplicacion);

		// 2. Devolver el saldo de anticipos si fue un cruce de anticipo
		if (aplicacion.getTipoDocPago() != null
				&& aplicacion.getTipoDocPago().intValue() == TipoDocPagoAplicacion.ANTICIPO) {

			AnticipoCliente anticipo = (aplicacion.getAnticipo() != null)
					? em.find(AnticipoCliente.class, aplicacion.getAnticipo().getId()) : null;

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
				// Cruce por valor contra el saldo global del cliente: se
				// devuelve el saldo global y se anula el movimiento negativo
				// que el cruce dejó en el listado de anticipos (si existe: los
				// cruces anteriores a ese registro no lo tienen).
				Factura factura = aplicacion.getFactura();
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
					anticipo.setEstado(3L); // Anulado
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
	 * Recalcula el estado de pago del documento al que pertenece la aplicación,
	 * sea factura o liquidación.
	 * @param aplicacion : Aplicación recién creada o reversada
	 * @throws Throwable : Excepcion
	 */
	private void recalcularEstadoPagoDeAplicacion(AplicacionPagoCxc aplicacion) throws Throwable {
		if (aplicacion.getFactura() != null) {
			recalcularEstadoPago(aplicacion.getFactura().getId());
		}
		if (aplicacion.getLiquidacion() != null) {
			recalcularEstadoPagoLiquidacion(aplicacion.getLiquidacion().getId());
		}
	}

	/**
	 * Determina el estado de pago a partir del total y de lo aplicado.
	 * @param total    : Total del documento
	 * @param aplicado : Suma de las aplicaciones activas
	 * @return         : 1 = Pendiente, 2 = Parcial, 3 = Pagada total
	 */
	private Long calculaEstadoPago(double total, double aplicado) {
		double saldo = total - aplicado;
		if (saldo <= TOLERANCIA) {
			return Long.valueOf(EstadoPagoFactura.PAGADA_TOTAL);
		}
		if (aplicado > 0) {
			return Long.valueOf(EstadoPagoFactura.PAGADA_PARCIAL);
		}
		return Long.valueOf(EstadoPagoFactura.PENDIENTE);
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
	 * Recupera la cuenta contable de anticipos del cliente (tipoCuenta = 2).
	 * <p>
	 * Filtra por rol Cliente: sobre esta cuenta se lee y se ESCRIBE el saldo de
	 * anticipos, así que en un titular que también es proveedor tomar la fila
	 * equivocada descontaría el anticipo de la cuenta del proveedor.
	 * @param idTitular  : Id del cliente
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Cuenta contable de anticipos
	 * @throws Throwable : Excepcion si no está configurada
	 */
	private PersonaCuentaContable obtenerCuentaAnticipos(Long idTitular, Long idEmpresa)
			throws Throwable {
		List<PersonaCuentaContable> lista = personaCuentaContableDaoService
				.selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.CLIENTE, 2L);

		if (lista.isEmpty()) {
			throw new IncomeException("El cliente no tiene configurada la cuenta contable de "
					+ "anticipos (Tipo 2, Rol: Cliente) en Tesorería → Persona → Cuentas Contables. "
					+ "Sin ella no es posible cruzar anticipos.");
		}
		return lista.get(0);
	}

	/**
	 * Obtiene el número del documento sustento (la factura de venta) desde los
	 * detalles de una retención recibida.
	 * @param entidadDetalle : Nombre de la entidad de detalle
	 * @param campoCabecera  : Nombre del campo que apunta a la cabecera
	 * @param idRetencion    : Id de la retención
	 * @return               : Número del documento sustento, null si no hay
	 * @throws Throwable     : Excepcion si la retención afecta a varias facturas
	 */
	private String obtenerNumeroDocSustento(String entidadDetalle, String campoCabecera,
			Long idRetencion) throws Throwable {
		@SuppressWarnings("unchecked")
		List<String> numeros = em.createQuery(
				" select distinct d.numDocReten from " + entidadDetalle + " d " +
				" where  d." + campoCabecera + ".id = :idRetencion " +
				" and    d.numDocReten is not null ")
			.setParameter("idRetencion", idRetencion)
			.getResultList();

		if (numeros.isEmpty()) {
			return null;
		}
		if (numeros.size() > 1) {
			throw new IncomeException("La retención afecta a " + numeros.size()
					+ " documentos distintos. El registro automático del cobro solo está soportado "
					+ "para retenciones de un solo documento sustento.");
		}
		return numeros.get(0);
	}

	/**
	 * Devuelve el nombre del banco de una cuenta bancaria propia.
	 * @param cuentaBancaria : Cuenta bancaria
	 * @return               : Nombre del banco o cadena vacía
	 */
	private String nombreBanco(CuentaBancaria cuentaBancaria) {
		if (cuentaBancaria != null && cuentaBancaria.getBanco() != null) {
			return cuentaBancaria.getBanco().getNombre();
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
