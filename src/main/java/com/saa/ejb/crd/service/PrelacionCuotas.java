package com.saa.ejb.crd.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.rubros.EstadoCuotaPrestamo;

/**
 * C33 (cuadre Superintendencia, 21-nov-2026): <b>no se aplica dinero a una cuota si una anterior tiene saldo</b>.
 * Todo pago se imputa a la cuota más antigua primero; el abono a capital y la precancelación tienen su propia regla y
 * quedan fuera. Ver {@code cuadreSuper/03-trabajo/cambios-saa/C33-CUOTA-PARCIAL-CON-POSTERIORES-PAGADAS.md} §5.1.
 *
 * <p>Esta clase es <b>pura</b> (sin EJB ni base): reglas, propiedades del sistema y el plan de la re-imputación en
 * orden, para que se puedan probar sin servidor ({@code src/test/java/.../PrelacionCuotasTest}). El motor
 * ({@code MotorPagoPrestamoServiceImpl}) la usa para leer la base y grabar.</p>
 *
 * <h3>Reglas</h3>
 * <ul>
 * <li><b>Pendiente real, no por estado (punto 1).</b> Una cuota PAGADA (4) con vencimiento desde
 * {@link #PROPIEDAD_DESDE} (defecto 1-jun-2025, el arranque del SAA) cuyo capital + interés no está cubierto por sus
 * pagos vigentes es una <i>marca sin pagos</i> (Delta 21): cuenta como pendiente. Antes del arranque la marca se
 * respeta: es la historia que el ancla de mayo 2025 (G48) ya da por pagada (en la v7 hay 3.300 cuotas previas con el
 * seguro de incendio sin PGPR). La CANCELADA_ANTICIPADA (7) nunca cuenta: la cierran la precancelación o el abono, que
 * registran el capital en la cuota ancla ({@code DTPRSLOT}), no en ella.</li>
 * <li><b>Pagos con fecha (punto 2).</b> Con fecha de corte solo cuentan los pagos con {@code PGPRFCHA <= corte}; un
 * pago sin fecha (migración) cuenta siempre.</li>
 * <li><b>Pagos exentos.</b> Tipo {@code PRECANCELACION}, {@code ABONO_CAPITAL} o {@code ACUERDO_CONDONACION}, o con
 * {@code PGPRSLOT > 0}: ni se bloquean ni se re-imputan.</li>
 * </ul>
 */
public final class PrelacionCuotas {

    /** Prefijo del error del invariante (punto 3) y del control de cierre (punto 6). */
    public static final String ERR_PRELACION_CUOTAS = "PRELACION_CUOTAS";

    /** Marca de los PGPR creados por la re-imputación (observación) y motivo de los anulados. */
    public static final String MARCA_REIMPUTACION = "C33-REIMPUTADO";

    /**
     * {@code bloquear} (defecto): el invariante lanza {@link #ERR_PRELACION_CUOTAS}. {@code avisar}: solo lo escribe
     * en el log (vuelta atrás si en la corrida bloquea algo inesperado; el control de cierre sigue bloqueando).
     */
    public static final String PROPIEDAD_MODO = "saa.cartera.prelacionCuotas";

    /** Primer vencimiento al que se aplica la regla del punto 1 ({@code AAAA-MM-DD}, defecto {@code 2025-06-01}). */
    public static final String PROPIEDAD_DESDE = "saa.cartera.prelacionDesde";

    /**
     * {@code si} (defecto): la reapertura de cuotas, la anulación de una operación y el re-proceso de una carga
     * re-imputan en orden los pagos del préstamo si quedó una cuota con saldo detrás de una con pagos. {@code no}: solo
     * lo escriben en el log/bitácora; el control de cierre lo bloquea y se corrige con {@code POST
     * /rest/prelacioncuotas/reimputar}.
     */
    public static final String PROPIEDAD_REIMPUTAR = "saa.cartera.reimputarEnOrden";

