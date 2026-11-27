package com.saa.ejb.cnt.service;

/** C52: pruebas sin framework de la regla de saldo contrario ({@link ReglaSaldoContrario}). Correr: {@code sh tools/pruebas/correr-pruebas-c52.sh}. */
public class ReglaSaldoContrarioPruebas {

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

    private static boolean contrario(String cuenta, double saldo, boolean provision) {
        return ReglaSaldoContrario.valorContrario(cuenta, saldo, provision) > 0.0;
    }

    public static void main(String[] args) {
        System.out.println("(a) activo: acreedor es contrario, salvo provision o valuacion");
        verifica("activo deudor: no", !contrario("1.1.01", 100.0, false));
        verifica("activo acreedor: si", contrario("1.1.01", -10.0, false));
        verifica("activo acreedor valor", Math.abs(ReglaSaldoContrario.valorContrario("1.1.01", -10.5, false) - 10.5) < 1e-9);
        verifica("provision 1.4.99.05 acreedor: no", !contrario("1.4.99.05", -500.0, ReglaSaldoContrario.esProvisionOValuacion("1.4.99.05", false)));
        verifica("provision 1.3.99.15 acreedor: no", !contrario("1.3.99.15", -500.0, ReglaSaldoContrario.esProvisionOValuacion("1.3.99.15", false)));
        verifica("depreciacion 1.5.02.99 (marca del plan «(»): no", !contrario("1.5.02.99", -5.0, ReglaSaldoContrario.esProvisionOValuacion("1.5.02.99", true)));
        verifica("provision 1.4.99.05 DEUDOR tampoco es contrario (solo el activo acreedor lo es)", !contrario("1.4.99.05", 5.0, true));

        System.out.println("(b) el «99» solo cuenta como tercer segmento");
        verifica("banco 1.1.02.05.99 NO es provision", !ReglaSaldoContrario.esProvisionOValuacion("1.1.02.05.99", false));
        verifica("tarjeta 1.2.01.05.01.99 NO es provision", !ReglaSaldoContrario.esProvisionOValuacion("1.2.01.05.01.99", false));
        verifica("banco 1.1.02.05.99 acreedor: contrario", contrario("1.1.02.05.99", -3.0, ReglaSaldoContrario.esProvisionOValuacion("1.1.02.05.99", false)));
        verifica("1.7.99.05 es provision", ReglaSaldoContrario.esProvisionOValuacion("1.7.99.05", false));
        verifica("una cuenta de pasivo nunca es provision de activo", !ReglaSaldoContrario.esProvisionOValuacion("2.1.99.01", true));

        System.out.println("(c) pasivo y patrimonio: deudor es contrario");
        verifica("pasivo acreedor: no", !contrario("2.5.01", -850.13, false));
        verifica("pasivo deudor 99,77: si (el caso de junio)", Math.abs(ReglaSaldoContrario.valorContrario("2.5.01", 99.77, false) - 99.77) < 1e-9);
        verifica("pasivo deudor 850,13: si (el caso de julio)", contrario("2.5.01", 850.13, false));
        verifica("patrimonio deudor: si", contrario("3.4.04", 1.0, false));

        System.out.println("(d) resultados: gasto acreedor e ingreso deudor");
        verifica("gasto deudor: no", !contrario("4.8.99.08", 5.0, false));
        verifica("gasto acreedor 0,11: si (4.8.99.08 de junio)", Math.abs(ReglaSaldoContrario.valorContrario("4.8.99.08", -0.11, false) - 0.11) < 1e-9);
        verifica("ingreso acreedor: no", !contrario("5.3.90.90", -5.0, false));
        verifica("ingreso deudor: si", contrario("5.3.90.90", 5.0, false));

        System.out.println("(e) tolerancia y cuentas de orden");
        verifica("saldo de 0,005 no es contrario", !contrario("2.5.01", 0.005, false) && !contrario("4.8.99.08", -0.005, false));
        verifica("saldo de 0,006 si", contrario("2.5.01", 0.006, false));
        verifica("cuenta de orden 6 fuera", !contrario("6.1.01", -100.0, false) && ReglaSaldoContrario.tipo("6.1.01") == null);
        verifica("cuenta de orden 7 fuera", !contrario("7.1.01", 100.0, false) && ReglaSaldoContrario.naturaleza("7.1.01") == null);

        System.out.println("(f) tipo, naturaleza y asiento de cierre del ejercicio");
        verifica("tipos", "BALANCE".equals(ReglaSaldoContrario.tipo("2.5.01")) && "RESULTADOS".equals(ReglaSaldoContrario.tipo("4.8.99.08")));
        verifica("naturalezas", "DEUDORA".equals(ReglaSaldoContrario.naturaleza("1.1")) && "ACREEDORA".equals(ReglaSaldoContrario.naturaleza("3.4"))
                && "DEUDORA".equals(ReglaSaldoContrario.naturaleza("4.1")) && "ACREEDORA".equals(ReglaSaldoContrario.naturaleza("5.1")));
        verifica("asiento de cierre del cargador (#CIE-)", ReglaSaldoContrario.esAsientoDeCierreDelEjercicio("[cierre.csv] #CIE-2025-12-001 | cierre de resultados"));
        verifica("asiento de cierre del motor", ReglaSaldoContrario.esAsientoDeCierreDelEjercicio("ASIENTO DE CIERRE DEL PERIODO 12 AÑO 2025"));
        verifica("otro asiento no", !ReglaSaldoContrario.esAsientoDeCierreDelEjercicio("[ajustes.csv] #AJC-2026-06-002 | ajuste") && !ReglaSaldoContrario.esAsientoDeCierreDelEjercicio(null));

        System.out.println();
        System.out.println(pruebas + " verificaciones, " + fallos + " fallos");
        if (fallos > 0) {
            System.exit(1);
        }
    }
}
