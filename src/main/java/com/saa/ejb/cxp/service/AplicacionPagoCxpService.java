package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.PagoProgramado;

import jakarta.ejb.Local;

/**
 * Aplicaciones de pago sobre facturas de compra (CXP).
 *
 * Cada abono a una factura de compra se registra como una AplicacionPagoCxp, de
 * modo que el saldo de la factura y los saldos contables reflejen siempre lo
 * mismo.
 *
 * Los métodos aplicar* de retención y notas se invocan DENTRO de la misma
 * transacción en la que se genera el asiento contable del documento: si la
 * aplicación falla, el asiento no debe quedar activo.
 */
@Local
public interface AplicacionPagoCxpService extends EntityService<AplicacionPagoCxp> {

	// ── Aplicaciones automáticas (junto con el asiento del documento) ────────

	/**
	 * Registra el abono a la factura de compra que produce una retención V2
	 * emitida al proveedor.
	 * @param retencion  : Retención V2 emitida
	 * @param asiento    : Asiento contable de la retención
	 * @param idEmpresa  : Id de la empresa contable
	 * @param usuario    : Nombre del usuario que registra
	 * @return           : Aplicación creada
	 * @throws Throwable : Excepcion
	 */
	AplicacionPagoCxp aplicarRetencionEmitida(RetencionV2 retencion, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	/**
	 * Registra el abono a la factura de compra que produce una nota de crédito
	 * recibida del proveedor.
	 * @param notaCredito : Nota de crédito recibida
	 * @param asiento     : Asiento contable de la nota de crédito
	 * @param idEmpresa   : Id de la empresa contable
	 * @param usuario     : Nombre del usuario que registra
	 * @return            : Aplicación creada
	 * @throws Throwable  : Excepcion
	 */
	AplicacionPagoCxp aplicarNotaCredito(NotaCreditoCompra notaCredito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	/**
	 * Registra el INCREMENTO del saldo de la factura de compra que produce una
	 * nota de débito recibida del proveedor. El monto se guarda NEGATIVO.
	 * @param notaDebito : Nota de débito recibida
	 * @param asiento    : Asiento contable de la nota de débito
	 * @param idEmpresa  : Id de la empresa contable
	 * @param usuario    : Nombre del usuario que registra
	 * @return           : Aplicación creada
	 * @throws Throwable : Excepcion
	 */
	AplicacionPagoCxp aplicarNotaDebito(NotaDebitoCompra notaDebito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	// ── Aplicaciones desde la pantalla de tesorería ──────────────────────────

	/**
	 * Cruza anticipos del proveedor contra una factura de compra indicando solo
	 * el monto total: el valor se reparte entre los anticipos con saldo del mas
	 * antiguo al mas nuevo (FIFO) y se genera una aplicacion por cada uno.
	 * Para elegir a mano de que anticipos sale el dinero, usar
	 * {@link #aplicarAnticipos(Long, List, String, Long, Long, String)}.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @param valor           : Valor de anticipo a cruzar
	 * @param fechaAplicacion : Fecha de la aplicación (formato yyyy-MM-dd, null = hoy)
	 * @param idEmpresa       : Id de la empresa contable
	 * @param idUsuario       : Id del usuario que registra
	 * @param observacion     : Observación de la aplicación
	 * @return                : Mapa con exito, mensaje, aplicacion, asiento y saldos
	 * @throws Throwable      : Excepcion
	 */
	Map<String, Object> aplicarAnticipo(Long idFacturaCompra, Double valor, String fechaAplicacion,
			Long idEmpresa, Long idUsuario, String observacion) throws Throwable;

	/**
	 * Cruza anticipos ESPECÍFICOS del proveedor contra una factura de compra.
	 * <p>
	 * Cada línea indica de qué anticipo sale el dinero y cuánto, y genera su
	 * propia aplicación con su propio asiento. Es lo que permite deshacer
	 * exactamente los abonos de un anticipo cuando se lo anula, en vez de
	 * estimarlos contra el saldo global del proveedor.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @param detalles        : Líneas del cruce: [{idAnticipo, valor}, ...]
	 * @param fechaAplicacion : Fecha de la aplicación (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa       : Id de la empresa contable
	 * @param idUsuario       : Id del usuario que registra
	 * @param observacion     : Observación de la aplicación
	 * @return                : Mapa con exito, mensaje, lineas (una por anticipo),
	 *                          totalCruzado, saldoAnticipos y los saldos de la factura
	 * @throws Throwable      : Excepcion si un anticipo no existe, no es del proveedor,
	 *                          no está confirmado o no tiene saldo suficiente
	 */
	Map<String, Object> aplicarAnticipos(Long idFacturaCompra,
			List<Map<String, Object>> detalles, String fechaAplicacion, Long idEmpresa,
			Long idUsuario, String observacion) throws Throwable;

	/**
	 * Registra el abono a la factura que produce un pago ya ejecutado por el
	 * banco. Genera el asiento y el movimiento bancario.
	 * Sirve a los dos orígenes: la transferencia que el banco confirma en el
	 * archivo de respuesta, y el débito automático (PGTRDBAT=1), que se aplica
	 * en el mismo momento en que se registra el pago. La diferencia está en la
	 * forma de pago de la aplicación (2=Transferencia / 4=Débito automático) y
	 * en el banco que se guarda; las cuentas del asiento son las mismas.
	 * @param pago       : Pago programado ya ejecutado por el banco
	 * @param idUsuario  : Id del usuario que registra o procesa el pago
	 * @return           : Aplicación creada
	 * @throws Throwable : Excepcion
	 */
	AplicacionPagoCxp aplicarPagoTransferencia(PagoProgramado pago, Long idUsuario) throws Throwable;

	// ── Reversión ────────────────────────────────────────────────────────────

	/**
	 * Reversa una aplicación: la marca como reversada, anula su asiento,
	 * devuelve el saldo de anticipos si aplica y anula el movimiento bancario.
	 * El estado de pago de la factura se recalcula y graba en el backend.
	 * @param idAplicacion : Id de la aplicación a reversar
	 * @param motivo       : Motivo de la reversión
	 * @param idUsuario    : Id del usuario que reversa
	 * @return             : Mapa con exito y mensaje
	 * @throws Throwable   : Excepcion
	 */
	Map<String, Object> revertirAplicacion(Long idAplicacion, String motivo, Long idUsuario)
			throws Throwable;

	/**
	 * Reversa todas las aplicaciones activas originadas por un documento, al
	 * anularlo.
	 * @param tipoDocumento : RETENCION, RETENCION_V2, NOTA_CREDITO o NOTA_DEBITO
	 * @param idDocumento   : Id del documento anulado
	 * @param motivo        : Motivo de la anulación
	 * @param idUsuario     : Id del usuario que anula
	 * @return              : Cantidad de aplicaciones reversadas
	 * @throws Throwable    : Excepcion
	 */
	int revertirAplicacionesDeDocumento(String tipoDocumento, Long idDocumento, String motivo,
			Long idUsuario) throws Throwable;

	/**
	 * Reversa y elimina físicamente las aplicaciones de un documento. Se usa en
	 * el proceso de reversión de la carga de documentos de CXP, que borra los
	 * registros en lugar de anularlos.
	 * @param tipoDocumento : RETENCION, RETENCION_V2, NOTA_CREDITO o NOTA_DEBITO
	 * @param idDocumento   : Id del documento
	 * @return              : Cantidad de aplicaciones eliminadas
	 * @throws Throwable    : Excepcion
	 */
	int eliminarAplicacionesDeDocumento(String tipoDocumento, Long idDocumento) throws Throwable;

	// ── Consultas ────────────────────────────────────────────────────────────

	/**
	 * Recupera el historial de aplicaciones de una factura de compra.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @param soloActivas     : true devuelve solo las activas
	 * @return                : Listado de aplicaciones
	 * @throws Throwable      : Excepcion
	 */
	List<AplicacionPagoCxp> consultarPorFactura(Long idFacturaCompra, boolean soloActivas)
			throws Throwable;

	/**
	 * Calcula el saldo pendiente de una factura de compra.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @return                : Mapa con total, totalAplicado, saldoPendiente y estadoPago
	 * @throws Throwable      : Excepcion
	 */
	Map<String, Object> saldoFactura(Long idFacturaCompra) throws Throwable;

	/**
	 * Recalcula y graba el estado de pago de una factura de compra a partir de
	 * sus aplicaciones activas.
	 * 1 = Pendiente, 2 = Pagada parcialmente, 3 = Pagada totalmente.
	 * Se invoca automáticamente al crear o reversar una aplicación; también
	 * puede usarse para reprocesar facturas cuyo estado haya quedado desfasado.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @return                : Nuevo estado de pago grabado
	 * @throws Throwable      : Excepcion
	 */
	Long recalcularEstadoPago(Long idFacturaCompra) throws Throwable;

	/**
	 * Localiza la factura de compra a la que se refiere un documento por su
	 * número. Lanza excepción si no existe o si hay más de una coincidencia.
	 * @param numeroDocumento : Número del documento referenciado
	 * @param idTitular       : Id del proveedor
	 * @param idEmpresa       : Id de la empresa
	 * @return                : Factura de compra encontrada
	 * @throws Throwable      : Excepcion si no hay coincidencia o hay ambigüedad
	 */
	FacturaCompra resolverFacturaCompraPorNumero(String numeroDocumento, Long idTitular,
			Long idEmpresa) throws Throwable;
}
