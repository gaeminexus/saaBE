package com.saa.ejb.cxc.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
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
import com.saa.rubros.EstadoAnticipoCliente;
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
	private AnticipoClienteDaoService anticipoClienteDaoService;

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
	public Map<String, Object> aplicarAnticipo(Long idFactura, Double valor,
			String fechaAplicacion, Long idEmpresa, Long idUsuario, String observacion)
			throws Throwable {

		System.out.println("=== aplicarAnticipo (FIFO) | factura=" + idFactura
				+ " | valor=" + valor + " | empresa=" + idEmpresa + " ===");

		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor a cruzar debe ser mayor a cero.");
		}

		Factura factura = cargaFactura(idFactura);
		Long idCliente = factura.getTitular().getCodigo();

		// Sin selección explícita se reparte el valor entre los anticipos
		// disponibles del más antiguo al más nuevo. Es el comportamiento que
		// mantiene funcionando a los clientes que sólo mandan el monto.
		List<AnticipoCliente> disponibles = anticipoClienteDaoService
				.selectDisponiblesByTitular(idCliente, idEmpresa);

		List<Object[]> reparto = repartoFifo(disponibles, valor,
				factura.getTitular().getNombre());

		return aplicaCruces(factura, reparto, fechaAplicacion, idEmpresa, idUsuario, observacion);
	}

	@Override
	public Map<String, Object> aplicarAnticipos(Long idFactura,
			List<Map<String, Object>> detalles, String fechaAplicacion, Long idEmpresa,
			Long idUsuario, String observacion) throws Throwable {

		System.out.println("=== aplicarAnticipos | factura=" + idFactura
				+ " | detalles=" + ((detalles != null) ? detalles.size() : 0)
				+ " | empresa=" + idEmpresa + " ===");

		if (detalles == null || detalles.isEmpty()) {
			throw new IncomeException("Debe indicar al menos un anticipo a cruzar.");
		}

		Factura factura = cargaFactura(idFactura);
		Long idCliente = factura.getTitular().getCodigo();

		List<Object[]> reparto = new ArrayList<>();
		java.util.Set<Long> vistos = new java.util.HashSet<>();

		for (Map<String, Object> detalle : detalles) {
			Long idAnticipo = toLong(detalle.get("idAnticipo"));
			Double monto = toDouble(detalle.get("valor"));

			if (idAnticipo == null) {
				throw new IncomeException("Cada línea del cruce debe indicar el anticipo "
						+ "(idAnticipo).");
			}
			if (monto == null || monto <= 0) {
				throw new IncomeException("El valor a cruzar del anticipo " + idAnticipo
						+ " debe ser mayor a cero.");
			}
			// Repetir el mismo anticipo en dos líneas haría que la segunda
			// validara contra un saldo ya comprometido por la primera.
			if (!vistos.add(idAnticipo)) {
				throw new IncomeException("El anticipo " + idAnticipo + " viene repetido en el "
						+ "cruce. Indique una sola línea por anticipo, con el valor total.");
			}

			AnticipoCliente anticipo = em.find(AnticipoCliente.class, idAnticipo);
			validaAnticipoCruzable(anticipo, idAnticipo, idCliente, idEmpresa, monto);
			reparto.add(new Object[]{anticipo, monto});
		}

		return aplicaCruces(factura, reparto, fechaAplicacion, idEmpresa, idUsuario, observacion);
	}

	/**
	 * Carga la factura de venta y valida que sirva para cruzar un anticipo.
	 * @param idFactura : Id de la factura de venta
	 * @return                : Factura de compra
	 * @throws Throwable      : Excepcion si no existe o no tiene cliente
	 */
	private Factura cargaFactura(Long idFactura) throws Throwable {
		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("No se encontró la factura de venta con ID: "
					+ idFactura);
		}
		if (factura.getTitular() == null || factura.getTitular().getCodigo() == null) {
			throw new IncomeException("La factura de venta " + idFactura
					+ " no tiene cliente asignado.");
		}
		return factura;
	}

	/**
	 * Valida que un anticipo elegido a mano pueda usarse para cruzar: que exista,
	 * que sea del cliente y la empresa de la factura, que esté confirmado y
	 * que le quede saldo suficiente.
	 * @param anticipo   : Anticipo cargado (puede venir null)
	 * @param idAnticipo : Id pedido, para el mensaje de error
	 * @param idTitular  : Cliente de la factura
	 * @param idEmpresa  : Empresa de la operación
	 * @param monto      : Monto que se pretende cruzar de ese anticipo
	 * @throws Throwable : Excepcion con el motivo exacto del rechazo
	 */
	private void validaAnticipoCruzable(AnticipoCliente anticipo, Long idAnticipo,
			Long idTitular, Long idEmpresa, Double monto) throws Throwable {

		if (anticipo == null) {
			throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
		}
		if (anticipo.getTitular() == null
				|| !idTitular.equals(anticipo.getTitular().getCodigo())) {
			throw new IncomeException("El anticipo " + idAnticipo + " no pertenece al cliente "
					+ "de la factura: no se puede cruzar.");
		}
		if (idEmpresa != null && anticipo.getEmpresa() != null
				&& !idEmpresa.equals(anticipo.getEmpresa().getCodigo())) {
			throw new IncomeException("El anticipo " + idAnticipo + " es de otra empresa "
					+ "contable: no se puede cruzar contra esta factura.");
		}
		if (anticipo.getEstado() == null
				|| anticipo.getEstado().intValue() != EstadoAnticipoCliente.CONFIRMADO) {
			throw new IncomeException("El anticipo " + idAnticipo + " no está confirmado "
					+ "(su pago aún no fue ejecutado por el banco): todavía no tiene saldo "
					+ "que cruzar.");
		}
		double disponible = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
		if (monto > disponible + TOLERANCIA) {
			throw new IncomeException("El anticipo " + idAnticipo + " ("
					+ nvl(anticipo.getNumeroDoc(), "sin número") + ") tiene un saldo disponible de $"
					+ String.format(java.util.Locale.US, "%.2f", disponible)
					+ " y no alcanza para cruzar $"
					+ String.format(java.util.Locale.US, "%.2f", monto) + ".");
		}
	}

	/**
	 * Reparte un valor entre los anticipos disponibles del más antiguo al más
	 * nuevo (FIFO), para los llamadores que sólo indican el monto total.
	 * @param disponibles   : Anticipos con saldo, ya ordenados FIFO
	 * @param valor         : Valor total a cruzar
	 * @param nombreTitular : Nombre del titular, para el mensaje de error
	 * @return              : Pares {anticipo, monto a aplicar}
	 * @throws Throwable    : Excepcion si el saldo de anticipos no alcanza
	 */
	private List<Object[]> repartoFifo(List<AnticipoCliente> disponibles, Double valor,
			String nombreTitular) throws Throwable {

		List<Object[]> reparto = new ArrayList<>();
		double pendiente = valor;

		for (AnticipoCliente anticipo : disponibles) {
			if (pendiente <= TOLERANCIA) {
				break;
			}
			double disponible = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
			if (disponible <= TOLERANCIA) {
				continue;
			}
			double aplica = Math.min(disponible, pendiente);
			reparto.add(new Object[]{anticipo, aplica});
			pendiente -= aplica;
		}

		if (pendiente > TOLERANCIA) {
			double cubierto = valor - pendiente;
			throw new IncomeException("El titular '" + nvl(nombreTitular, "")
					+ "' tiene anticipos con saldo por $"
					+ String.format(java.util.Locale.US, "%.2f", cubierto)
					+ " y no alcanza para cruzar $"
					+ String.format(java.util.Locale.US, "%.2f", valor) + ".");
		}
		return reparto;
	}

	/**
	 * Ejecuta el cruce: una aplicación por anticipo, cada una con su asiento.
	 * <p>
	 * Se genera una aplicación por anticipo (y no una sola por el total) porque
	 * es lo que permite reversar exactamente: al anular un anticipo se deshacen
	 * sus aplicaciones y ninguna otra.
	 * @param factura         : Factura de compra que recibe los abonos
	 * @param reparto         : Pares {AnticipoCliente, monto}
	 * @param fechaAplicacion : Fecha del cruce (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa       : Id de la empresa contable
	 * @param idUsuario       : Id del usuario que registra
	 * @param observacion     : Observación del usuario
	 * @return                : Mapa con exito, mensaje, aplicaciones, asientos y saldos
	 * @throws Throwable      : Excepcion
	 */
	private Map<String, Object> aplicaCruces(Factura factura, List<Object[]> reparto,
			String fechaAplicacion, Long idEmpresa, Long idUsuario, String observacion)
			throws Throwable {

		if (reparto.isEmpty()) {
			throw new IncomeException("No hay anticipos con saldo para cruzar.");
		}

		Long idCliente = factura.getTitular().getCodigo();

		double total = 0.0;
		for (Object[] linea : reparto) {
			total += (Double) linea[1];
		}

		// 1. La factura debe tener saldo suficiente para el total del cruce
		validaMontoContraSaldo(factura, total);

		// 2. El saldo global del cliente debe respaldar el cruce. Con el saldo
		//    por anticipo esto es redundante, pero mientras PRCC siga siendo la
		//    cuenta que mueve la contabilidad conviene detectar el descuadre
		//    aquí y no a mitad del proceso.
		PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(idCliente, idEmpresa);
		double saldoAnticipos = (cuentaAnticipos.getSaldoInicial() != null)
				? cuentaAnticipos.getSaldoInicial() : 0.0;
		if (saldoAnticipos + TOLERANCIA < total) {
			throw new IncomeException("El saldo de anticipos del cliente '"
					+ factura.getTitular().getNombre() + "' es de $"
					+ String.format(java.util.Locale.US, "%.2f", saldoAnticipos)
					+ " y no alcanza para cruzar $"
					+ String.format(java.util.Locale.US, "%.2f", total)
					+ ". Revise el cuadre entre los anticipos y la cuenta contable.");
		}

		LocalDate fecha = parseFecha(fechaAplicacion);
		List<Map<String, Object>> lineas = new ArrayList<>();

		for (Object[] linea : reparto) {
			AnticipoCliente anticipo = (AnticipoCliente) linea[0];
			Double monto = (Double) linea[1];

			String observacionAsiento = "Cruce de anticipo | Cliente: "
					+ factura.getTitular().getNombre()
					+ " | Factura: " + factura.getNumero()
					+ " | Anticipo: " + nvl(anticipo.getNumeroDoc(), "#" + anticipo.getId())
					+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", monto);

			// 3. Asiento contable del cruce (uno por anticipo consumido)
			Asiento asiento = asientoContableService.generarAsientoAplicacionAnticipoCliente(
					idCliente, monto, idEmpresa, TipoAsientos.APLICACION_ANTICIPO_CLIENTE,
					fecha, observacionAsiento, usuarioNombre(idUsuario));

			// 4. Descontar el saldo del anticipo consumido
			double saldoAnterior = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
			anticipo.setSaldo(redondea(saldoAnterior - monto));
			em.merge(anticipo);
			System.out.println("✓ Anticipo " + anticipo.getId() + " saldo: " + saldoAnterior
					+ " → " + anticipo.getSaldo());

			// 5. Aplicación enlazada al anticipo de ORIGEN: es lo que permite
			//    deshacer exactamente este abono si el anticipo se anula.
			AplicacionPagoCxc aplicacion = nuevaAplicacion(factura, idEmpresa,
					TipoDocPagoAplicacion.ANTICIPO, monto, fecha,
					construyeObservacionCruce(anticipo, observacion),
					usuarioNombre(idUsuario));
			aplicacion.setAsiento(asiento);
			aplicacion.setAnticipoOrigen(anticipo);
			aplicacion.setUsuario(em.find(Usuario.class, idUsuario));
			aplicacion = saveSingle(aplicacion);

			Map<String, Object> detalle = new HashMap<>();
			detalle.put("aplicacion", aplicacion.getId());
			detalle.put("idAnticipo", anticipo.getId());
			detalle.put("numeroDocAnticipo", anticipo.getNumeroDoc());
			detalle.put("montoAplicado", monto);
			detalle.put("saldoAnticipo", anticipo.getSaldo());
			detalle.put("asiento", asiento.getNumeroAlterno());
			lineas.add(detalle);
		}

		// 6. Descontar el saldo global del cliente una sola vez, por el total
		cuentaAnticipos.setSaldoInicial(redondea(saldoAnticipos - total));
		em.merge(cuentaAnticipos);
		System.out.println("✓ Saldo global de anticipos del cliente " + idCliente + ": "
				+ saldoAnticipos + " → " + cuentaAnticipos.getSaldoInicial());

		em.flush();

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", (lineas.size() == 1)
				? "Anticipo cruzado correctamente."
				: "Se cruzaron " + lineas.size() + " anticipos correctamente.");
		resultado.put("lineas", lineas);
		resultado.put("totalCruzado", redondea(total));
		resultado.put("saldoAnticipos", cuentaAnticipos.getSaldoInicial());
		// Compatibilidad con los clientes que leen un solo cruce
		resultado.put("aplicacion", lineas.get(0).get("aplicacion"));
		resultado.put("asiento", lineas.get(0).get("asiento"));
		resultado.putAll(saldoFactura(factura.getId()));
		return resultado;
	}

	/**
	 * Observación de la aplicación de un cruce, con el anticipo de origen a la
	 * vista para que el historial de la factura sea legible sin navegar.
	 * @param anticipo    : Anticipo consumido
	 * @param observacion : Observación que escribió el usuario
	 * @return            : Texto de la observación
	 */
	private String construyeObservacionCruce(AnticipoCliente anticipo, String observacion) {
		StringBuilder texto = new StringBuilder("Cruce de anticipo ")
				.append(nvl(anticipo.getNumeroDoc(), "#" + anticipo.getId()));
		if (anticipo.getFechaAnticipo() != null) {
			texto.append(" del ").append(anticipo.getFechaAnticipo());
		}
		if (observacion != null && !observacion.trim().isEmpty()) {
			texto.append(" | ").append(observacion.trim());
		}
		return texto.toString();
	}

	private Long toLong(Object valor) {
		if (valor == null) return null;
		if (valor instanceof Number) return ((Number) valor).longValue();
		String texto = valor.toString().trim();
		return texto.isEmpty() ? null : Long.valueOf(texto);
	}

	private Double toDouble(Object valor) {
		if (valor == null) return null;
		if (valor instanceof Number) return ((Number) valor).doubleValue();
		String texto = valor.toString().trim();
		return texto.isEmpty() ? null : Double.valueOf(texto);
	}

	private double redondea(double valor) {
		return Math.round(valor * 100.0) / 100.0;
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

			// El saldo global del cliente se devuelve siempre: es la cuenta
			// que mueve la contabilidad del cruce, exista o no el detalle por
			// anticipo.
			Factura factura = aplicacion.getFactura();
			Long idEmpresa = (aplicacion.getEmpresa() != null)
					? aplicacion.getEmpresa().getCodigo() : null;
			if (factura != null && factura.getTitular() != null) {
				PersonaCuentaContable cuentaAnticipos =
						obtenerCuentaAnticipos(factura.getTitular().getCodigo(), idEmpresa);
				double saldoActual = (cuentaAnticipos.getSaldoInicial() != null)
						? cuentaAnticipos.getSaldoInicial() : 0.0;
				cuentaAnticipos.setSaldoInicial(
						redondea(saldoActual + aplicacion.getMontoAplicado()));
				em.merge(cuentaAnticipos);
				System.out.println("✓ Saldo global de anticipos devuelto: " + saldoActual
						+ " → " + cuentaAnticipos.getSaldoInicial());
			}

			// Y el saldo del anticipo concreto que se consumió, si el cruce
			// sabe de cuál salió (todos los posteriores al 2026-08-20).
			AnticipoCliente origen = (aplicacion.getAnticipoOrigen() != null)
					? em.find(AnticipoCliente.class, aplicacion.getAnticipoOrigen().getId())
					: null;
			if (origen != null) {
				double saldoActual = (origen.getSaldo() != null) ? origen.getSaldo() : 0.0;
				origen.setSaldo(redondea(saldoActual + aplicacion.getMontoAplicado()));
				em.merge(origen);
				System.out.println("✓ Saldo del anticipo " + origen.getId() + " devuelto: "
						+ saldoActual + " → " + origen.getSaldo());
			} else {
				System.out.println("⚠ El cruce " + aplicacion.getId() + " no tiene anticipo de "
						+ "origen (cruce anterior a la migración): sólo se devolvió el saldo global.");
			}

			// Los cruces viejos dejaban además un movimiento negativo en
			// CBR.ANTC; si esta aplicación tiene uno, se anula para que no
			// siga restando en los listados históricos.
			AnticipoCliente movimiento = (aplicacion.getAnticipo() != null)
					? em.find(AnticipoCliente.class, aplicacion.getAnticipo().getId()) : null;
			if (movimiento != null && movimiento.getValor() != null
					&& movimiento.getValor() < 0) {
				movimiento.setEstado(Long.valueOf(EstadoAnticipoCliente.ANULADO));
				movimiento.setObservacion(nvl(movimiento.getObservacion(), "")
						+ " | REVERSADO: " + motivo);
				em.merge(movimiento);
				System.out.println("✓ Movimiento negativo " + movimiento.getId()
						+ " anulado por reversión del cruce.");
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

	@Override
	public List<Map<String, Object>> listar(Long idEmpresa, Long idTitular, LocalDate desde,
			LocalDate hasta, Long formaPago, Long estado) throws Throwable {
		System.out.println("=== listar (CXC) | idEmpresa=" + idEmpresa + " | idTitular=" + idTitular
				+ " | desde=" + desde + " | hasta=" + hasta + " | formaPago=" + formaPago
				+ " | estado=" + estado + " ===");

		List<Object[]> filas = aplicacionPagoCxcDaoService.selectListado(
				idEmpresa, idTitular, desde, hasta, formaPago, estado);

		List<Map<String, Object>> resultado = new ArrayList<>();
		for (Object[] fila : filas) {
			// Columnas: 0 id, 1 fechaAplicacion, 2 idTitularFactura, 3 nombreTitularFactura,
			// 4 idTitularLiquidacion, 5 nombreTitularLiquidacion, 6 idFactura, 7 numeroFactura,
			// 8 idLiquidacion, 9 numeroLiquidacion, 10 tipoDocPago, 11 formaPago,
			// 12 montoAplicado, 13 idAsiento, 14 numeroAlternoAsiento, 15 estado.
			Long idFactura = (Long) fila[6];
			Long idLiquidacion = (Long) fila[8];

			Map<String, Object> titular = new HashMap<>();
			if (idFactura != null) {
				titular.put("codigo", fila[2]);
				titular.put("nombre", fila[3]);
			} else {
				titular.put("codigo", fila[4]);
				titular.put("nombre", fila[5]);
			}

			// Factura y liquidación son mutuamente excluyentes por fila (ver
			// javadoc de AplicacionPagoCxc): se expone cuál de las dos es,
			// en vez de asumir siempre "factura" — hoy en la práctica casi
			// todas las filas son de factura, pero el modelo admite ambas.
			Map<String, Object> documentoAfectado = new HashMap<>();
			if (idFactura != null) {
				documentoAfectado.put("tipo", "FACTURA");
				documentoAfectado.put("id", idFactura);
				documentoAfectado.put("numero", fila[7]);
			} else if (idLiquidacion != null) {
				documentoAfectado.put("tipo", "LIQUIDACION_COMPRA");
				documentoAfectado.put("id", idLiquidacion);
				documentoAfectado.put("numero", fila[9]);
			}

			Map<String, Object> item = new HashMap<>();
			item.put("id", fila[0]);
			item.put("fecha", fila[1]);
			item.put("titular", titular);
			item.put("documentoAfectado", documentoAfectado);
			item.put("tipoDocPago", fila[10]);
			item.put("formaPago", fila[11]);
			item.put("valor", fila[12]);
			if (fila[13] != null) {
				Map<String, Object> asiento = new HashMap<>();
				asiento.put("id", fila[13]);
				asiento.put("numeroAlterno", fila[14]);
				item.put("asiento", asiento);
			} else {
				item.put("asiento", null);
			}
			item.put("estado", fila[15]);
			resultado.add(item);
		}
		return resultado;
	}
}