    /**
     * Punto 5 (defecto {@code false}): la mora se calcula sobre el capital pendiente de la cuota en cada tramo (los
     * pagos de capital la detienen), no sobre el capital completo. Ver {@link #moraSobreCapitalPendiente}.
     */
    public static final String PROPIEDAD_MORA_CAPITAL_PENDIENTE = "saa.cartera.moraSobreCapitalPendiente";

    public static final LocalDate DESDE_POR_DEFECTO = LocalDate.of(2025, 6, 1);

    public static final String TIPO_PRECANCELACION = "PRECANCELACION";
    public static final String TIPO_ABONO_CAPITAL = "ABONO_CAPITAL";
    /**
     * El acuerdo con condonación cierra todas las cuotas pendientes (7) y deja su único pago en la cuota ancla, igual
     * que la precancelación: misma excepción.
     */
    public static final String TIPO_ACUERDO_CONDONACION = "ACUERDO_CONDONACION";

    /** Lista SQL de los tipos exentos (para las consultas nativas). */
    public static final String TIPOS_EXENTOS_SQL = "'" + TIPO_PRECANCELACION + "', '" + TIPO_ABONO_CAPITAL + "', '"
        + TIPO_ACUERDO_CONDONACION + "'";

    /** Un centavo: misma tolerancia del motor. */
    public static final double TOLERANCIA = 0.01;

    // Índices de componentes, en el orden de la prelación del motor (seguro, desgravamen, mora, IV, interés, capital).
    public static final int SEGURO = 0;
    public static final int DESGRAVAMEN = 1;
    public static final int MORA = 2;
    public static final int INTERES_VENCIDO = 3;
    public static final int INTERES = 4;
    public static final int CAPITAL = 5;
    public static final int COMPONENTES = 6;
    public static final String[] NOMBRES = { "seguro", "desgravamen", "mora", "interesVencido", "interes", "capital" };

    private PrelacionCuotas() {
    }

    // ========================================================================
    // Propiedades
    // ========================================================================

    /** {@code true} salvo {@code saa.cartera.prelacionCuotas=avisar}. */
    public static boolean esBloqueante() {
        String v = System.getProperty(PROPIEDAD_MODO);
        return v == null || !"avisar".equalsIgnoreCase(v.trim());
    }

    /**
     * {@code true} solo con {@code saa.cartera.reimputarEnOrden=si}. Por defecto NO re-imputa sola (decisión del líder,
     * 21-nov-2026, D-130): la re-imputación mueve pagos sin asiento de reclasificación, así que en la corrida se simula,
     * se revisa y se aplica por mes con {@code POST /prelacioncuotas/reimputarAlCorte}.
     */
    public static boolean reimputaAutomaticamente() {
        String v = System.getProperty(PROPIEDAD_REIMPUTAR);
        return v != null && ("si".equalsIgnoreCase(v.trim()) || "true".equalsIgnoreCase(v.trim()));
    }

    /** {@code true} solo con {@code saa.cartera.moraSobreCapitalPendiente=true}. */
    public static boolean moraSobreCapitalPendienteActiva() {
        return "true".equalsIgnoreCase(String.valueOf(System.getProperty(PROPIEDAD_MORA_CAPITAL_PENDIENTE)).trim());
    }

    /** Fecha de la propiedad {@link #PROPIEDAD_DESDE}; inválida o vacía = {@link #DESDE_POR_DEFECTO}. */
    public static LocalDate desde() {
        String v = System.getProperty(PROPIEDAD_DESDE);
        if (v == null || v.trim().isEmpty()) {
            return DESDE_POR_DEFECTO;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (Exception e) {
            System.out.println("C33: " + PROPIEDAD_DESDE + "=" + v + " no es AAAA-MM-DD; se usa " + DESDE_POR_DEFECTO);
            return DESDE_POR_DEFECTO;
        }
    }

    // ========================================================================
    // Reglas
    // ========================================================================

    /** Último instante del día (corte inclusivo). */
    public static LocalDateTime finDelDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atTime(23, 59, 59);
    }

    /** Punto 2: el pago cuenta al corte si no hay corte, si no trae fecha (migración) o si su fecha es {@code <= corte}. */
    public static boolean cuentaAlCorte(LocalDateTime fechaPago, LocalDateTime corte) {
        return corte == null || fechaPago == null || !fechaPago.isAfter(corte);
    }

