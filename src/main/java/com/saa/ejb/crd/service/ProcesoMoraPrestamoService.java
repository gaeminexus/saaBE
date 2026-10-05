package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.ResultadoCalculoMora;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Proceso diario de cálculo del interés de mora de las cuotas vencidas.
 *
 * Replica la fórmula que ya usan los reportes financieros (G48 Grupo 2, CCPM) a través de
 * {@code DetallePrestamoDaoService.calcularInteresMoraBatch}, con dos diferencias:
 * <ul>
 *   <li>Calcula con la fecha de HOY, no con la fecha de corte de un reporte.</li>
 *   <li><b>Persiste</b> el resultado en la cuota, en vez de solo informarlo.</li>
 * </ul>
 *
 * Fórmula por cuota vencida (idéntica a la de los reportes):
 * <pre>
 *   mora = capital × (prestamo.interesNominal / 100 / 360) × díasDeMora
 *   díasDeMora = días entre la fechaVencimiento de la cuota y la fecha de corte
 *   Si interesNominal es nulo o &lt;= 0 se usa 9.0 (mismo default que el G48)
 * </pre>
 *
 * El total de un préstamo, {@code SUM(DTPRMRAA)} sobre sus cuotas vencidas, coincide con el
 * valor que hoy reporta el G48: es la misma sumatoria, descompuesta por cuota.
 *
 * <b>Universo</b>: cuotas pendientes ({@code estado IS NULL OR estado NOT IN (4,7)}) con
 * {@code fechaVencimiento} anterior a la fecha de corte, de préstamos en estado
 * <b>VIGENTE(2) o EN_MORA(11)</b>.
 *
 * <p><b>DE_PLAZO_VENCIDO(8) NO entra</b> (corregido el 2026-08-24). El universo original
 * copiaba el del Grupo 2 del G48, que sí incluye el 8, y eso era un defecto: el G48 solo
 * <i>lee</i> la mora, mientras que este proceso <i>escribe</i> el estado del préstamo. El
 * resultado fue que todos los préstamos en DE PLAZO VENCIDO quedaron reclasificados a
 * EN_MORA(11) en producción. La exclusión está en dos niveles: en la consulta del universo y
 * en una guarda dentro de {@code calcularMoraPrestamo}, porque el endpoint por préstamo no
 * pasa por la consulta.</p>
 *
 * <b>Qué escribe en cada cuota vencida</b>: {@code DTPRMRAA} (mora), {@code DTPRMRCL}
 * (mora calculada), {@code DTPRDSMR} (días de mora), {@code DTPRTTLL} (total, con la mora
 * incluida), y el estado a EN_MORA(5). A nivel de préstamo marca EN_MORA(11) y devuelve a
 * VIGENTE(2) los que ya no tienen cuotas vencidas.
 *
 * <b>Idempotente</b>: puede correrse varias veces el mismo día. El total se recompone como
 * {@code total − moraAnterior + moraNueva}, de modo que la mora nunca se acumula sobre sí misma
 * y se respeta la base original de la cuota (incluidas las tablas cargadas desde Excel).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface ProcesoMoraPrestamoService {

    /** 404 - El préstamo no existe */
    String ERR_PRESTAMO_NO_ENCONTRADO = "PRESTAMO_NO_ENCONTRADO";
    /** 422 - La fecha de corte no es válida */
    String ERR_FECHA_INVALIDA = "FECHA_INVALIDA";

    /** Usuario con el que el temporizador registra la corrida automática */
    String USUARIO_PROCESO = "SAA_MORA";

    /**
     * Calcula y persiste el interés de mora de TODAS las cuotas vencidas del sistema.
     *
     * Es el método que dispara el temporizador nocturno y el que expone el endpoint manual de
     * recuperación. Cada préstamo se procesa en su propia transacción: un préstamo con datos
     * malos se cuenta como error y NO aborta el lote.
     *
     * @param fechaCorte Fecha con la que se calcula la mora; si es null se usa hoy. No puede ser futura
     * @param usuario    Usuario que ejecuta (o {@link #USUARIO_PROCESO} en la corrida automática)
     * @return Resumen de la corrida con conteos, total calculado y errores
     * @throws Throwable Si ocurre un error irrecuperable
     */
    ResultadoCalculoMora calcularMoraDiaria(LocalDate fechaCorte, String usuario) throws Throwable;

    /**
     * Calcula y persiste el interés de mora de las cuotas vencidas de UN préstamo, en su propia
     * transacción. Es la unidad de trabajo del lote y también sirve para recalcular un préstamo
     * puntual desde el frontend.
     *
     * <p><b>Un préstamo en DE_PLAZO_VENCIDO(8) se saltea</b>: devuelve el resumen en cero sin
     * calcular mora y <b>sin tocar ningún estado</b>, ni el del préstamo ni el de sus cuotas.
     * La guarda está acá y no solo en el universo del lote porque el endpoint por préstamo
     * entra directamente a este método, salteándose la consulta que arma ese universo.</p>
     *
     * <p>Los estados terminales (3, 4, 5) tampoco se tocan, pero eso ya lo resolvía la lógica
     * de estado del préstamo.</p>
     *
     * @param idPrestamo Código del préstamo
     * @param fechaCorte Fecha con la que se calcula la mora; si es null se usa hoy
     * @param usuario    Usuario que ejecuta
     * @return Resumen de la corrida para ese préstamo; en cero si el préstamo está en 8
     * @throws Throwable Si ocurre un error
     */
    ResultadoCalculoMora calcularMoraPrestamo(Long idPrestamo, LocalDate fechaCorte, String usuario) throws Throwable;

    /**
     * Si el préstamo está EN_MORA(11) y HOY ya no tiene cuotas vencidas, lo regresa a
     * VIGENTE(2). No hace nada si el préstamo no está en 11, si todavía tiene cuotas
     * vencidas, o si está en un estado terminal (3, 4, 5): esos nunca se reabren
     * automáticamente. Escribe únicamente en {@code PRSTIDST}, nunca en {@code ESPSCDGO}.
     *
     * <p>Es la misma lógica que ya corría dentro de {@link #calcularMoraPrestamo} (extraída
     * el 2026-08-27, pedido 10 del plan de devengo de aportes) para que un préstamo no se
     * quede en mora hasta el proceso de las 02:00 cuando el partícipe se pone al día con un
     * cruce o un abono inmediato. "Cuota vencida" se decide con el mismo criterio del
     * proceso diario ({@code DetallePrestamoDaoService.selectCuotasVencidasByPrestamo}, corte
     * de HOY), no con uno nuevo.</p>
     *
     * @param idPrestamo Código del préstamo
     * @return true si se regularizó (pasó de EN_MORA a VIGENTE); false si no había nada que hacer
     * @throws Throwable Si ocurre un error
     */
    boolean regularizarPrestamoSiSinMora(Long idPrestamo) throws Throwable;

    /**
     * Tasa diaria de mora del préstamo: {@code interesNominal / 100 / 360}, con el mismo
     * default silencioso de {@code TASA_POR_DEFECTO} (9%) que usa {@link #calcularMoraPrestamo}
     * cuando el préstamo no tiene {@code interesNominal} (PRSTINNM). No lee de BD ni persiste
     * nada — usa el {@code prestamo} tal cual se lo pasen.
     *
     * @param prestamo        Préstamo a evaluar
     * @param loguearDefault  Si es {@code true} y se activa el default, imprime la misma
     *                        advertencia que el proceso diario. Pasar {@code false} cuando se
     *                        va a llamar una vez por cuota (p. ej. desde un simulador que
     *                        recorre varias cuotas del mismo préstamo) para no repetir el log.
     * @return La tasa diaria ya resuelta
     */
    double tasaDiariaDelPrestamo(Prestamo prestamo, boolean loguearDefault);

    /**
     * Fórmula PURA de mora — {@code capital × tasaDiaria × díasMora} — sin persistir nada ni
     * marcar la cuota o el préstamo EN_MORA. Es el mismo cálculo que
     * {@link #calcularMoraPrestamo} aplica y guarda para el proceso diario, extraído el
     * 2026-08-28 para que un simulador (p. ej. la precancelación) pueda recalcular la mora a la
     * fecha que elija el usuario, en vez de leer el {@code DTPRMRAA}/{@code DTPRSLMR} que dejó
     * el último proceso de las 02:00.
     *
     * @param cuota      Cuota a evaluar (usa {@code capital} y {@code fechaVencimiento})
     * @param tasaDiaria Tasa diaria ya resuelta — ver {@link #tasaDiariaDelPrestamo}
     * @param fecha      Fecha de corte
     * @return La mora que correspondería a esa fecha; {@code 0.0} si la cuota no está vencida a
     *         esa fecha, si {@code fechaVencimiento} es nula, o si el capital es 0/nulo
     */
    double calcularMoraCuota(DetallePrestamo cuota, double tasaDiaria, LocalDate fecha);

    /**
     * H42 (cobro tardío, FASE 1, {@code docs/logica-negocio/crd/DISENO-COBRO-CON-FECHA-EFECTIVA.md}
     * §4quater): recalcula y <b>persiste</b> la mora de TODAS las cuotas no PAGADA(4) y no
     * CANCELADA_ANTICIPADA(7) de un préstamo, a la fecha EFECTIVA de un pago que se procesa
     * tarde — no a hoy. Se llama ANTES de aplicar el pago, para que el motor cobre la mora
     * correcta en vez de la que calculó la corrida nocturna hasta anoche.
     *
     * <p>NO es una reutilización de {@link #calcularMoraPrestamo}: ese método recorre solo las
     * cuotas YA vencidas a la fecha de corte y, si {@code diasMora <= 0}, hace {@code continue}
     * dejando la mora vieja intacta — que es justo lo que hay que limpiar acá (una cuota cuyo
     * vencimiento quedó posterior a la fecha de pago, pero anterior a hoy, ya tiene mora
     * calculada de más por la corrida nocturna). Este método recorre TODAS las no terminales y
     * decide la mora rama por rama:</p>
     * <ul>
     *   <li>{@code fechaVencimiento < fechaPago}: {@code moraNueva = calcularMoraCuota(cuota,
     *       tasaDiariaDelPrestamo(prestamo, false), fechaPago)}, con sus días de mora hasta esa
     *       fecha. El estado de la cuota no se toca acá (lo gobierna, como siempre, el proceso
     *       diario).</li>
     *   <li>{@code fechaVencimiento >= fechaPago}: {@code moraNueva = 0} y días de mora 0. Si la
     *       cuota está EN_MORA(5) — la marcó la corrida nocturna sin que el pago lo supiera —
     *       vuelve a PENDIENTE(1) en {@code estado} e {@code idEstado}. Una PARCIAL(6) no cambia
     *       de estado: ya recibió un pago real y eso lo gobierna el motor.</li>
     * </ul>
     * {@code total}/{@code totalConSeguro} se recomponen con el mismo patrón idempotente de
     * {@link #calcularMoraPrestamo} ({@code total - moraAnterior + moraNueva}), y
     * {@code saldoMora = max(0, moraNueva - moraPagado)}.
     *
     * <p><b>El estado del PRÉSTAMO no se toca</b> — lo regulariza, como siempre, el proceso
     * diario de esa misma noche.</p>
     *
     * @param idPrestamo Código del préstamo
     * @param fechaPago  Fecha efectiva del pago (anterior a hoy); la mora de cada cuota se
     *                   recalcula a esta fecha, no a la de hoy
     * @return La mora total eliminada del préstamo: Σ(mora anterior − mora nueva) de todas las
     *         cuotas recalculadas. Es el insumo de la FASE 2 (el reverso contable)
     * @throws Throwable Si ocurre un error
     */
    double recalcularMoraALaFechaDePago(Long idPrestamo, LocalDate fechaPago) throws Throwable;

    /**
     * Mismo recálculo que {@link #recalcularMoraALaFechaDePago} (de hecho, esa delega en este:
     * una sola implementación, nunca dos copias) pero con el detalle POR CUOTA — ÍTEM 5 del
     * frente de provisión de intereses (cobro tardío, §6.2/§7bis) necesita saber, cuota por
     * cuota, cuánta mora PROVISIONADA (tipo 3) y cuánta mora DEVENGADA (tipo 5, tipo 6) quedó de
     * más, y eso no se puede derivar del total agregado que devuelve el método original.
     *
     * @param idPrestamo Código del préstamo
     * @param fechaPago  Fecha efectiva del pago
     * @return Filas {@code [idCuota (Long), moraAnterior (Double), moraNueva (Double)]}, SOLO
     *         las cuotas realmente recalculadas por este método (no terminales, con
     *         vencimiento); una cuota con {@code moraAnterior == moraNueva} igual aparece (el
     *         llamador filtra "sin cambio" si le importa, acá no se descarta nada)
     * @throws Throwable Si ocurre un error
     */
    List<Object[]> recalcularMoraALaFechaDePagoDetalle(Long idPrestamo, LocalDate fechaPago) throws Throwable;
}
