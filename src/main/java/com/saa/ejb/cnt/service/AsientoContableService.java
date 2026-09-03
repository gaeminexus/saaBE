package com.saa.ejb.cnt.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.model.cxc.DetalleFactura;
import com.saa.model.tsr.Titular;

import jakarta.ejb.Local;

/**
 * Servicio genérico para generación de asientos contables desde cualquier módulo.
 * 
 * Recibe los datos del documento origen y genera automáticamente la cabecera
 * del asiento y sus líneas de detalle (debe/haber), consultando las cuentas
 * contables configuradas en:
 *  - PersonaCuentaContable (cuentas de clientes/proveedores)
 *  - GrupoProductoCobro    (cuentas por grupo de producto)
 *  - Tsri                  (cuentas de impuestos)
 *
 * El proceso es genérico: se puede reutilizar para facturas, notas de crédito,
 * liquidaciones de compra, retenciones, etc.
 */
@Local
public interface AsientoContableService {

    /**
     * Valida ANTES de grabar la factura que todas las cuentas contables
     * necesarias para generar el asiento estén configuradas.
     * 
     * Verifica:
     *  1. Cuenta CxC del cliente (PersonaCuentaContable, tipoCuenta=1, tipoPersona=1)
     *  2. Cuenta contable de cada GrupoProducto de los detalles
     *  3. Cuenta contable de cada tipo de IVA en TSRI (lsri.tabla='17')
     *
     * @param titular   Titular al que se emite la factura
     * @param detalles  Lista de detalles de la factura
     * @param idEmpresa ID de la empresa contable
     * @return Lista de mensajes de error. Si está vacía, todas las cuentas existen.
     */
    List<String> validarCuentasContables(Titular titular,
            List<DetalleFactura> detalles, Long idEmpresa);

    /**
     * Valida ANTES de grabar una Nota de Crédito que todas las cuentas contables
     * necesarias para generar el asiento estén configuradas.
     *
     * La lógica es idéntica a {@link #validarCuentasContables} pero adaptada para
     * {@link com.saa.model.cxc.DetalleNotaCredito}, cuyo campo {@code producto} es
     * un {@code Long} (ID) en lugar de una relación JPA cargada.
     *
     * @param titular   Titular al que se emite la nota de crédito
     * @param detalles  Lista de detalles de la nota de crédito
     * @param idEmpresa ID de la empresa contable
     * @return Lista de mensajes de error. Si está vacía, todas las cuentas existen.
     */
    List<String> validarCuentasContablesNC(Titular titular,
            List<com.saa.model.cxc.DetalleNotaCredito> detalles, Long idEmpresa);

    /**
     * Valida ANTES de grabar una Nota de Débito que todas las cuentas contables
     * necesarias estén configuradas.
     *
     * La ND no tiene detalles con producto; obtiene las cuentas de ingreso desde
     * los detalles de la factura relacionada ({@code notaDebito.factura}).
     *
     * @param notaDebito Nota de Débito a emitir (debe tener factura relacionada)
     * @param idEmpresa  ID de la empresa contable
     * @return Lista de mensajes de error. Si está vacía, todas las cuentas existen.
     */
    List<String> validarCuentasContablesND(com.saa.model.cxc.NotaDebito notaDebito, Long idEmpresa);

