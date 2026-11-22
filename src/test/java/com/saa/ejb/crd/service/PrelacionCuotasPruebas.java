package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;

/**
 * C33 — pruebas unitarias de {@link PrelacionCuotas} (las reglas puras: corte de pagos, marca sin respaldo, detección
 * de la prelación rota, plan de re-imputación y mora sobre el capital pendiente).
 *
 * <p>Sin framework a propósito: el repositorio no tiene JUnit y la máquina compila sin red ({@code mvn -o}). El nombre
 * no termina en {@code Test} para que surefire no la busque. Se corre con
 * {@code tools/pruebas/correr-pruebas-c33.sh} (compila contra {@code target/classes} y sale con código 1 si algo
 * falla).</p>
 */
public final class PrelacionCuotasPruebas {

    private static int ok;
    private static final List<String> fallas = new ArrayList<>();

    private static final LocalDate DESDE = LocalDate.of(2025, 6, 1);

    public static void main(String[] args) {
        cortePagos();
        marcaSinRespaldo();
        tiposExentos();
        roturaPrelacion();
        roturaExentaONoCuenta();
        roturaMarcaDelta();
        roturaSobreimputada();
        planMueveAlaMasAntigua();
        planSinCambio();
        planCronologico();
        planSinDestinoYCerradas();
        moraCapitalPendiente();
        c33bBucleAnterior();
        c33bPrimeraConSaldo();
        c33bSaldoInsignificante();
        c33bPagoPosteriorAlCorte();
        c33cExigiblesPorSaldoReal();
        c33cMontoDeLaMarca();
        c33cMoraPagadaReal();
        c33cFijarMora();
        c33cAncla();
        System.out.println("PrelacionCuotasPruebas: " + ok + " comprobaciones correctas, " + fallas.size() + " fallas");
        for (String f : fallas) {
            System.out.println("  FALLA: " + f);
        }
        if (!fallas.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ casos

    private static void cortePagos() {
        LocalDateTime corte = LocalDateTime.of(2025, 6, 30, 23, 59, 59);
        verdad("sin corte cuenta", PrelacionCuotas.cuentaAlCorte(LocalDateTime.of(2026, 1, 1, 0, 0), null));
        verdad("sin fecha cuenta", PrelacionCuotas.cuentaAlCorte(null, corte));
        verdad("mismo día cuenta", PrelacionCuotas.cuentaAlCorte(LocalDateTime.of(2025, 6, 30, 10, 0), corte));
        verdad("posterior no cuenta", !PrelacionCuotas.cuentaAlCorte(LocalDateTime.of(2026, 1, 13, 0, 0), corte));
        List<PagoPrestamo> pagos = Arrays.asList(pago(1L, null, "2025-06-15", 10, 0, null),
            pago(2L, null, "2026-01-13", 10, 0, null));
        igual("alCorte filtra", 1, PrelacionCuotas.alCorte(pagos, corte).size());
    }

    private static void marcaSinRespaldo() {
        DetallePrestamo marca = cuota(1L, 5, 4, "2025-10-31", 74.04, 5.0);
        verdad("4 sin pagos desde el arranque = marca", PrelacionCuotas.esMarcaSinRespaldo(marca, null, DESDE));
        verdad("4 cubierta no es marca", !PrelacionCuotas.esMarcaSinRespaldo(marca,
            Arrays.asList(pago(1L, marca, "2025-10-31", 74.04, 5.0, null)), DESDE));
        verdad("4 anterior al arranque no es marca",
            !PrelacionCuotas.esMarcaSinRespaldo(cuota(2L, 1, 4, "2025-05-31", 74.04, 5.0), null, DESDE));
        verdad("7 nunca es marca", !PrelacionCuotas.esMarcaSinRespaldo(cuota(3L, 9, 7, "2026-03-31", 74.04, 5.0), null, DESDE));
        verdad("7 cerrada por regla", PrelacionCuotas.estaCerradaPorRegla(cuota(3L, 9, 7, "2026-03-31", 1, 0), DESDE));
        verdad("4 previa cerrada por regla", PrelacionCuotas.estaCerradaPorRegla(cuota(2L, 1, 4, "2025-05-31", 1, 0), DESDE));
        verdad("4 en ventana no cerrada", !PrelacionCuotas.estaCerradaPorRegla(marca, DESDE));
    }

    private static void tiposExentos() {
        verdad("precancelación exenta", PrelacionCuotas.esTipoExento("PRECANCELACION"));
        verdad("abono exento", PrelacionCuotas.esTipoExento("ABONO_CAPITAL"));
        verdad("acuerdo exento", PrelacionCuotas.esTipoExento("ACUERDO_CONDONACION"));
        verdad("pago manual no exento", !PrelacionCuotas.esTipoExento("PAGO_MANUAL"));
        verdad("Petro (tipo nulo) no exento", !PrelacionCuotas.esTipoExento(null));
        PagoPrestamo ancla = pago(9L, null, "2025-06-01", 0, 0, "PAGO_MANUAL");
        ancla.setSaldoOtros(100.0);
        verdad("PGPRSLOT > 0 exento", PrelacionCuotas.esPagoExento(ancla));
    }

    /** Caso tipo C1: la 11 parcial (capital 28,12 pendiente) y la 12 pagada por ventanilla. */
    private static void roturaPrelacion() {
        DetallePrestamo c10 = cuota(10L, 10, 4, "2025-11-30", 74.0, 6.0);
        DetallePrestamo c11 = cuota(11L, 11, 6, "2025-12-31", 74.04, 6.0);
        DetallePrestamo c12 = cuota(12L, 12, 4, "2026-01-31", 74.5, 5.5);
        Map<Long, List<PagoPrestamo>> pagos = new LinkedHashMap<>();
        pagos.put(10L, lista(pago(1L, c10, "2025-11-30", 74.0, 6.0, null)));
        pagos.put(11L, lista(pago(2L, c11, "2025-12-12", 45.92, 6.0, null)));
        pagos.put(12L, lista(pago(3L, c12, "2026-01-20", 74.5, 5.5, "PAGO_MANUAL")));
        PrelacionCuotas.Rotura r = PrelacionCuotas.detectarRotura(Arrays.asList(c10, c11, c12), pagos, DESDE);
        verdad("rotura detectada", r != null);
        if (r != null) {
            igual("tipo", PrelacionCuotas.Rotura.PRELACION, r.tipo);
            igual("cuota con saldo", 11.0, r.numeroCuota);
            igual("posterior", 12.0, r.numeroPosterior);
            igualD("pendiente", 28.12, r.monto);
            igual("índice de inicio", 1, r.indiceInicio);
        }
    }

    private static void roturaExentaONoCuenta() {
        DetallePrestamo c11 = cuota(11L, 11, 6, "2025-12-31", 74.04, 6.0);
        DetallePrestamo c12 = cuota(12L, 12, 7, "2026-01-31", 74.5, 5.5);
        Map<Long, List<PagoPrestamo>> pagos = new LinkedHashMap<>();
        pagos.put(11L, lista(pago(2L, c11, "2025-12-12", 45.92, 6.0, null)));
        pagos.put(12L, lista(pago(3L, c12, "2026-01-20", 74.5, 0, "ABONO_CAPITAL")));
        verdad("posterior por abono a capital no rompe", PrelacionCuotas.detectarRotura(Arrays.asList(c11, c12), pagos, DESDE) == null);

        DetallePrestamo c13 = cuota(13L, 13, 6, "2026-02-28", 74.5, 5.5);
        pagos.put(13L, lista(pago(4L, c13, "2025-07-31", 0, 0, null)));
        pagos.get(13L).get(0).setValorSeguroIncendio(3.17);
        verdad("solo seguro (HS) en una posterior no rompe",
            PrelacionCuotas.detectarRotura(Arrays.asList(c11, c12, c13), pagos, DESDE) == null);
    }

    /** Caso C2: la 36 parcial y las siguientes marcadas por Delta sin PGPR: la marca es la abierta, no la posterior. */
    private static void roturaMarcaDelta() {
        DetallePrestamo c36 = cuota(36L, 36, 4, "2026-01-31", 30.0, 2.0);   // marca sin pagos
        DetallePrestamo c37 = cuota(37L, 37, 4, "2026-02-28", 30.0, 2.0);
        Map<Long, List<PagoPrestamo>> pagos = new LinkedHashMap<>();
        pagos.put(37L, lista(pago(5L, c37, "2026-02-28", 30.0, 2.0, null)));
        PrelacionCuotas.Rotura r = PrelacionCuotas.detectarRotura(Arrays.asList(c36, c37), pagos, DESDE);
        verdad("marca sin pagos antes de una pagada = rotura", r != null && r.numeroCuota == 36.0);

        DetallePrestamo h1 = cuota(1L, 1, 4, "2024-01-31", 30.0, 2.0);     // historia anterior al arranque
        verdad("marca anterior al arranque no rompe",
            PrelacionCuotas.detectarRotura(Arrays.asList(h1, c37), pagos, DESDE) == null);

        // Punto 2: la 37 la paga un pago con fecha posterior al corte → al corte no tiene pagos; no rompe.
        Map<Long, List<PagoPrestamo>> alCorte = new LinkedHashMap<>();
        verdad("pago posterior al corte no cuenta", PrelacionCuotas.detectarRotura(Arrays.asList(c36, c37), alCorte, DESDE) == null);
    }

    private static void roturaSobreimputada() {
        DetallePrestamo c20 = cuota(20L, 20, 4, "2026-08-31", 100.0, 10.0);
        Map<Long, List<PagoPrestamo>> pagos = new LinkedHashMap<>();
        pagos.put(20L, lista(pago(6L, c20, "2026-07-31", 100.0, 10.0, null), pago(7L, c20, "2026-08-31", 40.0, 0, "PAGO_MANUAL")));
        PrelacionCuotas.Rotura r = PrelacionCuotas.detectarRotura(Arrays.asList(c20), pagos, DESDE);
        verdad("sobreimputada", r != null && PrelacionCuotas.Rotura.SOBREIMPUTADA.equals(r.tipo));
        if (r != null) {
            igualD("exceso", 40.0, r.monto);
        }
    }

    /** La parcial debe 28,12 de capital; el pago de la posterior (74,50 + 5,50) se re-reparte en orden. */
    private static void planMueveAlaMasAntigua() {
        List<PrelacionCuotas.CuotaPlan> cuotas = Arrays.asList(
            plan(11L, 11, false, 0, 0, 0, 0, 0, 28.12),
            plan(12L, 12, false, 0, 0, 0, 0, 5.50, 74.50));
        List<PrelacionCuotas.PagoPlan> pagos = Arrays.asList(
            new PrelacionCuotas.PagoPlan(3L, 12L, LocalDateTime.of(2026, 1, 20, 0, 0), comp(0, 0, 0, 0, 5.50, 74.50)));
        PrelacionCuotas.Plan p = PrelacionCuotas.planificar(cuotas, pagos);
        verdad("hay cambios", p.tieneCambios());
        List<PrelacionCuotas.Asignacion> a = p.asignaciones.get(3L);
        igual("dos porciones", 2, a.size());
        igual("primero la 11", 11L, a.get(0).idCuota);
        igualD("capital a la 11", 28.12, a.get(0).componentes[PrelacionCuotas.CAPITAL]);
        igualD("interés a la 12", 5.50, a.get(1).componentes[PrelacionCuotas.INTERES]);
        igualD("capital a la 12", 46.38, a.get(1).componentes[PrelacionCuotas.CAPITAL]);
        igualD("mismo dinero", 80.00, a.get(0).total() + a.get(1).total());
        igualD("delta capital 0", 0.0, p.deltaComponentes[PrelacionCuotas.CAPITAL]);
        igualD("delta interés 0", 0.0, p.deltaComponentes[PrelacionCuotas.INTERES]);
        igualD("sin destino 0", 0.0, p.sinDestino);
    }

    private static void planSinCambio() {
        List<PrelacionCuotas.CuotaPlan> cuotas = Arrays.asList(
            plan(11L, 11, false, 0, 0, 0, 0, 0, 0),
            plan(12L, 12, false, 0, 0, 0, 0, 5.50, 74.50));
        List<PrelacionCuotas.PagoPlan> pagos = Arrays.asList(
            new PrelacionCuotas.PagoPlan(3L, 12L, LocalDateTime.of(2026, 1, 20, 0, 0), comp(0, 0, 0, 0, 5.50, 74.50)));
        PrelacionCuotas.Plan p = PrelacionCuotas.planificar(cuotas, pagos);
        verdad("sin cambios", !p.tieneCambios());
        igual("queda como está", 1, p.sinCambio.size());
    }

    /** Dos pagos: el más antiguo llena primero la cuota abierta, aunque tenga código mayor. */
    private static void planCronologico() {
        List<PrelacionCuotas.CuotaPlan> cuotas = Arrays.asList(
            plan(1L, 1, false, 0, 0, 0, 0, 0, 50.0),
            plan(2L, 2, false, 0, 0, 0, 0, 0, 50.0),
            plan(3L, 3, false, 0, 0, 0, 0, 0, 50.0));
        List<PrelacionCuotas.PagoPlan> pagos = Arrays.asList(
            new PrelacionCuotas.PagoPlan(20L, 3L, LocalDateTime.of(2026, 3, 1, 0, 0), comp(0, 0, 0, 0, 0, 50.0)),
            new PrelacionCuotas.PagoPlan(10L, 2L, LocalDateTime.of(2026, 4, 1, 0, 0), comp(0, 0, 0, 0, 0, 50.0)));
        PrelacionCuotas.Plan p = PrelacionCuotas.planificar(cuotas, pagos);
        igual("el de marzo va a la 1", 1L, p.asignaciones.get(20L).get(0).idCuota);
        // El de abril cae en la 2, que es donde ya estaba: no se toca.
        igual("el de abril queda donde estaba", Arrays.asList(10L), p.sinCambio);
        igual("solo se mueve el de marzo", Arrays.asList(20L), new ArrayList<>(p.asignaciones.keySet()));
    }

    private static void planSinDestinoYCerradas() {
        List<PrelacionCuotas.CuotaPlan> cuotas = Arrays.asList(
            plan(1L, 1, true, 0, 0, 0, 0, 0, 50.0),   // cerrada por regla: no recibe
            plan(2L, 2, false, 0, 0, 0, 0, 0, 30.0));
        List<PrelacionCuotas.PagoPlan> pagos = Arrays.asList(
            new PrelacionCuotas.PagoPlan(5L, 2L, LocalDateTime.of(2026, 3, 1, 0, 0), comp(0, 0, 0, 0, 0, 40.0)));
        PrelacionCuotas.Plan p = PrelacionCuotas.planificar(cuotas, pagos);
        igual("la cerrada no recibe", 2L, p.asignaciones.get(5L).get(0).idCuota);
        igualD("dinero sin destino", 10.0, p.sinDestino);
    }

    private static void moraCapitalPendiente() {
        double tasa = 9.0 / 100.0 / 360.0;
        LocalDate venc = LocalDate.of(2025, 12, 31);
        LocalDate fecha = LocalDate.of(2026, 11, 17);
        // Operación 70895, cuota 11 (C33 §1): sin pagos de capital = la fórmula de siempre (74,04 → 5,94).
        igualD("sin pagos = capital completo", 5.94,
            PrelacionCuotas.moraSobreCapitalPendiente(74.04, venc, null, tasa, fecha));
        // Con el pago del 12-dic (antes del vencimiento) de 45,92: 28,12 × 9 %/360 × 321 = 2,26.
        List<Object[]> pagos = new ArrayList<>();
        pagos.add(new Object[] { LocalDate.of(2025, 12, 12), 45.92 });
        igualD("sobre el capital pendiente", 2.26,
            PrelacionCuotas.moraSobreCapitalPendiente(74.04, venc, pagos, tasa, fecha));
        // Grupo A (4573): capital pagado completo el día X → la mora deja de correr ese día.
        LocalDate vencA = LocalDate.of(2025, 6, 30);
        LocalDate pagoA = LocalDate.of(2026, 10, 2);
        List<Object[]> completo = new ArrayList<>();
        completo.add(new Object[] { pagoA, 100.0 });
        double alPago = PrelacionCuotas.moraSobreCapitalPendiente(100.0, vencA, completo, tasa, pagoA);
        double alCorte = PrelacionCuotas.moraSobreCapitalPendiente(100.0, vencA, completo, tasa, LocalDate.of(2026, 10, 31));
        igualD("no corre después del pago completo", alPago, alCorte);
        // Pago parcial a mitad: 100 × 10 días + 40 × 10 días.
        List<Object[]> parcial = new ArrayList<>();
        parcial.add(new Object[] { vencA.plusDays(10), 60.0 });
        igualD("por tramos", PrelacionCuotas.redondear(100 * tasa * 10 + 40 * tasa * 10),
            PrelacionCuotas.moraSobreCapitalPendiente(100.0, vencA, parcial, tasa, vencA.plusDays(20)));
        // Un pago posterior a la fecha del cálculo no cuenta.
        igualD("pago futuro no cuenta", PrelacionCuotas.moraSobreCapitalPendiente(100.0, vencA, null, tasa, vencA.plusDays(20)),
            PrelacionCuotas.moraSobreCapitalPendiente(100.0, vencA, completo, tasa, vencA.plusDays(20)));
        igualD("no vencida = 0", 0.0, PrelacionCuotas.moraSobreCapitalPendiente(100.0, vencA, null, tasa, vencA));
    }

    // ------------------------------------------------------------------ C33b (búsqueda de la carga Petro)

    private static final LocalDateTime CORTE_JUNIO = LocalDateTime.of(2025, 6, 30, 23, 59, 59, 999_999_999);

    /** Modelo de la base: cuotas ordenadas y sus pagos vigentes. */
    private static final class Prestamo33b {
        final List<DetallePrestamo> cuotas = new ArrayList<>();
        final Map<Long, List<PagoPrestamo>> pagos = new LinkedHashMap<>();
        int llamadasSaldo;
        int marcadas;

        Prestamo33b cuota(DetallePrestamo d, PagoPrestamo... p) {
            cuotas.add(d);
            pagos.put(d.getCodigo(), lista(p));
            return this;
        }

        /** Como {@code saldoPosibleSql}: por estado, o 4 desde el arranque con capital + interés sin cubrir al corte. */
        List<DetallePrestamo> conSaldoPosible(LocalDateTime corte) {
            List<DetallePrestamo> r = new ArrayList<>();
            for (DetallePrestamo d : cuotas) {
                long e = d.getEstado() == null ? -1 : d.getEstado();
                if (e != 4 && e != 7) {
                    r.add(d);
                } else if (PrelacionCuotas.esMarcaSinRespaldo(d, PrelacionCuotas.alCorte(pagos.get(d.getCodigo()), corte), DESDE)) {
                    r.add(d);
                }
            }
            return r;
        }

        /** Saldo real al corte (capital + interés; sin pagos al corte, DTPRTTLL). */
        double saldo(DetallePrestamo d, LocalDateTime corte) {
            llamadasSaldo++;
            List<PagoPrestamo> alCorte = PrelacionCuotas.alCorte(pagos.get(d.getCodigo()), corte);
            if (alCorte.isEmpty()) {
                return PrelacionCuotas.nvl(d.getTotal());
            }
            return PrelacionCuotas.pendienteNucleo(d, alCorte);
        }

        DetallePrestamo buscar(LocalDateTime corte) {
            try {
                return PrelacionCuotas.primeraConSaldo(conSaldoPosible(corte), c -> saldo(c, corte), c -> {
                    marcadas++;
                    c.setEstado(4L);
                });
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        /** Réplica de buscarCuotaAPagar de saaBE 384cd97a: pide la mínima hasta 100 veces y salta toda 4. */
        Object[] buscarComoAntes() {
            int intentos = 0;
            while (intentos < 100) {
                intentos++;
                List<DetallePrestamo> posibles = conSaldoPosible(null);
                if (posibles.isEmpty()) {
                    break;
                }
                DetallePrestamo c = posibles.get(0);
                double s = saldo(c, null);
                if (c.getEstado() != null && c.getEstado() == 4L) {
                    continue;
                }
                if (s > 0.01) {
                    return new Object[] { c, intentos };
                }
                c.setEstado(4L);
            }
            return new Object[] { null, intentos };
        }
    }

    private static DetallePrestamo conTotal(DetallePrestamo d, double total) {
        d.setTotal(total);
        return d;
    }

    /** El defecto: 2945 c11 (def8-06), una 4 cuyo único pago es un SAA_HIST de capital 0 posterior al corte. */
    private static void c33bBucleAnterior() {
        DetallePrestamo c11 = conTotal(cuota(260746L, 11, 4, "2025-06-30", 105.21, 6.77), 115.54);
        DetallePrestamo c12 = conTotal(cuota(260750L, 12, 1, "2025-07-31", 105.80, 6.18), 115.54);
        Prestamo33b p = new Prestamo33b()
            .cuota(c11, pago(265233L, c11, "2025-12-30", 0, 0, null))
            .cuota(c12);
        Object[] antes = p.buscarComoAntes();
        igual("antes: sin cuota (la fila quedaba «sin cuota pendiente»)", null, antes[0]);
        igual("antes: 100 intentos con la misma cuota", 100, antes[1]);
        Prestamo33b q = new Prestamo33b()
            .cuota(conTotal(cuota(260746L, 11, 4, "2025-06-30", 105.21, 6.77), 115.54),
                pago(265233L, null, "2025-12-30", 0, 0, null))
            .cuota(conTotal(cuota(260750L, 12, 1, "2025-07-31", 105.80, 6.18), 115.54));
        DetallePrestamo ahora = q.buscar(CORTE_JUNIO);
        igual("C33b: paga la 4 con saldo (cuota 11)", 11.0, ahora != null ? ahora.getNumeroCuota() : null);
        igual("C33b: una sola consulta de saldo", 1, q.llamadasSaldo);
        igual("C33b: no marca nada", 0, q.marcadas);
        igual("C33b: la 4 sigue en 4 (el motor decide el estado al pagar)", 4L, ahora != null ? ahora.getEstado() : null);
    }

    /** Marca de Delta sin PGPR (los casos de julio 2025): es la cuota a pagar; la prelación la pone antes que la 1. */
    private static void c33bPrimeraConSaldo() {
        Prestamo33b p = new Prestamo33b()
            .cuota(conTotal(cuota(1L, 2, 4, "2025-06-30", 100, 5), 0), pago(10L, null, "2025-06-30", 100, 5, null))
            .cuota(conTotal(cuota(2L, 3, 4, "2025-07-31", 257.0, 5.35), 264.84))
            .cuota(conTotal(cuota(3L, 4, 1, "2025-08-31", 257.5, 4.85), 264.84));
        DetallePrestamo c = p.buscar(LocalDateTime.of(2025, 7, 31, 23, 59, 59));
        igual("marca sin PGPR: cuota 3", 3.0, c != null ? c.getNumeroCuota() : null);
        igual("la 4 cubierta no es candidata", 1, p.llamadasSaldo);
        igual("nada que marcar", 0, p.marcadas);
        igual("sin cuotas: null", null, safeBuscar(new Prestamo33b()));
        try {
            igual("lista nula: null", null, PrelacionCuotas.primeraConSaldo(null, x -> 1.0, null));
        } catch (Throwable t) {
            fallas.add("lista nula lanzó " + t);
        }
    }

    /** Saldo insignificante: la 4 se salta sin marcar (DTPRTTLL en 0), la pendiente por estado se marca; una pasada. */
    private static void c33bSaldoInsignificante() {
        DetallePrestamo c1 = conTotal(cuota(1L, 1, 4, "2025-06-30", 50, 2), 0.0);   // marca con DTPRTTLL 0
        DetallePrestamo c2 = conTotal(cuota(2L, 2, 1, "2025-07-31", 50, 2), 0.0);   // sin pagos y total 0
        DetallePrestamo c3 = cuota(3L, 3, 1, "2025-08-31", 50, 2);
        c3.setEstado(null);                                                         // estado nulo cubierto
        DetallePrestamo c4 = conTotal(cuota(4L, 4, 1, "2025-09-30", 50, 2), 52.0);
        Prestamo33b p = new Prestamo33b().cuota(c1).cuota(c2).cuota(c3, pago(30L, null, "2025-06-01", 50, 2, null)).cuota(c4);
        DetallePrestamo c = p.buscar(null);
        igual("salta las de saldo 0: cuota 4", 4.0, c != null ? c.getNumeroCuota() : null);
        igual("una consulta de saldo por cuota", 4, p.llamadasSaldo);
        igual("marca la 1 y la nula, no la 4", 2, p.marcadas);
        igual("la nula queda PAGADA", 4L, c3.getEstado());
        Prestamo33b todas = new Prestamo33b()
            .cuota(conTotal(cuota(1L, 1, 4, "2025-06-30", 50, 2), 0.0))
            .cuota(conTotal(cuota(2L, 2, 1, "2025-07-31", 50, 2), 0.0));
        igual("todas en 0: null", null, safeBuscar(todas));
        igual("todas en 0: una pasada", 2, todas.llamadasSaldo);
    }

    /** C33 punto 2 en la carga: una cuota saldada por un pago de 2026 está abierta al corte de junio 2025. */
    private static void c33bPagoPosteriorAlCorte() {
        DetallePrestamo c1 = conTotal(cuota(1L, 5, 4, "2025-06-30", 80, 4), 84.0);
        Prestamo33b p = new Prestamo33b()
            .cuota(c1, pago(11L, null, "2026-02-27", 80, 4, null))
            .cuota(conTotal(cuota(2L, 6, 1, "2025-07-31", 80, 4), 84.0));
        DetallePrestamo alCorte = p.buscar(CORTE_JUNIO);
        igual("al corte: la saldada en 2026 (cuota 5)", 5.0, alCorte != null ? alCorte.getNumeroCuota() : null);
        DetallePrestamo sinCorte = p.buscar(null);
        igual("sin corte: la siguiente (cuota 6)", 6.0, sinCorte != null ? sinCorte.getNumeroCuota() : null);
    }

    // ------------------------------------------------------------------ C33c (D-141 (5))

    private static final LocalDateTime CORTE_SEPTIEMBRE = LocalDateTime.of(2025, 9, 30, 23, 59, 59);

    /** Préstamo de prueba de C33c: cuotas y pagos, y las listas que arman selectCuotasExigibles y la precancelación. */
    private static final class Prestamo33c {
        final List<DetallePrestamo> cuotas = new ArrayList<>();
        final Map<Long, List<PagoPrestamo>> pagos = new LinkedHashMap<>();

        Prestamo33c cuota(DetallePrestamo c, PagoPrestamo... p) {
            cuotas.add(c);
            pagos.put(c.getCodigo(), lista(p));
            return this;
        }

        /** selectCuotasConSaldoPosibleOrdenadas(préstamo, desde, null): la regla C33 sin corte (saldoPosibleSql). */
        List<DetallePrestamo> conSaldoPosible() {
            List<DetallePrestamo> r = new ArrayList<>();
            for (DetallePrestamo c : cuotas) {
                if (PrelacionCuotas.tieneSaldoPosible(c, pagos.get(c.getCodigo()), DESDE)) {
                    r.add(c);
                }
            }
            return r;
        }

        /** selectCuotasExigibles con C33c. */
        List<DetallePrestamo> exigibles(LocalDateTime corte) {
            return PrelacionCuotas.vencidasAlCorte(conSaldoPosible(), corte);
        }

        /** selectCuotasExigibles hasta a12cb0b6: por estado. */
        List<DetallePrestamo> exigiblesComoAntes(LocalDateTime corte) {
            List<DetallePrestamo> r = new ArrayList<>();
            for (DetallePrestamo c : cuotas) {
                if (PrelacionCuotas.pendientePorEstado(c) && !c.getFechaVencimiento().isAfter(corte)) {
                    r.add(c);
                }
            }
            return r;
        }

        /** Futuras de calcularPrecancelacion: pendientes (C33 sin corte) que no son exigibles. */
        List<Double> futuras(List<DetallePrestamo> exigibles) {
            List<Double> r = new ArrayList<>();
            for (DetallePrestamo c : conSaldoPosible()) {
                if (!exigibles.contains(c)) {
                    r.add(c.getNumeroCuota());
                }
            }
            return r;
        }
    }

    private static List<Double> numeros(List<DetallePrestamo> cuotas) {
        List<Double> r = new ArrayList<>();
        for (DetallePrestamo c : cuotas) {
            r.add(c.getNumeroCuota());
        }
        return r;
    }

    /** El préstamo del caso: marca vencida, protecciones (arranque, 7, D-040 exento, pago posterior) y marca futura. */
    private static Prestamo33c prestamo33c() {
        DetallePrestamo c1 = cuota(1L, 1, 4, "2025-05-31", 100, 10);                // historia del ancla de mayo
        DetallePrestamo c2 = cuota(2L, 2, 4, "2025-06-30", 100, 10);                // pagada con PGPR
        DetallePrestamo c3 = conTotal(cuota(3L, 3, 4, "2025-07-31", 100, 10), 113); // marca de Delta, vencida
        c3.setDesgravamen(2.0);
        c3.setValorSeguroIncendio(1.0);
        DetallePrestamo c4 = cuota(4L, 4, 5, "2025-08-31", 100, 10);                // en mora
        DetallePrestamo c5 = cuota(5L, 5, 7, "2025-09-30", 100, 10);                // 7 de cierre
        DetallePrestamo c6 = cuota(6L, 6, 4, "2025-09-30", 100, 10);                // ancla del abono D-040 (exento)
        DetallePrestamo c7 = cuota(7L, 7, 4, "2025-09-30", 100, 10);                // saldada por un pago de 2026
        DetallePrestamo c8 = cuota(8L, 8, 1, "2025-10-31", 100, 10);                // futura
        DetallePrestamo c9 = cuota(9L, 9, 4, "2025-11-30", 100, 10);                // marca futura
        return new Prestamo33c()
            .cuota(c1)
            .cuota(c2, pago(20L, c2, "2025-06-30", 100, 10, null))
            .cuota(c3)
            .cuota(c4)
            .cuota(c5)
            .cuota(c6, pago(60L, c6, "2025-06-01", 450, 10, PrelacionCuotas.TIPO_ABONO_CAPITAL))
            .cuota(c7, pago(70L, c7, "2026-02-27", 100, 10, null))
            .cuota(c8)
            .cuota(c9);
    }

    /** selectCuotasExigibles por saldo real: entra la marca vencida y nada de lo protegido. */
    private static void c33cExigiblesPorSaldoReal() {
        Prestamo33c p = prestamo33c();
        List<DetallePrestamo> antes = p.exigiblesComoAntes(CORTE_SEPTIEMBRE);
        List<DetallePrestamo> ahora = p.exigibles(CORTE_SEPTIEMBRE);
        igual("antes: solo la 5 (#4)", Arrays.asList(4.0), numeros(antes));
        igual("antes: la marca #3 era futura", Arrays.asList(3.0, 8.0, 9.0), p.futuras(antes));
        igual("C33c: exigibles #3 (marca) y #4", Arrays.asList(3.0, 4.0), numeros(ahora));
        igual("C33c: futuras #8 y la marca futura #9", Arrays.asList(8.0, 9.0), p.futuras(ahora));
        verdad("la 4 anterior al arranque no es exigible", !numeros(ahora).contains(1.0));
        verdad("la 7 de cierre no es exigible", !numeros(ahora).contains(5.0));
        verdad("el ancla del abono D-040 (exento) no es exigible", !numeros(ahora).contains(6.0));
        verdad("la 4 saldada por un pago de 2026 no se cobra otra vez", !numeros(ahora).contains(7.0));
        // Modo al corte (C33 punto 2): al corte de septiembre 2025 la #7 está abierta (su pago es de 2026); la
        // precancelación mide sus saldos con todos los pagos (como calcularPrecancelacion), así que no la cobra.
        verdad("al corte la #7 tiene saldo", PrelacionCuotas.esMarcaSinRespaldo(p.cuotas.get(6),
            PrelacionCuotas.alCorte(p.pagos.get(7L), CORTE_SEPTIEMBRE), DESDE));
        igual("un día antes del vencimiento de la #4: solo la #3", Arrays.asList(3.0),
            numeros(p.exigibles(LocalDateTime.of(2025, 8, 30, 23, 59, 59))));
        igual("sin corte: ninguna", 0, PrelacionCuotas.vencidasAlCorte(p.conSaldoPosible(), null).size());
        DetallePrestamo sinVto = cuota(99L, 99, 1, "2025-06-30", 1, 0);
        sinVto.setFechaVencimiento(null);
        igual("sin vencimiento: no es exigible", 0,
            PrelacionCuotas.vencidasAlCorte(Arrays.asList(sinVto), CORTE_SEPTIEMBRE).size());
        igual("vence el mismo día del corte: exigible", 1, PrelacionCuotas.vencidasAlCorte(
            Arrays.asList(cuota(98L, 98, 1, "2025-09-30", 1, 0)), CORTE_SEPTIEMBRE).size());
    }

    /** Lo que se cobra de la marca: antes solo su capital; ahora los cinco conceptos con la mora a la fecha. */
    private static void c33cMontoDeLaMarca() {
        DetallePrestamo marca = prestamo33c().cuotas.get(2);
        // Saldos como calcularSaldosCuota sin pagos: componentes originales y total = DTPRTTLL.
        SaldosCuota s = saldosSinPagos(marca);
        double antes = s.getSaldoCapital();                                     // futura: solo capital
        // Mora a la fecha (calcularMoraCuota): 100 × 9 % / 360 × 61 días = 1,525 → 1,53.
        PrelacionCuotas.ajustarMoraALaFecha(s, 1.53, PrelacionCuotas.moraPagadaReal(marca, null), true);
        igualD("antes: solo el capital", 100.0, antes);
        igualD("C33c: capital + interés + desgravamen + seguro + mora", 114.53, s.getTotalPendiente());
        igualD("C33c: aumento de la precancelación (lo que se condonaba)", 14.53, s.getTotalPendiente() - antes);
        // DTPRTTLL de Delta en 0 (§7.6 de C33): el saldo real son los componentes.
        DetallePrestamo enCero = conTotal(prestamo33c().cuotas.get(2), 0.0);
        SaldosCuota s0 = saldosSinPagos(enCero);
        PrelacionCuotas.ajustarMoraALaFecha(s0, 1.53, 0.0, true);
        igualD("marca con DTPRTTLL en 0: por componentes", 114.53, s0.getTotalPendiente());
        // Una cuota que no es marca conserva la fórmula de siempre (total - mora anterior + mora a la fecha).
        SaldosCuota n = new SaldosCuota();
        n.setSaldoCapital(40.0);
        n.setSaldoInteres(8.0);
        n.setSaldoMora(2.0);
        n.setTotalPendiente(50.0);
        PrelacionCuotas.ajustarMoraALaFecha(n, 3.5, 1.0, false);
        igualD("no marca: mora a la fecha menos la pagada", 2.5, n.getSaldoMora());
        igualD("no marca: total como antes", 50.5, n.getTotalPendiente());
        SaldosCuota neg = new SaldosCuota();
        neg.setSaldoMora(0.0);
        neg.setTotalPendiente(10.0);
        PrelacionCuotas.ajustarMoraALaFecha(neg, 1.0, 4.0, false);
        igualD("mora pagada mayor que la mora a la fecha: 0", 0.0, neg.getSaldoMora());
    }

    /** En la marca, DTPRMRPG (Delta) no es saldo real: cuenta la mora de sus pagos vigentes. */
    private static void c33cMoraPagadaReal() {
        DetallePrestamo marca = cuota(1L, 3, 4, "2025-07-31", 100, 10);
        marca.setMoraPagado(5.0);
        PagoPrestamo p = pago(1L, marca, "2025-08-15", 20, 0, null);
        p.setMoraPagada(1.25);
        igualD("marca: la mora de sus PGPR", 1.25, PrelacionCuotas.moraPagadaReal(marca, lista(p)));
        igualD("marca sin PGPR: 0 aunque DTPRMRPG diga 5", 0.0, PrelacionCuotas.moraPagadaReal(marca, null));
        DetallePrestamo enMora = cuota(2L, 4, 5, "2025-08-31", 100, 10);
        enMora.setMoraPagado(5.0);
        igualD("cuota en mora: DTPRMRPG, como siempre", 5.0, PrelacionCuotas.moraPagadaReal(enMora, lista(p)));
    }

    /** Lo que graba la precancelación en la marca antes de que el motor la cobre. */
    private static void c33cFijarMora() {
        DetallePrestamo marca = conTotal(prestamo33c().cuotas.get(2), 0.0);
        PrelacionCuotas.fijarMoraALaFecha(marca, 1.53, 61, 0.0);
        igualD("mora a la fecha", 1.53, marca.getMora());
        igualD("total = componentes + mora (lo que cobra el motor sin pagos)", 114.53, marca.getTotal());
        igualD("total con seguro", 114.53, marca.getTotalConSeguro());
        igual("días de mora", 61L, marca.getDiasMora());
        igualD("saldo de mora", 1.53, marca.getSaldoMora());
        igual("sigue en 4: el estado lo decide el motor al pagar", 4L, marca.getEstado());
        // Lo que el motor ve después (calcularSaldosCuota sin pagos) = lo que mostró la simulación.
        SaldosCuota motor = saldosSinPagos(marca);
        SaldosCuota simulacion = saldosSinPagos(conTotal(prestamo33c().cuotas.get(2), 0.0));
        PrelacionCuotas.ajustarMoraALaFecha(simulacion, 1.53, 0.0, true);
        igualD("motor = simulación", simulacion.getTotalPendiente(), motor.getTotalPendiente());
        DetallePrestamo negativa = cuota(5L, 3, 4, "2025-07-31", 10, 1);
        PrelacionCuotas.fijarMoraALaFecha(negativa, -3.0, -2, 0.0);
        igualD("mora negativa: 0", 0.0, negativa.getMora());
        igual("días negativos: 0", 0L, negativa.getDiasMora());
    }

    /** Ancla del capital futuro: nunca una futura (el paso 3 la cierra en 7). */
    private static void c33cAncla() {
        Prestamo33c p = prestamo33c();
        List<DetallePrestamo> exigibles = p.exigibles(CORTE_SEPTIEMBRE);
        java.util.Set<Long> futuras = new java.util.HashSet<>();
        for (DetallePrestamo c : p.conSaldoPosible()) {
            if (!exigibles.contains(c)) {
                futuras.add(c.getCodigo());
            }
        }
        DetallePrestamo ultima = PrelacionCuotas.ultimaPagadaFuera(p.cuotas, null);
        igual("selectUltimaCuotaPagada da la marca futura #9", 9.0, ultima != null ? ultima.getNumeroCuota() : null);
        DetallePrestamo ancla = PrelacionCuotas.ultimaPagadaFuera(p.cuotas, futuras);
        igual("C33c: ancla = la última 4 que no es futura (#7)", 7.0, ancla != null ? ancla.getNumeroCuota() : null);
        Prestamo33c soloFuturas = new Prestamo33c()
            .cuota(cuota(1L, 1, 1, "2025-10-31", 100, 10))
            .cuota(cuota(2L, 2, 4, "2025-11-30", 100, 10));
        java.util.Set<Long> todas = new java.util.HashSet<>(Arrays.asList(1L, 2L));
        igual("solo marcas futuras: null (fallback de la primera futura)", null,
            PrelacionCuotas.ultimaPagadaFuera(soloFuturas.cuotas, todas));
        igual("lista nula: null", null, PrelacionCuotas.ultimaPagadaFuera(null, todas));
    }

    /** Réplica de MotorPagoPrestamoServiceImpl.calcularSaldosCuota sin pagos: originales y total = DTPRTTLL + IV. */
    private static SaldosCuota saldosSinPagos(DetallePrestamo c) {
        SaldosCuota s = new SaldosCuota();
        s.setSaldoCapital(PrelacionCuotas.nvl(c.getCapital()));
        s.setSaldoInteres(PrelacionCuotas.nvl(c.getInteres()));
        s.setSaldoDesgravamen(PrelacionCuotas.nvl(c.getDesgravamen()));
        s.setSaldoSeguroIncendio(PrelacionCuotas.nvl(c.getValorSeguroIncendio()));
        s.setSaldoMora(PrelacionCuotas.nvl(c.getMora()));
        s.setSaldoInteresVencido(PrelacionCuotas.nvl(c.getInteresVencido()));
        s.setTotalPendiente(PrelacionCuotas.redondear(PrelacionCuotas.nvl(c.getTotal())
            + PrelacionCuotas.nvl(c.getInteresVencido())));
        return s;
    }

    private static DetallePrestamo safeBuscar(Prestamo33b p) {
        return p.buscar(CORTE_JUNIO);
    }

    // ------------------------------------------------------------------ fábricas

    private static DetallePrestamo cuota(Long id, double numero, long estado, String vencimiento, double capital, double interes) {
        DetallePrestamo d = new DetallePrestamo();
        d.setCodigo(id);
        d.setNumeroCuota(numero);
        d.setEstado(estado);
        d.setFechaVencimiento(LocalDate.parse(vencimiento).atStartOfDay());
        d.setCapital(capital);
        d.setInteres(interes);
        d.setMora(0.0);
        d.setDesgravamen(0.0);
        d.setValorSeguroIncendio(0.0);
        d.setInteresVencido(0.0);
        return d;
    }

    private static PagoPrestamo pago(Long id, DetallePrestamo cuota, String fecha, double capital, double interes, String tipo) {
        PagoPrestamo p = new PagoPrestamo();
        p.setCodigo(id);
        p.setDetallePrestamo(cuota);
        p.setFecha(LocalDate.parse(fecha).atStartOfDay());
        p.setCapitalPagado(capital);
        p.setInteresPagado(interes);
        p.setMoraPagada(0.0);
        p.setDesgravamen(0.0);
        p.setValorSeguroIncendio(0.0);
        p.setSaldoOtros(0.0);
        p.setTipo(tipo);
        return p;
    }

    private static List<PagoPrestamo> lista(PagoPrestamo... p) {
        return new ArrayList<>(Arrays.asList(p));
    }

    private static PrelacionCuotas.CuotaPlan plan(Long id, double numero, boolean cerrada, double seguro, double desgravamen,
            double mora, double iv, double interes, double capital) {
        return new PrelacionCuotas.CuotaPlan(id, numero, cerrada, comp(seguro, desgravamen, mora, iv, interes, capital));
    }

    private static double[] comp(double seguro, double desgravamen, double mora, double iv, double interes, double capital) {
        return new double[] { seguro, desgravamen, mora, iv, interes, capital };
    }

    // ------------------------------------------------------------------ aserciones

    private static void verdad(String nombre, boolean cond) {
        if (cond) {
            ok++;
        } else {
            fallas.add(nombre);
        }
    }

    private static void igual(String nombre, Object esperado, Object real) {
        verdad(nombre + " (esperado " + esperado + ", real " + real + ")",
            esperado == null ? real == null : esperado.equals(real));
    }

    private static void igualD(String nombre, double esperado, double real) {
        verdad(nombre + " (esperado " + esperado + ", real " + real + ")", Math.abs(esperado - real) < 0.005);
    }
}