    /** Los pagos de la lista que cuentan al corte (lista nueva; {@code null} = vacía). */
    public static List<PagoPrestamo> alCorte(List<PagoPrestamo> pagos, LocalDateTime corte) {
        List<PagoPrestamo> r = new ArrayList<>();
        if (pagos != null) {
            for (PagoPrestamo p : pagos) {
                if (p != null && cuentaAlCorte(p.getFecha(), corte)) {
                    r.add(p);
                }
            }
        }
        return r;
    }

    /** Abono a capital y precancelación: su propia regla (ni invariante ni re-imputación). */
    public static boolean esTipoExento(String tipoPago) {
        return tipoPago != null && (TIPO_PRECANCELACION.equalsIgnoreCase(tipoPago.trim())
            || TIPO_ABONO_CAPITAL.equalsIgnoreCase(tipoPago.trim())
            || TIPO_ACUERDO_CONDONACION.equalsIgnoreCase(tipoPago.trim()));
    }

    /** Pago exento: tipo exento o con capital registrado en {@code PGPRSLOT} (ancla del abono/precancelación, DEP). */
    public static boolean esPagoExento(PagoPrestamo pago) {
        return pago == null || esTipoExento(pago.getTipo()) || nvl(pago.getSaldoOtros()) > TOLERANCIA;
    }

    /** Capital + interés de la cuota que no cubren los pagos dados (nunca negativo). */
    public static double pendienteNucleo(DetallePrestamo cuota, List<PagoPrestamo> pagos) {
        double pagado = 0.0;
        if (pagos != null) {
            for (PagoPrestamo p : pagos) {
                pagado += nvl(p.getCapitalPagado()) + nvl(p.getInteresPagado());
            }
        }
        return Math.max(0.0, redondear(nvl(cuota.getCapital()) + nvl(cuota.getInteres()) - pagado));
    }

    /**
     * Punto 1: cuota PAGADA (4) con vencimiento desde {@code desde} y capital + interés sin cubrir por los pagos dados
     * (ya filtrados al corte). Las 7 nunca; las 4 anteriores al arranque tampoco.
     */
    public static boolean esMarcaSinRespaldo(DetallePrestamo cuota, List<PagoPrestamo> pagosAlCorte, LocalDate desde) {
        if (cuota == null || cuota.getEstado() == null || cuota.getEstado().longValue() != EstadoCuotaPrestamo.PAGADA) {
            return false;
        }
        if (desde != null && (cuota.getFechaVencimiento() == null
                || cuota.getFechaVencimiento().toLocalDate().isBefore(desde))) {
            return false;
        }
        return pendienteNucleo(cuota, pagosAlCorte) > TOLERANCIA;
    }

    /**
     * Cuota que la regla no abre nunca: CANCELADA_ANTICIPADA (7), o PAGADA (4) anterior al arranque.
     */
    public static boolean estaCerradaPorRegla(DetallePrestamo cuota, LocalDate desde) {
        if (cuota == null || cuota.getEstado() == null) {
            return false;
        }
        long e = cuota.getEstado().longValue();
        if (e == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
            return true;
        }
        return e == EstadoCuotaPrestamo.PAGADA && desde != null
            && (cuota.getFechaVencimiento() == null || cuota.getFechaVencimiento().toLocalDate().isBefore(desde));
    }

    /** Texto del invariante (punto 3). */
    public static String mensajeInvariante(Long idPrestamo, Number numeroPrevia, double saldoPrevia, Number numeroDestino,
            LocalDateTime corte, String tipoPago) {
        return ERR_PRELACION_CUOTAS + ": cuota #" + entero(numeroPrevia) + " con saldo $" + formato(saldoPrevia)
            + " (préstamo " + idPrestamo + (corte != null ? ", al " + corte.toLocalDate() : "") + "): no se aplica "
            + (tipoPago != null ? tipoPago + " " : "") + "a la cuota #" + entero(numeroDestino)
            + ". Todo pago va primero a la cuota más antigua con saldo (C33); el abono a capital y la precancelación"
            + " tienen su propia regla.";
    }

