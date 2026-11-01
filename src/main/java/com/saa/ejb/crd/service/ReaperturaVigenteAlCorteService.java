package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * D-019 (decisión del líder, 2026-10-28) como servicio compartido: en un re-proceso de un mes
 * pasado, el estado de HOY del préstamo no decide. Una operación que estaba vigente al corte del
 * mes y hoy está cancelada se reabre para que el pago de ese mes se aplique como ocurrió, y las
 * cuotas del mes que la migración dejó saldadas sin ningún pago vuelven a pendiente.
 *
 * <p>Nació dentro del re-proceso de cargas Petro ({@code CargaArchivoPetroServiceImpl}, C4,
 * {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md} §4.8–4.9) y se extrajo acá para que
 * lo usen también el motor de cobros en modo re-proceso ({@code CobroCreditoServiceImpl},
 * {@code docs/logica-negocio/crd/API-REPROCESO-COBRO-FECHA-PASADA.md}) y la pensión complementaria
 * de un mes pasado ({@code PagoPensionComplementariaServiceImpl.reprocesarPeriodo}, C20). Las tres
 * rutas aplican la MISMA regla; nada se borra.</p>
 *
 * <p>La fecha de corte es siempre el ÚLTIMO DÍA del mes re-procesado (inclusive, hasta las
 * 23:59:59).</p>
 */
@Local
public interface ReaperturaVigenteAlCorteService {

    /** Texto de la marca que deja D-019 en la operación reabierta y en sus eventos posteriores. */
    String MARCA_CANCELACION_POSTERIOR = "cancelación posterior pendiente de re-aplicar";

    /** 3, 4, 5, 9 y 13: los estados de préstamo cancelado que D-019 puede reabrir. */
    boolean esPrestamoTerminal(Long idEstado);

    /**
     * ¿El préstamo estaba vigente al corte? Mismo criterio que
     * {@code PrestamoDaoService.selectByEntidadYProductoVigentesAlCorte} (el de Petro): activo hoy,
     * o terminal hoy que empezó a más tardar en el corte, con alguna cuota vencida a más tardar en
     * el corte y alguna cuota no saldada al corte. Solo consulta.
     *
     * @param fechaCorte último día del mes re-procesado
     */
    boolean estabaVigenteAlCorte(Prestamo prestamo, LocalDate fechaCorte) throws Throwable;

    /**
     * D-019 ampliada — regla "cuota saldada sin pago vigente = pendiente al corte": cada cuota con
     * vencimiento DENTRO del mes del corte, en PAGADA (4) o CANCELADA_ANTICIPADA (7) y sin ningún
     * pago vigente en {@code CRD.PGPR}, se reconstruye desde sus pagos
     * ({@code recalcularCuotaDesdePagos}: sin pagos vuelve a pendiente/en mora) y su mora se calcula
     * al corte. Las de otros meses y las que tienen algún pago vigente no se tocan.
     *
     * @param cuotas   cuotas del préstamo ya leídas con {@code selectByPrestamo}
     * @param bitacora recibe una línea por cuota que cambió de estado (puede ser {@code null})
     * @return cuántas cuotas cambiaron de estado
     */
    int reabrirCuotasSaldadasSinPagoDelMes(Prestamo prestamo, LocalDate fechaCorte, List<DetallePrestamo> cuotas,
            List<String> bitacora) throws Throwable;

    /**
     * D-019: reabre una operación vigente al corte que hoy está cancelada. Primero la regla de
     * {@link #reabrirCuotasSaldadasSinPagoDelMes}; después cada cuota saldada sin fecha de pago o
     * con fecha posterior al corte se reconstruye desde sus pagos vigentes; el préstamo pasa a
     * VIGENTE (2) con la marca {@code "<etiqueta>: cancelación posterior pendiente de re-aplicar
     * (estado anterior N)"} en su observación, y cada evento no anulado posterior al corte recibe
     * la misma marca (sin anularse: se recalcula al re-procesar su mes).
     *
     * @param cuotas          cuotas del préstamo ya leídas con {@code selectByPrestamo}
     * @param bitacoraCuotas  recibe las líneas de la regla de cuotas del mes (puede ser {@code null})
     * @return línea de resumen ("Préstamo N (estado anterior E): C cuota(s) reabiertas, V evento(s)
     *         posteriores marcados")
     */
    String reabrirPrestamoVigenteAlCorte(Prestamo prestamo, LocalDate fechaCorte, String etiqueta,
            List<DetallePrestamo> cuotas, List<String> bitacoraCuotas) throws Throwable;

    /**
     * Atajo para los motores que reciben UN préstamo (cobros, pensión): verifica que el préstamo
     * estaba vigente al corte y lo prepara — {@link #reabrirPrestamoVigenteAlCorte} si hoy es
     * terminal, {@link #reabrirCuotasSaldadasSinPagoDelMes} si no. Idempotente: una segunda llamada
     * sobre el mismo préstamo y corte no cambia nada (ya no es terminal y sus cuotas del mes ya
     * están pendientes o tienen pago).
     *
     * @param bitacora recibe las líneas de lo que cambió (puede ser {@code null})
     * @throws Throwable {@code IncomeException("PRESTAMO_NO_VIGENTE_AL_CORTE: ...")} si el préstamo
     *                   no estaba vigente al corte (cancelado antes, o todavía sin cuotas vencidas)
     */
    void prepararPrestamoAlCorte(Prestamo prestamo, LocalDate fechaCorte, String etiqueta, List<String> bitacora)
            throws Throwable;
}
