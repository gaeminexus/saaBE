package com.saa.ejb.rhh.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DetallePlanillaIess;
import com.saa.model.rhh.PlanillaIess;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad PlanillaIess: captura y conciliacion de las
 * planillas que emite el portal del IESS (Fase 1 -- el pago es la Fase 2,
 * ver docs/logica-negocio/rhh/API-PLANILLA-IESS.md #6).</p>
 */
@Local
public interface PlanillaIessService extends EntityService<PlanillaIess> {

	/**
	 * Recupera la planilla con sus renglones ya cargados.
	 *
	 * @param id			: Id de la planilla
	 * @return				: La planilla, con {@code getRenglones()} poblado
	 * @throws Throwable	: Excepcion
	 */
	@Override
	PlanillaIess selectById(Long id) throws Throwable;

	/**
	 * Planillas de un periodo (hasta cuatro: una por tipo).
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Las planillas del periodo, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<PlanillaIess> porPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Captura la planilla que emitio el portal, con sus renglones.
	 * Validaciones, en orden: el periodo existe y esta calculado; el tipo es
	 * valido (rubro 330); no existe ya una planilla activa del mismo periodo
	 * y tipo; el valor es mayor a cero; si vienen renglones, su suma cuadra
	 * con el valor total dentro de un centavo. Nace en estado REGISTRADA.
	 *
	 * @param planilla		: Cabecera de la planilla (sin codigo)
	 * @param renglones		: Renglones del comprobante (concepto + valorIess), puede venir vacio
	 * @param idUsuario		: Id del usuario que registra
	 * @return				: La planilla registrada, con sus renglones
	 * @throws Throwable	: IncomeException si alguna validacion falla
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	PlanillaIess registrar(PlanillaIess planilla, List<DetallePlanillaIess> renglones, Long idUsuario)
			throws Throwable;

	/**
	 * Enfrenta la planilla contra la planilla de control del periodo.
	 *
	 * <p>Solo el tipo ROL_NORMAL tiene control calculado hoy: para los otros
	 * tres, {@code valorControl} queda nulo -- no se inventa un total-- y el
	 * mensaje lo dice explicitamente. No bloquea por diferencia: la muestra,
	 * la graba y deja pasar a estado CONCILIADA (decision del usuario).</p>
	 *
	 * @param idPlanilla	: Id de la planilla
	 * @param idUsuario		: Id del usuario que concilia
	 * @return				: Mapa con idPlanilla, tipo, valorIess, valorControl, diferencia,
	 *                        hayDiferencia, renglones (comparacion renglon por renglon) y mensaje
	 * @throws Throwable	: IncomeException si la planilla no existe o no esta en estado REGISTRADA
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	Map<String, Object> conciliar(Long idPlanilla, Long idUsuario) throws Throwable;

	/**
	 * Anula la planilla. Solo admitido desde REGISTRADA o CONCILIADA: una
	 * planilla PAGADA no se anula, se reversa (Fase 2).
	 *
	 * @param idPlanilla	: Id de la planilla
	 * @param motivo		: Motivo de la anulacion (obligatorio)
	 * @param idUsuario		: Id del usuario que anula
	 * @return				: La planilla anulada
	 * @throws Throwable	: IncomeException si no existe, falta el motivo o el estado no lo admite
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	PlanillaIess anular(Long idPlanilla, String motivo, Long idUsuario) throws Throwable;

}
