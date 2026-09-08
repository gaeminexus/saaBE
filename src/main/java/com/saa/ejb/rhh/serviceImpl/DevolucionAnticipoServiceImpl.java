package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AnticipoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.CuotaDescuentoDaoService;
import com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService;
import com.saa.ejb.rhh.dao.DevolucionAnticipoDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.DevolucionAnticipoService;
import com.saa.ejb.tsr.service.IngresoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.rhh.AnticipoEmpleado;
import com.saa.model.rhh.CuotaDescuento;
import com.saa.model.rhh.DescuentoRecurrente;
import com.saa.model.rhh.DevolucionAnticipo;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.rubros.EstadoAnticipoEmpleado;
import com.saa.rubros.EstadoDevolucionAnticipo;
import com.saa.rubros.RhhEstadoCuotaDescuento;
import com.saa.rubros.RhhEstadoDescuentoRecurrente;
import com.saa.rubros.RhhEstadoPeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de DevolucionAnticipoService.</p>
 *
 * <p>La contabilidad la hace {@code tsr} ({@link IngresoService}): este
 * servicio no genera ningun asiento a mano. Agregar esa dependencia no abre
 * una direccion nueva -- {@code rhh} ya depende de {@code cxp} en el mismo
 * archivo de anticipos (ver
 * docs/logica-negocio/rhh/API-DEVOLUCION-ANTICIPO.md #7).</p>
 */
@Stateless
public class DevolucionAnticipoServiceImpl implements DevolucionAnticipoService {

	private static final double TOLERANCIA = 0.01;

	@EJB
	private DevolucionAnticipoDaoService devolucionAnticipoDaoService;

	@EJB
	private AnticipoEmpleadoDaoService anticipoEmpleadoDaoService;

	@EJB
	private DescuentoRecurrenteDaoService descuentoRecurrenteDaoService;

	@EJB
	private CuotaDescuentoDaoService cuotaDescuentoDaoService;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private IngresoService ingresoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public DevolucionAnticipo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById DevolucionAnticipo con id: " + id);
		return devolucionAnticipoDaoService.selectById(id, NombreEntidadesRhh.DEVOLUCION_ANTICIPO);
	}

	@Override
	public List<DevolucionAnticipo> selectAll() throws Throwable {
		List<DevolucionAnticipo> result = devolucionAnticipoDaoService.selectAll(NombreEntidadesRhh.DEVOLUCION_ANTICIPO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total DevolucionAnticipo no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<DevolucionAnticipo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<DevolucionAnticipo> result =
				devolucionAnticipoDaoService.selectByCriteria(datos, NombreEntidadesRhh.DEVOLUCION_ANTICIPO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio DevolucionAnticipo no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DevolucionAnticipo saveSingle(DevolucionAnticipo devolucion) throws Throwable {
		System.out.println("saveSingle - DevolucionAnticipo");
		if (devolucion.getCodigo() == null) {
			if (devolucion.getEstado() == null) {
				devolucion.setEstado(Long.valueOf(EstadoDevolucionAnticipo.VIGENTE));
			}
			if (devolucion.getFechaRegistro() == null) {
				devolucion.setFechaRegistro(LocalDateTime.now());
			}
		}
		// Sin endpoint de edición estándar (no hay PUT en /rest/dvan): esta entidad es un
		// registro de un hecho contable ya reversado por anular(), no algo que una pantalla
		// de edición libre deba poder tocar. No hace falta la guarda de payload parcial de
		// CajaChica/PlanillaIess porque no hay ninguna pantalla que llegue a este método.
		return devolucionAnticipoDaoService.save(devolucion, devolucion.getCodigo());
	}

	@Override
	public void save(List<DevolucionAnticipo> lista) throws Throwable {
		for (DevolucionAnticipo registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		DevolucionAnticipo entidad = new DevolucionAnticipo();
		for (Long registro : id) {
			devolucionAnticipoDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Lógica de negocio
	// =====================================================================

	@Override
	public List<DevolucionAnticipo> porAnticipo(Long idAnticipo) throws Throwable {
		System.out.println("=== porAnticipo devolucion | anticipo=" + idAnticipo + " ===");
		return devolucionAnticipoDaoService.selectByAnticipo(idAnticipo);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> registrar(Long idAnticipo, LocalDate fecha, Double valor, Long idCuentaBancaria,
			String referencia, String observacion, Long idUsuario) throws Throwable {

		System.out.println("=== registrar devolucion de anticipo | anticipo=" + idAnticipo
				+ " | valor=" + valor + " ===");

		// 1. El anticipo existe (la excepción de selectById sube tal cual).
		AnticipoEmpleado anticipo = anticipoEmpleadoDaoService.selectById(idAnticipo, NombreEntidadesRhh.ANTICIPO_EMPLEADO);

		// 2. Estado PAGADO o EN_DESCUENTO.
		int estadoAnticipo = (anticipo.getEstado() != null) ? anticipo.getEstado().intValue() : 0;
		if (estadoAnticipo != EstadoAnticipoEmpleado.PAGADO && estadoAnticipo != EstadoAnticipoEmpleado.EN_DESCUENTO) {
			throw new IncomeException("El anticipo " + idAnticipo + " está en estado " + estadoAnticipo
					+ ": sólo se puede devolver uno ya pagado.");
		}

		// 3. valor > 0.
		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor devuelto debe ser mayor a cero.");
		}

		// 4. valor no supera el saldo pendiente.
		double saldoAnterior = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
		if (valor > saldoAnterior + TOLERANCIA) {
			throw new IncomeException("Devuelve $" + fmt(valor) + " y el anticipo sólo tiene $"
					+ fmt(saldoAnterior) + " pendiente.");
		}

		// 6. Fecha presente.
		if (fecha == null) {
			throw new IncomeException("Debe indicar la fecha del depósito.");
		}

		Empleado empleado = anticipo.getEmpleado();
		Long idEmpresa = (empleado != null && empleado.getEmpresa() != null)
				? empleado.getEmpresa().getCodigo() : null;
		if (idEmpresa == null) {
			throw new IncomeException("El empleado del anticipo " + idAnticipo
					+ " no tiene empresa asignada: sin ella no se puede registrar la devolución.");
		}

		Long idProductoCobro = buscaProductoCobro("RHH-DVAN", idEmpresa);

		// 5. Cuenta bancaria: se delega en procesarIngreso, que valida su existencia y
		// reutiliza su propio mensaje — no se duplica la validación acá.
		//
		// Es también donde se contabiliza: DEBE banco / HABER la cuenta del grupo del
		// producto RHH-DVAN (reverso exacto de cómo salió la plata: la entrega debitó
		// CUENTAS_POR_COBRAR_EMPLEADOS, la devolución la acredita), y se emite el
		// movimiento bancario para que el depósito aparezca en la conciliación.
		Map<String, Object> resultadoIngreso = ingresoService.procesarIngreso(idEmpresa, null, idProductoCobro,
				"Devolución anticipo N° " + idAnticipo + " - " + nombreCompleto(empleado), valor,
				fecha.toString(), idCuentaBancaria, referencia, observacion, idUsuario);

		Long idIngreso = (Long) resultadoIngreso.get("ingreso");
		if (idIngreso == null) {
			throw new IncomeException("El circuito de tesorería no devolvió el ingreso generado para la "
					+ "devolución del anticipo " + idAnticipo + ".");
		}

		// Sólo los escalares del asiento: Ingreso no tiene el mismo grafo EAGER que
		// PagoProgramado, pero se mantiene la misma disciplina del resto del proyecto.
		Long idAsiento = null;
		String numeroAsiento = (String) resultadoIngreso.get("asiento");
		try {
			idAsiento = (Long) em.createQuery("select i.asiento.codigo from Ingreso i where i.id = :id")
					.setParameter("id", idIngreso)
					.getSingleResult();
		} catch (jakarta.persistence.NoResultException e) {
			// No debería pasar: procesarIngreso siempre contabiliza. Se deja idAsiento null
			// en vez de fallar acá — el ingreso y el efecto sobre las cuotas ya son reales.
		}

		// Efecto sobre las cuotas PENDIENTE/PARCIAL, más próximas por vencer primero.
		DescuentoRecurrente descuento = anticipo.getDescuentoRecurrente();
		List<Map<String, Object>> cuotasCanceladasDto = new ArrayList<>();
		Map<String, Object> cuotaAjustadaDto = null;
		List<Long> idsCuotasCanceladas = new ArrayList<>();
		Long idCuotaAjustada = null;
		Double valorOriginalCuotaAjustada = null;
		Set<String> avisosPeriodo = new LinkedHashSet<>();

		if (descuento != null) {
			double restante = valor.doubleValue();
			List<CuotaDescuento> cuotas = cuotaDescuentoDaoService.selectPendientesPorDescuento(descuento.getCodigo());
			for (CuotaDescuento cuota : cuotas) {
				if (restante <= TOLERANCIA) {
					break;
				}
				double totalCuota = (cuota.getTotal() != null) ? cuota.getTotal() : 0.0;
				if (restante + TOLERANCIA >= totalCuota) {
					// Cubre la cuota entera: ANULADA (no DESCONTADA, y no PARCIAL — PARCIAL
					// significa "se descontó una parte y el resto se reintenta", que es otra cosa).
					cuota.setEstado(Long.valueOf(RhhEstadoCuotaDescuento.ANULADA));
					cuota = cuotaDescuentoDaoService.save(cuota, cuota.getCodigo());
					restante -= totalCuota;
					idsCuotasCanceladas.add(cuota.getCodigo());

					Map<String, Object> item = new HashMap<>();
					item.put("numero", cuota.getNumeroCuota());
					item.put("vencimiento", cuota.getFechaVencimiento());
					item.put("valor", totalCuota);
					cuotasCanceladasDto.add(item);
				} else {
					// Resto menor que la cuota siguiente: baja de valor y SIGUE PENDIENTE.
					double nuevoValor = totalCuota - restante;
					valorOriginalCuotaAjustada = Double.valueOf(totalCuota);
					cuota.setTotal(Double.valueOf(nuevoValor));
					cuota = cuotaDescuentoDaoService.save(cuota, cuota.getCodigo());
					idCuotaAjustada = cuota.getCodigo();

					cuotaAjustadaDto = new HashMap<>();
					cuotaAjustadaDto.put("numero", cuota.getNumeroCuota());
					cuotaAjustadaDto.put("vencimiento", cuota.getFechaVencimiento());
					cuotaAjustadaDto.put("valorNuevo", nuevoValor);
					restante = 0.0;
				}
				String aviso = avisoSiPeriodoCalculado(cuota.getFechaVencimiento(), idEmpresa);
				if (aviso != null) {
					avisosPeriodo.add(aviso);
				}
			}

			double saldoDescuento = (descuento.getSaldo() != null) ? descuento.getSaldo() : 0.0;
			descuento.setSaldo(Double.valueOf(Math.max(0.0, saldoDescuento - valor.doubleValue())));
			descuento = descuentoRecurrenteDaoService.save(descuento, descuento.getCodigo());
		}

		// Saldo del anticipo.
		double saldoNuevo = saldoAnterior - valor.doubleValue();
		if (Math.abs(saldoNuevo) < TOLERANCIA) {
			saldoNuevo = 0.0;
		}
		anticipo.setSaldo(Double.valueOf(saldoNuevo));
		if (saldoNuevo <= TOLERANCIA) {
			anticipo.setEstado(Long.valueOf(EstadoAnticipoEmpleado.CANCELADO));
			if (descuento != null) {
				descuento.setEstado(Long.valueOf(RhhEstadoDescuentoRecurrente.CANCELADO));
				descuento = descuentoRecurrenteDaoService.save(descuento, descuento.getCodigo());
			}
		}
		anticipo = anticipoEmpleadoDaoService.save(anticipo, anticipo.getCodigo());

		// Graba la devolución con el resultado completo, para poder deshacerla exacto.
		DevolucionAnticipo devolucion = new DevolucionAnticipo();
		devolucion.setAnticipo(anticipo);
		devolucion.setFecha(fecha);
		devolucion.setValor(valor);
		devolucion.setCuentaBancaria(em.getReference(CuentaBancaria.class, idCuentaBancaria));
		devolucion.setReferencia(referencia);
		devolucion.setObservacion(observacion);
		devolucion.setIdIngreso(idIngreso);
		devolucion.setIdsCuotasCanceladas(idsCuotasCanceladas.isEmpty() ? null : join(idsCuotasCanceladas));
		devolucion.setIdCuotaAjustada(idCuotaAjustada);
		devolucion.setValorOriginalCuotaAjustada(valorOriginalCuotaAjustada);
		devolucion.setAsiento(idAsiento != null ? em.getReference(Asiento.class, idAsiento) : null);
		devolucion.setEstado(Long.valueOf(EstadoDevolucionAnticipo.VIGENTE));
		devolucion.setFechaRegistro(LocalDateTime.now());
		devolucion.setUsuario(idUsuario);
		devolucion = devolucionAnticipoDaoService.save(devolucion, null);

		System.out.println("✓ Devolución de anticipo registrada: id=" + devolucion.getCodigo()
				+ " | anticipo=" + idAnticipo + " | cuotas canceladas=" + cuotasCanceladasDto.size());

		String mensaje;
		if (!cuotasCanceladasDto.isEmpty()) {
			double sumaCanceladas = 0.0;
			for (Map<String, Object> item : cuotasCanceladasDto) {
				sumaCanceladas += ((Number) item.get("valor")).doubleValue();
			}
			mensaje = "Devolución registrada. Se cancelaron " + cuotasCanceladasDto.size() + " cuota(s) por $"
					+ fmt(sumaCanceladas) + ".";
			if (cuotaAjustadaDto != null) {
				mensaje += " Además se redujo el valor de una cuota.";
			}
		} else if (cuotaAjustadaDto != null) {
			mensaje = "Devolución registrada. Se redujo el valor de la cuota N° "
					+ cuotaAjustadaDto.get("numero") + ".";
		} else {
			mensaje = "Devolución registrada. El anticipo no tenía cuotas pendientes que descontar.";
		}

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idDevolucion", devolucion.getCodigo());
		resultado.put("idAnticipo", idAnticipo);
		resultado.put("valor", valor);
		resultado.put("saldoAnterior", saldoAnterior);
		resultado.put("saldoNuevo", saldoNuevo);
		resultado.put("estadoAnticipo", anticipo.getEstado());
		resultado.put("cuotasCanceladas", cuotasCanceladasDto);
		resultado.put("cuotaAjustada", cuotaAjustadaDto);
		resultado.put("idIngreso", idIngreso);
		resultado.put("idAsiento", idAsiento);
		resultado.put("numeroAsiento", numeroAsiento);
		resultado.put("avisoPeriodoCalculado", avisosPeriodo.isEmpty() ? null : String.join(" ", avisosPeriodo));
		resultado.put("mensaje", mensaje);
		return resultado;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> anular(Long idDevolucion, String motivo, Long idUsuario) throws Throwable {

		System.out.println("=== anular devolucion de anticipo | id=" + idDevolucion + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}

		DevolucionAnticipo devolucion = devolucionAnticipoDaoService.selectById(idDevolucion,
				NombreEntidadesRhh.DEVOLUCION_ANTICIPO);
		if (devolucion.getEstado() != null
				&& devolucion.getEstado().intValue() == EstadoDevolucionAnticipo.ANULADA) {
			throw new IncomeException("La devolución " + idDevolucion + " ya está anulada.");
		}

		// 1. Anula el ingreso: reversa asiento y movimiento bancario.
		ingresoService.anularIngreso(devolucion.getIdIngreso(), motivo.trim(), idUsuario);

		// 2. Devuelve a PENDIENTE las cuotas que había cancelado, y restaura el valor de
		// la que hubiera ajustado — salvo que ya no siga en un estado tocable (ver abajo).
		StringBuilder avisoCuotasIntocadas = new StringBuilder();
		String idsCanceladas = devolucion.getIdsCuotasCanceladas();
		if (idsCanceladas != null && !idsCanceladas.trim().isEmpty()) {
			for (String idTexto : idsCanceladas.split(",")) {
				Long idCuota = Long.valueOf(idTexto.trim());
				CuotaDescuento cuota = cuotaDescuentoDaoService.selectById(idCuota, NombreEntidadesRhh.CUOTA_DESCUENTO);
				cuota.setEstado(Long.valueOf(RhhEstadoCuotaDescuento.PENDIENTE));
				cuotaDescuentoDaoService.save(cuota, cuota.getCodigo());
			}
		}
		if (devolucion.getIdCuotaAjustada() != null) {
			CuotaDescuento cuota = cuotaDescuentoDaoService.selectById(devolucion.getIdCuotaAjustada(),
					NombreEntidadesRhh.CUOTA_DESCUENTO);
			// La cuota ajustada siguió PENDIENTE, así que en principio nadie más la tocó —
			// salvo que la nómina ya la haya descontado con el valor reducido mientras tanto.
			// Las cuotas DESCONTADA no se tocan (mismo criterio que registrar): se avisa en
			// vez de pisar un descuento que ya ocurrió.
			if (cuota.getEstado() != null && cuota.getEstado().intValue() == RhhEstadoCuotaDescuento.DESCONTADA) {
				avisoCuotasIntocadas.append("La cuota N° ").append(cuota.getNumeroCuota())
						.append(" ya se descontó con el valor reducido: no se restauró su valor original ($")
						.append(fmt(devolucion.getValorOriginalCuotaAjustada())).append("); revísela a mano. ");
			} else {
				cuota.setTotal(devolucion.getValorOriginalCuotaAjustada());
				cuotaDescuentoDaoService.save(cuota, cuota.getCodigo());
			}
		}

		// 3. Repone saldos del anticipo y del descuento; si el anticipo había quedado
		// CANCELADO, vuelve a EN_DESCUENTO y reactiva el descuento.
		AnticipoEmpleado anticipo = anticipoEmpleadoDaoService.selectById(devolucion.getAnticipo().getCodigo(),
				NombreEntidadesRhh.ANTICIPO_EMPLEADO);
		boolean reactivar = anticipo.getEstado() != null
				&& anticipo.getEstado().intValue() == EstadoAnticipoEmpleado.CANCELADO;

		double saldoAnticipo = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
		anticipo.setSaldo(Double.valueOf(saldoAnticipo + devolucion.getValor().doubleValue()));
		if (reactivar) {
			anticipo.setEstado(Long.valueOf(EstadoAnticipoEmpleado.EN_DESCUENTO));
		}
		anticipo = anticipoEmpleadoDaoService.save(anticipo, anticipo.getCodigo());

		DescuentoRecurrente descuento = anticipo.getDescuentoRecurrente();
		if (descuento != null) {
			double saldoDescuento = (descuento.getSaldo() != null) ? descuento.getSaldo() : 0.0;
			descuento.setSaldo(Double.valueOf(saldoDescuento + devolucion.getValor().doubleValue()));
			if (reactivar) {
				descuento.setEstado(Long.valueOf(RhhEstadoDescuentoRecurrente.VIGENTE));
			}
			descuentoRecurrenteDaoService.save(descuento, descuento.getCodigo());
		}

		// 4. Marca la devolución ANULADA.
		devolucion.setEstado(Long.valueOf(EstadoDevolucionAnticipo.ANULADA));
		devolucion.setMotivoAnulacion(motivo.trim());
		devolucion = devolucionAnticipoDaoService.save(devolucion, devolucion.getCodigo());

		System.out.println("✓ Devolución de anticipo " + idDevolucion + " anulada.");

		String mensaje = "Devolución anulada." + (avisoCuotasIntocadas.length() > 0
				? " " + avisoCuotasIntocadas.toString().trim() : "");

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idDevolucion", devolucion.getCodigo());
		resultado.put("estado", devolucion.getEstado());
		resultado.put("mensaje", mensaje);
		return resultado;
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	private Long buscaProductoCobro(String codigo, Long idEmpresa) throws Throwable {
		try {
			return (Long) em.createQuery("select p.id from ProductoCobro p where p.codigo = :codigo "
					+ "and p.empresa.codigo = :idEmpresa")
					.setParameter("codigo", codigo)
					.setParameter("idEmpresa", idEmpresa)
					.getSingleResult();
		} catch (jakarta.persistence.NoResultException e) {
			throw new IncomeException("No existe el producto de cobro '" + codigo + "' para la empresa "
					+ idEmpresa + ": falta correr rhh/sql/lap1-11-devolucion-anticipo.sql.");
		}
	}

	/**
	 * Si el período de nómina que contiene la fecha indicada ya está CALCULADO, arma el
	 * texto de aviso — anular/reducir la cuota ahora no cambia ese rol, el cálculo ya la
	 * tomó (docs/logica-negocio/rhh/API-DEVOLUCION-ANTICIPO.md #4). No es un error: no
	 * rechaza nada, sólo informa.
	 * @param fechaVencimiento : Fecha de la cuota tocada
	 * @param idEmpresa        : Id de la empresa del empleado
	 * @return                 : El texto del aviso, o null si el período no está calculado
	 * @throws Throwable       : Excepcion
	 */
	private String avisoSiPeriodoCalculado(LocalDate fechaVencimiento, Long idEmpresa) throws Throwable {
		if (fechaVencimiento == null) {
			return null;
		}
		PeriodoNomina periodo = periodoNominaDaoService.selectByFechaEmpresa(idEmpresa, fechaVencimiento);
		if (periodo == null || periodo.getEstado() == null
				|| periodo.getEstado().intValue() != RhhEstadoPeriodoNomina.CALCULADO) {
			return null;
		}
		String etiqueta = periodo.getAnio() + "-" + String.format(Locale.US, "%02d", periodo.getMes());
		return "El período " + etiqueta + " ya está calculado: la cuota anulada ya entró en ese rol. "
				+ "Recalcule el período o el descuento saldrá igual este mes.";
	}

	private String nombreCompleto(Empleado empleado) {
		if (empleado == null) {
			return "";
		}
		String apellidos = (empleado.getApellidos() != null) ? empleado.getApellidos() : "";
		String nombres = (empleado.getNombres() != null) ? empleado.getNombres() : "";
		return (apellidos + " " + nombres).trim();
	}

	private String join(List<Long> ids) {
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(id);
		}
		return sb.toString();
	}

	private String fmt(double valor) {
		return String.format(Locale.US, "%.2f", valor);
	}

}