    /**
     * Genera el asiento contable completo para una factura de venta autorizada.
     *
     * @param idFactura            ID de la factura autorizada
     * @param idEmpresa            ID de la empresa contable
     * @param codigoAltTipoAsiento Código alterno del TipoAsiento (TipoAsientos.FACTURAS_VENTA = 2)
     * @param fechaAsiento         Fecha contable del asiento
     * @param observaciones        Descripción del asiento
     * @param usuario              Nombre del usuario que genera el asiento
     * @return                     Asiento generado y grabado con todos sus detalles
     * @throws Throwable           Si no hay período contable abierto, etc.
     */
    Asiento generarAsientoFactura(Long idFactura, Long idEmpresa,
            int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Método genérico de bajo nivel para generar un asiento desde cualquier proceso.
     *
     * @param idEmpresa            ID de la empresa contable
     * @param codigoAltTipoAsiento Código alterno del TipoAsiento
     * @param fechaAsiento         Fecha contable
     * @param observaciones        Descripción del asiento
     * @param usuario              Usuario que genera el asiento
     * @param lineas               Lista de líneas de detalle
     * @return                     Asiento grabado
     * @throws Throwable           Si hay error en la generación
     */
    Asiento generarAsiento(Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas) throws Throwable;

    /**
     * Genera el asiento contable para un anticipo de cliente confirmado.
     *
     * Estructura del asiento:
     *  DEBE:  Cuenta de caja/banco (PersonaCuentaContable, tipoCuenta=3, tipoPersona=1)
     *         → valor: total del anticipo
     *
     *  HABER: Cuenta de anticipos del cliente (PersonaCuentaContable, tipoCuenta=2, tipoPersona=1)
     *         → valor: total del anticipo
     *
     * @param anticipo             AnticipoCliente confirmado
     * @param codigoAltTipoAsiento Código alterno del TipoAsiento (TipoAsientos.ANTICIPOS_CLIENTE = 8)
     * @param usuario              Nombre del usuario que confirma
     * @return                     Asiento generado
     * @throws Throwable           Si falta período, cuentas no configuradas, etc.
     */
    Asiento generarAsientoAnticipo(AnticipoCliente anticipo,
            int codigoAltTipoAsiento, String usuario) throws Throwable;

    /**
     * Genera el asiento contable para un anticipo a proveedor confirmado.
     *
     * Estructura del asiento:
     *  DEBE:  Cuenta de anticipos del proveedor (PersonaCuentaContable, tipoCuenta=2, tipoPersona=2)
     *         → valor: total del anticipo
     *
     *  HABER: Cuenta contable vinculada a la cuenta bancaria (CuentaBancaria.planCuenta)
     *         → valor: total del anticipo
     *
     * @param anticipo             AnticipoProveedor confirmado
     * @param idCuentaBancaria     ID de la CuentaBancaria desde la que se paga
     * @param codigoAltTipoAsiento Código alterno del TipoAsiento (TipoAsientos.ANTICIPOS_PROVEEDOR = 9)
     * @param fechaAsiento         Fecha del asiento: la fecha real del pago que confirmó
     *                             el banco. Si viene nula se usa la fecha del anticipo.
     * @param usuario              Nombre del usuario que confirma
     * @return                     Asiento generado
     * @throws Throwable           Si falta período, cuentas no configuradas, etc.
     */
    Asiento generarAsientoAnticipoProveedor(com.saa.model.cxp.AnticipoProveedor anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String usuario) throws Throwable;

    /**
     * Igual que {@link #generarAsientoAnticipoProveedor(com.saa.model.cxp.AnticipoProveedor,
     * Long, int, java.time.LocalDate, String)}, agregando una nota que se anexa a la
     * observación de cabecera del asiento (por ejemplo, el número de cheque cuando el
     * anticipo se paga con cheque). {@code null} o vacío no agrega nada.
     * @param observaciones : Nota a anexar a la observación del asiento, puede ser null
     */
    Asiento generarAsientoAnticipoProveedor(com.saa.model.cxp.AnticipoProveedor anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String usuario, String observaciones) throws Throwable;

    /**
     * Genera el asiento contable para un anticipo de cliente (proceso unificado).
     *
     * Estructura del asiento:
     *  DEBE:  Cuenta contable vinculada a la cuenta bancaria (CuentaBancaria.planCuenta)
     *         → valor: total del anticipo
     *
     *  HABER: Cuenta de anticipos del cliente (PersonaCuentaContable, tipoCuenta=2, rol Cliente)
     *         → valor: total del anticipo
     *
     * @param anticipo             AnticipoCliente confirmado
     * @param idCuentaBancaria     ID de la CuentaBancaria en la que se recibe el pago
     * @param codigoAltTipoAsiento Código alterno del TipoAsiento (TipoAsientos.ANTICIPOS_CLIENTE = 8)
     * @param usuario              Nombre del usuario que confirma
     * @return                     Asiento generado
     * @throws Throwable           Si falta período, cuentas no configuradas, etc.
     */
    Asiento generarAsientoAnticipoCliente(AnticipoCliente anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, String usuario) throws Throwable;

    // =========================================================================
    // CXC — Documentos de Cobro (emitidos por la empresa)
    // =========================================================================
    // NOTA GENERAL:
    //   · codigoAltTipoAsiento → constante en TipoAsientos (pendiente definir en BD)
    //   · AuxiliarUno típico   → código grupo de producto + código cliente/proveedor
    //   Cada método lanzará UnsupportedOperationException hasta que se configure
    //   la plantilla y los auxiliares correspondientes.
    // =========================================================================

    /**
     * Genera el asiento contable de una Nota de Crédito emitida (CXC).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.NOTAS_CREDITO_VENTA}<br>
     * TODO — AuxiliarUno: código del grupo de producto del detalle + código del cliente (Titular)
     */
    Asiento generarAsientoNotaCredito(Long idNotaCredito, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Nota de Débito emitida (CXC).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.NOTAS_DEBITO_VENTA}<br>
     * TODO — AuxiliarUno: código del grupo de producto del detalle + código del cliente (Titular)
     */
    Asiento generarAsientoNotaDebito(Long idNotaDebito, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Valida ANTES de emitir una Liquidación de Compra (CXC) que todas las
     * cuentas contables necesarias para su recepción en CXP estén
     * configuradas — la liquidación emitida no genera su propia cuenta por
     * pagar; al autorizarse crea un documento CXP y se contabiliza como tal
     * (ver {@link com.saa.rubros.TipoAsientos#LIQUIDACIONES_COMPRA_RECIBIDAS}),
     * así que valida con los mismos criterios que esa recepción:
     *  1. Cuenta CxP del proveedor/prestador (obtenerCuentaProveedor)
     *  2. Cuenta de IVA crédito tributario (obtenerCuentaIVACxp)
     *  3. Cuenta del grupo de cada producto de los detalles (producto.grupoProducto.planCuenta)
     * @param liquidacion : Liquidación a emitir (con titular ya asignado)
     * @param detalles    : Detalles de la liquidación (cada uno con producto asignado)
     * @param idEmpresa   : Empresa contable
     * @return : Lista de mensajes de error. Si está vacía, todas las cuentas existen.
     */
    List<String> validarCuentasContablesLiquidacion(com.saa.model.cxc.LiquidacionCompra liquidacion,
            List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles, Long idEmpresa) throws Throwable;

    /**
     * Valida ANTES de emitir una Retención que todas las cuentas contables necesarias
     * estén configuradas:
     *  1. Cuenta CxP del proveedor (PersonaCuentaContable, tipoCuenta=1, tipoPersona=2)
     *  2. Cuenta contable de cada código de retención (Tsri.planCuenta, por Tsri.codigo)
     *
     * @param retencion Retención a emitir (debe tener proveedor)
     * @param detalles  Lista de detalles de retención
     * @param idEmpresa ID de la empresa contable
     * @return Lista de mensajes de error. Si está vacía, todas las cuentas existen.
     */
    List<String> validarCuentasContablesRetencion(com.saa.model.cxc.Retencion retencion,
            List<com.saa.model.cxc.DetalleRetencion> detalles, Long idEmpresa);

    /**
     * Genera el asiento contable para una Retención electrónica emitida (CXC/RTNC).
     * <p>
     * Estructura:
     * <pre>
     *   DEBE:  Cuenta CxP del proveedor sujeto a retención
     *          → valor: sumatoria de todos los valorReten de los detalles
     *
     *   HABER: Una línea por cada DetalleRetencion
     *          → cuenta: Tsri.planCuenta donde Tsri.codigo = DetalleRetencion.codRetencion
     *          → valor:  DetalleRetencion.valorReten
     * </pre>
     *
     * @param idRetencion          ID de la retención autorizada
     * @param idEmpresa            ID de la empresa contable
     * @param codigoAltTipoAsiento Código alterno del TipoAsiento (TipoAsientos.RETENCIONES_EMITIDAS)
     * @param fechaAsiento         Fecha contable
     * @param observaciones        Descripción del asiento
     * @param usuario              Usuario que genera el asiento
     * @return Asiento generado y grabado
     * @throws Throwable si faltan cuentas, período cerrado, etc.
     */
    Asiento generarAsientoRetencion(Long idRetencion, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Retención electrónica v2 emitida (CXC).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.RETENCIONES_EMITIDAS_V2}<br>
     * TODO — AuxiliarUno: código de la cuenta de retención según código SRI del impuesto
     */
    Asiento generarAsientoRetencionV2(Long idRetencionV2, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    // =========================================================================
    // CXP — Documentos de Compra (recibidos de proveedor vía SRI)
    // =========================================================================

    /**
     * Genera el asiento contable de una Factura de Compra recibida (CXP).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.FACTURAS_COMPRA}<br>
     * TODO — AuxiliarUno: código del grupo de producto (ProductoPago) + código del proveedor
     */
    Asiento generarAsientoFacturaCompra(Long idFacturaCompra, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Nota de Crédito de compra recibida (CXP).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.NOTAS_CREDITO_COMPRA}<br>
     * TODO — AuxiliarUno: código del grupo de producto + código del proveedor
     */
    Asiento generarAsientoNotaCreditoCompra(Long idNotaCreditoCompra, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Nota de Débito de compra recibida (CXP).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.NOTAS_DEBITO_COMPRA}<br>
     * TODO — AuxiliarUno: código del grupo de producto + código del proveedor
     */
    Asiento generarAsientoNotaDebitoCompra(Long idNotaDebitoCompra, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Liquidación de Compra recibida (CXP).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS}<br>
     * TODO — AuxiliarUno: código del grupo de producto + código del prestador de servicio
     */
    Asiento generarAsientoLiquidacionCompraCompra(Long idLiquidacion, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Retención v1 recibida de proveedor (CXP).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.RETENCIONES_RECIBIDAS}<br>
     * TODO — AuxiliarUno: código de la cuenta de retención según código SRI del impuesto
     */
    Asiento generarAsientoRetencionCompra(Long idRetencionCompra, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento contable de una Retención v2 recibida de proveedor (CXP).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.RETENCIONES_RECIBIDAS_V2}<br>
     * TODO — AuxiliarUno: código de la cuenta de retención según código SRI del impuesto
     */
    Asiento generarAsientoRetencionCompraV2(Long idRetencionCompraV2, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    // =====================================================================
    // Tesorería: aplicación de pagos y cobros a facturas
    // =====================================================================

    /**
     * Variante de {@link #generarAsiento} que permite indicar a qué módulo del
     * sistema se clasifica el asiento (rubro {@code ModuloSistema}).
     * La versión sin este parámetro clasifica siempre como CUENTAS_POR_COBRAR.
     *
     * @param moduloSistema : valor del rubro {@code ModuloSistema}
     *                        (2=Tesorería, 3=CxP, 4=CxC)
     */
    Asiento generarAsiento(Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario,
            java.util.List<com.saa.model.cnt.DetalleAsiento> lineas,
            Long moduloSistema) throws Throwable;

    /**
     * Genera el asiento del cruce de un anticipo de proveedor contra una factura
     * de compra (CXP).
     * <p>
     * DEBE:  cuenta CxP del proveedor  (PersonaCuentaContable tipoCuenta=1, rol Proveedor)<br>
     * HABER: cuenta de anticipos       (PersonaCuentaContable tipoCuenta=2, rol Proveedor)
     * <p>
     * TODO — Plantilla: {@code TipoAsientos.APLICACION_ANTICIPO_PROVEEDOR} (codigoAlterno por definir en BD)
     *
     * @param idTitular   : Id del proveedor
     * @param valor       : Valor del anticipo que se cruza
     * @param idEmpresa   : Id de la empresa contable
     * @return            : Asiento generado
     * @throws Throwable  : Excepcion
     */
    Asiento generarAsientoAplicacionAnticipoProveedor(Long idTitular, Double valor, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento del cruce de un anticipo de cliente contra una factura
     * de venta (CXC).
     * <p>
     * DEBE:  cuenta de anticipos    (PersonaCuentaContable tipoCuenta=2, rol Cliente)<br>
     * HABER: cuenta CxC del cliente (PersonaCuentaContable tipoCuenta=1, rol Cliente)
     * <p>
     * TODO — Plantilla: {@code TipoAsientos.APLICACION_ANTICIPO_CLIENTE} (codigoAlterno por definir en BD)
     */
    Asiento generarAsientoAplicacionAnticipoCliente(Long idTitular, Double valor, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento del pago a un proveedor por transferencia bancaria (CXP).
     * Se invoca únicamente cuando el banco confirma la transferencia.
     * <p>
     * DEBE:  cuenta CxP del proveedor (PersonaCuentaContable tipoCuenta=1, rol Proveedor)<br>
     * HABER: cuenta contable de la cuenta bancaria propia (CuentaBancaria.planCuenta)
     * <p>
     * TODO — Plantilla: {@code TipoAsientos.PAGO_TRANSFERENCIA_CXP} (codigoAlterno por definir en BD)
     *
     * @param idCuentaBancaria : Id de la cuenta bancaria propia desde la que sale el dinero
     */
    Asiento generarAsientoPagoTransferenciaCxp(Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento del cobro de un cliente por transferencia bancaria (CXC).
     * <p>
     * DEBE:  cuenta contable de la cuenta bancaria receptora (CuentaBancaria.planCuenta)<br>
     * HABER: cuenta CxC del cliente (PersonaCuentaContable tipoCuenta=1, rol Cliente)
     * <p>
     * TODO — Plantilla: {@code TipoAsientos.COBRO_TRANSFERENCIA_CXC} (codigoAlterno por definir en BD)
     *
     * @param idCuentaBancaria : Id de la cuenta bancaria propia en la que se recibe el dinero
     */
    Asiento generarAsientoCobroTransferenciaCxc(Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento del pago de un egreso de tesorería sin documento
     * físico (TSR.EGRS): comisiones, administración de cuenta, etc.
     * <p>
     * DEBE:  cuenta del grupo del producto CXP (GrupoProductoPago.planCuenta)<br>
     * HABER: cuenta contable de la cuenta bancaria propia (CuentaBancaria.planCuenta)
     * <p>
     * Plantilla: {@code TipoAsientos.EGRESO_TESORERIA} (codigoAlterno 5, TEGRESO)
     *
     * @param idProductoPago   : Id del producto CXP que clasifica el gasto (PGS.PRDP)
     * @param concepto         : Concepto del egreso (va en las líneas del asiento)
     * @param valor            : Valor del egreso
     * @param idCuentaBancaria : Id de la cuenta bancaria propia desde la que sale el dinero
     * @param idEmpresa        : Id de la empresa contable
     * @return                 : Asiento generado
     * @throws Throwable       : Excepcion (producto sin grupo o grupo sin cuenta contable)
     */
    Asiento generarAsientoEgresoTesoreria(Long idProductoPago, String concepto, Double valor,
            Long idCuentaBancaria, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento de un ingreso de tesorería sin documento físico
     * (TSR.INGR): intereses ganados, créditos bancarios, etc.
     * <p>
     * DEBE:  cuenta contable de la cuenta bancaria receptora (CuentaBancaria.planCuenta)<br>
     * HABER: cuenta del grupo del producto CXC (GrupoProductoCobro.planCuenta)
     * <p>
     * Plantilla: {@code TipoAsientos.INGRESO_TESORERIA} (codigoAlterno 4, TINGRESO)
     *
     * @param idProductoCobro  : Id del producto CXC que clasifica el ingreso (CBR.PRDC)
     * @param concepto         : Concepto del ingreso (va en las líneas del asiento)
     * @param valor            : Valor del ingreso
     * @param idCuentaBancaria : Id de la cuenta bancaria propia que recibió el dinero
     * @param idEmpresa        : Id de la empresa contable
     * @return                 : Asiento generado
     * @throws Throwable       : Excepcion (producto sin grupo o grupo sin cuenta contable)
     */
    Asiento generarAsientoIngresoTesoreria(Long idProductoCobro, String concepto, Double valor,
            Long idCuentaBancaria, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable;

    // ---------------------------------------------------------------
    // Caja chica
    // ---------------------------------------------------------------

    /**
     * Genera el asiento de un gasto de caja chica.
     * <p>
     * DEBE:  cuenta del grupo del producto CXP que clasifica el gasto<br>
     * HABER: cuenta contable de la caja chica (CajaChica.planCuenta)
     * <p>
     * Plantilla: {@code TipoAsientos.EGRESO_TESORERIA} (codigoAlterno 5, T-EGRESOS).
     *
     * @param idProductoPago  : Id del producto CXP que clasifica el gasto (PGS.PRDP)
     * @param nombreCaja      : Nombre de la caja chica, para la glosa de la línea HABER
     * @param descripcion     : Concepto del gasto (va en las líneas del asiento)
     * @param valor           : Valor del gasto
     * @param idPlanCuentaCaja: Id de la cuenta contable de la caja (CajaChica.planCuenta)
     * @param idEmpresa       : Id de la empresa contable
     * @param fechaAsiento    : Fecha del asiento
     * @param observaciones   : Observación de cabecera del asiento
     * @param usuario         : Nombre del usuario que registra
     * @return                : Asiento generado
     * @throws Throwable      : Excepcion (producto sin grupo o grupo sin cuenta contable)
     */
    Asiento generarAsientoGastoCajaChica(Long idProductoPago, String nombreCaja, String descripcion,
            Double valor, Long idPlanCuentaCaja, Long idEmpresa, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento de un gasto de caja chica que paga un documento del proveedor
     * (factura o liquidación de compra), en vez de reconocer gasto contra la cuenta del
     * producto de pago — evita reconocer dos veces un gasto que ya se reconoció al registrar
     * el documento (docs/logica-negocio/tsr/PLAN-GASTO-CAJA-CHICA-PAGA-FACTURA.md #2).
     * <p>
     * DEBE:  cuenta CxP del proveedor (tipoCuenta=1) — mismo camino que
     * {@link #generarAsientoAplicacionAnticipoProveedor}, verificado sin acoplamiento a
     * anticipo.<br>
     * HABER: cuenta contable de la caja chica (CajaChica.planCuenta), igual que
     * {@link #generarAsientoGastoCajaChica}.
     * <p>
     * Plantilla: {@code TipoAsientos.EGRESO_TESORERIA} — sigue siendo un egreso de
     * tesorería, no una aplicación de anticipo: no se consume ningún anticipo acá.
     *
     * @param idTitular       : Id del proveedor (TSR.TTLR) dueño del documento pagado
     * @param valor           : Valor del gasto, igual al monto aplicado al documento
     * @param idPlanCuentaCaja: Id de la cuenta contable de la caja (CajaChica.planCuenta)
     * @param idEmpresa       : Id de la empresa contable
     * @param fechaAsiento    : Fecha del asiento
     * @param observaciones   : Observación de cabecera del asiento
     * @param usuario         : Nombre del usuario que registra
     * @return                : Asiento generado
     * @throws Throwable      : Excepcion (proveedor sin cuenta CxP tipo 1, o caja sin cuenta)
     */
    Asiento generarAsientoAplicacionCajaChica(Long idTitular, Double valor, Long idPlanCuentaCaja,
            Long idEmpresa, java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable;

    /**
     * Genera el asiento de una apertura o reposición de caja chica pagada desde
     * una cuenta bancaria.
     * <p>
     * DEBE:  cuenta contable de la caja chica (CajaChica.planCuenta)<br>
     * HABER: cuenta contable de la cuenta bancaria de origen (CuentaBancaria.planCuenta)
     * <p>
     * Plantilla: {@code TipoAsientos.EGRESO_TESORERIA} (codigoAlterno 5, T-EGRESOS).
     *
     * @param idPlanCuentaCaja : Id de la cuenta contable de la caja
     * @param idCuentaBancaria : Id de la cuenta bancaria propia de origen
     * @param valor            : Valor de la apertura o reposición
     * @param idEmpresa        : Id de la empresa contable
     * @param fechaAsiento     : Fecha del asiento
     * @param observaciones    : Observación de cabecera del asiento
     * @param usuario          : Nombre del usuario que registra
     * @return                 : Asiento generado
     * @throws Throwable       : Excepcion
     */
    Asiento generarAsientoReposicionCajaChica(Long idPlanCuentaCaja, Long idCuentaBancaria, Double valor,
            Long idEmpresa, java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable;

    /**
     * Genera el asiento de ajuste de un cierre de caja chica con diferencia
     * entre el saldo según libros y el saldo físico contado.
     * <p>
     * Sobrante (saldo físico &gt; libros): DEBE caja / HABER cuenta de diferencia.<br>
     * Faltante (saldo físico &lt; libros): DEBE cuenta de diferencia / HABER caja.
     * <p>
     * Plantilla: {@code TipoAsientos.EGRESO_TESORERIA} (codigoAlterno 5, T-EGRESOS).
     *
     * @param idPlanCuentaCaja       : Id de la cuenta contable de la caja
     * @param idPlanCuentaDiferencia : Id de la cuenta de faltantes/sobrantes elegida por el usuario
     * @param valor                  : Valor absoluto de la diferencia (siempre positivo)
     * @param sobrante               : true si es sobrante, false si es faltante
     * @param idEmpresa              : Id de la empresa contable
     * @param fechaAsiento           : Fecha del asiento
     * @param observaciones          : Observación de cabecera del asiento
     * @param usuario                : Nombre del usuario que registra
     * @return                       : Asiento generado
     * @throws Throwable             : Excepcion
     */
    Asiento generarAsientoAjusteCajaChica(Long idPlanCuentaCaja, Long idPlanCuentaDiferencia,
            Double valor, boolean sobrante, Long idEmpresa, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

    /**
     * Genera el asiento de la entrega de un anticipo de sueldo a un
     * colaborador (RHH.ANTE, vía {@code PagoProgramado} de origen externo
     * {@code RHH_ANTICIPO_EMPLEADO}).
     * <p>
     * DEBE: la cuenta que resuelve {@code RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS}
     * (línea 14 del rubro 214) contra la plantilla de rol
     * ({@code ConfiguracionNomina.plantillaRol}) de la empresa — la MISMA
     * cuenta que ya usa {@code ContabilizacionNominaServiceImpl} para el
     * descuento del rol, resuelta con el mismo mecanismo (plantilla +
     * cuenta marcadora), no hardcodeada. Así el ciclo cuadra solo: la
     * entrega la debita, el descuento del rol la acredita.<br>
     * HABER: cuenta contable de la cuenta bancaria de origen.
     * <p>
     * Plantilla de asiento: {@code TipoAsientos.EGRESO_TESORERIA}, módulo
     * {@code ModuloSistema.TESORERIA}.
     * @param idEmpleado    : Id del empleado que recibe el anticipo
     * @param valor         : Valor entregado
     * @param idCuentaBancaria : Id de la cuenta bancaria de origen
     * @param idEmpresa     : Id de la empresa contable
     * @param fechaAsiento  : Fecha del asiento
     * @param observaciones : Observación de cabecera del asiento
     * @param usuario       : Nombre del usuario que registra
     * @return              : Asiento generado
     * @throws Throwable    : Excepcion
     */
    Asiento generarAsientoAnticipoEmpleado(Long idEmpleado, Double valor, Long idCuentaBancaria,
            Long idEmpresa, java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable;

    /**
     * Verifica ESTRICTAMENTE que un titular tenga una
     * {@code PersonaCuentaContable} configurada bajo un rol exacto —sin el
     * fallback "sin filtro de rol" de
     * {@code PersonaCuentaContableDaoService.selectByTitularRolTipoCuenta}
     * (pensado para datos antiguos sin {@code rubroRolPersonaH} poblado),
     * que puede devolver la cuenta del rol contrario. Medido contra la
     * base: de 87 titulares con cuenta, 61 sólo tienen Proveedor, 24 sólo
     * Cliente y 2 ambos — el fallback se dispara para 85 de 87 en cuanto se
     * usa el titular en el rol contrario, y contabiliza contra la cuenta
     * equivocada en silencio.
     * @param codigoTitular : Código del titular
     * @param idEmpresa     : Empresa contable
     * @param tipoCuenta    : 1=Facturas, 2=Anticipos, 3=Caja/Banco
     * @param rolPersona    : {@link com.saa.rubros.RolPersona#CLIENTE} o {@link com.saa.rubros.RolPersona#PROVEEDOR}
     * @return : true si existe al menos una fila con el rol pedido, sin fallback
     */
    boolean existeCuentaConRolEstricto(Long codigoTitular, Long idEmpresa, Long tipoCuenta, int rolPersona);
}
