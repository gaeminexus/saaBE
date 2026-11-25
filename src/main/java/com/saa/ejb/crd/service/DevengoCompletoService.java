package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * C27b (D-172) — reversos de devengo cuando una operación quita interés ya devengado y no cobrado.
 *
 * <p>Con {@code saa.cartera.devengoCompletoDesde} activa, el paso ④ del cierre devenga el interés COMPLETO de las cuotas
 * del mes que abre. Si después una operación elimina o baja el interés de una cuota cuyo mes ya abrió, ese ingreso se
 * devengó y no se va a cobrar: este servicio arma las líneas D 5.1.02.xx / H 1.4.02.xx (plantilla 17, papeles 30 y 10
 * del tipo de préstamo; las cuentas salen de la plantilla, nunca del código) para que la operación las agregue a SU
 * asiento, de modo que anularla las reverse.</p>
 *
 * <p>«Mes ya devengado» = vencimiento hasta el último día del mes siguiente al corte de la última corrida de cierre
 * EJECUTADA de la empresa, y solo si esa corrida tiene corte {@code >= devengoCompletoDesde}.</p>
 */
@Local
public interface DevengoCompletoService {

    /**
     * Último día cuyo interés ya devengó el cierre.
     *
     * @param idEmpresa empresa contable
     * @return el último día del mes siguiente al corte de la última corrida EJECUTADA, o null si el devengo completo no
     *         está activo (propiedad ausente, sin corridas o con la última corrida anterior a la propiedad)
     */
    LocalDate limiteDevengado(Long idEmpresa) throws Throwable;

    /**
     * Abono a capital: Σ interés de las cuotas historizadas menos Σ interés de las cuotas nuevas, de las que vencen hasta
     * {@link #limiteDevengado}, como líneas D ingreso / H interés por cobrar.
     *
     * @return las líneas, o una lista vacía si no hay nada que reversar
     */
    List<DetalleAsiento> lineasReversoAbono(Long idEmpresa, Prestamo prestamo, List<HistDetallePrestamo> historizadas,
            List<DetallePrestamo> nuevas, String prefijo) throws Throwable;

    /**
     * Precancelación: Σ interés no cobrado de las cuotas del préstamo que ESTA operación pasó a CANCELADA_ANTICIPADA (7)
     * (las que {@code precancelar()} cierra: vencimiento posterior a {@code fechaEvento}) y vencen entre el primer mes
     * devengado por el ④ nuevo y {@link #limiteDevengado}, como líneas D ingreso / H interés por cobrar. Las cuotas en 7 de
     * eventos viejos o migradas quedan fuera.
     *
     * @param fechaEvento fecha de la operación (las cuotas con vencimiento posterior a ella son las que cerró)
     * @return las líneas, o una lista vacía si no hay nada que reversar
     */
    List<DetalleAsiento> lineasReversoCuotasCanceladas(Long idEmpresa, Prestamo prestamo, LocalDate fechaEvento,
            String prefijo)
            throws Throwable;
}
