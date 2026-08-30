package com.saa.ejb.crd.service;

import java.time.LocalDate;

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
}