    // ========================================================================
    // Punto 6 / punto 4 — detección de la prelación rota en un préstamo (misma regla que el control de cierre)
    // ========================================================================

    /** Rotura de la prelación encontrada en un préstamo. */
    public static final class Rotura {
        public static final String PRELACION = "PRELACION";
        public static final String SOBREIMPUTADA = "SOBREIMPUTADA";

        /** {@link #PRELACION} o {@link #SOBREIMPUTADA}. */
        public final String tipo;
        /** Índice (en la lista ordenada) de la primera cuota desde la que hay que re-imputar. */
        public final int indiceInicio;
        public final Double numeroCuota;
        /** Primera cuota posterior con pagos (PRELACION) o {@code null}. */
        public final Double numeroPosterior;
        /** Capital + interés pendiente de la cuota (PRELACION) o pagado de más (SOBREIMPUTADA). */
        public final double monto;

        Rotura(String tipo, int indiceInicio, Double numeroCuota, Double numeroPosterior, double monto) {
            this.tipo = tipo;
            this.indiceInicio = indiceInicio;
            this.numeroCuota = numeroCuota;
            this.numeroPosterior = numeroPosterior;
            this.monto = monto;
        }

        public String texto() {
            return PRELACION.equals(tipo)
                ? "cuota #" + entero(numeroCuota) + " con capital + interés pendiente $" + formato(monto)
                    + " y la cuota #" + entero(numeroPosterior) + " (posterior) con pagos"
                : "cuota #" + entero(numeroCuota) + " con capital o interés pagado de más $" + formato(monto);
        }
    }

    /** Capital + interés pagado por pagos NO exentos. */
    public static double nucleoNoExento(List<PagoPrestamo> pagos) {
        double t = 0.0;
        if (pagos != null) {
            for (PagoPrestamo p : pagos) {
                if (!esPagoExento(p)) {
                    t += nvl(p.getCapitalPagado()) + nvl(p.getInteresPagado());
                }
            }
        }
        return redondear(t);
    }

    /**
     * Misma regla que {@code DetallePrestamoDaoService#selectControlPrelacionAlCorte}, para un préstamo:
     * <ol>
     * <li><b>PRELACION:</b> la primera cuota con capital + interés pendiente al corte (estado distinto de 4/7, o 4
     * desde {@code desde}) y una posterior con capital o interés pagado al corte por pagos no exentos.</li>
     * <li><b>SOBREIMPUTADA:</b> una cuota (vencimiento desde {@code desde}) cuyos pagos al corte pagan más capital o
     * más interés que la cuota, con algún pago no exento y ninguno exento (la ancla de un abono, precancelación o
     * acuerdo tiene su propia regla).</li>
     * </ol>
     *
     * @param cuotasOrdenadas cuotas del préstamo por número
     * @param pagosAlCorte    pagos vigentes al corte por código de cuota
     * @param desde           arranque de la regla de las 4
     * @return la rotura, o {@code null} si el préstamo cumple la prelación
     */
    public static Rotura detectarRotura(List<DetallePrestamo> cuotasOrdenadas, Map<Long, List<PagoPrestamo>> pagosAlCorte,
            LocalDate desde) {
        int n0 = -1;
        for (int i = 0; i < cuotasOrdenadas.size(); i++) {
            DetallePrestamo c = cuotasOrdenadas.get(i);
            List<PagoPrestamo> pagos = pagosAlCorte.get(c.getCodigo());
            if (n0 < 0) {
                if (cuentaComoAbierta(c, desde) && pendienteNucleo(c, pagos) > TOLERANCIA) {
                    n0 = i;
                }
                continue;
            }
            if (nucleoNoExento(pagos) > TOLERANCIA) {
                DetallePrestamo c0 = cuotasOrdenadas.get(n0);
                return new Rotura(Rotura.PRELACION, n0, c0.getNumeroCuota(), c.getNumeroCuota(),
                    pendienteNucleo(c0, pagosAlCorte.get(c0.getCodigo())));
            }
        }
        for (int i = 0; i < cuotasOrdenadas.size(); i++) {
            DetallePrestamo c = cuotasOrdenadas.get(i);
            if (desde != null && (c.getFechaVencimiento() == null || c.getFechaVencimiento().toLocalDate().isBefore(desde))) {
                continue;
            }
            List<PagoPrestamo> pagos = pagosAlCorte.get(c.getCodigo());
            if (pagos == null || nucleoNoExento(pagos) <= TOLERANCIA) {
                continue;
            }
            // La cuota ancla de un abono, precancelación o acuerdo tiene su propia regla: no se cuenta como pagada de más.
            boolean conExento = false;
            for (PagoPrestamo p : pagos) {
                conExento |= esPagoExento(p);
            }
            if (conExento) {
                continue;
            }
            double capital = 0.0;
            double interes = 0.0;
            for (PagoPrestamo p : pagos) {
                capital += nvl(p.getCapitalPagado());
                interes += nvl(p.getInteresPagado());
            }
            double exceso = Math.max(redondear(capital - nvl(c.getCapital())), 0.0)
                + Math.max(redondear(interes - nvl(c.getInteres())), 0.0);
            if (exceso > TOLERANCIA) {
                return new Rotura(Rotura.SOBREIMPUTADA, i, c.getNumeroCuota(), null, redondear(exceso));
            }
        }
        return null;
    }

