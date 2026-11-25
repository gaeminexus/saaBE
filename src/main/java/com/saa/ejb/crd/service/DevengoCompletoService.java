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
 * EJECUTADA de la empresa CON CORTE ANTERIOR a la fecha de la operación (lote 16: el cierre que ya existía ese día, no el
 * último de la base), y solo si esa corrida tiene corte {@code >= devengoCompletoDesde}.</p>
 */
@Local
public interface DevengoCompletoService {

    /**
     * Último día cuyo interés ya devengó el cierre.
     *
     * @param idEmpresa      empresa contable
     * @param fechaOperacion fecha de la operación (abono, precancelación, acuerdo): solo cuentan las corridas con corte
     *                       estrictamente anterior
     * @return el último día del mes siguiente al corte de la última corrida EJECUTADA con corte anterior a la operación, o
     *         null si el devengo completo no está activo (propiedad ausente, sin corridas anteriores o con la última
     *         anterior a la propiedad)
     */
    LocalDate limiteDevengado(Long idEmpresa, LocalDate fechaOperacion) throws Throwable;

    /**
     * Abono a capital: Σ interés de las cuotas historizadas menos Σ interés de las cuotas nuevas, de las que vencen hasta
     * {@link #limiteDevengado} (al día de la operación), como líneas D ingreso / H interés por cobrar.
     *
     * @param fechaOperacion fecha de la operación: fija qué cierre ya existía (lote 16)
     * @return las líneas, o una lista vacía si no hay nada que reversar
     */
    List<DetalleAsiento> lineasReversoAbono(Long idEmpresa, Prestamo prestamo, List<HistDetallePrestamo> historizadas,
            List<DetallePrestamo> nuevas, LocalDate fechaOperacion, String prefijo) throws Throwable;

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
    /**
     * Lote 20: el reverso del abono con su detalle por cuota (para el libro, MVIC tipo 12). Reparto: por número de cuota, interés
     * historizado − interés nuevo cuando da positivo; el residuo de las cuotas que desaparecen y los ajustes van a la cuota de número mayor
     * que queda dentro del límite, de modo que la suma del libro sea el reverso del asiento, al centavo.
     *
     * @param idEvento código del evento del abono (idOrigen de los MVIC)
     */
    com.saa.ejb.crd.service.dto.ResultadoReversoDevengo calcularReversoAbono(Long idEmpresa, Prestamo prestamo,
            List<HistDetallePrestamo> historizadas, List<DetallePrestamo> nuevas, LocalDate fechaOperacion, String prefijo,
            Long idEvento) throws Throwable;

    /**
     * Lote 20: el reverso de las cuotas canceladas sin cobrar con su detalle por cuota. Cada cuota se redondea y el asiento es la suma de
     * lo que se graba en el libro.
     *
     * @param idEvento código del evento (precancelación o acuerdo) que será el idOrigen de los MVIC; puede ser null
     */
    com.saa.ejb.crd.service.dto.ResultadoReversoDevengo calcularReversoCuotasCanceladas(Long idEmpresa, Prestamo prestamo,
            LocalDate fechaEvento, String prefijo, Long idEvento) throws Throwable;

    /**
     * Lote 20: graba el libro del reverso (MVIC tipo 12, componente interés, un movimiento por cuota) con el asiento de la operación. Va
     * DESPUÉS de generar el asiento. Requiere {@code sql/406} (CK_MVIC_TIPO hasta 12): sin él, el INSERT falla.
     */
    void registrarReverso(com.saa.ejb.crd.service.dto.ResultadoReversoDevengo resultado, Long idAsiento, LocalDate fechaContable,
            String usuario) throws Throwable;

    /** Lote 20: anula los MVIC 12 de una operación (precancelación, acuerdo o abono) al anular su evento. */
    int anularPorEvento(Long idEvento) throws Throwable;
}
