package com.saa.ejb.rhh.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.model.rhh.DetalleOrdenPagoNomina;
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
     * @param usuario			: Usuario que ejecuta, para las columnas de auditoria (*USRR)
     * @param idUsuario			: Id de SCP.PJRQ del usuario que ejecuta. Es el que viaja a la
     *							  bandeja de aprobacion de tesoreria (PagoProgramado.usuario, FK
     *							  real) — no se resuelve por nombre, ver
     *							  docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md
     *							  #4.2 «El idUsuario». Obligatorio salvo en un periodo HISTORICO,
     *							  que no pasa por la bandeja.
     * @return					: La orden generada, con su detalle
     * @throws Throwable		: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    OrdenPagoNomina generar(Long idPeriodoNomina, Long idCuentaBancaria, String usuario, Long idUsuario)
            throws Throwable;

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
     * @param usuario				: Usuario que ejecuta, para las columnas de auditoria (*USRR)
     * @param idUsuario				: Id de SCP.PJRQ del usuario que ejecuta. Ver el Javadoc de
     *								  {@link #generar}; obligatorio salvo en un periodo HISTORICO.
     * @return						: La orden actualizada
     * @throws Throwable			: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    OrdenPagoNomina confirmar(Long idOrdenPago, LocalDate fechaAcreditacion, String usuario, Long idUsuario)
            throws Throwable;

    // ===== INICIO nomina por empleado (docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md) =====

    /**
     * Sincroniza, por cada DRPG PENDIENTE de la orden, el estado de su pago en la bandeja de
     * tesoreria -- como los jubilados. Exclusivo de ordenes nuevas (sin pago consolidado
     * RHH_NOMINA) y de periodos NO historicos.
     *
     * <p>La primera vez que la orden queda sin ningun DRPG pendiente, aplica los efectos de la
     * orden (periodo en PAGADO, cierre de cuotas/anticipos) SIN asiento consolidado: cada pago
     * ya genero el suyo en Tesoreria al confirmarse.</p>
     *
     * @param idOrdenPago	: Id de la orden de pago
     * @return				: La orden con su estado recalculado
     * @throws Throwable	: IncomeException si la orden es HISTORICA o del camino viejo (RHH_NOMINA)
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    OrdenPagoNomina sincronizarPagos(Long idOrdenPago) throws Throwable;

    /**
     * Sincroniza UN DRPG. En su propia transaccion (REQUIRES_NEW): solo invocable a traves del
     * proxy del EJB (nunca <code>this.sincronizaUnDetalle(...)</code>), para que un error en un
     * empleado no marque rollback-only la transaccion de {@link #sincronizarPagos}, que ya
     * pudo haber confirmado a otros.
     *
     * @param idDetalle		: Id del detalle (RHH.DRPG)
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    void sincronizaUnDetalle(Long idDetalle) throws Throwable;

    /**
     * Reenvia UN DRPG rechazado: relee la cuenta activa actual del empleado, actualiza el
     * snapshot, limpia el rechazo y registra un pago nuevo con el mismo idOrigen. No re-reparte
     * el neto entre cuentas; reenvia solo esta fila con su valor.
     *
     * @param idDetalle		: Id del detalle (RHH.DRPG) RECHAZADO
     * @param idUsuario		: Id de SCP.PJRQ del usuario que ejecuta
     * @return				: El detalle reenviado, PENDIENTE de nuevo
     * @throws Throwable	: IncomeException si el detalle no esta RECHAZADO, o si no se puede
     *						  determinar una cuenta sin ambiguedad
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    DetalleOrdenPagoNomina reenviar(Long idDetalle, Long idUsuario) throws Throwable;

    /**
     * Detalle de una orden con los transitorios <code>idPago</code>/<code>estadoPago</code> del
     * ultimo pago de cada DRPG, para la pantalla de RRHH.
     *
     * @param idOrdenPago	: Id de la orden de pago
     * @return				: El detalle de la orden, con los transitorios poblados
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<DetalleOrdenPagoNomina> detalleConEstadoPago(Long idOrdenPago) throws Throwable;

    /**
     * Pobla el transitorio {@code OrdenPagoNomina.pagoPorEmpleado} de TODA la lista, con UNA
     * sola consulta (no una por orden): false si la orden tiene un pago consolidado RHH_NOMINA
     * -cualquier estado-, true si no. Mismo criterio que decide el camino viejo/nuevo en
     * {@link #confirmar}/{@link #generarArchivoBancario} (contrato §4, ITEM 8).
     *
     * @param ordenes	: Lista a poblar, modificada in-place
     * @return			: La misma lista, para encadenar
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<OrdenPagoNomina> poblarPagoPorEmpleado(List<OrdenPagoNomina> ordenes) throws Throwable;

    /**
     * Igual que {@link #poblarPagoPorEmpleado(List)}, para una sola orden (getId).
     *
     * @param orden		: Orden a poblar
     * @return			: La misma orden
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    OrdenPagoNomina poblarPagoPorEmpleado(OrdenPagoNomina orden) throws Throwable;

    // ===== FIN nomina por empleado =====

}
