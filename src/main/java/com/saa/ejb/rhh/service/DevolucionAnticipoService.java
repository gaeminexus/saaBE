package com.saa.ejb.rhh.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DevolucionAnticipo;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DevolucionAnticipo: registra que un empleado
 * devolvió (total o parcialmente) un anticipo depositándolo en una cuenta de
 * la empresa, en vez de seguir descontándoselo del rol. Ver
 * docs/logica-negocio/rhh/API-DEVOLUCION-ANTICIPO.md.</p>
 */
@Local
public interface DevolucionAnticipoService extends EntityService<DevolucionAnticipo> {

	/**
	 * Devoluciones de un anticipo, más recientes primero.
	 *
	 * @param idAnticipo	: Id del anticipo
	 * @return				: Las devoluciones del anticipo, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<DevolucionAnticipo> porAnticipo(Long idAnticipo) throws Throwable;

	/**
	 * Registra la devolución. En orden: valida (anticipo existe, estado PAGADO
	 * o EN_DESCUENTO, valor mayor a cero, valor no supera el saldo pendiente,
	 * cuenta bancaria -- delegado a {@code IngresoService.procesarIngreso} --,
	 * fecha presente); contabiliza el depósito por tesorería
	 * ({@code IngresoService.procesarIngreso}, DEBE banco / HABER cuentas por
	 * cobrar empleados); sobre las cuotas PENDIENTE/PARCIAL más próximas por
	 * vencer, cancela las que el monto cubra entera y, si sobra un resto menor
	 * que la siguiente, esa cuota baja de valor y sigue PENDIENTE; ajusta el
	 * saldo del anticipo y del descuento recurrente; si el saldo queda en cero
	 * (tolerancia de un centavo), cancela el anticipo y desactiva el descuento.
	 *
	 * @param idAnticipo        : Id del anticipo que se devuelve
	 * @param fecha             : Fecha real del depósito
	 * @param valor             : Valor devuelto
	 * @param idCuentaBancaria  : Cuenta bancaria de la empresa donde depositó
	 * @param referencia        : N° de papeleta o transferencia (opcional)
	 * @param observacion       : Observaciones (opcional)
	 * @param idUsuario         : Id del usuario que registra
	 * @return                  : Mapa con idDevolucion, idAnticipo, valor, saldoAnterior,
	 *                            saldoNuevo, estadoAnticipo, cuotasCanceladas, cuotaAjustada,
	 *                            idIngreso, idAsiento, numeroAsiento, avisoPeriodoCalculado, mensaje
	 * @throws Throwable        : IncomeException si alguna validación falla
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	Map<String, Object> registrar(Long idAnticipo, LocalDate fecha, Double valor, Long idCuentaBancaria,
			String referencia, String observacion, Long idUsuario) throws Throwable;

	/**
	 * Anula la devolución: deshace todo, en orden -- anula el ingreso
	 * ({@code IngresoService.anularIngreso}, que reversa asiento y movimiento
	 * bancario), devuelve a PENDIENTE las cuotas que había cancelado y
	 * restaura el valor de la que hubiera ajustado, repone los saldos del
	 * anticipo y del descuento, y si el anticipo había quedado CANCELADO
	 * vuelve a EN_DESCUENTO reactivando el descuento.
	 *
	 * @param idDevolucion  : Id de la devolución
	 * @param motivo        : Motivo de la anulación (obligatorio)
	 * @param idUsuario     : Id del usuario que anula
	 * @return              : Mapa con idDevolucion, estado, mensaje
	 * @throws Throwable    : IncomeException si no existe, falta el motivo o ya está anulada
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	Map<String, Object> anular(Long idDevolucion, String motivo, Long idUsuario) throws Throwable;

}
