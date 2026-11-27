package com.saa.ejb.crd.service;

import java.util.Locale;

/**
 * C53: reglas puras (sin EJB ni BD) del control de aportes antes del cierre de cartera.
 * <ul>
 * <li><b>Control A, aportes sin asiento</b> ({@code APORTES_SIN_ASIENTO}): CRD.APRT vigente ({@code AporteVigenteSql}), de movimiento {@link #MOVIMIENTO_CON_VINCULO}
 * (REGISTRO APORTE: {@code AJUSTE_MANUAL}), positivo, con fecha de caja en el mes, SIN respaldo. Respaldo = su {@code CRD.PGAP} vigente tiene
 * {@code PGAPNMAS} (asiento) o es la linea ({@code CRD.DCBC.PGAPCDGO}) de un cobro {@code CRD.CBCR} cuyo asiento definitivo ({@code CBCRASN2}) esta vigente.
 * Los demas movimientos (mensual, migrado, excedente Petro, pago de prestamo, devolucion, reverso, jubilacion, pago de pension) NO tienen un vinculo APRT a asiento por
 * aporte (se contabilizan por carga, por cobro o por devolucion): los cubre el control B por saldo.</li>
 * <li><b>Control B, 2.1 contable contra el modulo</b> ({@code APORTES_21_VS_MODULO}): suma neta de aportes vigentes al corte (la misma lectura de la G42, por tipo) contra el
 * saldo acreedor de la cuenta de pasivo del tipo (CRD.CTAP vigente de la empresa), por cuenta y en total 2.1. Tolerancia 0,005.</li>
 * </ul>
 */
public final class ReglaControlAportes {

    public static final double TOLERANCIA = 0.005;
    /** {@code CrdTipoMovimientoAporte.AJUSTE_MANUAL}: el REGISTRO APORTE de ventanilla y de los cobros CBCR REGISTRO_APORTE. */
    public static final long MOVIMIENTO_CON_VINCULO = 2L;
    /** Raiz del pasivo de aportes. */
    public static final String RAIZ_APORTES = "2.1";

    private ReglaControlAportes() {
    }

    /** Diferencia contabilidad menos modulo (contable = saldo acreedor, haber - debe). Negativa: la contabilidad tiene MENOS que el modulo (aportes sin asiento). */
    public static double diferencia(double valorModulo, double saldoContable) {
        return Math.round((saldoContable - valorModulo) * 100.0) / 100.0;
    }

    public static boolean hayDiferencia(double valorModulo, double saldoContable) {
        return Math.abs(valorModulo - saldoContable) > TOLERANCIA;
    }

    /** La cuenta de pasivo del tipo pertenece al total 2.1: es la 2.1 o cuelga de ella. Sin cuenta (nulo) tambien: se queda en el total, como el Resumen Financiero. */
    public static boolean enTotal21(String cuentaPasivo) {
        return cuentaPasivo == null || cuentaPasivo.equals(RAIZ_APORTES) || cuentaPasivo.startsWith(RAIZ_APORTES + ".");
    }

    public static String monto(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
