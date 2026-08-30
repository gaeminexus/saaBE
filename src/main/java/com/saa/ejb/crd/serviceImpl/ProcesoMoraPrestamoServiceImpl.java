package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.dto.ResultadoCalculoMora;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación del proceso diario de interés de mora.
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class ProcesoMoraPrestamoServiceImpl implements ProcesoMoraPrestamoService {

    /** Base de días del cálculo financiero, igual que en el G48 y el CCPM */
    private static final double BASE_DIAS_ANIO = 360.0;

    /** Tasa por defecto cuando el préstamo no tiene interés nominal (mismo default del G48) */
    private static final double TASA_POR_DEFECTO = 9.0;

    /** Máximo de errores que se detallan en el resumen */
    private static final int MAX_ERRORES_DETALLADOS = 50;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    /**
     * Auto-inyección: permite que el bucle del lote invoque {@code calcularMoraPrestamo} a
     * TRAVÉS del proxy EJB, para que cada préstamo corra en su propia transacción
     * (REQUIRES_NEW). Una llamada directa {@code this.calcularMoraPrestamo(...)} se saltaría el
     * interceptor y todo el lote quedaría en una sola transacción.
     */
    @EJB
    private ProcesoMoraPrestamoService self;

    // ========================================================================
    // Lote diario
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ResultadoCalculoMora calcularMoraDiaria(LocalDate fechaCorte, String usuario) throws Throwable {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDate fecha = fechaCorte != null ? fechaCorte : LocalDate.now();

        System.out.println("========================================");
        System.out.println("PROCESO DIARIO DE INTERÉS DE MORA - Fecha de corte: " + fecha
            + " - Usuario: " + usuario);
        System.out.println("========================================");

        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha de corte " + fecha + " es futura");
        }

        ResultadoCalculoMora resumen = new ResultadoCalculoMora();
        resumen.setFechaCorte(fecha);
        resumen.setFechaInicio(inicio);
        resumen.setPrestamosProcesados(0);
        resumen.setCuotasActualizadas(0);
        resumen.setCuotasMarcadasEnMora(0);
        resumen.setPrestamosMarcadosEnMora(0);
        resumen.setPrestamosRegularizados(0);
        resumen.setPrestamosConError(0);
        resumen.setTotalMoraCalculada(0.0);

        LocalDateTime corte = corteDelDia(fecha);

        // 1. Préstamos con al menos una cuota vencida impaga
        List<Long> prestamos = detallePrestamoDaoService.selectPrestamosConCuotasVencidas(corte);
        resumen.setPrestamosEvaluados(prestamos != null ? prestamos.size() : 0);
        System.out.println("Préstamos a procesar: " + resumen.getPrestamosEvaluados());

        if (prestamos != null) {
            for (Long idPrestamo : prestamos) {
                try {
                    // A través del proxy: cada préstamo commitea por separado
                    ResultadoCalculoMora parcial = self.calcularMoraPrestamo(idPrestamo, fecha, usuario);
                    acumular(resumen, parcial);
                    resumen.setPrestamosProcesados(resumen.getPrestamosProcesados() + 1);

                } catch (Throwable e) {
                    // Un préstamo con datos malos no aborta el lote
                    resumen.setPrestamosConError(resumen.getPrestamosConError() + 1);
                    if (resumen.getErrores().size() < MAX_ERRORES_DETALLADOS) {
                        resumen.getErrores().add("Préstamo " + idPrestamo + ": " + e.getMessage());
                    }
                    System.err.println("Error al calcular la mora del préstamo " + idPrestamo + ": "
                        + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // 2. Préstamos EN_MORA que ya no tienen cuotas vencidas → vuelven a VIGENTE
        try {
            List<Long> regularizados = detallePrestamoDaoService.selectPrestamosEnMoraSinCuotasVencidas(corte);
            if (regularizados != null) {
                for (Long idPrestamo : regularizados) {
                    try {
                        if (self.calcularMoraPrestamo(idPrestamo, fecha, usuario) != null) {
                            resumen.setPrestamosRegularizados(resumen.getPrestamosRegularizados() + 1);
                        }
                    } catch (Throwable e) {
                        resumen.setPrestamosConError(resumen.getPrestamosConError() + 1);
                        if (resumen.getErrores().size() < MAX_ERRORES_DETALLADOS) {
                            resumen.getErrores().add("Préstamo " + idPrestamo + " (regularización): "
                                + e.getMessage());
                        }
                        System.err.println("Error al regularizar el préstamo " + idPrestamo + ": "
                            + e.getMessage());
                    }
                }
            }
        } catch (Throwable e) {
            System.err.println("Error al buscar préstamos a regularizar: " + e.getMessage());
            e.printStackTrace();
        }

        LocalDateTime fin = LocalDateTime.now();
        resumen.setFechaFin(fin);
        resumen.setDuracionMs(ChronoUnit.MILLIS.between(inicio, fin));
        resumen.setTotalMoraCalculada(redondear(resumen.getTotalMoraCalculada()));

        System.out.println("========================================");
        System.out.println("PROCESO DE MORA TERMINADO"
            + " - Préstamos evaluados: " + resumen.getPrestamosEvaluados()
            + " - Procesados: " + resumen.getPrestamosProcesados()
            + " - Cuotas actualizadas: " + resumen.getCuotasActualizadas()
            + " - Mora total: $" + resumen.getTotalMoraCalculada()
            + " - Marcados EN_MORA: " + resumen.getPrestamosMarcadosEnMora()
            + " - Regularizados: " + resumen.getPrestamosRegularizados()
            + " - Errores: " + resumen.getPrestamosConError()
            + " - Duración: " + resumen.getDuracionMs() + " ms");
        System.out.println("========================================");

        return resumen;
    }

    // ========================================================================
    // Unidad de trabajo: un préstamo, una transacción
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoCalculoMora calcularMoraPrestamo(Long idPrestamo, LocalDate fechaCorte, String usuario)
            throws Throwable {

        LocalDate fecha = fechaCorte != null ? fechaCorte : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha de corte " + fecha + " es futura");
        }

        System.out.println("  → Calculando mora del préstamo " + idPrestamo + " al " + fecha);

        ResultadoCalculoMora resumen = new ResultadoCalculoMora();
        resumen.setFechaCorte(fecha);
        resumen.setPrestamosEvaluados(1);
        resumen.setPrestamosProcesados(1);
        resumen.setCuotasActualizadas(0);
        resumen.setCuotasMarcadasEnMora(0);
        resumen.setPrestamosMarcadosEnMora(0);
        resumen.setPrestamosRegularizados(0);
        resumen.setPrestamosConError(0);
        resumen.setTotalMoraCalculada(0.0);

        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ERR_PRESTAMO_NO_ENCONTRADO + ": no existe el préstamo " + idPrestamo);
        }

        // GUARDA: DE_PLAZO_VENCIDO(8) queda fuera de este proceso (corregido el 2026-08-24).
        //
        // El universo del lote ya lo excluye (selectPrestamosConCuotasVencidas), pero el
        // endpoint POST /prst/calcularMora/{idPrestamo} entra directamente acá salteándose esa
        // consulta. Sin esta guarda, un préstamo en 8 invocado a mano se seguiría
        // reclasificando a EN_MORA(11) más abajo. Defensa en los dos niveles.
        //
        // Se sale SIN calcular mora y SIN TOCAR NINGÚN ESTADO: ni el del préstamo ni el de sus
        // cuotas. El resumen vuelve en cero, que es exactamente lo que ocurrió.
        if (prestamo.getIdEstado() != null
                && prestamo.getIdEstado().intValue() == EstadoPrestamo.DE_PLAZO_VENCIDO) {
            System.out.println("      Préstamo " + idPrestamo + " en estado 8 (DE PLAZO VENCIDO):"
                + " fuera del proceso de mora. No se calcula mora y no se toca ningún estado.");
            resumen.setPrestamosProcesados(0);
            resumen.setFechaFin(LocalDateTime.now());
            return resumen;
        }

        LocalDateTime corte = corteDelDia(fecha);

        // Tasa de mora = tasa nominal del préstamo; mismo default que el G48 cuando falta
        double tasaDiaria = tasaDiariaDelPrestamo(prestamo, true);

        List<DetallePrestamo> vencidas =
            detallePrestamoDaoService.selectCuotasVencidasByPrestamo(idPrestamo, corte);

        double moraDelPrestamo = 0.0;

        if (vencidas != null) {
            for (DetallePrestamo cuota : vencidas) {
                if (cuota.getFechaVencimiento() == null) {
                    continue;
                }
                long diasMora = ChronoUnit.DAYS.between(cuota.getFechaVencimiento().toLocalDate(), fecha);
                if (diasMora <= 0) {
                    continue;
                }

                // Bug corregido 2026-08-28: la fórmula de mora se extrajo a calcularMoraCuota
                // (pura, sin persistir) para que la precancelación pueda reusarla con la fecha
                // que elija el usuario. Acá diasMora se recalcula arriba (sin cambios) porque
                // DTPRDSMR se persiste con ese valor más abajo, aparte de moraNueva.
                double moraNueva = calcularMoraCuota(cuota, tasaDiaria, fecha);

                // IDEMPOTENCIA: el total se recompone quitando la mora anterior y sumando la
                // nueva. Así el proceso puede correrse N veces el mismo día sin acumular, y se
                // respeta la base original de la cuota (incluidas las tablas cargadas de Excel).
                double moraAnterior = nvl(cuota.getMora());
                double totalBase = redondear(nvl(cuota.getTotal()) - moraAnterior);
                double totalNuevo = redondear(totalBase + moraNueva);

                cuota.setMora(moraNueva);
                cuota.setMoraCalculada(moraNueva);
                cuota.setDiasMora(diasMora);
                cuota.setTotal(totalNuevo);
                cuota.setTotalConSeguro(totalNuevo);
                // saldoMora = mora − moraPagado, igual que lo reconstruye el motor de pagos
                cuota.setSaldoMora(Math.max(0, redondear(moraNueva - nvl(cuota.getMoraPagado()))));

                // La cuota vencida pasa a EN_MORA salvo que esté PARCIAL: ese estado indica que
                // ya recibió un pago y lo maneja el motor.
                boolean marcada = false;
                Long estadoActual = cuota.getEstado();
                if (estadoActual == null
                        || (estadoActual != EstadoCuotaPrestamo.EN_MORA
                         && estadoActual != EstadoCuotaPrestamo.PARCIAL)) {
                    cuota.setEstado((long) EstadoCuotaPrestamo.EN_MORA);
                    cuota.setIdEstado((long) EstadoCuotaPrestamo.EN_MORA);
                    marcada = true;
                }

                detallePrestamoService.saveSingle(cuota);

                moraDelPrestamo += moraNueva;
                resumen.setCuotasActualizadas(resumen.getCuotasActualizadas() + 1);
                if (marcada) {
                    resumen.setCuotasMarcadasEnMora(resumen.getCuotasMarcadasEnMora() + 1);
                }

                System.out.println("      Cuota #" + cuota.getNumeroCuota()
                    + " - Días: " + diasMora + " - Mora: $" + moraNueva
                    + " - Total: " + totalBase + " → " + totalNuevo);
            }
        }

        // Estado del préstamo. NUNCA se toca ESPSCDGO ni un estado terminal (3, 4, 5).
        boolean tieneVencidas = vencidas != null && !vencidas.isEmpty();
        Long estadoPrestamo = prestamo.getIdEstado();

        if (!esEstadoTerminalPrestamo(estadoPrestamo)) {
            if (tieneVencidas
                    && estadoPrestamo != null && estadoPrestamo != EstadoPrestamo.EN_MORA) {
                prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.EN_MORA));
                prestamo.setFechaModificacion(LocalDateTime.now());
                prestamoDaoService.save(prestamo, prestamo.getCodigo());
                resumen.setPrestamosMarcadosEnMora(1);
                System.out.println("      Préstamo " + idPrestamo + ": " + estadoPrestamo
                    + " → 11 (EN_MORA)");

            } else if (regularizarSiSinCuotasVencidas(prestamo, tieneVencidas)) {
                resumen.setPrestamosRegularizados(1);
            }
        }

        resumen.setTotalMoraCalculada(redondear(moraDelPrestamo));
        resumen.setFechaFin(LocalDateTime.now());
        return resumen;
    }

    @Override
    public boolean regularizarPrestamoSiSinMora(Long idPrestamo) throws Throwable {
        if (idPrestamo == null) {
            return false;
        }
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null || esEstadoTerminalPrestamo(prestamo.getIdEstado())
                || prestamo.getIdEstado() == null
                || prestamo.getIdEstado() != EstadoPrestamo.EN_MORA) {
            return false;
        }
        List<DetallePrestamo> vencidas = detallePrestamoDaoService.selectCuotasVencidasByPrestamo(
            idPrestamo, corteDelDia(LocalDate.now()));
        boolean tieneVencidas = vencidas != null && !vencidas.isEmpty();
        return regularizarSiSinCuotasVencidas(prestamo, tieneVencidas);
    }

    // ========================================================================
    // Fórmula de mora — PURA, sin persistir (extraída 2026-08-28)
    // ========================================================================

    @Override
    public double tasaDiariaDelPrestamo(Prestamo prestamo, boolean loguearDefault) {
        double tasaNominal = prestamo != null && prestamo.getInteresNominal() != null
            ? prestamo.getInteresNominal() : 0.0;
        if (tasaNominal <= 0.0) {
            tasaNominal = TASA_POR_DEFECTO;
            if (loguearDefault) {
                // Traza del default silencioso (PLAN-SIMULADORES-PRESTAMOS.md decisión 11 / D10):
                // desde que PrestamoServiceImpl.saveSingle deriva interesNominal de tasa en cada
                // guardado, esto solo debería activarse en préstamos guardados ANTES del fix.
                System.out.println("      ADVERTENCIA: préstamo " + (prestamo != null ? prestamo.getCodigo() : null)
                    + " sin interesNominal (PRSTINNM); se usa el default silencioso de "
                    + TASA_POR_DEFECTO + "% (mismo default del G48).");
            }
        }
        return tasaNominal / 100.0 / BASE_DIAS_ANIO;
    }

    @Override
    public double calcularMoraCuota(DetallePrestamo cuota, double tasaDiaria, LocalDate fecha) {
        if (cuota == null || cuota.getFechaVencimiento() == null || fecha == null) {
            return 0.0;
        }
        long diasMora = ChronoUnit.DAYS.between(cuota.getFechaVencimiento().toLocalDate(), fecha);
        if (diasMora <= 0) {
            return 0.0;
        }
        double capital = nvl(cuota.getCapital());
        return capital > 0.0 ? redondear(capital * tasaDiaria * diasMora) : 0.0;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Núcleo compartido de la regularización EN_MORA → VIGENTE. Ver
     * {@link ProcesoMoraPrestamoService#regularizarPrestamoSiSinMora}.
     *
     * @param prestamo      Préstamo YA CARGADO (se usa tal cual, no se vuelve a leer de BD)
     * @param tieneVencidas Si el préstamo tiene cuotas vencidas, ya calculado por el llamador
     *                      con el mismo criterio del proceso diario
     * @return true si se regularizó
     */
    private boolean regularizarSiSinCuotasVencidas(Prestamo prestamo, boolean tieneVencidas) throws Throwable {
        if (tieneVencidas || prestamo == null || esEstadoTerminalPrestamo(prestamo.getIdEstado())) {
            return false;
        }
        if (prestamo.getIdEstado() == null || prestamo.getIdEstado() != EstadoPrestamo.EN_MORA) {
            return false;
        }
        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.VIGENTE));
        prestamo.setFechaModificacion(LocalDateTime.now());
        prestamoDaoService.save(prestamo, prestamo.getCodigo());
        System.out.println("      Préstamo " + prestamo.getCodigo() + ": 11 → 2 (VIGENTE, sin cuotas vencidas)");
        return true;
    }

    private void acumular(ResultadoCalculoMora resumen, ResultadoCalculoMora parcial) {
        if (parcial == null) {
            return;
        }
        resumen.setCuotasActualizadas(resumen.getCuotasActualizadas() + nvlInt(parcial.getCuotasActualizadas()));
        resumen.setCuotasMarcadasEnMora(resumen.getCuotasMarcadasEnMora() + nvlInt(parcial.getCuotasMarcadasEnMora()));
        resumen.setPrestamosMarcadosEnMora(
            resumen.getPrestamosMarcadosEnMora() + nvlInt(parcial.getPrestamosMarcadosEnMora()));
        resumen.setTotalMoraCalculada(resumen.getTotalMoraCalculada() + nvl(parcial.getTotalMoraCalculada()));
    }

    /**
     * Instante de corte del día. Se usa el INICIO del día: una cuota que vence hoy todavía no
     * está vencida, igual que en los reportes (donde la cuota del mes va a "por vencer").
     */
    private LocalDateTime corteDelDia(LocalDate fecha) {
        return fecha.atStartOfDay();
    }

    private boolean esEstadoTerminalPrestamo(Long idEstado) {
        return idEstado != null
            && (idEstado == EstadoPrestamo.CANCELADO
             || idEstado == EstadoPrestamo.CANCELADO_ANTICIPADO
             || idEstado == EstadoPrestamo.CANCELADO_POR_NOVACION);
    }

    private int nvlInt(Integer valor) {
        return valor != null ? valor : 0;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