    /** Estado distinto de 4/7 (o nulo), o PAGADA con vencimiento desde el arranque. */
    private static boolean cuentaComoAbierta(DetallePrestamo c, LocalDate desde) {
        Long e = c.getEstado();
        if (e == null || (e.longValue() != EstadoCuotaPrestamo.PAGADA && e.longValue() != EstadoCuotaPrestamo.CANCELADA_ANTICIPADA)) {
            return true;
        }
        return e.longValue() == EstadoCuotaPrestamo.PAGADA && desde != null && c.getFechaVencimiento() != null
            && !c.getFechaVencimiento().toLocalDate().isBefore(desde);
    }

    // ========================================================================
    // Punto 5 — mora sobre el capital pendiente, por tramos
    // ========================================================================

    /**
     * Mora de la cuota a {@code fecha} sobre el capital pendiente de cada tramo:
     * <pre>Σ capital pendiente en [t(i-1), t(i)) × tasaDiaria × días</pre>
     * desde el vencimiento hasta {@code fecha}; cada pago de capital (fecha, capital) baja la base desde su fecha. Los
     * pagos anteriores al vencimiento bajan la base inicial; los posteriores a {@code fecha} no cuentan. Con el capital
     * cubierto la mora deja de correr: una cuota saldada que vuelve a PARCIAL por otra causa conserva la mora que tenía
     * el día de su pago (caso 4573, grupo A de C33). Sin pagos, igual que la fórmula de siempre (capital completo).
     *
     * @param capital     capital de la cuota (DTPRCPTL)
     * @param vencimiento fecha de vencimiento
     * @param pagos       pares {fecha (LocalDate), capital pagado (Double)}, en cualquier orden
     * @param tasaDiaria  tasa diaria de mora
     * @param fecha       fecha a la que se calcula
     */
    public static double moraSobreCapitalPendiente(double capital, LocalDate vencimiento, List<Object[]> pagos,
            double tasaDiaria, LocalDate fecha) {
        if (vencimiento == null || fecha == null || !fecha.isAfter(vencimiento) || capital <= 0.0) {
            return 0.0;
        }
        List<Object[]> orden = new ArrayList<>();
        double base = capital;
        if (pagos != null) {
            for (Object[] p : pagos) {
                LocalDate f = (LocalDate) p[0];
                double c = p[1] != null ? ((Number) p[1]).doubleValue() : 0.0;
                if (c == 0.0) {
                    continue;
                }
                if (f == null || !f.isAfter(vencimiento)) {
                    base -= c;
                } else if (!f.isAfter(fecha)) {
                    orden.add(new Object[] { f, c });
                }
            }
        }
        orden.sort(Comparator.comparing(p -> (LocalDate) p[0]));
        double mora = 0.0;
        LocalDate desdeTramo = vencimiento;
        for (Object[] p : orden) {
            LocalDate f = (LocalDate) p[0];
            if (base > 0.0) {
                mora += base * tasaDiaria * ChronoUnit.DAYS.between(desdeTramo, f);
            }
            base -= (Double) p[1];
            desdeTramo = f;
        }
        if (base > 0.0) {
            mora += base * tasaDiaria * ChronoUnit.DAYS.between(desdeTramo, fecha);
        }
        return Math.max(0.0, redondear(mora));
    }

