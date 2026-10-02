package com.saa.ejb.rhh.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DetalleLiquidacionExterna;
import com.saa.model.rhh.LiquidacionExterna;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad LiquidacionExterna (liquidaciones de ex-colaboradores de la
 * administracion anterior). Diseno:
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md.</p>
 */
@Local
public interface LiquidacionExternaService extends EntityService<LiquidacionExterna> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	LiquidacionExterna selectById(Long id) throws Throwable;

	/**
	 * Detalle de una liquidacion, ordenado por orden y luego codigo (§6).
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @return				: Detalle
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleLiquidacionExterna> detalle(Long idLiquidacion) throws Throwable;

	/**
	 * Crea la cabecera y los detalles en una sola transaccion. Estado REGISTRADA (§6).
	 *
	 * <p>El servidor calcula totalIngresos, totalDescuentos y neto (ignora lo que mande el
	 * cliente en esos tres campos), sella fechaRegistro, trimea la identificacion y pone
	 * apellidos y nombres en mayusculas. Valida §6 completo.</p>
	 *
	 * @param liquidacion	: Cabecera
	 * @param detalles		: Conceptos de la liquidacion
	 * @return				: La liquidacion creada
	 * @throws Throwable	: IncomeException si alguna validacion de §6 falla
	 */
	LiquidacionExterna registrar(LiquidacionExterna liquidacion, List<DetalleLiquidacionExterna> detalles)
			throws Throwable;

	/**
	 * Actualiza una liquidacion en estado REGISTRADA: reemplaza todos los detalles
	 * (borra y recrea) y recalcula los totales. Conserva lo que el cliente no manda en los
	 * campos internos (estado, fechaRegistro, idPago, idAsiento, fechaPago,
	 * motivoAnulacion) -- el merge desnudo de EntityDaoImpl.save los grabaria en NULL
	 * (REGISTRO-RESERVAS-EQUIPOS.md §8.2).
	 *
	 * @param liquidacion	: Cabecera, con codigo
	 * @param detalles		: Conceptos de la liquidacion, reemplazan a los existentes
	 * @return				: La liquidacion actualizada
	 * @throws Throwable	: IncomeException si no esta REGISTRADA o si alguna validacion de §6 falla
	 */
	LiquidacionExterna actualizar(LiquidacionExterna liquidacion, List<DetalleLiquidacionExterna> detalles)
			throws Throwable;

	/**
	 * Registra el pago en la bandeja de tesoreria (§4). Solo desde REGISTRADA. Idempotente:
	 * si ya existe un pago vigente de este origen y este id, no crea otro.
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @param idUsuario		: Id del usuario que envia
	 * @return				: Mapa con liquidacion e idPago
	 * @throws Throwable	: IncomeException si no esta REGISTRADA o si falta algun dato
	 */
	Map<String, Object> enviarATesoreria(Long idLiquidacion, Long idUsuario) throws Throwable;

	/**
	 * Sincroniza el estado de la liquidacion contra el pago en tesoreria (§4): si el pago
	 * esta CONFIRMADO, copia fechaPago e idAsiento y pasa a PAGADA; si esta RECHAZADO o
	 * ANULADO, vuelve a REGISTRADA con idPago null. Sin efecto si la liquidacion no esta
	 * EN_TESORERIA.
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @return				: La liquidacion, sincronizada
	 * @throws Throwable	: Excepcion
	 */
	LiquidacionExterna sincronizarPago(Long idLiquidacion) throws Throwable;

	/**
	 * Anula la liquidacion (§4). En REGISTRADA pasa directo a ANULADA; en EN_TESORERIA
	 * solo si el pago sigue POR_APROBAR (lo anula tambien en tesoreria); en PAGADA se
	 * rechaza.
	 *
	 * @param idLiquidacion	: Id de la liquidacion
	 * @param idUsuario		: Id del usuario que anula
	 * @param motivo		: Motivo, obligatorio
	 * @return				: La liquidacion anulada
	 * @throws Throwable	: IncomeException si el motivo falta o el estado no lo permite
	 */
	LiquidacionExterna anular(Long idLiquidacion, Long idUsuario, String motivo) throws Throwable;

}
