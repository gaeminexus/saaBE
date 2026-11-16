package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.ejb.crd.service.dto.ResultadoSincronizacionGarantia;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * C31 (16-nov-2026): la garantía de un préstamo en cuentas de orden sale por el MONTO OTORGADO
 * ({@code PRSTMNSL}, el mismo valor con que la entrega la registra) cuando el préstamo termina, y vuelve a
 * entrar si la terminación se revierte. Generaliza C30 (novación) a todos los caminos de terminación.
 *
 * <p><b>C31c (16-nov-2026):</b> la baja automática solo la genera la operación que <b>hace pasar el préstamo
 * de vivo a terminado</b>, no el hecho de encontrarlo terminado sin baja. Cada proceso captura el estado
 * del préstamo antes de su operación y lo pasa a {@link #sincronizar}. Para los terminados históricos sin
 * baja está {@link #sincronizarExplicito} ({@code POST /prst/sincronizarGarantia}).</p>
 *
 * <p>Las dos vías son idempotentes: comparan con el neto de las bajas ya contabilizadas para ese préstamo
 * (marca {@code [C31 PRST id]} en la glosa de las líneas) y no duplican.</p>
 */
@Local
public interface GarantiaOrdenPrestamoService {

    /**
     * Deja las cuentas de orden de la garantía del préstamo de acuerdo con la operación que se acaba de
     * aplicar:
     * <ul>
     *   <li><b>Baja</b> (plantilla 26, monto otorgado más el bien si es prendario/hipotecario) solo si
     *       {@code estadoAnterior} era vivo (2, 8, 10, 11), el estado actual es CANCELADO (3) o
     *       CANCELADO_ANTICIPADO (4) y no hay baja vigente. Si el préstamo ya estaba terminado antes de la
     *       operación no se genera baja aunque no tenga una (C31c).</li>
     *   <li><b>Control cronológico</b> (solo si {@code pagosHasta} no es null, es decir en la cancelación
     *       por pago de cuotas): además, ningún pago vigente del préstamo puede tener fecha posterior al día
     *       de la operación. Si lo hay, el préstamo «termina» solo porque sus cuotas siguientes ya figuran
     *       pagadas por cobros posteriores (re-proceso de un mes pasado sobre la copia de producción): no es
     *       la terminación de esta operación y no se da de baja.</li>
     *   <li><b>Anulación</b>: vivo con una baja vigente (la terminación se revirtió): se anula el asiento de
     *       baja (o se reversa en el primer período abierto si el suyo está cerrado), cualquiera sea
     *       {@code estadoAnterior}.</li>
     *   <li>Cualquier otro caso: no hace nada. La novación (5) la resuelve el asiento de la sustitución
     *       (C30); la reestructuración (13) queda pendiente de decisión.</li>
     * </ul>
     *
     * @param prestamo       préstamo ya con su estado nuevo
     * @param estadoAnterior estado del préstamo ({@code PRSTIDST}) capturado ANTES de la operación; null =
     *                       desconocido, no genera baja (solo puede anular)
     * @param idEmpresa      empresa contable; si es null se toma la del asiento de entrega o la única empresa
     *                       con la plantilla 26
     * @param fecha          fecha contable de la terminación (la del cobro / afectación); null = hoy
     * @param pagosHasta     fecha con que la operación registró sus propios pagos ({@code PGPRFCHA}); si no es
     *                       null activa el control cronológico, con límite en el mayor día entre
     *                       {@code fecha} y este. null = sin control (precancelación, acuerdo, reaperturas)
     * @param usuario        usuario que registra o anula
     * @param causa          texto corto para la glosa (camino de terminación)
     * @return código del asiento generado o anulado; null si no hubo nada que hacer
     */
    Long sincronizar(Prestamo prestamo, Long estadoAnterior, Long idEmpresa, LocalDate fecha,
            LocalDateTime pagosHasta, String usuario, String causa) throws Throwable;

    /**
     * C31c: sincronización explícita, a pedido, de un préstamo a una fecha ({@code POST /prst/sincronizarGarantia}).
     * Sirve para dar de baja los préstamos terminados que no la tienen (terminados antes de C31, los de
     * junio 2025, los que la vía automática dejó sin baja por el control cronológico) sin depender de una
     * operación:
     * <ul>
     *   <li>CANCELADO (3) o CANCELADO_ANTICIPADO (4) sin baja vigente: baja a {@code fecha}.</li>
     *   <li>Vivo con baja vigente: anula la baja.</li>
     *   <li>Terminado con baja vigente, vivo sin baja, u otro estado: no hace nada (lo informa).</li>
     * </ul>
     * Misma idempotencia por la marca {@code [C31 PRST id]}: llamarlo dos veces no duplica.
     *
     * @param idPrestamo préstamo ({@code PRSTCDGO})
     * @param idEmpresa  empresa contable; null = la del asiento de entrega o la única con la plantilla 26
     * @param fecha      fecha contable del asiento de baja (obligatoria, no futura; su período debe estar abierto)
     * @param usuario    usuario que ordena la sincronización (se valida que exista)
     * @param motivo     texto opcional para la glosa (p. ej. «pase a producción, terminado en junio 2025»)
     * @return lo que se hizo y el asiento
     */
    ResultadoSincronizacionGarantia sincronizarExplicito(Long idPrestamo, Long idEmpresa, LocalDate fecha,
            String usuario, String motivo) throws Throwable;
}
