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
import com.saa.ejb.tsr.dao.EgresoDaoService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.EgresoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Egreso;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoEgresoTesoreria;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.RolPersona;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class EgresoServiceImpl implements EgresoService {

	/** Tolerancia de centavos, igual que AplicacionPagoCxpServiceImpl / DevolucionAnticipoProveedorServiceImpl. */
	private static final double TOLERANCIA = 0.01;

	@EJB
	private EgresoDaoService egresoDaoService;

	@EJB
	private PagoProgramadoService pagoProgramadoService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@EJB
	private PersonaCuentaContableDaoService personaCuentaContableDaoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public Egreso selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById Egreso con id: " + id);
		return egresoDaoService.selectById(id, NombreEntidadesTesoreria.EGRESO);
	}

	@Override
	public List<Egreso> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll EgresoService");
		List<Egreso> result = egresoDaoService.selectAll(NombreEntidadesTesoreria.EGRESO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total Egreso no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<Egreso> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria EgresoService");
		List<Egreso> result =
				egresoDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.EGRESO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio Egreso no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public Egreso saveSingle(Egreso egreso) throws Throwable {
		System.out.println("saveSingle - Egreso");
		if (egreso.getId() == null) {
			if (egreso.getEstado() == null) {
				egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PENDIENTE_PAGO));
			}
			if (egreso.getDebitoAutomatico() == null) {
				egreso.setDebitoAutomatico(Long.valueOf(0));
			}
			if (egreso.getFechaRegistro() == null) {
				egreso.setFechaRegistro(LocalDateTime.now());
			}
		}
		return egresoDaoService.save(egreso, egreso.getId());
	}

	@Override
	public void save(List<Egreso> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de EgresoService");
		for (Egreso registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de EgresoService");
		Egreso entidad = new Egreso();
		for (Long registro : id) {
			egresoDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Proceso de negocio
	// =====================================================================

	@Override
	public Map<String, Object> procesarEgreso(Long idEmpresa, Long idTitular, Long idProductoPago,
			String descripcion, Double valor, String fecha, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, boolean debitoAutomatico, String referencia,
			String observacion, Long idUsuario) throws Throwable {
		return procesarEgreso(idEmpresa, idTitular, idProductoPago, descripcion, valor, fecha,
				idCuentaBancariaOrigen, idCuentaDestinoTitular, debitoAutomatico, referencia,
				observacion, idUsuario, null);
	}

	@Override
	public Map<String, Object> procesarEgreso(Long idEmpresa, Long idTitular, Long idProductoPago,
			String descripcion, Double valor, String fecha, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, boolean debitoAutomatico, String referencia,
			String observacion, Long idUsuario, Long formaPago) throws Throwable {
		return procesarEgreso(idEmpresa, idTitular, idProductoPago, descripcion, valor, fecha,
				idCuentaBancariaOrigen, idCuentaDestinoTitular, debitoAutomatico, referencia,
				observacion, idUsuario, formaPago, null);
	}

	@Override
	public Map<String, Object> procesarEgreso(Long idEmpresa, Long idTitular, Long idProductoPago,
			String descripcion, Double valor, String fecha, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, boolean debitoAutomatico, String referencia,
			String observacion, Long idUsuario, Long formaPago, Long idAnticipo) throws Throwable {

		System.out.println("=== procesarEgreso | empresa=" + idEmpresa + " | producto=" + idProductoPago
				+ " | valor=" + valor + " | debitoAutomatico=" + debitoAutomatico
				+ " | formaPago=" + formaPago + " | idAnticipo=" + idAnticipo + " ===");

		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del egreso debe ser mayor a cero.");
		}
		if (descripcion == null || descripcion.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el concepto del egreso.");
		}
		if (idAnticipo == null && idCuentaBancariaOrigen == null && debitoAutomatico) {
			throw new IncomeException("Un egreso con débito automático necesita la cuenta bancaria de "
					+ "origen: se contabiliza en el acto y no pasa por la bandeja de aprobación.");
		}

		// El producto define la cuenta contable del gasto: se valida aquí para
		// que el error salga al registrar, no recién cuando el banco confirme.
		validaProducto(idProductoPago);

		Titular titular = null;
		if (idTitular != null) {
			titular = em.find(Titular.class, idTitular);
			if (titular == null) {
				throw new IncomeException("No se encontró el titular con ID: " + idTitular);
			}
		}

		// ÍTEM 2 (docs/logica-negocio/tsr/DISENO-EGRESO-CON-SALDO-DE-ANTICIPO.md §3.2): con
		// idAnticipo, un camino nuevo que NO pasa por PagoProgramadoServiceImpl -- el dinero ya
		// salió al entregar el anticipo, acá sólo se consume su saldo contra el gasto.
		if (idAnticipo != null) {
			return procesarEgresoConAnticipo(idEmpresa, titular, idProductoPago, descripcion,
					valor, fecha, observacion, idUsuario, idAnticipo);
		}

		// 1. Grabar el egreso pendiente de pago
		Egreso egreso = new Egreso();
		egreso.setEmpresa(em.find(Empresa.class, idEmpresa));
		egreso.setTitular(titular);
		egreso.setProducto(em.find(ProductoPago.class, idProductoPago));
		egreso.setDescripcion(descripcion.trim());
		egreso.setDebitoAutomatico(Long.valueOf(debitoAutomatico ? 1 : 0));
		egreso.setValor(valor);
		egreso.setFecha(parseFecha(fecha));
		egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PENDIENTE_PAGO));
		egreso.setObservacion(observacion);
		egreso.setUsuario(em.find(Usuario.class, idUsuario));
		egreso.setFechaRegistro(LocalDateTime.now());
		egreso = saveSingle(egreso);
		em.flush();

		System.out.println("✓ Egreso registrado: id=" + egreso.getId());

		// 2. Crear su pago en el circuito de PagoProgramado (con débito
		// automático el pago nace confirmado y contabiliza aquí mismo).
		Map<String, Object> resultado = pagoProgramadoService.registrarPagoDeEgreso(
				egreso.getId(), idCuentaBancariaOrigen, idCuentaDestinoTitular,
				idUsuario, debitoAutomatico, referencia, formaPago);

		// ÍTEM 2 (docs/logica-negocio/cnt/DISENO-IMPRIMIR-ASIENTO-DESDE-ORIGEN.md §2.2): con
		// débito automático el pago contabiliza en este mismo paso -- si la respuesta trae
		// asiento, el egreso ya lo tiene; se relee por id porque registrarPagoDeEgreso (otro
		// bean, PagoProgramadoServiceImpl) es quien lo actualizó, no esta misma instancia.
		if (resultado.get("asiento") != null) {
			Egreso egresoActualizado = em.find(Egreso.class, egreso.getId());
			if (egresoActualizado != null && egresoActualizado.getAsiento() != null) {
				resultado.put("idAsiento", egresoActualizado.getAsiento().getCodigo());
			}
		}

		resultado.put("egreso", egreso.getId());
		return resultado;
	}

	/**
	 * ÍTEM 2 (docs/logica-negocio/tsr/DISENO-EGRESO-CON-SALDO-DE-ANTICIPO.md §3.2): egreso pagado
	 * con el saldo de un anticipo del proveedor -- gasto sin sustento (viáticos, etc.). Sin banco,
	 * sin {@code PagoProgramado}: el dinero ya salió al entregar el anticipo, acá sólo se consume
	 * su saldo contra el gasto. El egreso queda PAGADO en el mismo paso, con su propio asiento.
	 * @param idEmpresa      : Id de la empresa
	 * @param titular        : Beneficiario, ya resuelto y validado por el llamador (no nulo)
	 * @param idProductoPago : Id del producto CXP que clasifica el gasto
	 * @param descripcion    : Concepto del egreso
	 * @param valor          : Valor del egreso
	 * @param fecha          : Fecha del egreso (yyyy-MM-dd, null = hoy)
	 * @param observacion    : Observaciones (opcional)
	 * @param idUsuario      : Id del usuario que registra
	 * @param idAnticipo     : Id del anticipo (PGS.ANTP) cuyo saldo paga el egreso
	 * @return                : Mapa con exito, mensaje, egreso, asiento (numeroAlterno) e idAsiento
	 * @throws Throwable      : IncomeException si no se cumple alguna validación
	 */
	private Map<String, Object> procesarEgresoConAnticipo(Long idEmpresa, Titular titular,
			Long idProductoPago, String descripcion, Double valor, String fecha, String observacion,
			Long idUsuario, Long idAnticipo) throws Throwable {

		// 2. Titular obligatorio
		if (titular == null) {
			throw new IncomeException("Para pagar con un anticipo hay que indicar el beneficiario.");
		}
		Long idTitular = titular.getCodigo();

		// 3. El anticipo existe, es del mismo titular y empresa, CONFIRMADO, valor <= saldo+0.01
		AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
		if (anticipo == null) {
			throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
		}
		if (anticipo.getTitular() == null || !idTitular.equals(anticipo.getTitular().getCodigo())) {
			throw new IncomeException("El anticipo " + idAnticipo + " no pertenece al beneficiario "
					+ "indicado: no se puede pagar el egreso con él.");
		}
		if (anticipo.getEmpresa() == null || !idEmpresa.equals(anticipo.getEmpresa().getCodigo())) {
			throw new IncomeException("El anticipo " + idAnticipo + " es de otra empresa contable: "
					+ "no se puede pagar el egreso con él.");
		}
		if (anticipo.getEstado() == null
				|| anticipo.getEstado().intValue() != EstadoAnticipoProveedor.CONFIRMADO) {
			throw new IncomeException("El anticipo " + idAnticipo + " no está confirmado: no tiene "
					+ "saldo para pagar el egreso.");
		}
		double saldoAnticipo = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
		if (valor > saldoAnticipo + TOLERANCIA) {
			throw new IncomeException("El anticipo " + idAnticipo + " ("
					+ nvl(anticipo.getNumeroDoc(), "sin número") + ") tiene un saldo disponible de $"
					+ String.format(java.util.Locale.US, "%.2f", saldoAnticipo)
					+ " y no alcanza para pagar $" + String.format(java.util.Locale.US, "%.2f", valor) + ".");
		}

		// 4. El PRCC tipo 2 del titular existe y su saldoInicial >= valor (mismo mensaje que
		//    DevolucionAnticipoProveedorServiceImpl, adaptado a "pagar este egreso")
		PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(idTitular, idEmpresa);
		double saldoAnticipos = (cuentaAnticipos.getSaldoInicial() != null)
				? cuentaAnticipos.getSaldoInicial() : 0.0;
		if (saldoAnticipos + TOLERANCIA < valor) {
			throw new IncomeException("El saldo de anticipos del proveedor '" + titular.getNombre()
					+ "' es de $" + String.format(java.util.Locale.US, "%.2f", saldoAnticipos)
					+ " y no alcanza para pagar este egreso de $"
					+ String.format(java.util.Locale.US, "%.2f", valor)
					+ ". Revise el cuadre entre los anticipos y la cuenta contable.");
		}

		Usuario usuario = (idUsuario != null) ? em.find(Usuario.class, idUsuario) : null;
		String nombreUsuario = (usuario != null && usuario.getNombre() != null)
				? usuario.getNombre() : "SISTEMA";
		LocalDate fechaEgreso = parseFecha(fecha);

		String observacionAsiento = "Egreso pagado con el anticipo "
				+ nvl(anticipo.getNumeroDoc(), "#" + anticipo.getId())
				+ " | Concepto: " + descripcion.trim();
		if (observacion != null && !observacion.trim().isEmpty()) {
			observacionAsiento += " | " + observacion.trim();
		}

		// 5. Asiento: DEBE la cuenta del grupo del producto / HABER anticipos del proveedor
		Asiento asiento = asientoContableService.generarAsientoEgresoContraAnticipo(
				idProductoPago, idTitular, valor, idEmpresa, fechaEgreso, observacionAsiento,
				nombreUsuario);

		// 6. Egreso PAGADO en el acto, con asiento y anticipo. Sin PagoProgramado ni movimiento
		//    bancario: el dinero ya salió al entregar el anticipo.
		Egreso egreso = new Egreso();
		egreso.setEmpresa(em.find(Empresa.class, idEmpresa));
		egreso.setTitular(titular);
		egreso.setProducto(em.find(ProductoPago.class, idProductoPago));
		egreso.setDescripcion(descripcion.trim());
		egreso.setDebitoAutomatico(Long.valueOf(0));
		egreso.setValor(valor);
		egreso.setFecha(fechaEgreso);
		egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PAGADO));
		egreso.setAsiento(asiento);
		egreso.setAnticipo(anticipo);
		egreso.setObservacion(observacion);
		egreso.setUsuario(usuario);
		egreso.setFechaRegistro(LocalDateTime.now());
		egreso = saveSingle(egreso);

		anticipo.setSaldo(redondea(saldoAnticipo - valor));
		em.merge(anticipo);

		cuentaAnticipos.setSaldoInicial(redondea(saldoAnticipos - valor));
		em.merge(cuentaAnticipos);

		em.flush();

		System.out.println("✓ Egreso " + egreso.getId() + " pagado con el anticipo " + idAnticipo
				+ " | asiento=" + asiento.getNumeroAlterno());

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", "Egreso registrado y pagado con el saldo del anticipo "
				+ nvl(anticipo.getNumeroDoc(), "#" + anticipo.getId()) + ".");
		resultado.put("egreso", egreso.getId());
		resultado.put("asiento", asiento.getNumeroAlterno());
		resultado.put("idAsiento", asiento.getCodigo());
		return resultado;
	}

	/**
	 * Cuenta contable de anticipos (tipo 2, rol Proveedor) de un titular. Mismo criterio y mismo
	 * mensaje que {@code AplicacionPagoCxpServiceImpl.obtenerCuentaAnticipos} /
	 * {@code DevolucionAnticipoProveedorServiceImpl.obtenerCuentaAnticipos}.
	 */
	private PersonaCuentaContable obtenerCuentaAnticipos(Long idTitular, Long idEmpresa) throws Throwable {
		List<PersonaCuentaContable> lista = personaCuentaContableDaoService
				.selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);
		if (lista.isEmpty()) {
			throw new IncomeException("El proveedor no tiene configurada la cuenta contable de "
					+ "anticipos (Tipo 2, Rol: Proveedor) en Tesorería → Persona → Cuentas Contables. "
					+ "Sin ella no es posible pagar con el saldo de un anticipo.");
		}
		return lista.get(0);
	}

	private double redondea(double valor) {
		return Math.round(valor * 100.0) / 100.0;
	}

	@Override
	public Map<String, Object> anularEgreso(Long idEgreso, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== anularEgreso | egreso=" + idEgreso + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}

		Egreso egreso = em.find(Egreso.class, idEgreso);
		if (egreso == null) {
			throw new IncomeException("No se encontró el egreso con ID: " + idEgreso);
		}

		int estado = (egreso.getEstado() != null) ? egreso.getEstado().intValue() : 0;
		if (estado == EstadoEgresoTesoreria.ANULADO) {
			throw new IncomeException("El egreso " + idEgreso + " ya está anulado.");
		}

		// ÍTEM 3 (docs/logica-negocio/tsr/DISENO-EGRESO-CON-SALDO-DE-ANTICIPO.md §3.3): un egreso
		// PAGADO con el saldo de un anticipo no tiene pago que reversar -- nunca pasó por
		// PagoProgramado. Se anula directo acá, ANTES de la guarda de abajo (que rechaza todo
		// egreso PAGADO): este caso es la excepción.
		if (egreso.getAnticipo() != null) {
			return anularEgresoConAnticipo(egreso, motivo, idUsuario);
		}

		if (estado == EstadoEgresoTesoreria.PAGADO) {
			throw new IncomeException("El egreso " + idEgreso + " ya está pagado y tiene "
					+ "contabilidad generada. Reverse el pago (pgtr/revertirConfirmado) primero.");
		}

		// Un pago Registrado se anula junto con el egreso; uno En archivo está
		// en poder del banco y bloquea la anulación hasta procesar la respuesta.
		// Uno Por_aprobar se anula igual que uno Registrado -esta incluso mas atras
		// en el circuito, ni siquiera llego a la bandeja de tesoreria-.
		//
		// selectVigentesByEgreso (PagoProgramadoDaoService) NO incluye POR_APROBAR: es una
		// consulta compartida con otros equipos (ver docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md
		// #11) y no se toca. Sin este reemplazo, un pago registrado sin cuenta de origen
		// (nace POR_APROBAR) quedaria invisible aqui: el egreso se anularia igual, pero el
		// pago seguiria vivo apuntandole, y tesoreria podria aprobarlo y confirmarlo despues
		// -generando contabilidad y movimiento bancario real de un egreso ya anulado-.
		List<PagoProgramado> vigentes = pagosVivosDelEgreso(idEgreso);
		for (PagoProgramado pago : vigentes) {
			int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
			if (estadoPago == EstadoPagoProgramado.EN_ARCHIVO) {
				throw new IncomeException("El pago " + pago.getId() + " del egreso está en un "
						+ "archivo enviado al banco. Procese la respuesta del banco antes de anular.");
			}
			if (estadoPago == EstadoPagoProgramado.REGISTRADO
					|| estadoPago == EstadoPagoProgramado.POR_APROBAR) {
				pagoProgramadoService.anularPago(pago.getId(),
						"Anulación del egreso: " + motivo.trim(), idUsuario);
			}
		}

		egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.ANULADO));
		egreso.setObservacion(nvl(egreso.getObservacion(), "") + " | ANULADO: " + motivo.trim());
		em.merge(egreso);
		em.flush();

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", "Egreso anulado correctamente.");
		resultado.put("egreso", idEgreso);
		return resultado;
	}

	/**
	 * ÍTEM 3 (docs/logica-negocio/tsr/DISENO-EGRESO-CON-SALDO-DE-ANTICIPO.md §3.3): anula un
	 * egreso PAGADO con el saldo de un anticipo. Anula el asiento, repone el saldo del anticipo y
	 * el PRCC tipo 2, y deja el estado en ANULADO. SIN tragarse errores en ningún paso (a
	 * diferencia de {@code anularEgreso}, que sí captura al anular pagos de PagoProgramado): acá
	 * no hay pago ni movimiento bancario que reversar, un fallo en cualquier paso tiene que
	 * abortar la anulación entera.
	 * @param egreso    : Egreso ya validado (ANULADO descartado por el llamador) y con anticipo
	 * @param motivo    : Motivo de la anulación, ya validado por el llamador
	 * @param idUsuario : Id del usuario que anula (no se usa en el asiento, se deja por simetría)
	 * @return           : Mapa con exito, mensaje y egreso
	 * @throws Throwable : Excepcion si falla el asiento o la cuenta de anticipos no existe
	 */
	private Map<String, Object> anularEgresoConAnticipo(Egreso egreso, String motivo, Long idUsuario)
			throws Throwable {

		Long idAsiento = (egreso.getAsiento() != null) ? egreso.getAsiento().getCodigo() : null;
		if (idAsiento != null) {
			asientoService.anulaAsiento(idAsiento);
		}

		AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, egreso.getAnticipo().getId());
		if (anticipo != null) {
			double valor = (egreso.getValor() != null) ? egreso.getValor() : 0.0;

			double saldoAnterior = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
			anticipo.setSaldo(redondea(saldoAnterior + valor));
			em.merge(anticipo);
			System.out.println("✓ Anticipo " + anticipo.getId() + " saldo repuesto: " + saldoAnterior
					+ " → " + anticipo.getSaldo());

			Long idTitular = (anticipo.getTitular() != null) ? anticipo.getTitular().getCodigo() : null;
			Long idEmpresa = (egreso.getEmpresa() != null) ? egreso.getEmpresa().getCodigo() : null;
			if (idTitular != null && idEmpresa != null) {
				PersonaCuentaContable cuentaAnticipos = obtenerCuentaAnticipos(idTitular, idEmpresa);
				double saldoAnticiposAnterior = (cuentaAnticipos.getSaldoInicial() != null)
						? cuentaAnticipos.getSaldoInicial() : 0.0;
				cuentaAnticipos.setSaldoInicial(redondea(saldoAnticiposAnterior + valor));
				em.merge(cuentaAnticipos);
				System.out.println("✓ Saldo global de anticipos del proveedor " + idTitular + ": "
						+ saldoAnticiposAnterior + " → " + cuentaAnticipos.getSaldoInicial());
			}
		}

		egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.ANULADO));
		egreso.setObservacion(nvl(egreso.getObservacion(), "") + " | ANULADO: " + motivo.trim());
		em.merge(egreso);
		em.flush();

		System.out.println("✓ Egreso " + egreso.getId() + " (pagado con anticipo) anulado.");

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", "Egreso anulado. El asiento fue reversado y el saldo del anticipo "
				+ "fue repuesto.");
		resultado.put("egreso", egreso.getId());
		return resultado;
	}

	/**
	 * Pagos vivos de un egreso, incluyendo POR_APROBAR(0).
	 *
	 * <p><code>PagoProgramadoDaoService.selectVigentesByEgreso</code> no lo incluye, y es una
	 * consulta compartida con otros equipos (crd la usa tambien): no se toca, se resuelve con
	 * consulta propia — mismo patron que <code>tienePagoVivoEnBandeja</code> en
	 * <code>GeneracionOrdenPagoServiceImpl</code> (item 7). Ver
	 * docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md #11.</p>
	 *
	 * @param idEgreso		: Id del egreso
	 * @return				: Pagos en POR_APROBAR, REGISTRADO, EN_ARCHIVO o CONFIRMADO
	 * @throws Throwable	: Excepcion
	 */
	@SuppressWarnings("unchecked")
	private List<PagoProgramado> pagosVivosDelEgreso(Long idEgreso) throws Throwable {
		return em.createQuery(" select   p "
				+ " from     PagoProgramado p "
				+ " where    p.egreso.id = :idEgreso "
				+ "          and p.estado in (:porAprobar, :registrado, :enArchivo, :confirmado) ")
				.setParameter("idEgreso", idEgreso)
				.setParameter("porAprobar", Long.valueOf(EstadoPagoProgramado.POR_APROBAR))
				.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO))
				.setParameter("enArchivo", Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO))
				.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO))
				.getResultList();
	}

	@Override
	public List<Egreso> listar(Long idEmpresa, Long estado, Long idTitular, String concepto, String desde,
			String hasta) throws Throwable {
		System.out.println("=== listar egresos | empresa=" + idEmpresa + " | estado=" + estado
				+ " | titular=" + idTitular + " | concepto=" + concepto + " | desde=" + desde
				+ " | hasta=" + hasta + " ===");
		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		LocalDate fechaDesde = (desde != null && !desde.trim().isEmpty())
				? LocalDate.parse(desde.trim()) : null;
		LocalDate fechaHasta = (hasta != null && !hasta.trim().isEmpty())
				? LocalDate.parse(hasta.trim()) : null;
		List<Egreso> egresos = egresoDaoService.selectByEmpresaEstado(idEmpresa, estado, idTitular,
				concepto, fechaDesde, fechaHasta);
		completaFormaPago(egresos);
		return egresos;
	}

	/**
	 * Puebla los campos transitorios {@code formaPago} y {@code numeroCheque}
	 * de cada egreso con los del PagoProgramado más reciente no anulado
	 * asociado (TSR.EGRS no guarda la forma de pago real, sólo el espejo
	 * {@code debitoAutomatico}; el cheque vive únicamente en PGS.PGTR). Una
	 * sola consulta para toda la página, no una por fila.
	 * @param egresos : Egresos ya cargados (se modifican en el sitio)
	 * @throws Throwable : Excepcion
	 */
	private void completaFormaPago(List<Egreso> egresos) throws Throwable {
		if (egresos == null || egresos.isEmpty()) {
			return;
		}
		List<Long> ids = new java.util.ArrayList<>();
		for (Egreso egreso : egresos) {
			ids.add(egreso.getId());
		}

		@SuppressWarnings("unchecked")
		List<PagoProgramado> pagos = em.createQuery(
				"select p from PagoProgramado p where p.egreso.id in :ids "
				+ "and p.estado <> :anulado order by p.fechaRegistro desc")
				.setParameter("ids", ids)
				.setParameter("anulado", Long.valueOf(EstadoPagoProgramado.ANULADO))
				.getResultList();

		// El primer pago que aparece por egreso es el más reciente (la
		// consulta ya viene ordenada desc); los siguientes para el mismo
		// egreso se descartan.
		Map<Long, PagoProgramado> pagoPorEgreso = new HashMap<>();
		for (PagoProgramado pago : pagos) {
			if (pago.getEgreso() != null) {
				pagoPorEgreso.putIfAbsent(pago.getEgreso().getId(), pago);
			}
		}

		for (Egreso egreso : egresos) {
			PagoProgramado pago = pagoPorEgreso.get(egreso.getId());
			if (pago != null) {
				egreso.setFormaPago(pago.getFormaPago());
				egreso.setNumeroCheque((pago.getCheque() != null) ? pago.getCheque().getNumero() : null);
			}
		}
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	/**
	 * Valida que el producto exista y que su grupo tenga cuenta contable: sin
	 * eso el asiento del pago no se puede generar.
	 * @param idProductoPago : Id del producto CXP
	 * @throws Throwable     : Excepcion con mensaje accionable
	 */
	private void validaProducto(Long idProductoPago) throws Throwable {
		if (idProductoPago == null) {
			throw new IncomeException("Debe indicar el producto que clasifica el egreso.");
		}
		ProductoPago producto = em.find(ProductoPago.class, idProductoPago);
		if (producto == null) {
			throw new IncomeException("No se encontró el producto CXP con ID: " + idProductoPago);
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
