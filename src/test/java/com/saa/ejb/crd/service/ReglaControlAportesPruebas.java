package com.saa.ejb.crd.service;

/** C53: pruebas sin framework de las reglas puras del control de aportes ({@link ReglaControlAportes}) y de la propiedad de bloqueo. Correr: {@code sh tools/pruebas/correr-pruebas-c53.sh}. */
public class ReglaControlAportesPruebas {

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
        System.out.println("(a) diferencia contabilidad menos modulo y tolerancia 0,005");
        verifica("el caso de julio: el modulo tiene 161,47 mas que la contabilidad da -161,47", ReglaControlAportes.diferencia(1161.47, 1000.00) == -161.47);
        verifica("diferencia positiva: la contabilidad tiene mas que el modulo", ReglaControlAportes.diferencia(1000.00, 1161.47) == 161.47);
        verifica("0,005 exacto no es diferencia", !ReglaControlAportes.hayDiferencia(100.005, 100.0));
        verifica("0,006 si es diferencia", ReglaControlAportes.hayDiferencia(100.006, 100.0));
        verifica("iguales no es diferencia", !ReglaControlAportes.hayDiferencia(48700549.48, 48700549.48));

        System.out.println("(b) que cuentas entran al total 2.1");
        verifica("2.1 entra", ReglaControlAportes.enTotal21("2.1"));
        verifica("2.1.02.25.01 (pension complementaria) entra", ReglaControlAportes.enTotal21("2.1.02.25.01"));
        verifica("sin cuenta en CTAP se queda en el total", ReglaControlAportes.enTotal21(null));
        verifica("2.3.90.90.11 (seguro por entregar) NO entra", !ReglaControlAportes.enTotal21("2.3.90.90.11"));
        verifica("2.10 no es hija de 2.1", !ReglaControlAportes.enTotal21("2.10.01"));

        System.out.println("(c) vinculo y formato");
        verifica("solo el REGISTRO APORTE (AJUSTE_MANUAL = 2) tiene vinculo por aporte", ReglaControlAportes.MOVIMIENTO_CON_VINCULO == 2L);
        verifica("monto con punto decimal", "161.47".equals(ReglaControlAportes.monto(161.47)));

        System.out.println("(d) propiedad de bloqueo, apagada por defecto");
        String anterior = System.getProperty(ControlAportesService.PROPIEDAD_BLOQUEO);
        System.clearProperty(ControlAportesService.PROPIEDAD_BLOQUEO);
        verifica("apagada sin propiedad", !ControlAportesService.bloqueoActivo());
        System.setProperty(ControlAportesService.PROPIEDAD_BLOQUEO, "true");
        verifica("encendida con true", ControlAportesService.bloqueoActivo());
        if (anterior != null) {
            System.setProperty(ControlAportesService.PROPIEDAD_BLOQUEO, anterior);
        } else {
            System.clearProperty(ControlAportesService.PROPIEDAD_BLOQUEO);
        }

        System.out.println();
        System.out.println(pruebas + " verificaciones, " + fallos + " fallos");
        if (fallos > 0) {
            System.exit(1);
        }
    }
}
