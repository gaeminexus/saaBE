package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.tsr.dao.EgresoDaoService;
import com.saa.ejb.tsr.service.EgresoService;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Egreso;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoEgresoTesoreria;
import com.saa.rubros.EstadoPagoProgramado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class EgresoServiceImpl implements EgresoService {

	@EJB
	private EgresoDaoService egresoDaoService;

	@EJB
	private PagoProgramadoService pagoProgramadoService;

	@EJB
	private PagoProgramadoDaoService pagoProgramadoDaoService;

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

		System.out.println("=== procesarEgreso | empresa=" + idEmpresa + " | producto=" + idProductoPago
				+ " | valor=" + valor + " | debitoAutomatico=" + debitoAutomatico
				+ " | formaPago=" + formaPago + " ===");

		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del egreso debe ser mayor a cero.");
		}
		if (descripcion == null || descripcion.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el concepto del egreso.");
		}
		if (idCuentaBancariaOrigen == null) {
			throw new IncomeException("Debe indicar la cuenta bancaria propia del egreso.");
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

		resultado.put("egreso", egreso.getId());
		return resultado;
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
		if (estado == EstadoEgresoTesoreria.PAGADO) {
			throw new IncomeException("El egreso " + idEgreso + " ya está pagado y tiene "
					+ "contabilidad generada. Reverse el pago (pgtr/revertirConfirmado) primero.");
		}

		// Un pago Registrado se anula junto con el egreso; uno En archivo está
		// en poder del banco y bloquea la anulación hasta procesar la respuesta.
		List<PagoProgramado> vigentes = pagoProgramadoDaoService.selectVigentesByEgreso(idEgreso);
		for (PagoProgramado pago : vigentes) {
			int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
			if (estadoPago == EstadoPagoProgramado.EN_ARCHIVO) {
				throw new IncomeException("El pago " + pago.getId() + " del egreso está en un "
						+ "archivo enviado al banco. Procese la respuesta del banco antes de anular.");
			}
			if (estadoPago == EstadoPagoProgramado.REGISTRADO) {
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

	@Override
	public List<Egreso> listar(Long idEmpresa, Long estado) throws Throwable {
		System.out.println("=== listar egresos | empresa=" + idEmpresa + " | estado=" + estado + " ===");
		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		List<Egreso> egresos = egresoDaoService.selectByEmpresaEstado(idEmpresa, estado);
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
