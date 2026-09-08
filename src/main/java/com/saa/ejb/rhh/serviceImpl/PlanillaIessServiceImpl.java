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
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.cxp.service.dto.LineaContablePago;
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
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.EstadoPlanillaIess;
import com.saa.rubros.FormaPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhConceptoPlanillaIess;
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
 * <p>Captura, conciliacion y pago. El pago (Fase 2, decision del usuario del
 * 2026-09-07 -- ver docs/logica-negocio/rhh/API-PLANILLA-IESS.md #6) va por el
 * circuito de tesoreria (PagoProgramadoService), por su camino generico de
 * desglose contable (PGS.DPGT): no hizo falta escribir ningun metodo de
 * contabilizacion propio dentro de com.saa.ejb.cxp.</p>
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

	@EJB
	private PagoProgramadoService pagoProgramadoService;

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

		// 5. Si vienen renglones, su suma cuadra con valorIess dentro de un centavo,
		// y el conceptoTipo de cada uno (si viene) es válido contra el rubro 331.
		if (renglones != null && !renglones.isEmpty()) {
			double sumaRenglones = 0.0;
			for (DetallePlanillaIess renglon : renglones) {
				sumaRenglones += (renglon.getValorIess() != null) ? renglon.getValorIess() : 0.0;
				Long conceptoTipo = renglon.getConceptoTipo();
				if (conceptoTipo != null && (conceptoTipo.intValue() < RhhConceptoPlanillaIess.APORTE_PERSONAL
						|| conceptoTipo.intValue() > RhhConceptoPlanillaIess.OTRO)) {
					throw new IncomeException("El concepto '" + renglon.getConcepto() + "' trae un tipo de "
							+ "concepto " + conceptoTipo + " inválido: debe ser 1 (aporte personal), 2 "
							+ "(aporte patronal), 3 (contribución CCC), 4 (seguro de salud tiempo parcial), "
							+ "5 (otro) o venir vacío (sin clasificar).");
				}
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

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> pagar(Long idPlanilla, Long idCuentaBancaria, LocalDate fechaPago, Long idUsuario)
			throws Throwable {

		System.out.println("=== pagar planilla IESS | id=" + idPlanilla + " ===");

		PlanillaIess planilla = recuperaPlanilla(idPlanilla);

		// 1. Estado, cuenta bancaria y que no tenga ya un pago vivo.
		if (planilla.getEstado() == null || planilla.getEstado().intValue() != EstadoPlanillaIess.CONCILIADA) {
			throw new IncomeException("La planilla " + idPlanilla + " está en estado " + planilla.getEstado()
					+ ": solo se puede pagar una planilla en estado Conciliada. El sentido de conciliar "
					+ "antes de pagar es no pagar sin haber cuadrado.");
		}
		if (idCuentaBancaria == null) {
			throw new IncomeException("Debe indicar la cuenta bancaria desde la que el IESS debita.");
		}
		rechazaSiTienePagoVivo(idPlanilla);

		List<DetallePlanillaIess> renglones = detallePlanillaIessDaoService.selectByPlanilla(idPlanilla);

		// 3. El desglose se arma ANTES de llamar al circuito: con débito automático el
		// pago nace CONFIRMADO y contabiliza en el acto, así que PGS.DPGT tiene que
		// existir cuando registrarPagoDeOrigenExterno arma el asiento.
		List<LineaContablePago> desglose = armaDesgloseContable(planilla, renglones);

		LocalDate fecha = (fechaPago != null) ? fechaPago : LocalDate.now();

		BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
		beneficiario.setNombre("IESS");
		beneficiario.setIdentificacion("IESS-" + planilla.getTipo());

		Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeOrigenExterno(
				OrigenPagoExterno.RHH_PLANILLA_IESS, planilla.getCodigo(), planilla.getEmpresa().getCodigo(),
				idCuentaBancaria, planilla.getValorIess(), fecha.toString(), beneficiario, desglose,
				"Pago planilla IESS " + descripcionTipo(planilla.getTipo()) + " N° "
						+ planilla.getNumeroComprobante(),
				idUsuario, true, null, Long.valueOf(FormaPagoProgramado.DEBITO_AUTOMATICO));

		Long idPago = (Long) resultadoPago.get("pago");
		if (idPago == null) {
			// No debería pasar (registrarPagoDeOrigenExterno siempre devuelve "pago"),
			// pero se deja explícito en vez de un NPE silencioso más abajo.
			throw new IncomeException("El circuito de pagos no devolvió el pago generado para la planilla "
					+ idPlanilla + ".");
		}

		// Sólo el escalar del asiento: PagoProgramado tiene trece @ManyToOne EAGER, no se
		// carga la entidad (mismo criterio que en todo este archivo y en CajaChica).
		Long idAsiento;
		try {
			idAsiento = (Long) em.createQuery("select p.asiento.codigo from PagoProgramado p where p.id = :id")
					.setParameter("id", idPago)
					.getSingleResult();
		} catch (jakarta.persistence.NoResultException e) {
			idAsiento = null;
		}

		planilla.setEstado(Long.valueOf(EstadoPlanillaIess.PAGADA));
		planilla.setFechaPago(fecha);
		planilla.setAsiento(idAsiento != null ? em.getReference(Asiento.class, idAsiento) : null);
		planilla = planillaIessDaoService.save(planilla, planilla.getCodigo());

		System.out.println("✓ Planilla IESS " + idPlanilla + " pagada | idPago=" + idPago
				+ " | asiento=" + idAsiento);

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idPlanilla", planilla.getCodigo());
		resultado.put("estado", planilla.getEstado());
		resultado.put("idPago", idPago);
		resultado.put("idAsiento", idAsiento);
		resultado.put("numeroAsiento", resultadoPago.get("asiento"));
		resultado.put("mensaje", resultadoPago.get("mensaje"));
		return resultado;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public PlanillaIess reversarPago(Long idPlanilla, String motivo, Long idUsuario) throws Throwable {

		System.out.println("=== reversarPago planilla IESS | id=" + idPlanilla + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la reversión.");
		}
		PlanillaIess planilla = recuperaPlanilla(idPlanilla);
		if (planilla.getEstado() == null || planilla.getEstado().intValue() != EstadoPlanillaIess.PAGADA) {
			throw new IncomeException("La planilla " + idPlanilla + " está en estado " + planilla.getEstado()
					+ ": solo se puede reversar el pago de una planilla Pagada.");
		}

		// La planilla no guarda el id del pago (sólo el asiento): se busca por
		// (origen, idOrigen), igual que rechazaSiTienePagoVivo. Sólo el escalar.
		Long idPago;
		try {
			idPago = (Long) em.createQuery("select p.id from PagoProgramado p where p.origenExterno = :origen "
					+ "and p.idOrigen = :idPlanilla and p.estado = :confirmado")
					.setParameter("origen", OrigenPagoExterno.RHH_PLANILLA_IESS)
					.setParameter("idPlanilla", idPlanilla)
					.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO))
					.getSingleResult();
		} catch (jakarta.persistence.NoResultException e) {
			throw new IncomeException("La planilla " + idPlanilla + " está Pagada pero no se encontró su "
					+ "pago confirmado: revísela antes de continuar.");
		}

		// revertirPagoConfirmado no sabe nada de PLIS -- no hay un
		// anularPlanillaIessSiAplica del lado de cxp, y no hace falta agregarlo. Es rhh
		// quien actualiza su propia planilla después de que el reverso vuelva (#6.3).
		pagoProgramadoService.revertirPagoConfirmado(idPago, motivo.trim(), idUsuario);

		planilla.setEstado(Long.valueOf(EstadoPlanillaIess.CONCILIADA));
		planilla.setFechaPago(null);
		planilla.setAsiento(null);
		planilla = planillaIessDaoService.save(planilla, planilla.getCodigo());

		System.out.println("✓ Pago de la planilla IESS " + idPlanilla + " reversado.");
		return planilla;
	}

	// =====================================================================
	// Helpers privados
	// =====================================================================

	/**
	 * Arma el desglose contable del pago (una {@link LineaContablePago} por fila de
	 * {@code PGS.DPGT}), resuelto por parametrización — nunca hardcodeando una cuenta —
	 * buscando el {@code ProductoPago} por su CODIGO en {@code rhh/sql/lap1-09-productos-pago-iess.sql}.
	 * <p>
	 * <b>Cómo se decide una fila por renglón o una sola por el total, y por qué:</b> el rol
	 * normal es el único comprobante que de verdad desglosa en conceptos con cuentas
	 * distintas (aporte personal / aporte patronal — #2 de la especificación), así que ahí
	 * se arma una línea por renglón, resuelta por su {@code conceptoTipo}. Quirografarios,
	 * hipotecarios y fondos de reserva son, cada uno, un pago de un solo concepto (#2 de la
	 * especificación: "cada una tiene su propio comprobante"), así que se arma una sola
	 * línea por el valor total de la planilla — los renglones que se hayan capturado ahí
	 * son solo para la conciliación (#3.2), no participan del desglose contable.
	 * <p>
	 * Son siete productos (IESS-APER, IESS-APAT, IESS-CCC, IESS-STP, IESS-PRSQ, IESS-PRSH,
	 * IESS-FRES), mapeo 1:1 — nada se pliega en Java. Si dos conceptos alguna vez deben
	 * terminar en la misma cuenta contable, eso se resuelve apuntando sus dos grupos de
	 * producto a la misma cuenta en el .sql, no con un switch acá: es parametrización, no
	 * código (lección del ítem 6.d, donde una suposición sobre el CCC resultó falsa).
	 * Quirografarios e hipotecarios ya no comparten producto (ítem 7): {@code RhhLineaAsiento}
	 * ganó la línea 19 propia para el hipotecario, y {@code ContabilizacionNominaServiceImpl}
	 * los separa desde ahí.
	 * @param planilla  : Planilla a pagar (con tipo y empresa resueltos)
	 * @param renglones : Renglones ya cargados de la planilla
	 * @return          : El desglose a pasar a {@code registrarPagoDeOrigenExterno}
	 * @throws Throwable : IncomeException si un renglón del rol no tiene concepto clasificado,
	 *                     si el rol no tiene renglones, o si falta el producto en la parametrización
	 */
	private List<LineaContablePago> armaDesgloseContable(PlanillaIess planilla, List<DetallePlanillaIess> renglones)
			throws Throwable {

		Long idEmpresa = planilla.getEmpresa().getCodigo();
		int tipo = planilla.getTipo().intValue();
		List<LineaContablePago> desglose = new ArrayList<>();

		if (tipo == RhhTipoPlanillaIess.ROL_NORMAL) {
			if (renglones == null || renglones.isEmpty()) {
				throw new IncomeException("La planilla " + planilla.getCodigo() + " no tiene renglones "
						+ "clasificados: no se puede armar el desglose contable del pago del rol.");
			}
			for (DetallePlanillaIess renglon : renglones) {
				// §6.2: un renglón sin producto rechaza el pago citándolo — no se inventa una
				// cuenta genérica ni se omite la línea (omitirla descuadraría el asiento
				// contra el valor del pago, y el circuito lo rechazaría sin decir por qué).
				String codigoProducto = codigoProductoPorConceptoTipo(renglon.getConceptoTipo());
				if (codigoProducto == null) {
					throw new IncomeException("El renglón '" + renglon.getConcepto() + "' (id "
							+ renglon.getCodigo() + ") de la planilla " + planilla.getCodigo() + " no tiene "
							+ "un concepto clasificado (aporte personal, patronal, CCC o seguro de tiempo "
							+ "parcial): no se puede determinar la cuenta contable de su pago. Clasifíquelo "
							+ "antes de pagar.");
				}
				LineaContablePago linea = new LineaContablePago();
				linea.setIdProductoPago(buscaProductoPago(codigoProducto, idEmpresa));
				linea.setValor(renglon.getValorIess());
				linea.setConcepto(renglon.getConcepto());
				desglose.add(linea);
			}
			return desglose;
		}

		String codigoProducto;
		if (tipo == RhhTipoPlanillaIess.FONDOS_DE_RESERVA) {
			codigoProducto = "IESS-FRES";
		} else if (tipo == RhhTipoPlanillaIess.PRESTAMOS_HIPOTECARIOS) {
			codigoProducto = "IESS-PRSH";
		} else {
			codigoProducto = "IESS-PRSQ";
		}
		LineaContablePago linea = new LineaContablePago();
		linea.setIdProductoPago(buscaProductoPago(codigoProducto, idEmpresa));
		linea.setValor(planilla.getValorIess());
		linea.setConcepto("Planilla IESS " + descripcionTipo(planilla.getTipo()) + " N° "
				+ planilla.getNumeroComprobante());
		desglose.add(linea);
		return desglose;
	}

	private String codigoProductoPorConceptoTipo(Long conceptoTipo) {
		if (conceptoTipo == null) {
			return null;
		}
		switch (conceptoTipo.intValue()) {
			case RhhConceptoPlanillaIess.APORTE_PERSONAL:
				return "IESS-APER";
			case RhhConceptoPlanillaIess.APORTE_PATRONAL:
				return "IESS-APAT";
			case RhhConceptoPlanillaIess.CONTRIBUCION_CCC:
				return "IESS-CCC";
			case RhhConceptoPlanillaIess.SEGURO_SALUD_TIEMPO_PARCIAL:
				return "IESS-STP";
			default:
				// OTRO (5): sin producto, rechaza en armaDesgloseContable.
				return null;
		}
	}

	private Long buscaProductoPago(String codigo, Long idEmpresa) throws Throwable {
		try {
			return (Long) em.createQuery("select p.id from ProductoPago p where p.codigo = :codigo "
					+ "and p.empresa.codigo = :idEmpresa")
					.setParameter("codigo", codigo)
					.setParameter("idEmpresa", idEmpresa)
					.getSingleResult();
		} catch (jakarta.persistence.NoResultException e) {
			throw new IncomeException("No existe el producto de pago '" + codigo + "' para la empresa "
					+ idEmpresa + ": falta correr rhh/sql/lap1-09-productos-pago-iess.sql.");
		}
	}

	private String descripcionTipo(Long tipo) {
		if (tipo == null) {
			return "";
		}
		switch (tipo.intValue()) {
			case RhhTipoPlanillaIess.ROL_NORMAL:
				return "Rol Normal";
			case RhhTipoPlanillaIess.PRESTAMOS_QUIROGRAFARIOS:
				return "Préstamos Quirografarios";
			case RhhTipoPlanillaIess.PRESTAMOS_HIPOTECARIOS:
				return "Préstamos Hipotecarios";
			case RhhTipoPlanillaIess.FONDOS_DE_RESERVA:
				return "Fondos de Reserva";
			default:
				return String.valueOf(tipo);
		}
	}

	/**
	 * Rechaza si la planilla ya tiene un {@code PGS.PGTR} vivo (origen
	 * {@link OrigenPagoExterno#RHH_PLANILLA_IESS}) — estado distinto de RECHAZADO o
	 * ANULADO. Mismo criterio que {@code CajaChicaServiceImpl.rechazaSiTienePagosEnCurso}:
	 * sólo id y estado, nunca la entidad {@code PagoProgramado}.
	 * @param idPlanilla : Id de la planilla a validar
	 * @throws Throwable : IncomeException si hay un pago vivo
	 */
	private void rechazaSiTienePagoVivo(Long idPlanilla) throws Throwable {
		@SuppressWarnings("unchecked")
		List<Object[]> pagosVivos = em.createQuery(
				"select p.id, p.estado from PagoProgramado p where p.origenExterno = :origen "
				+ "and p.idOrigen = :idPlanilla and p.estado not in (:rechazado, :anulado)")
				.setParameter("origen", OrigenPagoExterno.RHH_PLANILLA_IESS)
				.setParameter("idPlanilla", idPlanilla)
				.setParameter("rechazado", Long.valueOf(EstadoPagoProgramado.RECHAZADO))
				.setParameter("anulado", Long.valueOf(EstadoPagoProgramado.ANULADO))
				.getResultList();
		if (!pagosVivos.isEmpty()) {
			Object[] fila = pagosVivos.get(0);
			throw new IncomeException("La planilla " + idPlanilla + " ya tiene el pago N° " + fila[0]
					+ " en curso (estado " + fila[1] + "): no se puede registrar otro pago.");
		}
	}

	private PlanillaIess recuperaPlanilla(Long idPlanilla) throws Throwable {
		PlanillaIess planilla = planillaIessDaoService.selectById(idPlanilla, NombreEntidadesRhh.PLANILLA_IESS);
		if (planilla == null) {
			throw new IncomeException("No existe la planilla del IESS " + idPlanilla + ".");
		}
		return planilla;
	}

	/**
	 * Asigna a cada renglón el valor de control que le corresponde, según su
	 * {@code conceptoTipo} — un dato explícito capturado al registrar, no una
	 * adivinanza por el texto libre del comprobante (docs/logica-negocio/rhh/
	 * API-PLANILLA-IESS.md #4.d). Antes de este ítem se buscaba por palabra
	 * clave en {@code concepto}; se retiró entero porque dos renglones con la
	 * misma palabra ("PERSONAL" en dos conceptos distintos, por ejemplo)
	 * recibían el mismo total y la suma de diferencias por renglón dejaba de
	 * cuadrar con la diferencia de cabecera — un descuadre invisible es peor
	 * que no comparar.
	 * @param renglones : Renglones de la planilla (se modifican en el sitio)
	 * @param control   : Planilla de control ya generada del período
	 */
	private void asignaValorControlRenglones(List<DetallePlanillaIess> renglones, PlanillaControlIess control) {
		for (DetallePlanillaIess renglon : renglones) {
			Double valorControl = valorControlPorConceptoTipo(renglon.getConceptoTipo(), control);
			renglon.setValorControl(valorControl);
			renglon.setDiferencia((valorControl != null && renglon.getValorIess() != null)
					? Double.valueOf(renglon.getValorIess().doubleValue() - valorControl.doubleValue())
					: null);
		}
	}

	/**
	 * @param conceptoTipo : Detalle del rubro 331, o null si el renglón no está clasificado
	 * @param control      : Planilla de control ya generada del período
	 * @return : El total de control correspondiente, o null si no está clasificado (OTRO
	 *           incluido) o si no hay contraparte para ese concepto. Null y cero no son lo
	 *           mismo: cero afirmaría que nuestro control dice que ese concepto vale cero,
	 *           que es un dato falso y se leería como una diferencia por el total del
	 *           renglón; null dice, correctamente, que no hay con qué comparar.
	 */
	private Double valorControlPorConceptoTipo(Long conceptoTipo, PlanillaControlIess control) {
		if (conceptoTipo == null) {
			return null;
		}
		switch (conceptoTipo.intValue()) {
			case RhhConceptoPlanillaIess.APORTE_PERSONAL:
				return control.getTotalAportePersonal();
			case RhhConceptoPlanillaIess.APORTE_PATRONAL:
				return control.getTotalAportePatronal();
			case RhhConceptoPlanillaIess.CONTRIBUCION_CCC:
				return control.getContribucionCcc();
			case RhhConceptoPlanillaIess.SEGURO_SALUD_TIEMPO_PARCIAL:
				return control.getTotalSeguroTiempoParcial();
			default:
				return null;
		}
	}

	private List<Map<String, Object>> renglonesADto(List<DetallePlanillaIess> renglones) {
		List<Map<String, Object>> lista = new ArrayList<>();
		for (DetallePlanillaIess renglon : renglones) {
			Map<String, Object> item = new HashMap<>();
			item.put("concepto", renglon.getConcepto());
			item.put("conceptoTipo", renglon.getConceptoTipo());
			item.put("valorIess", renglon.getValorIess());
			item.put("valorControl", renglon.getValorControl());
			item.put("diferencia", renglon.getDiferencia());
			lista.add(item);
		}
		return lista;
	}

}
