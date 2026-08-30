package com.saa.ejb.rhh.service;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>T4 del ciclo de descuentos recurrentes (incluye anticipos a empleados): cierra en el rol
 * las cuotas que un pago de nomina ya contabilizado efectivamente descuento.</p>
 *
 * <h3>Por que es un EJB aparte y no un metodo privado</h3>
 *
 * <p><code>ContabilizacionNominaServiceImpl.contabilizarPago</code> corre en la transaccion
 * JTA del pago: genera el asiento y deja el periodo en <code>PAGADO</code>, un estado del que
 * <code>reabrirPeriodo</code> ya no permite volver. Si el cierre de cuotas fuera un metodo
 * privado de esa misma clase, correria dentro de <b>esa misma transaccion</b>: un error de
 * persistencia ahi adentro marca la transaccion JTA <code>rollback-only</code>, y ese marcado
 * <b>no lo deshace un <code>try/catch</code></b> — el commit del pago fallaria igual con
 * <code>RollbackException</code>, perdiendo el asiento y el estado del periodo que ya se habian
 * generado. Anotar el metodo privado con <code>REQUIRES_NEW</code> tampoco resuelve nada: los
 * interceptores de EJB que aplican <code>@TransactionAttribute</code> solo actuan sobre llamadas
 * que pasan por el proxy del bean, nunca sobre <code>this.metodo()</code>.</p>
 *
 * <p>Por eso este cierre vive en un <code>@Stateless</code> propio, inyectado con
 * <code>@EJB</code> e invocado a traves de esta interfaz <code>@Local</code> — la unica forma de
 * que el proxy intercepte la llamada y honre <code>REQUIRES_NEW</code>: una transaccion nueva y
 * suspendida, independiente de la del pago. Si algo falla adentro, esa transaccion nueva se
 * pierde ella sola; la del pago —ya con el asiento generado y el periodo en PAGADO— sigue
 * intacta y comitea sin problema.</p>
 *
 * <p><b>No volver a "simplificar" esto a un metodo privado</b>: es exactamente el error que este
 * diseno existe para evitar.</p>
 */
@Local
public interface CierreCuotasDescuentoService {

    /**
     * Marca como cobradas las cuotas (<code>RHH.CTDS</code>) que un periodo de nomina ya pagado
     * efectivamente descuento, y cascada el efecto a <code>DescuentoRecurrente</code> y, si
     * aplica, a <code>AnticipoEmpleado</code>.
     *
     * <p>Corre en su propia transaccion (<code>REQUIRES_NEW</code>) y <b>nunca propaga una
     * excepcion</b>: todo el proceso, y ademas cada renglon por separado, queda envuelto en su
     * propio <code>try/catch</code> que solo registra en el log. Esta pensado para llamarse
     * despues de que el pago de nomina ya se contabilizo y el periodo ya quedo PAGADO —un dato
     * de reporte desactualizado (el saldo de un anticipo) es un problema menor comparado con
     * perder la contabilizacion de un pago que la empresa ya hizo.</p>
     *
     * @param idPeriodoNomina	: Id del periodo de nomina que se acaba de pagar
     * @param idOrdenPago		: Id de la orden de pago, solo para los mensajes de log
     * @param usuario			: Usuario que contabiliza, solo para los mensajes de log
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    void descuentaCuotasDelPeriodo(Long idPeriodoNomina, Long idOrdenPago, String usuario);

}
