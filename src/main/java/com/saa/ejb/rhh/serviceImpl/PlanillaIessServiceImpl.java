package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetallePlanillaIessDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.PlanillaIessDaoService;
import com.saa.ejb.rhh.service.PlanillaControlIessService;
import com.saa.ejb.rhh.service.PlanillaIessService;
import com.saa.model.cnt.Asiento;
import com.saa.model.rhh.DetallePlanillaIess;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.PlanillaControlIess;
import com.saa.model.rhh.PlanillaIess;
import com.saa.model.scp.Empresa;
import com.saa.rubros.EstadoPlanillaIess;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhTipoPlanillaIess;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de PlanillaIessService.</p>
 *
 * <p>Fase 1 solamente: captura y conciliacion. El pago (Fase 2) tiene una
 * decision de arquitectura abierta -- ver
 * docs/logica-negocio/rhh/API-PLANILLA-IESS.md #6 -- y no se implementa aqui.</p>
 */
@Stateless
public class PlanillaIessServiceImpl implements PlanillaIessService {

	@EJB
	private PlanillaIessDaoService planillaIessDaoService;

	@EJB
	private DetallePlanillaIessDaoService detallePlanillaIessDaoService;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private PlanillaControlIessService planillaControlIessService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public PlanillaIess selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById PlanillaIess con id: " + id);
		PlanillaIess planilla = planillaIessDaoService.selectById(id, NombreEntidadesRhh.PLANILLA_IESS);
		planilla.setRenglones(detallePlanillaIessDaoService.selectByPlanilla(id));
		return planilla;
	}

	@Override
	public List<PlanillaIess> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PlanillaIessService");
		List<PlanillaIess> result = planillaIessDaoService.selectAll(NombreEntidadesRhh.PLANILLA_IESS);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total PlanillaIess no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<PlanillaIess> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria PlanillaIessService");
		List<PlanillaIess> result = planillaIessDaoService.selectByCriteria(datos, NombreEntidadesRhh.PLANILLA_IESS);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio PlanillaIess no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public PlanillaIess saveSingle(PlanillaIess planilla) throws Throwable {
		System.out.println("saveSingle - PlanillaIess");
		if (planilla.getCodigo() == null) {
			if (planilla.getEstado() == null) {
				planilla.setEstado(Long.valueOf(EstadoPlanillaIess.REGISTRADA));
			}
			if (planilla.getFechaRegistro() == null) {
				planilla.setFechaRegistro(LocalDateTime.now());
			}
		} else {
			// ── Guarda de los campos internos en la edición ──────────────────────
			// Mismo patrón que AnticipoClienteServiceImpl:212-225 y
			// CajaChicaServiceImpl.saveSingle (docs/general/MERGE-DESNUDO-EN-ENTITYDAOIMPL.md):
			// el PUT de una pantalla de edición de la cabecera no manda estado, fechaRegistro,
			// usuario, asiento, fechaPago ni motivoAnulacion — son estado interno que se
			// escribe desde registrar/conciliar/anular, no desde la pantalla de edición.
			try {
				Object[] previos = (Object[]) em.createQuery(
						"SELECT p.estado, p.fechaRegistro, p.usuario, p.asiento, p.fechaPago, "
								+ "p.motivoAnulacion FROM PlanillaIess p WHERE p.codigo = :id")
						.setParameter("id", planilla.getCodigo())
						.getSingleResult();
				planilla.setEstado((Long) previos[0]);
				planilla.setFechaRegistro((LocalDateTime) previos[1]);
				planilla.setUsuario((Long) previos[2]);
				planilla.setAsiento((Asiento) previos[3]);
				planilla.setFechaPago((LocalDate) previos[4]);
				planilla.setMotivoAnulacion((String) previos[5]);
			} catch (jakarta.persistence.NoResultException e) {
				System.out.println("⚠ saveSingle PlanillaIess: no existe el id " + planilla.getCodigo()
						+ " para preservar los campos internos.");
			}
			if (planilla.getEstado() == null) {
				planilla.setEstado(Long.valueOf(EstadoPlanillaIess.REGISTRADA));
			}
		}
		return planillaIessDaoService.save(planilla, planilla.getCodigo());
	}

	@Override
	public void save(List<PlanillaIess> lista) throws Throwable {
		for (PlanillaIess registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		PlanillaIess entidad = new PlanillaIess();
		for (Long registro : id) {
			planillaIessDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Lógica de negocio
	// =====================================================================

	@Override
	public List<PlanillaIess> porPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("=== porPeriodo planilla IESS | periodo=" + idPeriodo + " ===");
		return planillaIessDaoService.selectByPeriodo(idPeriodo);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public PlanillaIess registrar(PlanillaIess planilla, List<DetallePlanillaIess> renglones, Long idUsuario)
			throws Throwable {

		System.out.println("=== registrar planilla IESS ===");

		if (planilla == null) {
			throw new IncomeException("No llegaron los datos de la planilla.");
		}
		if (planilla.getEmpresa() == null || planilla.getEmpresa().getCodigo() == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}
		if (planilla.getPeriodo() == null || planilla.getPeriodo().getCodigo() == null) {
			throw new IncomeException("Debe indicar el período de nómina.");
		}

		Empresa empresa = em.find(Empresa.class, planilla.getEmpresa().getCodigo());
		if (empresa == null) {
			throw new IncomeException("No se encontró la empresa con ID: " + planilla.getEmpresa().getCodigo());
		}

		// 1. El período existe y está calculado.
		PeriodoNomina periodo = periodoNominaDaoService.selectById(planilla.getPeriodo().getCodigo(),
				NombreEntidadesRhh.PERIODO_NOMINA);
		if (periodo == null || periodo.getEstado() == null
				|| periodo.getEstado().intValue() != RhhEstadoPeriodoNomina.CALCULADO) {
			throw new IncomeException("El período de nómina " + planilla.getPeriodo().getCodigo()
					+ " no existe o no está calculado: no se puede registrar la planilla del IESS.");
		}

		// 2. El tipo es válido contra el rubro 330.
		Long tipo = planilla.getTipo();
		if (tipo == null || tipo.intValue() < RhhTipoPlanillaIess.ROL_NORMAL
				|| tipo.intValue() > RhhTipoPlanillaIess.FONDOS_DE_RESERVA) {
			throw new IncomeException("El tipo de planilla " + tipo + " no es válido: debe ser 1 (rol "
					+ "normal), 2 (préstamos quirografarios), 3 (préstamos hipotecarios) o 4 (fondos de "
					+ "reserva).");
		}

		// 3. No existe ya una planilla activa del mismo período y tipo.
		if (planillaIessDaoService.existeActivaEnPeriodoTipo(periodo.getCodigo(), tipo)) {
			throw new IncomeException("Ya existe una planilla del IESS activa para el período "
					+ periodo.getCodigo() + " y el tipo " + tipo + ".");
		}

		// 4. valorIess > 0.
		if (planilla.getValorIess() == null || planilla.getValorIess() <= 0) {
			throw new IncomeException("El valor de la planilla debe ser mayor a cero.");
		}

		// 5. Si vienen renglones, su suma cuadra con valorIess dentro de un centavo.
		if (renglones != null && !renglones.isEmpty()) {
			double sumaRenglones = 0.0;
			for (DetallePlanillaIess renglon : renglones) {
				sumaRenglones += (renglon.getValorIess() != null) ? renglon.getValorIess() : 0.0;
			}
			if (Math.abs(sumaRenglones - planilla.getValorIess()) > 0.01) {
				throw new IncomeException("La suma de los renglones ($"
						+ String.format(Locale.US, "%.2f", sumaRenglones)
						+ ") no cuadra con el valor total de la planilla ($"
						+ String.format(Locale.US, "%.2f", planilla.getValorIess()) + ").");
			}
		}

		planilla.setCodigo(null);
		planilla.setEmpresa(empresa);
		planilla.setPeriodo(periodo);
		planilla.setEstado(Long.valueOf(EstadoPlanillaIess.REGISTRADA));
		planilla.setValorControl(null);
		planilla.setDiferencia(null);
		planilla.setFechaPago(null);
		planilla.setAsiento(null);
		planilla.setMotivoAnulacion(null);
		planilla.setFechaRegistro(LocalDateTime.now());
		planilla.setUsuario(idUsuario);

		planilla = planillaIessDaoService.save(planilla, null);

		List<DetallePlanillaIess> renglonesGuardados = new ArrayList<>();
		if (renglones != null) {
			for (DetallePlanillaIess renglon : renglones) {
				renglon.setCodigo(null);
				renglon.setPlanilla(planilla);
				renglon.setValorControl(null);
				renglon.setDiferencia(null);
				renglonesGuardados.add(detallePlanillaIessDaoService.save(renglon, null));
			}
		}
		planilla.setRenglones(renglonesGuardados);

		System.out.println("✓ Planilla IESS registrada: id=" + planilla.getCodigo());
		return planilla;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> conciliar(Long idPlanilla, Long idUsuario) throws Throwable {

		System.out.println("=== conciliar planilla IESS | id=" + idPlanilla + " ===");

		PlanillaIess planilla = recuperaPlanilla(idPlanilla);
		if (planilla.getEstado() == null || planilla.getEstado().intValue() != EstadoPlanillaIess.REGISTRADA) {
			throw new IncomeException("La planilla " + idPlanilla + " está en estado " + planilla.getEstado()
					+ ": solo se puede conciliar una planilla en estado Registrada.");
		}

		List<DetallePlanillaIess> renglones = detallePlanillaIessDaoService.selectByPlanilla(idPlanilla);

		// ⚠️ La planilla de control SOLO cubre hoy el tipo 1 (rol normal). Para
		// quirografarios, hipotecarios y fondos de reserva no hay contraparte
		// calculada: NO se inventa un total (docs/logica-negocio/rhh/API-PLANILLA-IESS.md
		// #5.2). valorControl queda null, la diferencia también, y el mensaje lo dice.
		boolean hayControl = planilla.getTipo() != null
				&& planilla.getTipo().intValue() == RhhTipoPlanillaIess.ROL_NORMAL;
		Double valorControl = null;

		if (hayControl) {
			PlanillaControlIess control = planillaControlIessService.generar(planilla.getPeriodo().getCodigo());
			valorControl = control.getTotalComprobante();
			asignaValorControlRenglones(renglones, control);
		}

		Double diferencia = (valorControl != null && planilla.getValorIess() != null)
				? Double.valueOf(planilla.getValorIess().doubleValue() - valorControl.doubleValue())
				: null;

		planilla.setValorControl(valorControl);
		planilla.setDiferencia(diferencia);
		planilla.setEstado(Long.valueOf(EstadoPlanillaIess.CONCILIADA));
		planilla = planillaIessDaoService.save(planilla, planilla.getCodigo());

		for (DetallePlanillaIess renglon : renglones) {
			detallePlanillaIessDaoService.save(renglon, renglon.getCodigo());
		}

		boolean hayDiferencia = diferencia != null && Math.abs(diferencia.doubleValue()) > 0.01;
		String mensaje;
		if (!hayControl) {
			mensaje = "Planilla conciliada: no hay control disponible para este tipo de planilla.";
		} else if (hayDiferencia) {
			mensaje = "Planilla conciliada con una diferencia de $"
					+ String.format(Locale.US, "%.2f", Math.abs(diferencia.doubleValue()))
					+ (diferencia.doubleValue() > 0 ? " a favor del IESS." : " a favor de la empresa.");
		} else {
			mensaje = "Planilla conciliada sin diferencias.";
		}

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idPlanilla", planilla.getCodigo());
		resultado.put("tipo", planilla.getTipo());
		resultado.put("valorIess", planilla.getValorIess());
		resultado.put("valorControl", valorControl);
		resultado.put("diferencia", diferencia);
		resultado.put("hayDiferencia", hayDiferencia);
		resultado.put("renglones", renglonesADto(renglones));
		resultado.put("mensaje", mensaje);
		return resultado;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public PlanillaIess anular(Long idPlanilla, String motivo, Long idUsuario) throws Throwable {

		System.out.println("=== anular planilla IESS | id=" + idPlanilla + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}
		PlanillaIess planilla = recuperaPlanilla(idPlanilla);
		int estado = (planilla.getEstado() != null) ? planilla.getEstado().intValue() : 0;
		if (estado != EstadoPlanillaIess.REGISTRADA && estado != EstadoPlanillaIess.CONCILIADA) {
			throw new IncomeException("La planilla " + idPlanilla + " está en estado " + estado
					+ ": solo se puede anular desde Registrada o Conciliada. Una planilla Pagada se "
					+ "reversa, no se anula.");
		}

		planilla.setEstado(Long.valueOf(EstadoPlanillaIess.ANULADA));
		planilla.setMotivoAnulacion(motivo.trim());
		planilla = planillaIessDaoService.save(planilla, planilla.getCodigo());

		System.out.println("✓ Planilla IESS " + idPlanilla + " anulada.");
		return planilla;
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	private PlanillaIess recuperaPlanilla(Long idPlanilla) throws Throwable {
		PlanillaIess planilla = planillaIessDaoService.selectById(idPlanilla, NombreEntidadesRhh.PLANILLA_IESS);
		if (planilla == null) {
			throw new IncomeException("No existe la planilla del IESS " + idPlanilla + ".");
		}
		return planilla;
	}

	/**
	 * Asigna a cada renglón el valor de control que le corresponde, buscando
	 * por palabra clave en el concepto capturado. Es un mejor esfuerzo, no una
	 * correspondencia garantizada: {@code PlanillaControlIess} no expone un
	 * desglose por el mismo texto libre que trae el comprobante del portal, así
	 * que un concepto que no case con ninguna palabra clave queda con
	 * {@code valorControl = null} — igual que los tipos sin control, no se
	 * inventa un valor. Verificar contra capturas reales del portal.
	 * @param renglones : Renglones de la planilla (se modifican en el sitio)
	 * @param control   : Planilla de control ya generada del período
	 */
	private void asignaValorControlRenglones(List<DetallePlanillaIess> renglones, PlanillaControlIess control) {
		for (DetallePlanillaIess renglon : renglones) {
			Double valorControl = valorControlPorConcepto(renglon.getConcepto(), control);
			renglon.setValorControl(valorControl);
			renglon.setDiferencia((valorControl != null && renglon.getValorIess() != null)
					? Double.valueOf(renglon.getValorIess().doubleValue() - valorControl.doubleValue())
					: null);
		}
	}

	private Double valorControlPorConcepto(String concepto, PlanillaControlIess control) {
		if (concepto == null) {
			return null;
		}
		String texto = concepto.trim().toUpperCase(Locale.ROOT);
		if (texto.contains("PATRONAL")) {
			return control.getTotalAportePatronal();
		}
		if (texto.contains("PERSONAL")) {
			return control.getTotalAportePersonal();
		}
		if (texto.contains("CCC")) {
			return control.getContribucionCcc();
		}
		if (texto.contains("PARCIAL")) {
			return control.getTotalSeguroTiempoParcial();
		}
		return null;
	}

	private List<Map<String, Object>> renglonesADto(List<DetallePlanillaIess> renglones) {
		List<Map<String, Object>> lista = new ArrayList<>();
		for (DetallePlanillaIess renglon : renglones) {
			Map<String, Object> item = new HashMap<>();
			item.put("concepto", renglon.getConcepto());
			item.put("valorIess", renglon.getValorIess());
			item.put("valorControl", renglon.getValorControl());
			item.put("diferencia", renglon.getDiferencia());
			lista.add(item);
		}
		return lista;
	}

}