    // ========================================================================
    // Punto 4 — plan de la re-imputación en orden (puro)
    // ========================================================================

    /** Cuota del plan: lo que le falta por componente, sin los pagos que se van a mover. */
    public static final class CuotaPlan {
        public final Long id;
        public final double numero;
        public final boolean cerrada;
        public final double[] pendiente;

        public CuotaPlan(Long id, double numero, boolean cerrada, double[] pendiente) {
            this.id = id;
            this.numero = numero;
            this.cerrada = cerrada;
            this.pendiente = pendiente.clone();
        }
    }

    /** Pago a mover: dónde está hoy, su fecha y sus componentes. */
    public static final class PagoPlan {
        public final Long id;
        public final Long idCuota;
        public final LocalDateTime fecha;
        public final double[] componentes;

        public PagoPlan(Long id, Long idCuota, LocalDateTime fecha, double[] componentes) {
            this.id = id;
            this.idCuota = idCuota;
            this.fecha = fecha;
            this.componentes = componentes.clone();
        }

        public double total() {
            double t = 0.0;
            for (double c : componentes) {
                t += c;
            }
            return redondear(t);
        }
    }

    /** Una porción de un pago en una cuota. */
    public static final class Asignacion {
        public final Long idCuota;
        public final double[] componentes = new double[COMPONENTES];

        Asignacion(Long idCuota) {
            this.idCuota = idCuota;
        }

        public double total() {
            double t = 0.0;
            for (double c : componentes) {
                t += c;
            }
            return redondear(t);
        }
    }

    /** Resultado del plan. */
    public static final class Plan {
        /** Reparto nuevo de cada pago (en el orden en que se re-aplicaron). */
        public final Map<Long, List<Asignacion>> asignaciones = new LinkedHashMap<>();
        /** Pagos cuyo reparto no cambia (se dejan como están). */
        public final List<Long> sinCambio = new ArrayList<>();
        /** Dinero que no encontró cuota (si es &gt; 0 el plan no se aplica). */
        public double sinDestino;
        /** Diferencia por componente (nuevo − original), suma de todos los pagos movidos. */
        public final double[] deltaComponentes = new double[COMPONENTES];

        public boolean tieneCambios() {
            return !asignaciones.isEmpty();
        }
    }

