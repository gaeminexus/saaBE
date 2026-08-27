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
	 * Igual que {@link #registrarPago(Long, Long, Long, Double, String, Long, Long, String,
	 * boolean, String)}, con la forma de pago explícita (ver
	 * {@link com.saa.rubros.FormaPagoProgramado}). Con formaPago=CHEQUE (3) la cuenta de
	 * origen debe manejar chequera; el sistema asigna el siguiente cheque disponible, el
	 * pago nace CONFIRMADO y se contabiliza en la misma llamada, igual que el débito
	 * automático.
	 * @param formaPago : Forma de pago (1=Efectivo, 2=Transferencia, 3=Cheque,
	 *                    4=Débito automático); null equivale a la forma inferida de
	 *                    debitoAutomatico
	 */
	Map<String, Object> registrarPago(Long idFacturaCompra, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Double valor, String fechaProgramada, Long idEmpresa,
			Long idUsuario, String observacion, boolean debitoAutomatico, String referencia,
			Long formaPago) throws Throwable;

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
	 * Igual que {@link #registrarPagoDeEgreso(Long, Long, Long, Long, boolean, String)},
	 * con la forma de pago explícita. Ver {@link com.saa.rubros.FormaPagoProgramado}.
	 * @param formaPago : Forma de pago; null equivale a la forma inferida de debitoAutomatico
	 */
	Map<String, Object> registrarPagoDeEgreso(Long idEgreso, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia, Long formaPago) throws Throwable;

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
	 * Igual que {@link #registrarPagoDeAnticipo(Long, Long, Long, Long, boolean, String)},
	 * con la forma de pago explícita. Ver {@link com.saa.rubros.FormaPagoProgramado}.
	 * @param formaPago : Forma de pago; null equivale a la forma inferida de debitoAutomatico
	 */
	Map<String, Object> registrarPagoDeAnticipo(Long idAnticipo, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, Long idUsuario, boolean debitoAutomatico,
			String referencia, Long formaPago) throws Throwable;

	/**
	 * Registra un pago cuyo documento de ORIGEN vive en otro módulo del sistema.
	 *
	 * CXP guarda la etiqueta del origen y el id del documento, pero <b>nunca los
	 * resuelve</b>: para este servicio son datos opacos. Es lo que permite que otros
	 * módulos disparen órdenes de pago sin que CXP tenga que conocerlos, y que retirar
	 * cualquiera de esos módulos no rompa la compilación de CXP.
	 *
	 * El beneficiario va DENORMALIZADO porque puede no existir en el maestro de titulares
	 * de tesorería; se usa en el archivo del banco cuando no hay cuenta de destino.
	 *
	 * El desglose clasifica contablemente el pago: al confirmarse se genera <b>una línea
	 * DEBE por producto</b> (cuenta del grupo del producto) y <b>una línea HABER</b> a la
	 * cuenta contable del banco por el total. Así una sola transferencia puede cubrir
	 * varios conceptos con cuentas contables distintas.
	 *
	 * El pago entra al mismo circuito que los demás: aparece en el listado, se selecciona
	 * para un lote, viaja en el archivo y se confirma. Con débito automático nace
	 * CONFIRMADO y se contabiliza en esta misma llamada.
	 *
	 * @param origen                 : Etiqueta opaca del proceso origen, obligatoria
	 *                                 (ver {@link com.saa.rubros.OrigenPagoExterno})
	 * @param idOrigen               : Id del documento en el módulo origen, obligatorio
	 * @param idEmpresa              : Id de la empresa contable
	 * @param idCuentaBancariaOrigen : Id de la cuenta bancaria propia (TSR.CNBC) de la que
	 *                                 sale el dinero
	 * @param valor                  : Valor a transferir; debe ser mayor a cero
	 * @param fechaProgramada        : Fecha programada en formato yyyy-MM-dd (null = hoy)
	 * @param beneficiario           : Datos del beneficiario, obligatorio
	 * @param desglose               : Desglose contable, al menos una línea; la suma de sus
	 *                                 valores debe igualar {@code valor} con tolerancia 0.01
	 * @param observacion            : Observación del pago
	 * @param idUsuario              : Id del usuario que registra
	 * @param debitoAutomatico       : true si el banco ya debitó la cuenta por convenio
	 * @param referencia             : Referencia del débito (opcional)
	 * @return                       : Mapa con exito, mensaje, pago, origen e idOrigen; en
	 *                                 débito automático además asiento
	 * @throws Throwable             : Excepcion
	 */
	Map<String, Object> registrarPagoDeOrigenExterno(String origen, Long idOrigen, Long idEmpresa,
			Long idCuentaBancariaOrigen, Double valor, String fechaProgramada,
			com.saa.ejb.cxp.service.dto.BeneficiarioOcasional beneficiario,
			List<com.saa.ejb.cxp.service.dto.LineaContablePago> desglose, String observacion,
			Long idUsuario, boolean debitoAutomatico, String referencia) throws Throwable;

	/**
	 * Igual que el método sin {@code formaPago}, con la forma de pago explícita. Ver
	 * {@link com.saa.rubros.FormaPagoProgramado}. Con formaPago=CHEQUE (3) no se exige
	 * cuenta ni banco del beneficiario (el cheque se gira desde la cuenta de origen).
	 * @param formaPago : Forma de pago; null equivale a la forma inferida de debitoAutomatico
	 */
	Map<String, Object> registrarPagoDeOrigenExterno(String origen, Long idOrigen, Long idEmpresa,
			Long idCuentaBancariaOrigen, Double valor, String fechaProgramada,
			com.saa.ejb.cxp.service.dto.BeneficiarioOcasional beneficiario,
			List<com.saa.ejb.cxp.service.dto.LineaContablePago> desglose, String observacion,
			Long idUsuario, boolean debitoAutomatico, String referencia, Long formaPago) throws Throwable;

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
