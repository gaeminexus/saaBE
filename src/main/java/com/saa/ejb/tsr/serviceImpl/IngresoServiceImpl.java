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
import com.saa.ejb.tsr.dao.IngresoDaoService;
import com.saa.ejb.tsr.service.IngresoService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.ProductoCobro;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.Ingreso;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoIngresoTesoreria;
import com.saa.rubros.OrigenMovimientoConciliacion;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class IngresoServiceImpl implements IngresoService {

	@EJB
	private IngresoDaoService ingresoDaoService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@EJB
	private MovimientoBancoService movimientoBancoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public Ingreso selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById Ingreso con id: " + id);
		return ingresoDaoService.selectById(id, NombreEntidadesTesoreria.INGRESO);
	}

	@Override
	public List<Ingreso> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll IngresoService");
		List<Ingreso> result = ingresoDaoService.selectAll(NombreEntidadesTesoreria.INGRESO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total Ingreso no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<Ingreso> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria IngresoService");
		List<Ingreso> result =
				ingresoDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.INGRESO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio Ingreso no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public Ingreso saveSingle(Ingreso ingreso) throws Throwable {
		System.out.println("saveSingle - Ingreso");
		if (ingreso.getId() == null) {
			if (ingreso.getEstado() == null) {
				ingreso.setEstado(Long.valueOf(EstadoIngresoTesoreria.ACTIVO));
			}
			if (ingreso.getFechaRegistro() == null) {
				ingreso.setFechaRegistro(LocalDateTime.now());
			}
		}
		return ingresoDaoService.save(ingreso, ingreso.getId());
	}

	@Override
	public void save(List<Ingreso> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de IngresoService");
		for (Ingreso registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de IngresoService");
		Ingreso entidad = new Ingreso();
		for (Long registro : id) {
			ingresoDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Proceso de negocio
	// =====================================================================

	@Override
	public Map<String, Object> procesarIngreso(Long idEmpresa, Long idTitular, Long idProductoCobro,
			String descripcion, Double valor, String fecha, Long idCuentaBancaria,
			String referencia, String observacion, Long idUsuario) throws Throwable {

		System.out.println("=== procesarIngreso | empresa=" + idEmpresa + " | producto=" + idProductoCobro
				+ " | valor=" + valor + " | cuenta=" + idCuentaBancaria + " ===");

		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del ingreso debe ser mayor a cero.");
		}
		if (descripcion == null || descripcion.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el concepto del ingreso.");
		}

		CuentaBancaria cuentaBancaria = em.find(CuentaBancaria.class, idCuentaBancaria);
		if (cuentaBancaria == null) {
			throw new IncomeException("No se encontró la cuenta bancaria con ID: " + idCuentaBancaria);
		}

		Titular titular = null;
		if (idTitular != null) {
			titular = em.find(Titular.class, idTitular);
			if (titular == null) {
				throw new IncomeException("No se encontró el titular con ID: " + idTitular);
			}
		}

		LocalDate fechaIngreso = parseFecha(fecha);
		String concepto = descripcion.trim();

		// 1. Asiento contable: DEBE banco / HABER cuenta del grupo del producto.
		// El generador valida producto → grupo → cuenta contable.
		String observacionAsiento = "Ingreso tesorería | Concepto: " + concepto
				+ " | Ref: " + nvl(referencia, "")
				+ " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);
		Asiento asiento = asientoContableService.generarAsientoIngresoTesoreria(
				idProductoCobro, concepto, valor, idCuentaBancaria, idEmpresa,
				TipoAsientos.INGRESO_TESORERIA, fechaIngreso, observacionAsiento,
				usuarioNombre(idUsuario));

		// 2. Registro del ingreso, ya contabilizado
		Ingreso ingreso = new Ingreso();
		ingreso.setEmpresa(em.find(Empresa.class, idEmpresa));
		ingreso.setTitular(titular);
		ingreso.setProducto(em.find(ProductoCobro.class, idProductoCobro));
		ingreso.setDescripcion(concepto);
		ingreso.setValor(valor);
		ingreso.setFecha(fechaIngreso);
		ingreso.setCuentaBancaria(cuentaBancaria);
		ingreso.setReferencia((referencia != null && !referencia.trim().isEmpty())
				? referencia.trim() : null);
		ingreso.setEstado(Long.valueOf(EstadoIngresoTesoreria.ACTIVO));
		ingreso.setAsiento(asiento);
		ingreso.setObservacion(observacion);
		ingreso.setUsuario(em.find(Usuario.class, idUsuario));
		ingreso.setFechaRegistro(LocalDateTime.now());
		ingreso = saveSingle(ingreso);

		// 3. Movimiento bancario de ingreso para la conciliación
		movimientoBancoService.creaMovimientoPorTransferencia(idEmpresa,
				"Ingreso tesorería: " + concepto + " | Ref: " + nvl(referencia, ""),
				asiento, cuentaBancaria, valor,
				TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO,
				OrigenMovimientoConciliacion.COBROS);

		em.flush();

		System.out.println("✓ Ingreso registrado y contabilizado: id=" + ingreso.getId()
				+ " | asiento=" + asiento.getNumeroAlterno());

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", "Ingreso registrado. El asiento contable y el movimiento "
				+ "bancario fueron generados.");
		resultado.put("ingreso", ingreso.getId());
		resultado.put("asiento", asiento.getNumeroAlterno());
		return resultado;
	}

	@Override
	public Map<String, Object> anularIngreso(Long idIngreso, String motivo, Long idUsuario)
			throws Throwable {

		System.out.println("=== anularIngreso | ingreso=" + idIngreso + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}

		Ingreso ingreso = em.find(Ingreso.class, idIngreso);
		if (ingreso == null) {
			throw new IncomeException("No se encontró el ingreso con ID: " + idIngreso);
		}
		if (ingreso.getEstado() != null
				&& ingreso.getEstado().intValue() == EstadoIngresoTesoreria.ANULADO) {
			throw new IncomeException("El ingreso " + idIngreso + " ya está anulado.");
		}

		Long idAsiento = (ingreso.getAsiento() != null) ? ingreso.getAsiento().getCodigo() : null;

		// 1. Anular el movimiento bancario del asiento
		if (idAsiento != null) {
			try {
				movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
						Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
			} catch (Exception e) {
				System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
						+ idAsiento + ": " + e.getMessage());
			}
			// 2. Anular / reversar el asiento contable
			try {
				asientoService.anulaAsiento(idAsiento);
				System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
			} catch (Throwable e) {
				System.err.println("⚠ No se pudo anular el asiento " + idAsiento + ": " + e.getMessage());
			}
		}

		// 3. El ingreso queda anulado
		ingreso.setEstado(Long.valueOf(EstadoIngresoTesoreria.ANULADO));
		ingreso.setObservacion(nvl(ingreso.getObservacion(), "") + " | ANULADO: " + motivo.trim());
		em.merge(ingreso);
		em.flush();

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("exito", true);
		resultado.put("mensaje", "Ingreso anulado. El asiento y el movimiento bancario fueron reversados.");
		resultado.put("ingreso", idIngreso);
		return resultado;
	}

	@Override
	public List<Ingreso> listar(Long idEmpresa, Long estado) throws Throwable {
		System.out.println("=== listar ingresos | empresa=" + idEmpresa + " | estado=" + estado + " ===");
		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		return ingresoDaoService.selectByEmpresaEstado(idEmpresa, estado);
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

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
