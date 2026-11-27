package com.saa.ejb.cnt.service;

/**
 * C52: regla pura (sin EJB ni BD) de que es un saldo contrario a la naturaleza de la cuenta (D-150 / D-222 del cuadre).
 * <ul>
 * <li>Solo cuentas de movimiento y asientos vigentes (estados 1 y 3). Saldo = debe - haber.</li>
 * <li>BALANCE (acumulado al corte): activo (1) con saldo acreedor, salvo las cuentas de provision/valuacion; pasivo (2) o patrimonio (3) con saldo deudor.</li>
 * <li>RESULTADOS (acumulado del anio, sin el asiento de cierre del ejercicio): gasto (4) con saldo acreedor; ingreso (5) con saldo deudor.</li>
 * <li>Cuentas de orden (6, 7) y cualquier otra clase: fuera. Tolerancia 0,005.</li>
 * </ul>
 * <b>Provision/valuacion del activo</b> (verificado contra el plan de cuentas de ASOPREP, export q4-plan.csv del cuadre): son las que cuelgan de un nodo
 * cuyo nombre empieza con «(» (convencion del plan: (PROVISIONES…, (DEPRECIACION…, (AMORTIZACION ACUMULADA…) o que tienen «99» como tercer segmento
 * (1.2.99, 1.3.99, 1.4.99, 1.5.99, 1.6.99, 1.7.99, 1.9.99). El «99» como ULTIMO segmento NO lo es (1.1.02.05.99 es un banco; 1.2.01.05.0x.99, una tarjeta).
 */
public final class ReglaSaldoContrario {

    public static final double TOLERANCIA = 0.005;
    public static final String BALANCE = "BALANCE";
    public static final String RESULTADOS = "RESULTADOS";
    /** Marca del asiento de cierre del ejercicio que carga el cuadre: «#CIE-AAAA-MM-NNN» en la observacion (cargador C5). */
    public static final String MARCA_CIERRE_EJERCICIO = "#CIE-";
    /** Observacion del asiento de cierre que genera el motor (AsientoServiceImpl.generaCabeceraCierre). */
    public static final String PREFIJO_CIERRE_MOTOR = "ASIENTO DE CIERRE DEL PERIODO";

    private ReglaSaldoContrario() {
    }

    /** Clase de la cuenta (primer caracter) o {@code 0} si no hay. */
    public static char clase(String cuenta) {
        return cuenta == null || cuenta.trim().isEmpty() ? '0' : cuenta.trim().charAt(0);
    }

    /** {@code BALANCE} (1, 2, 3), {@code RESULTADOS} (4, 5) o {@code null} (fuera: cuentas de orden y otras). */
    public static String tipo(String cuenta) {
        switch (clase(cuenta)) {
            case '1': case '2': case '3': return BALANCE;
            case '4': case '5': return RESULTADOS;
            default: return null;
        }
    }

    /** Naturaleza esperada: DEUDORA (1, 4) o ACREEDORA (2, 3, 5); {@code null} si la cuenta esta fuera. */
    public static String naturaleza(String cuenta) {
        switch (clase(cuenta)) {
            case '1': case '4': return "DEUDORA";
            case '2': case '3': case '5': return "ACREEDORA";
            default: return null;
        }
    }

    /** Cuenta de provision/valuacion del activo: {@code 99} como tercer segmento, o ella o un ancestro con nombre que empieza con «(». */
    public static boolean esProvisionOValuacion(String cuenta, boolean ancestroOPropioConParentesis) {
        if (clase(cuenta) != '1') {
            return false;
        }
        if (ancestroOPropioConParentesis) {
            return true;
        }
        String[] partes = cuenta.trim().split("\\.");
        return partes.length >= 3 && "99".equals(partes[2]);
    }

    /**
     * Valor (positivo) en que el saldo es contrario, o {@code 0} si no lo es.
     *
     * @param cuenta              numero de la cuenta
     * @param saldoDebeMenosHaber saldo deudor positivo, acreedor negativo
     * @param provisionOValuacion resultado de {@link #esProvisionOValuacion}
     */
    public static double valorContrario(String cuenta, double saldoDebeMenosHaber, boolean provisionOValuacion) {
        switch (clase(cuenta)) {
            case '1': // activo: acreedor es contrario, salvo provision/valuacion
                return !provisionOValuacion && saldoDebeMenosHaber < -TOLERANCIA ? -saldoDebeMenosHaber : 0.0;
            case '2': case '3': // pasivo y patrimonio: deudor es contrario
                return saldoDebeMenosHaber > TOLERANCIA ? saldoDebeMenosHaber : 0.0;
            case '4': // gasto: acreedor es contrario
                return saldoDebeMenosHaber < -TOLERANCIA ? -saldoDebeMenosHaber : 0.0;
            case '5': // ingreso: deudor es contrario
                return saldoDebeMenosHaber > TOLERANCIA ? saldoDebeMenosHaber : 0.0;
            default:
                return 0.0;
        }
    }

    /** {@code true} si la observacion del asiento es la del cierre del ejercicio (se excluye del acumulado de resultados). */
    public static boolean esAsientoDeCierreDelEjercicio(String observacion) {
        return observacion != null && (observacion.contains(MARCA_CIERRE_EJERCICIO) || observacion.startsWith(PREFIJO_CIERRE_MOTOR));
    }
}
