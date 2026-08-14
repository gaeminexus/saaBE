package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;

import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.dto.ResultadoCalculoMora;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timer;

/**
 * Temporizador del proceso diario de interés de mora.
 *
 * Es el PRIMER timer EJB del proyecto. Corre todos los días a las 02:00 de la madrugada, hora
 * del servidor, y calcula la mora con la fecha de ese día.
 *
 * <p><b>Cómo cambiar el horario</b>: editar la anotación {@code @Schedule} de
 * {@link #ejecutarCalculoDiario(Timer)} (por ejemplo {@code hour = "3"}). Requiere recompilar y
 * redesplegar; el proyecto no tiene un mecanismo de configuración externa para esto.</p>
 *
 * <p><b>persistent = false</b>: el timer NO se guarda en la base de datos de timers de WildFly.
 * Consecuencias buscadas:</p>
 * <ul>
 *   <li>Si el servidor está apagado a las 02:00, la corrida se PIERDE (no se recupera al
 *       arrancar). Para recuperarla está el endpoint manual
 *       {@code POST /rest/prst/calcularMora}.</li>
 *   <li>No quedan timers huérfanos tras un redespliegue, ni hace falta limpiar
 *       {@code standalone/data/timer-service-data}.</li>
 * </ul>
 *
 * <p>El proceso es idempotente: correrlo dos veces el mismo día da el mismo resultado, así que
 * relanzarlo a mano tras una falla es seguro.</p>
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Singleton
public class ProcesoMoraPrestamoTimer {

    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    /**
     * Corrida automática de las 02:00. Atrapa {@code Throwable} para que un fallo no deje al
     * timer en estado de error ni provoque reintentos automáticos de WildFly: el resumen del
     * proceso ya registra los préstamos con error y la recuperación es manual por el endpoint.
     *
     * @param timer Timer que disparó la ejecución (lo inyecta el contenedor)
     */
    @Schedule(hour = "2", minute = "0", second = "0", persistent = false,
              info = "Cálculo diario de interés de mora de cuotas vencidas")
    public void ejecutarCalculoDiario(Timer timer) {
        System.out.println("TIMER MORA - Disparo automático: " + (timer != null ? timer.getInfo() : ""));

        try {
            ResultadoCalculoMora resultado = procesoMoraPrestamoService.calcularMoraDiaria(
                LocalDate.now(), ProcesoMoraPrestamoService.USUARIO_PROCESO);

            System.out.println("TIMER MORA - Corrida OK"
                + " - Préstamos evaluados: " + resultado.getPrestamosEvaluados()
                + " - Cuotas actualizadas: " + resultado.getCuotasActualizadas()
                + " - Mora total: $" + resultado.getTotalMoraCalculada()
                + " - Errores: " + resultado.getPrestamosConError());

            if (resultado.getPrestamosConError() != null && resultado.getPrestamosConError() > 0) {
                System.err.println("TIMER MORA - La corrida terminó con "
                    + resultado.getPrestamosConError() + " préstamo(s) con error. Detalle:");
                for (String error : resultado.getErrores()) {
                    System.err.println("   - " + error);
                }
            }

        } catch (Throwable e) {
            System.err.println("TIMER MORA - La corrida automática FALLÓ: " + e.getMessage());
            e.printStackTrace();
            System.err.println("TIMER MORA - Relanzar manualmente con POST /SaaBE/rest/prst/calcularMora");
        }
    }
}
