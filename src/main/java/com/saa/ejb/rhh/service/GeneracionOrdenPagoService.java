package com.saa.ejb.rhh.service;

import java.time.LocalDate;

import com.saa.model.rhh.OrdenPagoNomina;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Generacion de la orden de pago del neto de un periodo de nomina.</p>
 *
 * <p><b>Por que no se llama <code>OrdenPagoNominaService</code>,</b> como decia el §6.5 del
 * plan: ese nombre lo ocupa el CRUD de la tabla <code>RHH.RDPG</code>, que el checklist por
 * entidad exige. El proceso vive aparte, con el mismo criterio con que
 * <code>ProcesoNominaService</code> convive con <code>NominaService</code> y
 * <code>GeneracionRolPagoService</code> con <code>RolPagoService</code>.</p>
 */
@Local
public interface GeneracionOrdenPagoService {

    /**
     * Genera la orden de pago de un periodo con su detalle por empleado.
     *
     * <p>Toma el neto de cada nomina del periodo y lo reparte entre las cuentas activas del
     * empleado: a la principal si solo hay una, o segun <code>CBEMPRCN</code> si el empleado
     * divide su sueldo. <b>El residuo del redondeo va a la principal</b>, de modo que la suma
     * del detalle siempre es el neto exacto.</p>
     *
     * <p>Los cinco campos de snapshot del detalle se copian aqui y no se releen nunca: son la
     * constancia de a que cuenta se ordeno pagar.</p>
     *
     * <p>Exige el periodo APROBADO o CONTABILIZADO. Es idempotente mientras la orden no se
     * haya acreditado: regenera el detalle, de modo que un cambio de cuenta bancaria del
     * empleado se refleja. Una orden ya acreditada no se toca.</p>
     *
     * @param idPeriodoNomina	: Id del periodo de nomina
     * @param idCuentaBancaria	: Cuenta de la empresa de la que sale el pago
     * @param usuario			: Usuario que ejecuta
     * @return					: La orden generada, con su detalle
     * @throws Throwable		: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    OrdenPagoNomina generar(Long idPeriodoNomina, Long idCuentaBancaria, String usuario) throws Throwable;

    /**
     * Produce el archivo bancario de la orden.
     *
     * <p><b>No disponible: falta el formato.</b> Ni el cliente ha entregado la especificacion
     * del banco ni el modelo tiene donde guardarla —<code>RHH.FMRC</code>/<code>DFMR</code>
     * describen el archivo de <b>entrada</b> del biometrico, no una salida—. Escribir un
     * formato quemado incumpliria la regla 1 del maestro, asi que el metodo lanza
     * <code>IncomeException</code> explicando que falta. Todo lo demas de la orden funciona:
     * el detalle se genera, se consulta y se contabiliza.</p>
     *
     * @param idOrdenPago	: Id de la orden de pago
     * @return				: Contenido del archivo
     * @throws Throwable	: IncomeException mientras el formato no este parametrizado
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    byte[] generarArchivoBancario(Long idOrdenPago) throws Throwable;

    /**
     * Confirma la acreditacion de la orden y dispara el asiento de pago.
     *
     * <p>La contabilizacion la hace <code>ContabilizacionNominaService.contabilizarPago</code>,
     * que respeta el interruptor del modo historico: en un periodo historico se registra la
     * fecha pero no se emite asiento.</p>
     *
     * @param idOrdenPago			: Id de la orden de pago
     * @param fechaAcreditacion		: Fecha en que el banco acredito
     * @param usuario				: Usuario que ejecuta
     * @return						: La orden actualizada
     * @throws Throwable			: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    OrdenPagoNomina confirmar(Long idOrdenPago, LocalDate fechaAcreditacion, String usuario) throws Throwable;

}
