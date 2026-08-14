package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.DetalleAplicacionCuota;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Motor de pagos de préstamos: núcleo compartido por TODOS los procesos de pago
 * (pago manual, pago con aportes, precancelación, abono a capital).
 *
 * Es una extracción desacoplada del proceso Petro
 * (com.saa.ejb.asoprep.serviceImpl.CargaArchivoPetroServiceImpl), extendida con mora e
 * interés vencido y con la prelación de 6 componentes:
 *
 * <pre>
 *   1. Seguro de incendio
 *   2. Seguro de desgravamen
 *   3. Interés de mora
 *   4. Interés vencido      (hoy siempre 0: ningún proceso lo alimenta)
 *   5. Interés ordinario
 *   6. Capital
 * </pre>
 *
 * Orden confirmado por negocio el 2026-08-14.
 *
 * Principios (§6.1 de ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md):
 * <ul>
 *   <li>Tolerancia de UN CENTAVO (0.01) para toda comparación de aplicación. La tolerancia
 *       de $1 es exclusiva de la validación del proceso Petro y aquí no aplica.</li>
 *   <li><b>PGPR es la fuente de verdad</b>: nunca se confía en los campos {@code *Pagado}
 *       persistidos de la cuota; los saldos siempre se reconstruyen desde los PagoPrestamo
 *       VIGENTES (los anulados por un reverso no cuentan).</li>
 *   <li>Estados espejo: toda escritura de estado de cuota setea {@code estado} e
 *       {@code idEstado} con el mismo valor.</li>
 *   <li>El estado del préstamo se lee y escribe en {@code idEstado} (PRSTIDST); NUNCA en
 *       {@code estadoPrestamo} (ESPSCDGO).</li>
 * </ul>
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface MotorPagoPrestamoService {

    /**
     * Reconstruye los saldos reales de una cuota desde sus PagoPrestamo VIGENTES.
     *
     * Efecto secundario deliberado (autocorrección, §6.2): si la cuota resulta liquidada
     * (totalPendiente &lt;= 0.01) y no está PAGADA(4) ni CANCELADA_ANTICIPADA(7), la pasa a
     * PAGADA, sincroniza sus campos {@code *Pagado} y la persiste.
     *
     * @param cuota Cuota a evaluar
     * @return Saldos por componente y total pendiente
     * @throws Throwable Si ocurre un error
     */
    SaldosCuota calcularSaldosRealesCuota(DetallePrestamo cuota) throws Throwable;

    /**
     * Deuda total pendiente del préstamo: suma del totalPendiente de todas sus cuotas
     * pendientes. Es el tope que valida {@code pagarCuota} antes de aplicar (§7.1).
     *
     * @param idPrestamo Código del préstamo
     * @return Deuda total pendiente redondeada a 2 decimales
     * @throws Throwable Si ocurre un error
     */
    double calcularTotalPendientePrestamo(Long idPrestamo) throws Throwable;

    /**
     * Aplica un valor al préstamo EN CASCADA: imputa a la mínima cuota pendiente y, si
     * sobra, sigue con la siguiente, hasta agotar el valor o quedarse sin cuotas.
     *
     * @param idPrestamo Código del préstamo
     * @param valor      Valor a aplicar
     * @param ctx        Contexto de la operación (tipo, usuario, fecha, evento)
     * @return Resultado con el desglose por cuota y el estado final del préstamo
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Si el préstamo no existe o está en estado terminal
     */
    ResultadoAplicacionPago aplicarPago(Long idPrestamo, double valor, ContextoPago ctx) throws Throwable;

    /**
     * Aplica hasta {@code valorDisponible} sobre UNA cuota, respetando la prelación de 6
     * componentes, y crea su PagoPrestamo. No hay cascada: lo que no cabe en la cuota no
     * se aplica (el llamador decide qué hacer con el resto).
     *
     * @param cuota           Cuota destino
     * @param valorDisponible Valor máximo a imputar
     * @param ctx             Contexto de la operación
     * @return Detalle de lo aplicado; {@code totalAplicado} = 0 si la cuota ya no tenía saldo
     * @throws Throwable Si ocurre un error
     */
    DetalleAplicacionCuota aplicarPagoACuota(DetallePrestamo cuota, double valorDisponible, ContextoPago ctx) throws Throwable;

    /**
     * Cancela el préstamo (CANCELADO = 3) si ya no le quedan cuotas pendientes.
     *
     * Escribe en {@code idEstado} (PRSTIDST), nunca en ESPSCDGO; no toca {@code fechaFin};
     * exige que el préstamo tenga tabla de amortización; y captura {@code Throwable}
     * internamente: un fallo aquí NO aborta el pago.
     *
     * @param prestamo Préstamo a evaluar
     * @return true si esta llamada dejó el préstamo en CANCELADO
     * @throws Throwable Si ocurre un error (no se propaga: el método atrapa internamente)
     */
    boolean verificarYActualizarEstadoPrestamo(Prestamo prestamo) throws Throwable;

    /**
     * Reconstruye la cuota ÍNTEGRAMENTE desde sus PagoPrestamo vigentes: campos
     * {@code *Pagado}, saldos, saldo global, estado y fechaPagado. Es la operación que usa
     * el reverso (§7.6) después de marcar pagos como anulados.
     *
     * @param cuota Cuota a reconstruir
     * @throws Throwable Si ocurre un error
     */
    void recalcularCuotaDesdePagos(DetallePrestamo cuota) throws Throwable;
}
