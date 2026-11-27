package com.saa.ejb.crd.service;

import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.serviceImpl.ControlCuotasSinFechaServiceImpl;

/**
 * C48: pruebas sin framework de las reglas puras del control de cuotas 7 sin fecha (texto de la lista y propiedad de bloqueo).
 * Correr: {@code sh tools/pruebas/correr-pruebas-c48.sh}. Sale con código 1 si algo falla.
 */
public class ControlCuotasSinFechaPruebas {

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

    private static Object[] fila(long prestamo, Object operacion, Object cuota) {
        return new Object[]{ Long.valueOf(prestamo), operacion, cuota };
    }

    public static void main(String[] args) {
        System.out.println("(a) sin cuotas -> sin control");
        verifica("null si no hay filas", ControlCuotasSinFechaServiceImpl.armaTexto(new ArrayList<Object[]>()) == null);
        verifica("null si la lista es nula", ControlCuotasSinFechaServiceImpl.armaTexto(null) == null);

        System.out.println("(b) lista préstamo/cuota");
        List<Object[]> filas = new ArrayList<Object[]>();
        filas.add(fila(10, Long.valueOf(500), Long.valueOf(7)));
        filas.add(fila(10, Long.valueOf(500), Long.valueOf(8)));
        filas.add(fila(20, null, Long.valueOf(3)));
        String t = ControlCuotasSinFechaServiceImpl.armaTexto(filas);
        verifica("cuenta 3 cuotas en 2 préstamos", t != null && t.contains("Hay 3 cuota(s)") && t.contains("en 2 préstamo(s)"));
        verifica("lista el préstamo 10 con su operación y sus cuotas", t != null && t.contains(" 10 (500) [7, 8];"));
        verifica("lista el préstamo 20 sin operación", t != null && t.contains(" 20 [3];"));
        verifica("remite al script 412", t != null && t.contains("412_C48_DTPRFCPG_CUOTAS_7_SIN_FECHA.sql"));

        System.out.println("(c) la lista se acota pero el total se dice");
        List<Object[]> muchas = new ArrayList<Object[]>();
        for (int i = 1; i <= 45; i++) {
            muchas.add(fila(i, Long.valueOf(1000 + i), Long.valueOf(1)));
        }
        String m = ControlCuotasSinFechaServiceImpl.armaTexto(muchas);
        verifica("dice 45 cuotas en 45 préstamos", m != null && m.contains("Hay 45 cuota(s)") && m.contains("en 45 préstamo(s)"));
        verifica("lista 30 y avisa de los 15 que faltan", m != null && m.contains("y 15 préstamo(s) más") && m.contains(" 30 (1030) [1];") && !m.contains(" 31 (1031)"));

        System.out.println("(d) propiedad de bloqueo, apagada por defecto");
        String anterior = System.getProperty(ControlCuotasSinFechaService.PROPIEDAD_BLOQUEO);
        System.clearProperty(ControlCuotasSinFechaService.PROPIEDAD_BLOQUEO);
        verifica("apagada si no está definida", !ControlCuotasSinFechaService.bloqueoActivo());
        System.setProperty(ControlCuotasSinFechaService.PROPIEDAD_BLOQUEO, "true");
        verifica("encendida con true", ControlCuotasSinFechaService.bloqueoActivo());
        System.setProperty(ControlCuotasSinFechaService.PROPIEDAD_BLOQUEO, "falso");
        verifica("apagada con un valor que no es true", !ControlCuotasSinFechaService.bloqueoActivo());
        if (anterior != null) {
            System.setProperty(ControlCuotasSinFechaService.PROPIEDAD_BLOQUEO, anterior);
        } else {
            System.clearProperty(ControlCuotasSinFechaService.PROPIEDAD_BLOQUEO);
        }

        System.out.println();
        System.out.println(pruebas + " verificaciones, " + fallos + " fallos");
        if (fallos > 0) {
            System.exit(1);
        }
    }
}
