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
     * Genera el asiento contable de una Liquidación de Compra emitida (CXC).
     * <p>
     * TODO — Plantilla:   {@code TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS}<br>
     * TODO — AuxiliarUno: código del grupo de producto del detalle + código del proveedor/prestador
     */
    Asiento generarAsientoLiquidacionCompra(Long idLiquidacion, Long idEmpresa,
            int codigoAltTipoAsiento, java.time.LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable;

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
}
