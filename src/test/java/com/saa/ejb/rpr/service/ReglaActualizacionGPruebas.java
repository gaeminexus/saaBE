package com.saa.ejb.rpr.service;

import java.time.LocalDateTime;

/**
 * C50: pruebas sin framework de la regla pura {@link ReglaActualizacionG}. Correr: {@code sh tools/pruebas/correr-pruebas-c50.sh}. Sale con 1 si algo falla.
 */
public class ReglaActualizacionGPruebas {

    private static int fallos = 0;
    private static int pruebas = 0;

    private static void verifica(String nombre, boolean condicion) {
        pruebas++;
        if (!condicion) {
            fallos++;
            System.out.println("  FALLA: " + nombre);
        } else {
            System.out.println("  ok: " + nombre);
        }
    }

    public static void main(String[] args) {
        LocalDateTime t0 = LocalDateTime.of(2026, 7, 31, 18, 0);
        LocalDateTime t1 = t0.plusHours(2);

        System.out.println("(a) sin corrida grabada: sin dato");
        ReglaActualizacionG.Resultado r = ReglaActualizacionG.evaluar(null, Long.valueOf(267), null, null, t1);
        verifica("SIN_DATO", ReglaActualizacionG.SIN_DATO.equals(r.getEstado()));
        verifica("no está desactualizada", !r.estaDesactualizada());

        System.out.println("(b) la corrida se reversó (ya no hay vigente)");
        r = ReglaActualizacionG.evaluar(Long.valueOf(266), null, t0, t0, t0);
        verifica("DESACTUALIZADA", r.estaDesactualizada());
        verifica("el motivo dice la corrida 266 y que se reverso", r.getMotivo().contains("266") && r.getMotivo().contains("reversada"));

        System.out.println("(c) se reversó la 266 y se ejecutó la 267 (caso de julio)");
        r = ReglaActualizacionG.evaluar(Long.valueOf(266), Long.valueOf(267), t0, t0, t0);
        verifica("DESACTUALIZADA", r.estaDesactualizada());
        verifica("el motivo dice 266 y 267", r.getMotivo().contains("266") && r.getMotivo().contains("267"));

        System.out.println("(d) misma corrida, sin asientos nuevos");
        r = ReglaActualizacionG.evaluar(Long.valueOf(267), Long.valueOf(267), t0, t0, t0);
        verifica("VIGENTE (último asiento igual)", ReglaActualizacionG.VIGENTE.equals(r.getEstado()));
        r = ReglaActualizacionG.evaluar(Long.valueOf(267), Long.valueOf(267), t0, t0, t0.minusHours(1));
        verifica("VIGENTE (último asiento anterior)", ReglaActualizacionG.VIGENTE.equals(r.getEstado()));
        r = ReglaActualizacionG.evaluar(Long.valueOf(267), Long.valueOf(267), null, t0, null);
        verifica("VIGENTE (no había asientos ni hay)", ReglaActualizacionG.VIGENTE.equals(r.getEstado()));

        System.out.println("(e) misma corrida, asiento posterior");
        r = ReglaActualizacionG.evaluar(Long.valueOf(267), Long.valueOf(267), t0, t0, t1);
        verifica("DESACTUALIZADA", r.estaDesactualizada());
        verifica("el motivo habla del asiento posterior", r.getMotivo().contains("asiento"));
        r = ReglaActualizacionG.evaluar(Long.valueOf(267), Long.valueOf(267), null, t0, t1);
        verifica("sin último grabado compara contra el instante de generación", r.estaDesactualizada());
        r = ReglaActualizacionG.conAsiento(r, Long.valueOf(4321), "2026-07-31");
        verifica("conAsiento agrega número y fecha contable", r.getMotivo().contains("4321") && r.getMotivo().contains("2026-07-31"));

        System.out.println();
        System.out.println(pruebas + " verificaciones, " + fallos + " fallos");
        if (fallos > 0) {
            System.exit(1);
        }
    }
}
