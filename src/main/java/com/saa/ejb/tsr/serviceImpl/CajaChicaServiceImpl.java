package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.CajaChicaDaoService;
import com.saa.ejb.tsr.dao.CierreCajaChicaDaoService;
import com.saa.ejb.tsr.dao.MovimientoCajaChicaDaoService;
import com.saa.ejb.tsr.service.CajaChicaService;
import com.saa.model.tsr.CajaChica;
import com.saa.model.tsr.CierreCajaChica;
import com.saa.model.tsr.MovimientoCajaChica;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.EstadoCajaChica;
import com.saa.rubros.EstadoMovimientoCajaChica;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.TipoMovimientoCajaChica;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementación de CajaChicaService.</p>
 */
@Stateless
public class CajaChicaServiceImpl implements CajaChicaService {

	private static final double TOLERANCIA = 0.01;

	@EJB
	private CajaChicaDaoService cajaChicaDaoService;

	@EJB
	private MovimientoCajaChicaDaoService movimientoCajaChicaDaoService;

	@EJB
	private CierreCajaChicaDaoService cierreCajaChicaDaoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public CajaChica selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById CajaChica con id: " + id);
		return cajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_CHICA);
	}

	@Override
	public List<CajaChica> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll CajaChicaService");
		List<CajaChica> result = cajaChicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total CajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<CajaChica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria CajaChicaService");
		List<CajaChica> result = cajaChicaDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio CajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public CajaChica saveSingle(CajaChica caja) throws Throwable {
		System.out.println("saveSingle - CajaChica");
		if (caja.getCodigo() == null) {
			if (caja.getEstado() == null) {
				caja.setEstado(Long.valueOf(EstadoCajaChica.ACTIVA));
			}
			if (caja.getFechaRegistro() == null) {
				caja.setFechaRegistro(LocalDateTime.now());
			}
		} else {
			// ── Guarda de los campos internos en la edición ──────────────────────
			// El DAO genérico hace em.merge() con el objeto tal cual llegó del JSON: un campo que
			// el cliente no manda NO queda como estaba, se graba null (ver
			// docs/general/MERGE-DESNUDO-EN-ENTITYDAOIMPL.md). La pantalla de parametrización arma
			// el payload de edición desde cero y no incluye estado/fechaRegistro/usuario/custodio,
			// así que editar una caja la dejaba con CJCHESTD null y la hacía desaparecer de
			// /cjch/activas (gastos, reposición, cierre, semáforo de saldos). Pasó en producción
			// el 2026-09-07. motivoAnulacion se agrega a la misma guarda (ítem 2): es el mismo tipo
			// de campo interno — lo escriben /cjch/anular y /cjch/activar, no la pantalla de
			// parametrización — y quedaría igual de expuesto al mismo defecto si no se preserva acá.
			//
			// Se resuelve del lado del servidor, releyendo los campos, y no pidiéndole al
			// frontend que los mande: son estado interno, ningún cliente tiene por qué conocerlos,
			// y confiar en que los reenvíe es exactamente lo que falla en silencio. Se pisan
			// SIEMPRE con el valor de la fila, no solo cuando vienen null.
			try {
				Object[] previos = (Object[]) em.createQuery(
						"SELECT c.estado, c.fechaRegistro, c.usuario, c.custodio, c.motivoAnulacion "
								+ "FROM CajaChica c WHERE c.codigo = :id")
						.setParameter("id", caja.getCodigo())
						.getSingleResult();
				caja.setEstado((Long) previos[0]);
				caja.setFechaRegistro((LocalDateTime) previos[1]);
				caja.setUsuario((Long) previos[2]);
				caja.setCustodio((com.saa.model.scp.Usuario) previos[3]);
				caja.setMotivoAnulacion((String) previos[4]);
			} catch (jakarta.persistence.NoResultException e) {
				// Id inexistente: que siga y falle donde corresponde, no acá.
				System.out.println("⚠ saveSingle CajaChica: no existe el id " + caja.getCodigo()
						+ " para preservar los campos internos.");
			}
			// Red de seguridad: si la fila ya quedó dañada por este mismo defecto antes de que
			// corriera el script de recuperación, no propagar el null.
			if (caja.getEstado() == null) {
				caja.setEstado(Long.valueOf(EstadoCajaChica.ACTIVA));
			}
			if (caja.getFechaRegistro() == null) {
				caja.setFechaRegistro(LocalDateTime.now());
			}
		}
		return cajaChicaDaoService.save(caja, caja.getCodigo());
	}

	@Override
	public void save(List<CajaChica> lista) throws Throwable {
		for (CajaChica registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		CajaChica entidad = new CajaChica();
		for (Long registro : id) {
			cajaChicaDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Lógica de negocio
	// =====================================================================

	@Override
	public CajaChica registrar(CajaChica caja, Double saldoInicialMigrado, Long idUsuario) throws Throwable {

		System.out.println("=== registrar CajaChica | nombre=" + (caja != null ? caja.getNombre() : null)
				+ " | saldoInicialMigrado=" + saldoInicialMigrado + " ===");

		if (caja == null) {
			throw new IncomeException("Debe indicar los datos de la caja chica.");
		}
		if (caja.getEmpresa() == null || caja.getEmpresa().getCodigo() == null) {
			throw new IncomeException("Debe indicar la empresa contable.");
		}
		if (caja.getNombre() == null || caja.getNombre().trim().isEmpty()) {
			throw new IncomeException("Debe indicar el nombre de la caja chica.");
		}
		if (caja.getPlanCuenta() == null || caja.getPlanCuenta().getCodigo() == null) {
			throw new IncomeException("Debe indicar la cuenta contable de la caja chica.");
		}
		if (caja.getMontoFondo() == null || caja.getMontoFondo() <= 0) {
			throw new IncomeException("El monto del fondo debe ser mayor a cero.");
		}
		if (cajaChicaDaoService.existeNombreEnEmpresa(caja.getEmpresa().getCodigo(),
				caja.getNombre().trim(), caja.getCodigo())) {
			throw new IncomeException("Ya existe una caja chica llamada '" + caja.getNombre()
					+ "' en esta empresa. Elija otro nombre.");
		}

		// El REST arma empresa/planCuenta/custodio como instancias nuevas con
		// sólo el código (no tiene forma de cargar la entidad completa desde
		// un Map plano). Se resuelven aquí con em.find: un id inexistente da
		// un IncomeException legible en vez de reventar como ORA-02291 al
		// grabar la FK, y la respuesta JSON queda con las entidades completas
		// en lugar de objetos huecos.
		com.saa.model.scp.Empresa empresa = em.find(com.saa.model.scp.Empresa.class, caja.getEmpresa().getCodigo());
		if (empresa == null) {
			throw new IncomeException("No se encontró la empresa con ID: " + caja.getEmpresa().getCodigo());
		}
		caja.setEmpresa(empresa);

		com.saa.model.cnt.PlanCuenta planCuenta =
				em.find(com.saa.model.cnt.PlanCuenta.class, caja.getPlanCuenta().getCodigo());
		if (planCuenta == null) {
			throw new IncomeException("No se encontró la cuenta contable con ID: "
					+ caja.getPlanCuenta().getCodigo());
		}
		caja.setPlanCuenta(planCuenta);

		if (caja.getCustodio() != null && caja.getCustodio().getCodigo() != null) {
			com.saa.model.scp.Usuario custodio = em.find(com.saa.model.scp.Usuario.class,
					caja.getCustodio().getCodigo());
			if (custodio == null) {
				throw new IncomeException("No se encontró el usuario custodio con ID: "
						+ caja.getCustodio().getCodigo());
			}
			caja.setCustodio(custodio);
		}

		caja.setNombre(caja.getNombre().trim());
		if (caja.getPorcentajeAlerta() == null) {
			caja.setPorcentajeAlerta(20.0);
		}
		if (caja.getEstado() == null) {
			caja.setEstado(Long.valueOf(EstadoCajaChica.ACTIVA));
		}
		caja.setUsuario(idUsuario);
		caja = saveSingle(caja);
		em.flush();

		System.out.println("✓ CajaChica registrada: id=" + caja.getCodigo());

		if (saldoInicialMigrado != null && saldoInicialMigrado > 0) {
			MovimientoCajaChica movimiento = new MovimientoCajaChica();
			movimiento.setCajaChica(caja);
			movimiento.setTipo(Long.valueOf(TipoMovimientoCajaChica.APERTURA));
			movimiento.setFecha(java.time.LocalDate.now());
			movimiento.setValor(saldoInicialMigrado);
			movimiento.setDescripcion("SALDO INICIAL MIGRADO");
			movimiento.setObservacion("Saldo migrado desde cuenta bancaria legada de caja chica.");
			movimiento.setEstado(Long.valueOf(EstadoMovimientoCajaChica.ACTIVO));
			movimiento.setFechaRegistro(LocalDateTime.now());
			movimiento.setUsuario(idUsuario);
			// Sin asiento: el saldo ya está en la cuenta contable de la cuenta
			// bancaria legada (428/429) que se está retirando.
			movimientoCajaChicaDaoService.save(movimiento, null);
			em.flush();

			System.out.println("✓ Movimiento de apertura migrada creado para la caja " + caja.getCodigo()
					+ " | valor=" + saldoInicialMigrado);
		}

		return caja;
	}

	@Override
	public List<CajaChica> activas(Long idEmpresa) throws Throwable {
		System.out.println("=== activas | empresa=" + idEmpresa + " ===");
		return cajaChicaDaoService.selectByEmpresaEstado(idEmpresa, Long.valueOf(EstadoCajaChica.ACTIVA));
	}

	@Override
	public Map<String, Object> darDeBaja(Long idCaja, String motivo, Long idUsuario) throws Throwable {
		System.out.println("=== darDeBaja caja chica | caja=" + idCaja + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la baja.");
		}

		CajaChica caja = selectById(idCaja);

		if (caja.getEstado() != null && caja.getEstado().intValue() == EstadoCajaChica.INACTIVA) {
			throw new IncomeException("La caja chica '" + caja.getNombre() + "' ya está dada de baja.");
		}

		double saldoActual = ((Number) saldo(idCaja).get("saldo")).doubleValue();
		if (Math.abs(saldoActual) > TOLERANCIA) {
			throw new IncomeException("La caja chica '" + caja.getNombre() + "' tiene un saldo de $"
					+ String.format(java.util.Locale.US, "%.2f", saldoActual)
					+ ": regularícelo antes de darla de baja.");
		}

		rechazaSiTienePagosEnCurso(caja);

		CierreCajaChica borrador = cierreCajaChicaDaoService.selectBorrador(idCaja);
		if (borrador != null) {
			throw new IncomeException("Hay un cierre en preparación (BORRADOR N° " + borrador.getCodigo()
					+ ") que cubre del " + borrador.getFechaInicio() + " al " + borrador.getFechaFin()
					+ ": no se puede dar de baja la caja hasta confirmar o anular el cierre.");
		}

		// No usa saveSingle: acá se está grabando la entidad completa que ya se leyó de la
		// base (selectById), no un payload parcial del cliente — llamar a saveSingle
		// releería estado/motivoAnulacion desde la fila TODAVÍA no commiteada y pisaría
		// justo los dos campos que esta operación necesita cambiar.
		caja.setEstado(Long.valueOf(EstadoCajaChica.INACTIVA));
		caja.setMotivoAnulacion(motivo.trim());
		caja = cajaChicaDaoService.save(caja, caja.getCodigo());

		System.out.println("✓ Caja chica dada de baja: id=" + idCaja);

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idCaja", caja.getCodigo());
		resultado.put("nombre", caja.getNombre());
		resultado.put("estado", caja.getEstado());
		resultado.put("mensaje", "Caja chica dada de baja correctamente.");
		return resultado;
	}

	@Override
	public Map<String, Object> activar(Long idCaja, Long idUsuario) throws Throwable {
		System.out.println("=== activar caja chica | caja=" + idCaja + " ===");

		CajaChica caja = selectById(idCaja);
		if (caja.getEstado() == null || caja.getEstado().intValue() != EstadoCajaChica.INACTIVA) {
			throw new IncomeException("La caja chica '" + caja.getNombre() + "' no está dada de baja.");
		}

		caja.setEstado(Long.valueOf(EstadoCajaChica.ACTIVA));
		caja.setMotivoAnulacion(null);
		caja = cajaChicaDaoService.save(caja, caja.getCodigo());

		System.out.println("✓ Caja chica reactivada: id=" + idCaja);

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idCaja", caja.getCodigo());
		resultado.put("nombre", caja.getNombre());
		resultado.put("estado", caja.getEstado());
		resultado.put("mensaje", "Caja chica reactivada.");
		return resultado;
	}

	/**
	 * Rechaza la baja si hay un {@code PGS.PGTR} de esta caja (origen
	 * {@link OrigenPagoExterno#TSR_CAJA_CHICA}) en curso — {@code PGTRESTD} en
	 * (POR_APROBAR, REGISTRADO, EN_ARCHIVO). Trae sólo id y estado, nunca la entidad
	 * {@code PagoProgramado}: tiene trece {@code @ManyToOne} EAGER y ya causó un
	 * {@code ORA-04036} en producción (ver el javadoc de
	 * {@code MovimientoCajaChica.idPago}).
	 * @param caja       : Caja chica a validar
	 * @throws Throwable : IncomeException si hay un pago en curso
	 */
	private void rechazaSiTienePagosEnCurso(CajaChica caja) throws Throwable {
		@SuppressWarnings("unchecked")
		List<Long> idsMovimientos = em.createQuery(
				"select m.codigo from MovimientoCajaChica m where m.cajaChica.codigo = :idCaja")
				.setParameter("idCaja", caja.getCodigo())
				.getResultList();
		if (idsMovimientos.isEmpty()) {
			return;
		}

		@SuppressWarnings("unchecked")
		List<Object[]> pagosEnCurso = em.createQuery(
				"select p.id, p.estado from PagoProgramado p where p.origenExterno = :origen "
				+ "and p.idOrigen in :ids and p.estado in :estados")
				.setParameter("origen", OrigenPagoExterno.TSR_CAJA_CHICA)
				.setParameter("ids", idsMovimientos)
				.setParameter("estados", java.util.Arrays.asList(
						Long.valueOf(EstadoPagoProgramado.POR_APROBAR),
						Long.valueOf(EstadoPagoProgramado.REGISTRADO),
						Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO)))
				.getResultList();

		if (!pagosEnCurso.isEmpty()) {
			Object[] fila = pagosEnCurso.get(0);
			throw new IncomeException("La caja chica '" + caja.getNombre() + "' tiene el pago N° "
					+ fila[0] + " en curso (estado " + fila[1] + "): confírmelo, anúlelo o recháncelo "
					+ "antes de darla de baja.");
		}
	}

	@Override
	public Map<String, Object> saldo(Long idCaja) throws Throwable {
		System.out.println("=== saldo caja chica | id=" + idCaja + " ===");

		CajaChica caja = selectById(idCaja);

		double saldo = calcularSaldo(idCaja);
		double fondo = (caja.getMontoFondo() != null) ? caja.getMontoFondo() : 0.0;
		double porcentajeAlerta = (caja.getPorcentajeAlerta() != null) ? caja.getPorcentajeAlerta() : 20.0;

		double porcentaje = (fondo > 0) ? (saldo / fondo * 100.0) : 0.0;
		boolean alerta = fondo > 0 && saldo <= (fondo * porcentajeAlerta / 100.0);
		double montoSugeridoReposicion = Math.max(0.0, fondo - saldo);

		CierreCajaChica ultimoCierre = cierreCajaChicaDaoService.selectUltimoCerrado(idCaja);

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idCaja", idCaja);
		resultado.put("nombre", caja.getNombre());
		resultado.put("fondo", fondo);
		resultado.put("saldo", saldo);
		resultado.put("porcentaje", porcentaje);
		resultado.put("alerta", alerta);
		resultado.put("montoSugeridoReposicion", montoSugeridoReposicion);
		resultado.put("ultimoCierre", (ultimoCierre != null) ? ultimoCierre.getFechaFin() : null);
		return resultado;
	}

	@Override
	public List<Map<String, Object>> saldos(Long idEmpresa) throws Throwable {
		System.out.println("=== saldos caja chica | empresa=" + idEmpresa + " ===");
		List<CajaChica> cajas = activas(idEmpresa);
		List<Map<String, Object>> resultado = new ArrayList<>();
		for (CajaChica caja : cajas) {
			resultado.add(saldo(caja.getCodigo()));
		}
		return resultado;
	}

	/**
	 * Calcula el saldo actual de la caja: Σ(apertura + reposición + ajuste+) -
	 * Σ(gasto + ajuste-) de movimientos ACTIVOS. Nunca se guarda: se calcula
	 * cada vez.
	 * @param idCaja : Id de la caja chica
	 * @return       : Saldo actual
	 * @throws Throwable : Excepcion
	 */
	private double calcularSaldo(Long idCaja) throws Throwable {
		List<Object[]> sumas = movimientoCajaChicaDaoService.selectSumasPorTipo(idCaja, null, null);
		double saldo = 0.0;
		for (Object[] fila : sumas) {
			int tipo = ((Number) fila[0]).intValue();
			double suma = ((Number) fila[1]).doubleValue();
			switch (tipo) {
				case TipoMovimientoCajaChica.APERTURA:
				case TipoMovimientoCajaChica.REPOSICION:
				case TipoMovimientoCajaChica.AJUSTE_POSITIVO:
					saldo += suma;
					break;
				case TipoMovimientoCajaChica.GASTO:
				case TipoMovimientoCajaChica.AJUSTE_NEGATIVO:
					saldo -= suma;
					break;
				default:
					break;
			}
		}
		return saldo;
	}

}
