package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.ResultadoReimputacion;

import jakarta.ejb.Local;

/**
 * C33 (cuadre Superintendencia, 21-nov-2026): re-imputación en orden y control de la prelación de cuotas. Reglas en
 * {@link PrelacionCuotas}; diagnóstico en {@code cuadreSuper/03-trabajo/cambios-saa/C33-CUOTA-PARCIAL-CON-POSTERIORES-PAGADAS.md}.
 */
@Local
public interface PrelacionCuotasService {

    /**
     * Punto 4: re-imputa en orden los pagos del préstamo al corte {@code fecha} (fin del día; {@code null} = hoy).
     *
     * <p>Si el préstamo tiene la prelación rota al corte ({@link PrelacionCuotas#detectarRotura}), toma los pagos
     * vigentes NO exentos con capital o interés y fecha {@code <= corte} desde la primera cuota con saldo, los anula
     * (motivo {@code C33-REIMPUTADO}) y vuelve a registrar el mismo dinero con la misma fecha por la cascada del motor
     * (prelación de 6 componentes, la cuota más antigua primero). Los PGPR nuevos conservan tipo, usuario y fecha de
     * registro, carga, evento, asiento y documento del original, con la marca en la observación. Los pagos con fecha
     * posterior al corte no se tocan ni cuentan. Con {@code idEmpresa}, re-provisiona lo que los anulados habían
     * reversado y reversa la provisión de los nuevos (asientos D/H 149905-470510, como el re-proceso de una carga);
     * sin ella, lo anota en el mensaje.</p>
     *
     * <p>No genera asiento de aplicación: el dinero y la fecha no cambian; si el reparto por componente cambia
     * ({@code deltaComponentes} distinto de 0), falta el asiento de reclasificación (queda en el resultado).</p>
     *
     * @param idPrestamo Préstamo
     * @param fecha      Corte (inclusive)
     * @param usuario    Usuario (anulación); obligatorio
     * @return Resultado (sin rotura: {@code rotura} nula y nada grabado)
     * @throws Throwable Si ocurre un error
     */
    ResultadoReimputacion reimputarPagosEnOrden(Long idPrestamo, LocalDate fecha, String usuario) throws Throwable;

    /**
     * Igual que {@link #reimputarPagosEnOrden(Long, LocalDate, String)}, con empresa contable (para la provisión) y
     * modo simulación ({@code simular = true}: calcula el plan y no graba nada).
     */
    ResultadoReimputacion reimputarPagosEnOrden(Long idPrestamo, LocalDate fecha, String usuario, Long idEmpresa,
            boolean simular) throws Throwable;

    /** Igual que el anterior, en su propia transacción (para el lote). */
    ResultadoReimputacion reimputarPagosEnOrdenNuevaTransaccion(Long idPrestamo, LocalDate fecha, String usuario,
            Long idEmpresa, boolean simular) throws Throwable;

    /**
     * Gancho de la reapertura de cuotas, la anulación de una operación y el re-proceso de una carga: si la prelación
     * quedó rota al corte, re-imputa ({@code saa.cartera.reimputarEnOrden}, por defecto {@code si}) o solo lo avisa
     * ({@code no}). Nunca bloquea la operación que lo llama: un error se escribe en el log y en el texto devuelto
     * (el control de cierre lo vuelve a ver).
     *
     * @return Texto para la bitácora/observación, o {@code null} si el préstamo cumple la prelación
     */
    String asegurarPrelacion(Long idPrestamo, LocalDate corte, String usuario, Long idEmpresa, String origen);

    /**
     * Punto 6: control al corte de toda la cartera (solo lectura). Ver
     * {@code DetallePrestamoDaoService#selectControlPrelacionAlCorte}.
     */
    List<Object[]> controlAlCorte(LocalDate corte) throws Throwable;

    /** Texto del control al corte para el cierre de cartera, o {@code null} si no hay préstamos. */
    String textoControlAlCorte(LocalDate corte) throws Throwable;

    /**
     * Lote para después de las restituciones SQL: re-imputa (o simula) cada préstamo del control al corte, cada uno en
     * su propia transacción (un préstamo con error no deshace los demás).
     */
    List<ResultadoReimputacion> reimputarAlCorte(LocalDate corte, String usuario, Long idEmpresa, boolean simular)
            throws Throwable;
}
