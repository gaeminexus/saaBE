package com.saa.ejb.tsr.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.MovimientoCajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad MovimientoCajaChica: gastos, aperturas,
 * reposiciones y ajustes de una caja chica.</p>
 *
 * Los gastos contabilizan en el acto (DEBE grupo del producto / HABER caja).
 * Las aperturas y reposiciones desde banco pasan por el circuito de
 * {@code PagoProgramadoService} como origen externo
 * ({@link com.saa.rubros.OrigenPagoExterno#TSR_CAJA_CHICA}): nacen sin
 * asiento y lo reciben cuando el pago se confirma (incluido el caso cheque).
 */
@Local
public interface MovimientoCajaChicaService extends EntityService<MovimientoCajaChica> {

	/**
	 * Registra un gasto de caja chica y lo contabiliza en la misma transacción:
	 * DEBE cuenta del grupo del producto / HABER cuenta contable de la caja.
	 * @param idCaja          : Id de la caja chica
	 * @param fecha           : Fecha del gasto
	 * @param valor           : Valor del gasto (mayor a cero)
	 * @param descripcion     : Concepto del gasto
	 * @param observacion     : Observación (obligatoria)
	 * @param idProducto      : Id del producto CXP que clasifica el gasto (obligatorio)
	 * @param idTitular       : Id del beneficiario/proveedor (opcional)
	 * @param numeroDocumento : Número del comprobante pagado (opcional)
	 * @param idUsuario       : Id del usuario que registra
	 * @return                : Movimiento registrado, con su asiento
	 * @throws Throwable      : Excepcion
	 */
	MovimientoCajaChica registrarGasto(Long idCaja, LocalDate fecha, Double valor, String descripcion,
			String observacion, Long idProducto, Long idTitular, String numeroDocumento, Long idUsuario)
			throws Throwable;

	/**
	 * Registra la reposición del fondo desde una cuenta bancaria: crea el
	 * movimiento (tipo REPOSICION, sin asiento) y lo envía al circuito de pagos
	 * como origen externo. El pago nace CONFIRMADO (débito automático o
	 * cheque) o REGISTRADO (transferencia), igual que cualquier otro pago.
	 * @param idCaja                 : Id de la caja chica
	 * @param valor                  : Valor de la reposición (no debe superar fondo - saldo)
	 * @param idCuentaBancariaOrigen : Id de la cuenta bancaria propia de origen
	 * @param formaPago              : Forma de pago (ver FormaPagoProgramado), null = inferida
	 * @param debitoAutomatico       : true si el banco ya debitó la cuenta
	 * @param referencia             : Referencia del débito (opcional)
	 * @param fecha                  : Fecha programada / del débito
	 * @param descripcion            : Concepto de la reposición
	 * @param idUsuario              : Id del usuario que registra
	 * @return                       : Mapa con idMovimiento, idPago, estadoPago, numeroCheque
	 * @throws Throwable             : Excepcion
	 */
	Map<String, Object> registrarReposicion(Long idCaja, Double valor, Long idCuentaBancariaOrigen,
			Long formaPago, boolean debitoAutomatico, String referencia, LocalDate fecha,
			String descripcion, Long idUsuario) throws Throwable;

	/**
	 * Igual que {@link #registrarReposicion}, pero para la apertura (fondo
	 * inicial) de una caja chica nueva pagado desde una cuenta bancaria — no
	 * confundir con la apertura MIGRADA de {@code CajaChicaService.registrar}
	 * (esa no pasa por el banco ni genera asiento). Exige que el saldo actual
	 * de la caja sea cero.
	 */
	Map<String, Object> registrarApertura(Long idCaja, Double valor, Long idCuentaBancariaOrigen,
			Long formaPago, boolean debitoAutomatico, String referencia, LocalDate fecha,
			String descripcion, Long idUsuario) throws Throwable;

	/**
	 * Anula un gasto de caja chica: sólo tipo GASTO y estado ACTIVO, y sólo si
	 * no quedó incluido en un cierre CERRADO. Anula el asiento contable.
	 * Los movimientos de apertura/reposición no se anulan aquí: hay que
	 * revertir su pago programado.
	 * @param idMovimiento : Id del movimiento
	 * @param motivo       : Motivo de la anulación
	 * @param idUsuario    : Id del usuario que anula
	 * @throws Throwable   : Excepcion
	 */
	void anularGasto(Long idMovimiento, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Listado de movimientos de una caja, con filtros opcionales.
	 * @param idCaja : Id de la caja chica
	 * @param desde  : Fecha desde (opcional)
	 * @param hasta  : Fecha hasta (opcional)
	 * @param tipo   : Tipo (rubro TipoMovimientoCajaChica, opcional)
	 * @param estado : Estado (rubro EstadoMovimientoCajaChica, opcional)
	 * @return       : Movimientos ordenados por fecha, código
	 * @throws Throwable : Excepcion
	 */
	List<MovimientoCajaChica> listar(Long idCaja, LocalDate desde, LocalDate hasta, Long tipo, Long estado)
			throws Throwable;

}
