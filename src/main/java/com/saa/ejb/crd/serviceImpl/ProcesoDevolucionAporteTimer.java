package com.saa.ejb.crd.serviceImpl;

import com.saa.ejb.crd.service.DevolucionAporteService;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timer;

/**
 * Temporizador del reconciliador de devoluciones de aportes.
 *
 * Corre cada 30 minutos y pone al día el estado de las devoluciones que están esperando
 * que su orden de pago se resuelva en Cuentas por Pagar.
 *
 * <p><b>DESACTIVADO TEMPORALMENTE (2026-08-27, hasta nueva orden)</b>: el desarrollo de
 * devolución de aportes sigue en curso, así que el {@code @Schedule} de
 * {@link #ejecutarSincronizacion(Timer)} está comentado y el timer no se registra. Ver ese
 * método para cómo reactivarlo.</p>
 *
 * <p><b>Por qué existe</b>: el sistema se comercializa después SIN el módulo {@code crd},
 * así que nada en CXP puede nombrar a CRD — no hay callback posible cuando el pago se
 * confirma o se rechaza. El aviso de vuelta va al revés: <b>CRD consulta</b>. Este timer es
 * uno de los tres disparadores de esa consulta; los otros dos son el GET del listado (que
 * reconcilia antes de responder) y el endpoint manual
 * {@code POST /rest/dvap/sincronizar}.</p>
 *
 * <p><b>persistent = false</b>: el timer NO se guarda en la base de datos de timers de
 * WildFly, igual que {@link ProcesoMoraPrestamoTimer}. Consecuencias buscadas:</p>
 * <ul>
 *   <li>Si el servidor está apagado en el minuto de la corrida, esa corrida se PIERDE. No
 *       importa: la siguiente es a los 30 minutos y el proceso es idempotente. Para
 *       recuperarla de inmediato está el endpoint manual.</li>
 *   <li>No quedan timers huérfanos tras un redespliegue, ni hace falta limpiar
 *       {@code standalone/data/timer-service-data}.</li>
 * </ul>
 *
 * <p><b>Cómo cambiar la frecuencia</b>: editar la anotación {@code @Schedule} de
 * {@link #ejecutarSincronizacion(Timer)}. Requiere recompilar y redesplegar; el proyecto no
 * tiene un mecanismo de configuración externa para esto.</p>
 *
 * @author Sistema SAA
 * @since 2026-08-24
 */
@Singleton
public class ProcesoDevolucionAporteTimer {

    @EJB
    private DevolucionAporteService devolucionAporteService;

    /**
     * Corrida automática cada 30 minutos. Atrapa {@code Throwable} para que un fallo no deje
     * al timer en estado de error ni provoque reintentos automáticos de WildFly: el resumen
     * del proceso ya registra las devoluciones con error y la recuperación es manual por el
     * endpoint.
     *
     * <p><b>DESACTIVADO TEMPORALMENTE (2026-08-27)</b>: el desarrollo de devolución de
     * aportes sigue en curso. Se comenta el {@code @Schedule} en vez de dejarlo activo con
     * una bandera para que el timer no se registre en absoluto y no genere ruido en el log.
     * La reconciliación manual vía {@code POST /rest/dvap/sincronizar} y la que corre el GET
     * del listado siguen intactas. Para reactivar: descomentar la anotación de abajo.</p>
     *
     * @param timer Timer que disparó la ejecución (lo inyecta el contenedor)
     */
    // @Schedule(hour = "*", minute = "*/30", second = "0", persistent = false,
    //           info = "Reconciliación de devoluciones de aportes contra su orden de pago")
    public void ejecutarSincronizacion(Timer timer) {
        System.out.println("TIMER DEVOLUCION APORTES - Disparo automático: "
            + (timer != null ? timer.getInfo() : ""));

        try {
            ResultadoSincronizacion resultado = devolucionAporteService.sincronizarPagos();

            System.out.println("TIMER DEVOLUCION APORTES - Corrida OK"
                + " - Evaluadas: " + resultado.getEvaluadas()
                + " - Pagadas: " + resultado.getMarcadasPagadas()
                + " - Rechazadas: " + resultado.getMarcadasRechazadas()
                + " - Huérfanas: " + resultado.getHuerfanas()
                + " - Errores: " + resultado.getConError());

            if (resultado.getConError() != null && resultado.getConError() > 0) {
                System.err.println("TIMER DEVOLUCION APORTES - La corrida terminó con "
                    + resultado.getConError() + " devolución(es) con error. Detalle:");
                for (String error : resultado.getErrores()) {
                    System.err.println("   - " + error);
                }
            }

        } catch (Throwable e) {
            System.err.println("TIMER DEVOLUCION APORTES - La corrida automática FALLÓ: "
                + e.getMessage());
            e.printStackTrace();
            System.err.println("TIMER DEVOLUCION APORTES - Relanzar manualmente con "
                + "POST /SaaBE/rest/dvap/sincronizar");
        }
    }
}
