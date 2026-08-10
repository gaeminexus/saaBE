package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.PagoProgramado;

import jakarta.ejb.Local;

/**
 * Pagos a proveedores por transferencia bancaria (CXP).
 *
 * Flujo completo:
 *   1. registrarPago      → el pago queda REGISTRADO y aparece en el listado
 *   2. generarLote        → los pagos seleccionados pasan a EN_ARCHIVO y se
 *                           genera el archivo para la entidad financiera
 *                           (seleccionar un pago para el lote equivale a aprobarlo)
 *   3. procesarRespuestaBanco → los CONFIRMADOS generan la aplicación de pago,
 *                           el asiento contable y el movimiento bancario; los
 *                           RECHAZADOS quedan en seguimiento
 *
 * Mientras un pago no esté confirmado por el banco no se registra nada en
 * contabilidad ni en movimientos bancarios.
 */
@Local
public interface PagoProgramadoService extends EntityService<PagoProgramado> {

	/**
	 * Registra un pago a un proveedor sobre una factura de compra.
	 * @param idFacturaCompra          : Id de la factura a pagar
	 * @param idCuentaBancariaOrigen   : Id de la cuenta bancaria propia (TSR.CNBC)
	 * @param idCuentaDestinoTitular   : Id de la cuenta del proveedor (TSR.CTBN)
	 * @param valor                    : Valor a transferir
	 * @param fechaProgramada          : Fecha programada (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa                : Id de la empresa
	 * @param idUsuario                : Id del usuario que registra
	 * @param observacion              : Observación del pago
	 * @return                         : Mapa con exito, mensaje, pago y saldos de la factura
	 * @throws Throwable               : Excepcion
	 */
	Map<String, Object> registrarPago(Long idFacturaCompra, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Double valor, String fechaProgramada, Long idEmpresa,
			Long idUsuario, String observacion) throws Throwable;

	/**
	 * Lista los pagos de una empresa para la pantalla de selección.
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : Estado a filtrar, null para todos
	 * @param idTitular  : Id del proveedor, null para todos
	 * @return           : Listado de pagos
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> listar(Long idEmpresa, Long estado, Long idTitular) throws Throwable;

	/**
	 * Agrupa los pagos seleccionados en un lote y genera el archivo para el banco.
	 * Seleccionar un pago aquí equivale a aprobarlo: queda registrado el usuario
	 * que generó el lote.
	 * @param idsPagos       : Ids de los pagos seleccionados (deben estar REGISTRADOS)
	 * @param idCuentaOrigen : Id de la cuenta bancaria propia desde la que se paga
	 * @param idEmpresa      : Id de la empresa
	 * @param idUsuario      : Id del usuario que genera el lote
	 * @return               : Mapa con exito, mensaje, idLote, nombreArchivo y contenido
	 * @throws Throwable     : Excepcion
	 */
	Map<String, Object> generarLote(List<Long> idsPagos, Long idCuentaOrigen, Long idEmpresa,
			Long idUsuario) throws Throwable;

	/**
	 * Devuelve el contenido del archivo de un lote ya generado, para descargarlo
	 * de nuevo.
	 * @param idLote     : Id del lote
	 * @return           : Mapa con nombreArchivo y contenido
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> obtenerArchivoLote(Long idLote) throws Throwable;

	/**
	 * Procesa el archivo de respuesta del banco: confirma o rechaza cada pago del
	 * lote. Los confirmados generan aplicación de pago, asiento y movimiento
	 * bancario.
	 * @param idLote           : Id del lote enviado al banco
	 * @param archivoRespuesta : Contenido del archivo de respuesta
	 * @param idUsuario        : Id del usuario que procesa la respuesta
	 * @return                 : Mapa con exito, confirmados, rechazados y detalle de errores
	 * @throws Throwable       : Excepcion
	 */
	Map<String, Object> procesarRespuestaBanco(Long idLote, byte[] archivoRespuesta, Long idUsuario)
			throws Throwable;

	/**
	 * Anula un pago que aún no fue confirmado por el banco.
	 * @param idPago     : Id del pago
	 * @param motivo     : Motivo de la anulación
	 * @param idUsuario  : Id del usuario que anula
	 * @return           : Mapa con exito y mensaje
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> anularPago(Long idPago, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Reversa un pago ya confirmado: reversa su aplicación, anula el asiento y el
	 * movimiento bancario, y deja el pago como rechazado para su seguimiento.
	 * @param idPago     : Id del pago confirmado
	 * @param motivo     : Motivo de la reversión
	 * @param idUsuario  : Id del usuario que reversa
	 * @return           : Mapa con exito y mensaje
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> revertirPagoConfirmado(Long idPago, String motivo, Long idUsuario)
			throws Throwable;
}
