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
import com.saa.ejb.tsr.dao.CierreCajaChicaDaoService;
import com.saa.ejb.tsr.dao.MovimientoCajaChicaDaoService;
import com.saa.ejb.tsr.service.CierreCajaChicaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CajaChica;
import com.saa.model.tsr.CierreCajaChica;
import com.saa.model.tsr.MovimientoCajaChica;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.EstadoCierreCajaChica;
import com.saa.rubros.EstadoMovimientoCajaChica;
import com.saa.rubros.TipoMovimientoCajaChica;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementación de CierreCajaChicaService.</p>
 */
@Stateless
public class CierreCajaChicaServiceImpl implements CierreCajaChicaService {

	private static final double TOLERANCIA = 0.01;

	@EJB
	private CierreCajaChicaDaoService cierreCajaChicaDaoService;

	@EJB
	private MovimientoCajaChicaDaoService movimientoCajaChicaDaoService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private AsientoService asientoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public CierreCajaChica selectById(Long id) throws Throwable {
		return cierreCajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.CIERRE_CAJA_CHICA);
	}

	@Override
	public List<CierreCajaChica> selectAll() throws Throwable {
		List<CierreCajaChica> result =
				cierreCajaChicaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total CierreCajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<CierreCajaChica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<CierreCajaChica> result = cierreCajaChicaDaoService.selectByCriteria(datos,
				NombreEntidadesTesoreria.CIERRE_CAJA_CHICA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio CierreCajaChica no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public CierreCajaChica saveSingle(CierreCajaChica cierre) throws Throwable {
		if (cierre.getCodigo() == null) {
			if (cierre.getEstado() == null) {
				cierre.setEstado(Long.valueOf(EstadoCierreCajaChica.BORRADOR));
			}
			if (cierre.getFechaRegistro() == null) {
				cierre.setFechaRegistro(LocalDateTime.now());
			}
		}
		return cierreCajaChicaDaoService.save(cierre, cierre.getCodigo());
	}

	@Override
	public void save(List<CierreCajaChica> lista) throws Throwable {
		for (CierreCajaChica registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		CierreCajaChica entidad = new CierreCajaChica();
		for (Long registro : id) {
			cierreCajaChicaDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Lógica de negocio
	// =====================================================================

	@Override
	public Map<String, Object> prepararCierre(Long idCaja, LocalDate fecha, Long idUsuario) throws Throwable {

		System.out.println("=== prepararCierre caja chica | caja=" + idCaja + " | fecha=" + fecha + " ===");

		if (fecha == null) {
			throw new IncomeException("Debe indicar la fecha de corte del cierre.");
		}
		if (cierreCajaChicaDaoService.existeBorrador(idCaja)) {
			throw new IncomeException("La caja ya tiene un cierre en BORRADOR pendiente de confirmar.");
		}

		CajaChica caja = em.find(CajaChica.class, idCaja);
		if (caja == null) {
			throw new IncomeException("No se encontró la caja chica con ID: " + idCaja);
		}

		CierreCajaChica ultimoCerrado = cierreCajaChicaDaoService.selectUltimoCerrado(idCaja);
		LocalDate fechaInicio;
		if (ultimoCerrado != null && ultimoCerrado.getFechaFin() != null) {
			fechaInicio = ultimoCerrado.getFechaFin().plusDays(1);
		} else {
			LocalDate primerMovimiento = movimientoCajaChicaDaoService.selectFechaPrimerMovimiento(idCaja);
			fechaInicio = (primerMovimiento != null) ? primerMovimiento : fecha;
		}
		if (fecha.isBefore(fechaInicio)) {
			throw new IncomeException("La fecha del cierre (" + fecha + ") no puede ser anterior al "
					+ "inicio del periodo (" + fechaInicio + ").");
		}

		double saldoInicial = calcularSaldoHasta(idCaja, fechaInicio.minusDays(1));
		double[] totales = calcularTotalesPeriodo(idCaja, fechaInicio, fecha);
		double totalGastos = totales[0];
		double totalReposiciones = totales[1];
		double totalAjustes = totales[2];
		double saldoLibros = calcularSaldoHasta(idCaja, fecha);

		CierreCajaChica cierre = new CierreCajaChica();
		cierre.setCajaChica(caja);
		cierre.setFecha(fecha);
		cierre.setFechaInicio(fechaInicio);
		cierre.setFechaFin(fecha);
		cierre.setSaldoInicial(saldoInicial);
		cierre.setTotalGastos(totalGastos);
		cierre.setTotalReposiciones(totalReposiciones);
		cierre.setTotalAjustes(totalAjustes);
		cierre.setSaldoLibros(saldoLibros);
		cierre.setEstado(Long.valueOf(EstadoCierreCajaChica.BORRADOR));
		cierre.setUsuario(idUsuario);
		cierre = saveSingle(cierre);
		em.flush();

		List<MovimientoCajaChica> movimientos =
				movimientoCajaChicaDaoService.selectActivosEnRango(idCaja, fechaInicio, fecha);

		System.out.println("✓ Cierre BORRADOR preparado: id=" + cierre.getCodigo()
				+ " | periodo=" + fechaInicio + " a " + fecha + " | saldoLibros=" + saldoLibros);

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("cierre", cierre);
		resultado.put("movimientos", movimientos);
		return resultado;
	}

	@Override
	public CierreCajaChica confirmarCierre(Long idCierre, Double saldoFisico, String observacion,
			Long idPlanCuentaDiferencia, Long idUsuario) throws Throwable {

		System.out.println("=== confirmarCierre caja chica | cierre=" + idCierre
				+ " | saldoFisico=" + saldoFisico + " ===");

		if (saldoFisico == null) {
			throw new IncomeException("Debe indicar el saldo físico contado.");
		}
		CierreCajaChica cierre = selectById(idCierre);
		if (cierre.getEstado() == null || cierre.getEstado().intValue() != EstadoCierreCajaChica.BORRADOR) {
			throw new IncomeException("El cierre " + idCierre + " no está en BORRADOR.");
		}

		CajaChica caja = cierre.getCajaChica();

		// Recalcula en vez de confiar en lo que congeló prepararCierre: entre
		// preparar y confirmar se pueden registrar o anular gastos dentro del
		// periodo (el gasto sólo se bloquea contra un cierre CERRADO, no
		// contra este BORRADOR — ver rechazaSiEnBorrador en
		// MovimientoCajaChicaServiceImpl), y esos movimientos igual se marcan
		// con este cierre más abajo. Sin recalcular, la diferencia contra el
		// saldo físico —y el ajuste que de ahí sale— quedarían mal.
		double saldoLibros = calcularSaldoHasta(caja.getCodigo(), cierre.getFechaFin());
		double[] totales = calcularTotalesPeriodo(caja.getCodigo(), cierre.getFechaInicio(), cierre.getFechaFin());
		cierre.setTotalGastos(totales[0]);
		cierre.setTotalReposiciones(totales[1]);
		cierre.setTotalAjustes(totales[2]);
		cierre.setSaldoLibros(saldoLibros);

		double diferencia = redondea(saldoFisico - saldoLibros);

		if (Math.abs(diferencia) > TOLERANCIA) {
			if (idPlanCuentaDiferencia == null) {
				throw new IncomeException("Hay una diferencia de $"
						+ String.format(java.util.Locale.US, "%.2f", diferencia)
						+ " entre el saldo físico y el saldo según libros: debe indicar la cuenta "
						+ "de faltantes/sobrantes de caja para generar el ajuste.");
			}
			boolean sobrante = diferencia > 0;
			double valorAjuste = Math.abs(diferencia);

			MovimientoCajaChica ajuste = new MovimientoCajaChica();
			ajuste.setCajaChica(caja);
			ajuste.setTipo(Long.valueOf(sobrante
					? TipoMovimientoCajaChica.AJUSTE_POSITIVO : TipoMovimientoCajaChica.AJUSTE_NEGATIVO));
			ajuste.setFecha(cierre.getFechaFin());
			ajuste.setValor(valorAjuste);
			ajuste.setDescripcion("AJUSTE POR ARQUEO " + cierre.getFechaFin());
			ajuste.setEstado(Long.valueOf(EstadoMovimientoCajaChica.ACTIVO));
			ajuste.setUsuario(idUsuario);
			ajuste.setFechaRegistro(LocalDateTime.now());
			ajuste = movimientoCajaChicaDaoService.save(ajuste, null);
			em.flush();

			String observacionAjuste = "Ajuste por arqueo caja chica " + caja.getNombre()
					+ " | Cierre al " + cierre.getFechaFin()
					+ " | " + (sobrante ? "Sobrante" : "Faltante") + ": $"
					+ String.format(java.util.Locale.US, "%.2f", valorAjuste);

			Asiento asientoAjuste = asientoContableService.generarAsientoAjusteCajaChica(
					caja.getPlanCuenta().getCodigo(), idPlanCuentaDiferencia, valorAjuste, sobrante,
					caja.getEmpresa().getCodigo(), cierre.getFechaFin(), observacionAjuste,
					usuarioNombre(idUsuario));

			ajuste.setAsiento(asientoAjuste);
			movimientoCajaChicaDaoService.save(ajuste, ajuste.getCodigo());

			cierre.setAsiento(asientoAjuste);
			System.out.println("✓ Ajuste por arqueo generado: movimiento=" + ajuste.getCodigo()
					+ " | asiento=" + asientoAjuste.getNumeroAlterno());
		}

		cierre.setSaldoFisico(saldoFisico);
		cierre.setDiferencia(diferencia);
		cierre.setObservacion(observacion);
		cierre.setEstado(Long.valueOf(EstadoCierreCajaChica.CERRADO));
		cierre = cierreCajaChicaDaoService.save(cierre, cierre.getCodigo());
		em.flush();

		// Marca con el cierre todos los movimientos activos del periodo,
		// incluido el ajuste recién creado (su fecha cae dentro del periodo).
		List<MovimientoCajaChica> movimientos = movimientoCajaChicaDaoService
				.selectActivosEnRango(caja.getCodigo(), cierre.getFechaInicio(), cierre.getFechaFin());
		for (MovimientoCajaChica movimiento : movimientos) {
			movimiento.setCierre(cierre);
			movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());
		}

		System.out.println("✓ Cierre " + idCierre + " CERRADO | diferencia=" + diferencia
				+ " | movimientos marcados=" + movimientos.size());
		return cierre;
	}

	@Override
	public void anularCierre(Long idCierre, String motivo, Long idUsuario) throws Throwable {

		System.out.println("=== anularCierre caja chica | cierre=" + idCierre + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}
		CierreCajaChica cierre = selectById(idCierre);
		if (cierre.getEstado() == null || cierre.getEstado().intValue() != EstadoCierreCajaChica.CERRADO) {
			throw new IncomeException("El cierre " + idCierre + " no está CERRADO.");
		}

		CierreCajaChica ultimoCerrado = cierreCajaChicaDaoService.selectUltimoCerrado(cierre.getCajaChica().getCodigo());
		if (ultimoCerrado == null || !ultimoCerrado.getCodigo().equals(idCierre)) {
			throw new IncomeException("Sólo se puede anular el último cierre CERRADO de la caja.");
		}

		List<MovimientoCajaChica> movimientos = movimientoCajaChicaDaoService.selectByCierre(idCierre);
		for (MovimientoCajaChica movimiento : movimientos) {
			int tipo = (movimiento.getTipo() != null) ? movimiento.getTipo().intValue() : 0;
			if (tipo == TipoMovimientoCajaChica.AJUSTE_POSITIVO || tipo == TipoMovimientoCajaChica.AJUSTE_NEGATIVO) {
				Long idAsiento = (movimiento.getAsiento() != null) ? movimiento.getAsiento().getCodigo() : null;
				if (idAsiento != null) {
					try {
						asientoService.anulaAsiento(idAsiento);
					} catch (Throwable e) {
						System.err.println("⚠ No se pudo anular el asiento " + idAsiento + ": " + e.getMessage());
					}
				}
				movimiento.setEstado(Long.valueOf(EstadoMovimientoCajaChica.ANULADO));
				movimiento.setMotivoAnulacion("ANULACIÓN DE CIERRE: " + motivo.trim());
				movimiento.setAsiento(null);
				movimiento.setCierre(null);
				movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());
			} else {
				movimiento.setCierre(null);
				movimientoCajaChicaDaoService.save(movimiento, movimiento.getCodigo());
			}
		}

		cierre.setEstado(Long.valueOf(EstadoCierreCajaChica.ANULADO));
		cierre.setObservacion(nvl(cierre.getObservacion(), "") + " | ANULADO: " + motivo.trim());
		cierreCajaChicaDaoService.save(cierre, cierre.getCodigo());

		System.out.println("✓ Cierre " + idCierre + " anulado. Motivo: " + motivo);
	}

	@Override
	public List<CierreCajaChica> listar(Long idCaja) throws Throwable {
		return cierreCajaChicaDaoService.selectByCaja(idCaja);
	}

	@Override
	public List<MovimientoCajaChica> movimientos(Long idCierre) throws Throwable {
		return movimientoCajaChicaDaoService.selectByCierre(idCierre);
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	/**
	 * Saldo de la caja hasta una fecha límite (inclusive), con el mismo
	 * criterio que {@code CajaChicaServiceImpl.calcularSaldo}.
	 */
	private double calcularSaldoHasta(Long idCaja, LocalDate fechaLimite) throws Throwable {
		List<Object[]> sumas = movimientoCajaChicaDaoService.selectSumasPorTipo(idCaja, null, fechaLimite);
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

	/**
	 * Totales del periodo [desde, hasta] para las columnas informativas del
	 * cierre: {@code [totalGastos, totalReposiciones, totalAjustes]}.
	 * totalReposiciones incluye apertura + reposición; totalAjustes es
	 * positivos menos negativos.
	 */
	private double[] calcularTotalesPeriodo(Long idCaja, LocalDate desde, LocalDate hasta) throws Throwable {
		List<Object[]> sumas = movimientoCajaChicaDaoService.selectSumasPorTipo(idCaja, desde, hasta);
		double totalGastos = 0.0;
		double totalReposiciones = 0.0;
		double totalAjustes = 0.0;
		for (Object[] fila : sumas) {
			int tipo = ((Number) fila[0]).intValue();
			double suma = ((Number) fila[1]).doubleValue();
			switch (tipo) {
				case TipoMovimientoCajaChica.GASTO:
					totalGastos += suma;
					break;
				case TipoMovimientoCajaChica.APERTURA:
				case TipoMovimientoCajaChica.REPOSICION:
					totalReposiciones += suma;
					break;
				case TipoMovimientoCajaChica.AJUSTE_POSITIVO:
					totalAjustes += suma;
					break;
				case TipoMovimientoCajaChica.AJUSTE_NEGATIVO:
					totalAjustes -= suma;
					break;
				default:
					break;
			}
		}
		return new double[]{totalGastos, totalReposiciones, totalAjustes};
	}

	private double redondea(double valor) {
		return Math.round(valor * 100.0) / 100.0;
	}

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
