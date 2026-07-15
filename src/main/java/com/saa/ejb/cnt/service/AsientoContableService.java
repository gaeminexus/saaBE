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
}
