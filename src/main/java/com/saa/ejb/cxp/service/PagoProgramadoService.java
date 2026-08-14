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
 *
 * Los pagos por DÉBITO AUTOMÁTICO no recorren ese circuito: el banco ya debitó
 * la cuenta, así que no se aprueban ni se incluyen en ningún archivo. En el
 * paso 1 quedan CONFIRMADOS y ahí mismo abonan la factura y generan su asiento
 * y su movimiento bancario.
 */
@Local
public interface PagoProgramadoService extends EntityService<PagoProgramado> {

	/**
	 * Registra un pago a un proveedor sobre una factura de compra.
	 * Si el pago es por débito automático el banco ya movió el dinero: el pago
	 * nace CONFIRMADO y en la misma transacción se abona la factura y se generan
	 * el asiento contable y el movimiento bancario.
	 * @param idFacturaCompra          : Id de la factura a pagar
	 * @param idCuentaBancariaOrigen   : Id de la cuenta bancaria propia (TSR.CNBC)
	 * @param idCuentaDestinoTitular   : Id de la cuenta del proveedor (TSR.CTBN)
	 * @param valor                    : Valor a transferir
	 * @param fechaProgramada          : Fecha programada, o fecha del débito (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa                : Id de la empresa
	 * @param idUsuario                : Id del usuario que registra
	 * @param observacion              : Observación del pago
	 * @param debitoAutomatico         : true si el banco ya debitó la cuenta por convenio
	 * @param referencia               : Referencia del débito (nota de débito, convenio, etc.)
	 * @return                         : Mapa con exito, mensaje, pago y saldos de la factura;
	 *                                   en el débito automático además aplicacion y asiento
	 * @throws Throwable               : Excepcion
	 */
	Map<String, Object> registrarPago(Long idFacturaCompra, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Double valor, String fechaProgramada, Long idEmpresa,
			Long idUsuario, String observacion, boolean debitoAutomatico, String referencia)
			throws Throwable;

	/**
	 * Registra el pago de un egreso de tesorería sin documento físico
	 * (TSR.EGRS). El pago toma del egreso la empresa, el titular, el valor y la
	 * fecha, y entra al mismo circuito que los pagos de facturas: aparece en el
	 * listado de pagos a realizar y sigue lote → archivo → confirmación. Al
	 * confirmarse genera el asiento (DEBE cuenta del grupo del producto /
	 * HABER banco), el movimiento bancario, y deja el egreso como Pagado.
	 * Con débito automático todo eso ocurre en esta misma llamada.
	 * @param idEgreso               : Id del egreso a pagar (debe estar Pendiente)
	 * @param idCuentaBancariaOrigen : Id de la cuenta bancaria propia (TSR.CNBC)
	 * @param idCuentaDestinoTitular : Id de la cuenta del beneficiario (TSR.CTBN);
	 *                                 obligatoria salvo débito automático
	 * @param idUsuario              : Id del usuario que registra
	 * @param debitoAutomatico       : true si el banco ya debitó la cuenta
	 * @param referencia             : Referencia del débito (opcional)
	 * @return                       : Mapa con exito, mensaje y pago; en débito
	 *                                 automático además asiento
	 * @throws Throwable             : Excepcion
	 */
	Map<String, Object> registrarPagoDeEgreso(Long idEgreso, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia) throws Throwable;

	/**
	 * Registra el pago de un anticipo a proveedor (PGS.ANTP). El pago toma del
	 * anticipo la empresa, el proveedor, el valor y la fecha, y entra al mismo
	 * circuito que los pagos de facturas: aparece en el listado de pagos a
	 * realizar y sigue lote → archivo → confirmación. Al confirmarse genera el
	 * asiento de ANTICIPO (DEBE cuenta de anticipos del proveedor / HABER
	 * banco, TipoAsientos.ANTICIPOS_PROVEEDOR — no el de egreso), el movimiento
	 * bancario, acredita el saldo de anticipos del proveedor y deja el anticipo
	 * como Confirmado. Con débito automático todo eso ocurre en esta misma
	 * llamada.
	 * @param idAnticipo             : Id del anticipo a pagar (debe estar Ingresado)
	 * @param idCuentaBancariaOrigen : Id de la cuenta bancaria propia (TSR.CNBC)
	 * @param idCuentaDestinoTitular : Id de la cuenta del proveedor (TSR.CTBN);
	 *                                 obligatoria salvo débito automático
	 * @param idUsuario              : Id del usuario que registra
	 * @param debitoAutomatico       : true si el banco ya debitó la cuenta
	 * @param referencia             : Referencia del débito (opcional)
	 * @return                       : Mapa con exito, mensaje y pago; en débito
	 *                                 automático además asiento
	 * @throws Throwable             : Excepcion
	 */
	Map<String, Object> registrarPagoDeAnticipo(Long idAnticipo, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia) throws Throwable;

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
	 * Confirma manualmente uno o varios pagos, como si hubiera llegado la
	 * respuesta del banco. Produce exactamente el mismo efecto contable que
	 * procesarRespuestaBanco sobre un pago confirmado: aplicación de pago (o
	 * asiento de egreso), asiento contable y movimiento bancario.
	 *
	 * Existe porque la entidad financiera todavía no entrega el archivo de
	 * respuesta: mientras tanto la conciliación se hace contra el estado de
	 * cuenta y se confirma a mano.
	 *
	 * @param idsPagos   : Ids de los pagos a confirmar (Registrado o En archivo)
	 * @param referencia : Referencia o número de transacción del banco (opcional)
	 * @param fechaPago  : Fecha real del pago en formato yyyy-MM-dd; si viene
	 *                     vacía se usa la fecha actual. Es la fecha del asiento.
	 * @param observacion: Nota que se agrega a la observación del pago (opcional)
	 * @param idUsuario  : Id del usuario que confirma
	 * @return           : Mapa con exito, confirmados y detalle de errores
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> confirmarPagosManual(List<Long> idsPagos, String referencia,
			String fechaPago, String observacion, Long idUsuario) throws Throwable;

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
