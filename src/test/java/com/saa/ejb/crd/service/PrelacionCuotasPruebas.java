package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
