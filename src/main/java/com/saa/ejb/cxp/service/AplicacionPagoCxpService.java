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
import com.saa.model.tsr.MovimientoCajaChica;

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
	 * Cruza anticipos del proveedor contra una factura de compra O una liquidación
	 * de compra, indicando solo el monto total: el valor se reparte entre los
	 * anticipos con saldo del mas antiguo al mas nuevo (FIFO) y se genera una
	 * aplicacion por cada uno. Para elegir a mano de que anticipos sale el dinero,
	 * usar {@link #aplicarAnticipos(Long, Long, List, String, Long, Long, String)}.
	 * <p>
	 * <b>Exactamente uno de {@code idFacturaCompra}/{@code idLiquidacionCompra}</b>
	 * debe venir poblado (docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md
	 * §2.1 y §4.1): ninguno de los dos, o los dos, es {@code IncomeException}. No
	 * se elige uno por el llamador — un cliente que manda los dos está confundido
	 * y hay que decírselo, no adivinar.
	 * @param idFacturaCompra      : Id de la factura de compra, o null si se cruza contra liquidación
	 * @param idLiquidacionCompra  : Id de la liquidación de compra, o null si se cruza contra factura
	 * @param valor                : Valor de anticipo a cruzar
	 * @param fechaAplicacion      : Fecha de la aplicación (formato yyyy-MM-dd, null = hoy)
	 * @param idEmpresa            : Id de la empresa contable
	 * @param idUsuario            : Id del usuario que registra
	 * @param observacion          : Observación de la aplicación
	 * @return                     : Mapa con exito, mensaje, aplicacion, asiento y saldos
	 * @throws Throwable           : Excepcion
	 */
	Map<String, Object> aplicarAnticipo(Long idFacturaCompra, Long idLiquidacionCompra, Double valor,
			String fechaAplicacion, Long idEmpresa, Long idUsuario, String observacion) throws Throwable;

	/**
	 * Cruza anticipos ESPECÍFICOS del proveedor contra una factura de compra O una
	 * liquidación de compra. Ver la nota de exclusividad en
	 * {@link #aplicarAnticipo(Long, Long, Double, String, Long, Long, String)}.
	 * <p>
	 * Cada línea indica de qué anticipo sale el dinero y cuánto, y genera su
	 * propia aplicación con su propio asiento. Es lo que permite deshacer
	 * exactamente los abonos de un anticipo cuando se lo anula, en vez de
	 * estimarlos contra el saldo global del proveedor.
	 * @param idFacturaCompra      : Id de la factura de compra, o null si se cruza contra liquidación
	 * @param idLiquidacionCompra  : Id de la liquidación de compra, o null si se cruza contra factura
	 * @param detalles             : Líneas del cruce: [{idAnticipo, valor}, ...]
	 * @param fechaAplicacion      : Fecha de la aplicación (yyyy-MM-dd, null = hoy)
	 * @param idEmpresa            : Id de la empresa contable
	 * @param idUsuario            : Id del usuario que registra
	 * @param observacion          : Observación de la aplicación
	 * @return                     : Mapa con exito, mensaje, lineas (una por anticipo),
	 *                               totalCruzado, saldoAnticipos y los saldos del documento
	 * @throws Throwable           : Excepcion si un anticipo no existe, no es del proveedor,
	 *                               no está confirmado o no tiene saldo suficiente
	 */
	Map<String, Object> aplicarAnticipos(Long idFacturaCompra, Long idLiquidacionCompra,
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

	/**
	 * Igual que {@link #aplicarPagoTransferencia(PagoProgramado, Long)}, pero con
	 * control sobre si este pago emite su propio {@code MovimientoBanco} de cheque.
	 * Lo usa {@code PagoProgramadoServiceImpl.aprobar} cuando el cheque respalda
	 * varios pagos (agruparEnUnCheque=true): el movimiento se emite una sola vez
	 * por el total del grupo, no una vez por pago/factura. Sin cheque el parámetro
	 * no tiene efecto.
	 * @param pago                  : Pago programado ya ejecutado por el banco
	 * @param idUsuario             : Id del usuario que registra o procesa el pago
	 * @param emitirMovimientoCheque: false para suprimir el MovimientoBanco individual
	 *                                de este pago cuando tiene cheque (cheque agrupado)
	 * @return                      : Aplicación creada
	 * @throws Throwable            : Excepcion
	 */
	AplicacionPagoCxp aplicarPagoTransferencia(PagoProgramado pago, Long idUsuario,
			boolean emitirMovimientoCheque) throws Throwable;

	// ── Aplicación desde caja chica ──────────────────────────────────────────

	/**
	 * Registra el pago (parcial o total) de una factura de compra O una
	 * liquidación de compra con un gasto de caja chica, en vez de reconocer el
	 * gasto contra la cuenta del producto — evita reconocer dos veces un gasto
	 * que ya se reconoció al registrar el documento
	 * (docs/logica-negocio/tsr/PLAN-GASTO-CAJA-CHICA-PAGA-FACTURA.md §2).
	 * <p>
	 * <b>Exactamente uno de {@code idFacturaCompra}/{@code idLiquidacionCompra}</b>
	 * debe venir poblado — ver la nota de exclusividad en
	 * {@link #aplicarAnticipo(Long, Long, Double, String, Long, Long, String)}.
	 * <p>
	 * Revalida en el servidor que el titular del documento sea el beneficiario
	 * del gasto: no alcanza con que el combo del frontend ya haya filtrado.
	 * <p>
	 * El monto aplicado es el valor íntegro del gasto: el diseño es un gasto
	 * por factura (1:1, D2 del documento de diseño), así que no existe un monto
	 * de aplicación distinto — a diferencia del cruce de anticipos, acá no hay
	 * reparto entre varios orígenes.
	 * <p>
	 * No valida lo comprometido en la bandeja de pagos ({@code PagoProgramado}
	 * en estados no confirmados): esa validación es responsabilidad del
	 * llamador, antes de registrar el gasto ({@code PagoProgramadoService
	 * #validaValorContraSaldo}) — acá sólo se valida contra el saldo de
	 * aplicaciones ya activas del documento.
	 * @param idFacturaCompra     : Id de la factura de compra, o null si se paga una liquidación
	 * @param idLiquidacionCompra : Id de la liquidación de compra, o null si se paga una factura
	 * @param movimiento          : Gasto de caja chica ya persistido que paga el documento
	 * @param idPlanCuentaCaja    : Id de la cuenta contable de la caja (CajaChica.planCuenta)
	 * @param idEmpresa           : Id de la empresa contable
	 * @param idUsuario           : Id del usuario que registra
	 * @return                    : Aplicación creada, con su asiento
	 * @throws Throwable          : Excepcion si el documento no existe, no es del mismo
	 *                              proveedor que el gasto, o el monto supera su saldo
	 */
	AplicacionPagoCxp aplicarDesdeCajaChica(Long idFacturaCompra, Long idLiquidacionCompra,
			MovimientoCajaChica movimiento, Long idPlanCuentaCaja, Long idEmpresa, Long idUsuario)
			throws Throwable;

	// ── Reversión ────────────────────────────────────────────────────────────

	/**
	 * Reversa una aplicación: la marca como reversada, anula su asiento,
	 * devuelve el saldo de anticipos si aplica y anula el movimiento bancario.
	 * El estado de pago de la factura se recalcula y graba en el backend.
	 * <p>
	 * ⛔ Rechaza reversar una aplicación de origen caja chica (APLPTDPG =
	 * CAJA_CHICA): el camino válido para esas es anular el gasto
	 * ({@code MovimientoCajaChicaService#anularGasto}), que además valida
	 * cosas que este método no conoce (que el gasto no esté en un cierre, que
	 * no haya un BORRADOR abierto que lo cubra). Ver
	 * docs/logica-negocio/tsr/API-GASTO-CAJA-CHICA.md §3.
	 * @param idAplicacion : Id de la aplicación a reversar
	 * @param motivo       : Motivo de la reversión
	 * @param idUsuario    : Id del usuario que reversa
	 * @return             : Mapa con exito y mensaje
	 * @throws Throwable   : Excepcion, o IncomeException si el origen es caja chica
	 */
	Map<String, Object> revertirAplicacion(Long idAplicacion, String motivo, Long idUsuario)
			throws Throwable;

	/**
	 * Igual que {@link #revertirAplicacion(Long, String, Long)}, pero SIN el
	 * bloqueo de origen caja chica. Es el único camino válido para reversar una
	 * aplicación que vino de un gasto de caja chica, y lo usa exclusivamente
	 * {@code MovimientoCajaChicaServiceImpl#anularGasto} al anular el gasto que
	 * la originó, después de sus propias validaciones (cierre, borrador). No
	 * exponer este método por otro camino (REST, otra pantalla): el bloqueo de
	 * {@link #revertirAplicacion(Long, String, Long)} existe justamente para
	 * forzar a pasar por ahí.
	 * @param idAplicacion : Id de la aplicación a reversar
	 * @param motivo       : Motivo de la reversión
	 * @param idUsuario    : Id del usuario que reversa
	 * @return             : Mapa con exito y mensaje
	 * @throws Throwable   : Excepcion
	 */
	Map<String, Object> revertirAplicacionOrigenCajaChica(Long idAplicacion, String motivo, Long idUsuario)
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
	 * Recupera el historial de aplicaciones de una liquidación de compra.
	 * Equivalente de {@link #consultarPorFactura(Long, boolean)}.
	 * @param idLiquidacionCompra : Id de la liquidación de compra
	 * @param soloActivas         : true devuelve solo las activas
	 * @return                    : Listado de aplicaciones
	 * @throws Throwable          : Excepcion
	 */
	List<AplicacionPagoCxp> consultarPorLiquidacion(Long idLiquidacionCompra, boolean soloActivas)
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
	 * Calcula el saldo pendiente de una liquidación de compra. Equivalente de
	 * {@link #saldoFactura(Long)} para el documento afectado alternativo.
	 * <p>
	 * ⛔ No confundir con {@code GET /aplp/saldo/{id}} (que resuelve por
	 * {@code FacturaCompra}): {@code FCTC} y {@code LQCC} usan IDENTITY con
	 * numeraciones independientes, así que pasarle un id de liquidación a ese
	 * endpoint devolvería los datos de una factura ajena que coincida en número,
	 * sin ningún error (docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md §4.2).
	 * @param idLiquidacionCompra : Id de la liquidación de compra
	 * @return                    : Mapa con total, totalAplicado, saldoPendiente y estadoPago
	 * @throws Throwable          : Excepcion
	 */
	Map<String, Object> saldoLiquidacion(Long idLiquidacionCompra) throws Throwable;

	/**
	 * Recalcula y graba el estado de pago de una liquidación de compra a partir
	 * de sus aplicaciones activas. Equivalente de {@link #recalcularEstadoPago(Long)}.
	 * 1 = Pendiente, 2 = Pagada parcialmente, 3 = Pagada totalmente.
	 * @param idLiquidacionCompra : Id de la liquidación de compra
	 * @return                    : Nuevo estado de pago grabado
	 * @throws Throwable          : Excepcion
	 */
	Long recalcularEstadoPagoLiquidacion(Long idLiquidacionCompra) throws Throwable;

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
