package com.saa.ejb.crd.service;

import com.saa.model.cnt.PlanCuenta;

import jakarta.ejb.Local;

/**
 * Producto de pago (PGS.PRDP) de un proceso de CRD, leído de un PARÁMETRO del rubro
 * {@link com.saa.rubros.Rubros#CRD_PARAMETROS_CONTABILIDAD} (P9b/N1, 2026-11-01) y VERIFICADO contra la cuenta de la plantilla.
 * Reemplaza la búsqueda por cuenta ({@code selectActivosByEmpresaYCuentaGrupo}), que se rompe en cuanto dos productos
 * activos comparten cuenta.
 */
@Local
public interface ProductoPagoParametroService {

    /**
     * Resuelve el producto de pago del parámetro {@code alternoParametro} (valor numérico = id de PGS.PRDP).
     * Detiene el proceso con un mensaje claro, sin respaldo, si: falta el parámetro; el producto no existe; no está ACTIVO;
     * no tiene grupo o el grupo no tiene cuenta; o la cuenta del grupo no es {@code cuentaEsperada} (la de la línea de la
     * plantilla contable del proceso).
     *
     * @param alternoParametro detalle del rubro 237 (p. ej. {@code CrdParametroContabilidad.PRODUCTO_PAGO_DESEMBOLSO_PRESTAMO})
     * @param cuentaEsperada   cuenta de la línea de la plantilla contra la que se verifica el grupo del producto
     * @param que              descripción corta del pago, para los mensajes ("devolución al partícipe", …)
     * @param codigoError      prefijo estable del error ("PRODUCTO_PAGO_DEVOLUCION_NO_RESUELTO", …)
     * @return id del producto de pago
     */
    Long resolver(int alternoParametro, PlanCuenta cuentaEsperada, String que, String codigoError) throws Throwable;
}