    /**
     * Re-aplica los {@code pagos} en orden cronológico (fecha, código) sobre las {@code cuotas} (en orden de número),
     * con la prelación del motor dentro de cada cuota, como si cada pago se cobrara de nuevo con la cascada: cada uno
     * va a la cuota más antigua con saldo. Las cuotas cerradas por regla no reciben nada. Devuelve el reparto nuevo de
     * los pagos que cambian; los que quedan igual van a {@link Plan#sinCambio}.
     *
     * <p>El mismo dinero, la misma fecha; solo cambia la cuota (y, si la cuota anterior debía mora o seguros, el
     * componente: queda en {@link Plan#deltaComponentes}).</p>
     */
    public static Plan planificar(List<CuotaPlan> cuotas, List<PagoPlan> pagos) {
        Plan plan = new Plan();
        List<CuotaPlan> orden = new ArrayList<>(cuotas);
        orden.sort(Comparator.comparingDouble(c -> c.numero));
        Map<Long, double[]> pendiente = new LinkedHashMap<>();
        for (CuotaPlan c : orden) {
            pendiente.put(c.id, c.pendiente.clone());
        }
        List<PagoPlan> cronologico = new ArrayList<>(pagos);
        cronologico.sort(Comparator.comparing((PagoPlan p) -> p.fecha, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(p -> p.id, Comparator.nullsFirst(Comparator.naturalOrder())));

        for (PagoPlan pago : cronologico) {
            double restante = pago.total();
            List<Asignacion> reparto = new ArrayList<>();
            for (CuotaPlan c : orden) {
                if (restante <= 0.005) {
                    break;
                }
                if (c.cerrada) {
                    continue;
                }
                double[] p = pendiente.get(c.id);
                Asignacion a = null;
                for (int k = 0; k < COMPONENTES && restante > 0.005; k++) {
                    double toma = redondear(Math.min(restante, p[k]));
                    if (toma <= 0.0) {
                        continue;
                    }
                    if (a == null) {
                        a = new Asignacion(c.id);
                        reparto.add(a);
                    }
                    a.componentes[k] = redondear(a.componentes[k] + toma);
                    p[k] = redondear(p[k] - toma);
                    restante = redondear(restante - toma);
                }
            }
            if (restante > 0.005) {
                plan.sinDestino = redondear(plan.sinDestino + restante);
            }
            if (reparto.size() == 1 && reparto.get(0).idCuota != null && reparto.get(0).idCuota.equals(pago.idCuota)
                    && mismosComponentes(reparto.get(0).componentes, pago.componentes)) {
                plan.sinCambio.add(pago.id);
                continue;
            }
            plan.asignaciones.put(pago.id, reparto);
            double[] nuevo = new double[COMPONENTES];
            for (Asignacion a : reparto) {
                for (int k = 0; k < COMPONENTES; k++) {
                    nuevo[k] += a.componentes[k];
                }
            }
            for (int k = 0; k < COMPONENTES; k++) {
                plan.deltaComponentes[k] = redondear(plan.deltaComponentes[k] + nuevo[k] - pago.componentes[k]);
            }
        }
        return plan;
    }

    /** Componentes de un PGPR en el orden de la prelación. */
    public static double[] componentes(PagoPrestamo p) {
        double[] c = new double[COMPONENTES];
        c[SEGURO] = nvl(p.getValorSeguroIncendio());
        c[DESGRAVAMEN] = nvl(p.getDesgravamen());
        c[MORA] = nvl(p.getMoraPagada());
        c[INTERES_VENCIDO] = nvl(p.getInteresVencidoPagado());
        c[INTERES] = nvl(p.getInteresPagado());
        c[CAPITAL] = nvl(p.getCapitalPagado());
        return c;
    }

    /** Valores de la cuota por componente (lo que se debe, antes de pagos). */
    public static double[] valoresCuota(DetallePrestamo d) {
        double[] c = new double[COMPONENTES];
        c[SEGURO] = nvl(d.getValorSeguroIncendio());
        c[DESGRAVAMEN] = nvl(d.getDesgravamen());
        c[MORA] = nvl(d.getMora());
        c[INTERES_VENCIDO] = nvl(d.getInteresVencido());
        c[INTERES] = nvl(d.getInteres());
        c[CAPITAL] = nvl(d.getCapital());
        return c;
    }

    /** Texto corto de un vector de componentes (solo los distintos de cero). */
    public static String texto(double[] c) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < COMPONENTES; k++) {
            if (Math.abs(c[k]) > 0.004) {
                sb.append(sb.length() == 0 ? "" : ", ").append(NOMBRES[k]).append(' ').append(formato(c[k]));
            }
        }
        return sb.length() == 0 ? "sin diferencia" : sb.toString();
    }

    private static boolean mismosComponentes(double[] a, double[] b) {
        for (int k = 0; k < COMPONENTES; k++) {
            if (Math.abs(a[k] - b[k]) > 0.004) {
                return false;
            }
        }
        return true;
    }

    // ========================================================================
    // Utilidades
    // ========================================================================

    public static double nvl(Double v) {
        return v != null ? v : 0.0;
    }

    public static double redondear(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String formato(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private static String entero(Number n) {
        return n == null ? "?" : String.valueOf(n.longValue());
    }
}
