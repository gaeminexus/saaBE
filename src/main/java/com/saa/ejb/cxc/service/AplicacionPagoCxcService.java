package com.saa.ejb.cxc.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.AplicacionPagoCxc;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.NotaCredito;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxp.RetencionCompra;
import com.saa.model.cxp.RetencionCompraV2;

import jakarta.ejb.Local;

/**
 * Aplicaciones de cobro sobre facturas de venta (CXC).
 *
 * Cada abono a una factura de venta se registra como una AplicacionPagoCxc, de
 * modo que el saldo de la factura y los saldos contables reflejen siempre lo
 * mismo.
 *
 * Los métodos aplicar* de retención y notas se invocan DENTRO de la misma
 * transacción en la que se genera el asiento contable del documento: si la
 * aplicación falla, el asiento no debe quedar activo.
 */
@Local
public interface AplicacionPagoCxcService extends EntityService<AplicacionPagoCxc> {

	// ── Aplicaciones automáticas (junto con el asiento del documento) ────────

	/**
	 * Registra el abono a la factura de venta que produce una retención v1 que
	 * nos emite el cliente (llega por la carga de documentos del SRI).
	 * @param retencion  : Retención recibida
	 * @param asiento    : Asiento contable de la retención
	 * @param idEmpresa  : Id de la empresa contable
	 * @param usuario    : Nombre del usuario que registra
	 * @return           : Aplicación creada
	 * @throws Throwable : Excepcion
	 */
	AplicacionPagoCxc aplicarRetencionRecibida(RetencionCompra retencion, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	/**
	 * Registra el abono a la factura de venta que produce una retención v2 que
	 * nos emite el cliente.
	 * @param retencion  : Retención V2 recibida
	 * @param asiento    : Asiento contable de la retención
	 * @param idEmpresa  : Id de la empresa contable
	 * @param usuario    : Nombre del usuario que registra
	 * @return           : Aplicación creada
	 * @throws Throwable : Excepcion
	 */
	AplicacionPagoCxc aplicarRetencionRecibidaV2(RetencionCompraV2 retencion, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	/**
	 * Registra el abono a la factura de venta que produce una nota de crédito
	 * emitida al cliente.
	 * @param notaCredito : Nota de crédito emitida
	 * @param asiento     : Asiento contable de la nota de crédito
	 * @param idEmpresa   : Id de la empresa contable
	 * @param usuario     : Nombre del usuario que registra
	 * @return            : Aplicación creada
	 * @throws Throwable  : Excepcion
	 */
	AplicacionPagoCxc aplicarNotaCredito(NotaCredito notaCredito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	/**
	 * Registra el INCREMENTO del saldo de la factura de venta que produce una
	 * nota de débito emitida al cliente. El monto se guarda NEGATIVO.
	 * @param notaDebito : Nota de débito emitida
	 * @param asiento    : Asiento contable de la nota de débito
	 * @param idEmpresa  : Id de la empresa contable
	 * @param usuario    : Nombre del usuario que registra
	 * @return           : Aplicación creada
	 * @throws Throwable : Excepcion
	 */
	AplicacionPagoCxc aplicarNotaDebito(NotaDebito notaDebito, Asiento asiento,
			Long idEmpresa, String usuario) throws Throwable;

	// ── Aplicaciones desde la pantalla de tesorería ──────────────────────────

	/**
	 * Cruza anticipos del cliente contra una factura de venta indicando solo el
	 * monto total: el valor se reparte entre los anticipos con saldo del más
	 * antiguo al más nuevo (FIFO) y se genera una aplicación por cada uno.
	 * Para elegir a mano de qué anticipos sale el dinero, usar
	 * {@link #aplicarAnticipos(Long, List, String, Long, Long, String)}.
	 * @param idFactura       : Id de la factura de venta
	 * @param valor           : Valor de anticipo a cruzar
	 * @param fechaAplicacion : Fecha de la aplicación (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa       : Id de la empresa contable
	 * @param idUsuario       : Id del usuario que registra
	 * @param observacion     : Observación de la aplicación
	 * @return                : Mapa con exito, mensaje, aplicacion, asiento y saldos
	 * @throws Throwable      : Excepcion
	 */
	Map<String, Object> aplicarAnticipo(Long idFactura, Double valor, String fechaAplicacion,
			Long idEmpresa, Long idUsuario, String observacion) throws Throwable;

	/**
	 * Cruza anticipos ESPECÍFICOS del cliente contra una factura de venta.
	 * <p>
	 * Cada línea indica de qué anticipo sale el dinero y cuánto, y genera su
	 * propia aplicación con su propio asiento. Es lo que permite deshacer
	 * exactamente los abonos de un anticipo cuando se lo anula, en vez de
	 * estimarlos contra el saldo global del cliente.
	 * @param idFactura       : Id de la factura de venta
	 * @param detalles        : Líneas del cruce: [{idAnticipo, valor}, ...]
	 * @param fechaAplicacion : Fecha de la aplicación (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa       : Id de la empresa contable
	 * @param idUsuario       : Id del usuario que registra
	 * @param observacion     : Observación de la aplicación
	 * @return                : Mapa con exito, mensaje, lineas (una por anticipo),
	 *                          totalCruzado, saldoAnticipos y los saldos de la factura
	 * @throws Throwable      : Excepcion si un anticipo no existe, no es del cliente,
	 *                          no está confirmado o no tiene saldo suficiente
	 */
	Map<String, Object> aplicarAnticipos(Long idFactura,
			List<Map<String, Object>> detalles, String fechaAplicacion, Long idEmpresa,
			Long idUsuario, String observacion) throws Throwable;

	/**
	 * Registra un cobro recibido del cliente por transferencia bancaria.
	 * Genera el asiento y el movimiento bancario de ingreso. Admite varios
	 * cobros parciales sobre la misma factura.
	 * @param idFactura         : Id de la factura de venta
	 * @param valor             : Valor recibido
	 * @param fechaCobro        : Fecha del cobro (yyyy-MM-dd, null = hoy)
	 * @param numeroTransferencia : Número de la transferencia recibida
	 * @param idCuentaBancaria  : Id de la cuenta bancaria propia donde se recibió
	 * @param idEmpresa         : Id de la empresa contable
	 * @param idUsuario         : Id del usuario que registra
	 * @param observacion       : Observación del cobro
	 * @return                  : Mapa con exito, mensaje, aplicacion, asiento y saldos
	 * @throws Throwable        : Excepcion
	 */
	Map<String, Object> aplicarCobroTransferencia(Long idFactura, Double valor, String fechaCobro,
			String numeroTransferencia, Long idCuentaBancaria, Long idEmpresa, Long idUsuario,
			String observacion) throws Throwable;

	// ── Reversión ────────────────────────────────────────────────────────────

	/**
	 * Reversa una aplicación: la marca como reversada, anula su asiento,
	 * devuelve el saldo de anticipos si aplica y anula el movimiento bancario.
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
	 * Reversa todas las aplicaciones activas de una factura de venta, al anularla.
	 * @param idFactura  : Id de la factura anulada
	 * @param motivo     : Motivo de la anulación
	 * @param idUsuario  : Id del usuario que anula
	 * @return           : Cantidad de aplicaciones reversadas
	 * @throws Throwable : Excepcion
	 */
	int revertirAplicacionesDeFactura(Long idFactura, String motivo, Long idUsuario)
			throws Throwable;

	/**
	 * Reversa y elimina físicamente las aplicaciones de un documento. Se usa en
	 * el proceso de reversión de la carga de documentos de CXP, que borra los
	 * registros en lugar de anularlos.
	 * @param tipoDocumento : RETENCION o RETENCION_V2
	 * @param idDocumento   : Id del documento
	 * @return              : Cantidad de aplicaciones eliminadas
	 * @throws Throwable    : Excepcion
	 */
	int eliminarAplicacionesDeDocumento(String tipoDocumento, Long idDocumento) throws Throwable;

	// ── Consultas ────────────────────────────────────────────────────────────

	/**
	 * Recupera el historial de aplicaciones de una factura de venta.
	 * @param idFactura   : Id de la factura de venta
	 * @param soloActivas : true devuelve solo las activas
	 * @return            : Listado de aplicaciones
	 * @throws Throwable  : Excepcion
	 */
	List<AplicacionPagoCxc> consultarPorFactura(Long idFactura, boolean soloActivas)
			throws Throwable;

	/**
	 * Calcula el saldo pendiente de una factura de venta.
	 * @param idFactura  : Id de la factura de venta
	 * @return           : Mapa con total, totalAplicado, saldoPendiente y estadoPago
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> saldoFactura(Long idFactura) throws Throwable;

	/**
	 * Recalcula y graba el estado de pago de una factura de venta a partir de sus
	 * aplicaciones activas.
	 * 1 = Pendiente, 2 = Pagada parcialmente, 3 = Pagada totalmente.
	 * @param idFactura  : Id de la factura de venta
	 * @return           : Nuevo estado de pago grabado
	 * @throws Throwable : Excepcion
	 */
	Long recalcularEstadoPago(Long idFactura) throws Throwable;

	/**
	 * Recalcula y graba el estado de pago de una liquidación de compra emitida.
	 * @param idLiquidacion : Id de la liquidación
	 * @return              : Nuevo estado de pago grabado
	 * @throws Throwable    : Excepcion
	 */
	Long recalcularEstadoPagoLiquidacion(Long idLiquidacion) throws Throwable;

	/**
	 * Localiza la factura de venta a la que se refiere un documento por su
	 * número. Lanza excepción si no existe o si hay más de una coincidencia.
	 * @param numeroDocumento : Número del documento referenciado
	 * @param idTitular       : Id del cliente, null si no se conoce
	 * @param idEmpresa       : Id de la empresa
	 * @return                : Factura de venta encontrada
	 * @throws Throwable      : Excepcion si no hay coincidencia o hay ambigüedad
	 */
	Factura resolverFacturaPorNumero(String numeroDocumento, Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * Listado de aplicaciones de cobro para pantalla de consulta, con
	 * filtros opcionales (null = sin filtrar por ese criterio).
	 * @param idEmpresa : Empresa contable
	 * @param idTitular : Cliente/proveedor
	 * @param desde     : Fecha de aplicación desde (inclusive)
	 * @param hasta     : Fecha de aplicación hasta (inclusive)
	 * @param formaPago : 1 Efectivo, 2 Transferencia, 3 Cheque, 4 Tarjeta
	 * @param estado    : 1 Activo, 2 Reversado
	 * @return          : Filas listas para pantalla (ver
	 *                    AplicacionPagoCxcDaoService.selectListado para el
	 *                    detalle de columnas de origen)
	 * @throws Throwable : Excepcion
	 */
	List<Map<String, Object>> listar(Long idEmpresa, Long idTitular, java.time.LocalDate desde,
			java.time.LocalDate hasta, Long formaPago, Long estado) throws Throwable;
}
