package com.saa.ejb.crd.util;

/**
 * Convierte un entero a su forma en letras, en español ("ochenta y ocho"). Vive DENTRO de
 * {@code crd}, no en {@code basico} (que es compartido) — no hay otro utilitario de número a
 * letras en el backend (grep, 2026-09-30, API-PASE-A-PLAZO-VENCIDO.md §8). Se usa hoy solo para
 * las cuotas impagas de la liquidación de plazo vencido («ochenta y ocho (88)»), así que cubre
 * 0-999.999 — de sobra para ese caso — y no intenta decimales ni género.
 */
public final class NumeroALetrasUtil {

    private static final String[] UNIDADES = {
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete",
        "dieciocho", "diecinueve", "veinte"
    };

    private static final String[] DECENAS = {
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    private static final String[] CENTENAS = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    private NumeroALetrasUtil() {}

    /**
     * @param numero entero no negativo, hasta 999.999
     * @return el número en letras, en minúsculas ("ochenta y ocho"); {@code "cero"} si es null o 0
     */
    public static String convertir(Long numero) {
        if (numero == null || numero == 0L) {
            return "cero";
        }
        if (numero < 0 || numero > 999_999L) {
            // Fuera del rango que este utilitario cubre a propósito: mejor un número crudo
            // visible que un texto silenciosamente incompleto.
            return String.valueOf(numero);
        }
        int n = numero.intValue();
        if (n < 1000) {
            return convertirHastaMil(n);
        }
        int miles = n / 1000;
        int resto = n % 1000;
        String textoMiles = (miles == 1) ? "mil" : convertirHastaMil(miles) + " mil";
        return resto == 0 ? textoMiles : textoMiles + " " + convertirHastaMil(resto);
    }

    /**
     * Monto en dólares en letras, para el pagaré: {@code 5000.5} → {@code "cinco mil con 50/100 dólares"}.
     * Cubre hasta 999.999.999 dólares; por encima devuelve el número crudo (visible, no un texto
     * silenciosamente incompleto). Aplica la apócope («veintiún mil», «treinta y un dólares»).
     * Lo llama el {@code .jrxml} del pagaré (RPRT_PRST_PGRE).
     */
    public static String convertirMonto(Double monto) {
        double valor = monto != null ? Math.abs(monto) : 0.0;
        long centavosTotales = Math.round(valor * 100.0);
        long dolares = centavosTotales / 100L;
        int centavos = (int) (centavosTotales % 100L);
        if (dolares > 999_999_999L) {
            return String.valueOf(dolares) + " con " + String.format("%02d", centavos) + "/100 dólares";
        }
        return dolaresEnLetras(dolares) + " con " + String.format("%02d", centavos) + "/100 "
            + (dolares == 1L ? "dólar" : "dólares");
    }

    /**
     * Monto con el formato EXACTO del pagaré del cliente (MODELO_DE_PAGARE_V2, IM4):
     * {@code 64800} → {@code "SESENTA Y CUATRO MIL OCHOCIENTOS DÓLARES DE LOS ESTADOS UNIDOS DE AMÉRICA CON 00/100"}.
     * Va en mayúsculas y con la leyenda completa; el «(USD: …)» lo agrega el reporte. Por encima de
     * 999.999.999 devuelve el número crudo, igual que {@link #convertirMonto}.
     */
    public static String convertirMontoModelo(Double monto) {
        double valor = monto != null ? Math.abs(monto) : 0.0;
        long centavosTotales = Math.round(valor * 100.0);
        long dolares = centavosTotales / 100L;
        String centavos = String.format("%02d", (int) (centavosTotales % 100L));
        String letras = dolares > 999_999_999L ? String.valueOf(dolares) : dolaresEnLetras(dolares).toUpperCase();
        return letras + (dolares == 1L ? " DÓLAR" : " DÓLARES") + " DE LOS ESTADOS UNIDOS DE AMÉRICA CON " + centavos + "/100";
    }

    /**
     * Tasa con el formato del pagaré: {@code 9.0} → {@code "NUEVE CON 00/100"}; {@code 9.25} →
     * {@code "NUEVE CON 25/100"}. El «(9.0) %» lo agrega el reporte con {@link #formatearTasaModelo}.
     */
    public static String convertirTasaModelo(Double tasa) {
        double valor = tasa != null ? Math.abs(tasa) : 0.0;
        long centesimas = Math.round(valor * 100.0);
        return convertir(centesimas / 100L).toUpperCase() + " CON " + String.format("%02d", (int) (centesimas % 100L)) + "/100";
    }

    /** Tasa como la imprime el modelo dentro del paréntesis: {@code 9} → {@code "9.0"}, {@code 9.25} → {@code "9.25"}. */
    public static String formatearTasaModelo(Double tasa) {
        java.math.BigDecimal t = java.math.BigDecimal.valueOf(tasa != null ? tasa : 0.0)
            .setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        if (t.scale() < 1) {
            t = t.setScale(1);
        }
        return t.toPlainString();
    }

    /** Monto como lo imprime el modelo junto al «USD:» de las letras: {@code 64800} → {@code "64.800,00"}. */
    public static String formatearMontoEc(Double monto) {
        java.text.DecimalFormatSymbols simbolos = new java.text.DecimalFormatSymbols();
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return new java.text.DecimalFormat("#,##0.00", simbolos).format(monto != null ? monto : 0.0);
    }

    /** Cuota como la imprime el modelo junto al «USD.»: {@code 746.97} → {@code "746.97"} (punto decimal, sin miles). */
    public static String formatearCuotaModelo(Double cuota) {
        return String.format(java.util.Locale.US, "%.2f", cuota != null ? cuota : 0.0);
    }

    /** Dólares enteros (0-999.999.999) en letras minúsculas, con la apócope de «veintiún mil», «un millón». */
    private static String dolaresEnLetras(long dolares) {
        String letras;
        if (dolares == 0L) {
            letras = "cero";
        } else if (dolares < 1_000_000L) {
            letras = convertir(dolares);
        } else {
            long millones = dolares / 1_000_000L;
            long resto = dolares % 1_000_000L;
            String textoMillones = (millones == 1L) ? "un millón" : apocopar(convertir(millones)) + " millones";
            letras = resto == 0L ? textoMillones : textoMillones + " " + convertir(resto);
        }
        // Apócope del último "uno" y de los multiplicadores: «veintiún mil», «un dólar».
        letras = letras.replace("veintiuno mil", "veintiún mil").replace("uno mil", "un mil");
        return apocopar(letras);
    }

    private static String apocopar(String texto) {
        if (texto.endsWith("veintiuno")) {
            return texto.substring(0, texto.length() - "veintiuno".length()) + "veintiún";
        }
        if (texto.endsWith("uno")) {
            return texto.substring(0, texto.length() - 3) + "un";
        }
        return texto;
    }

    private static String convertirHastaMil(int n) {
        if (n == 100) {
            return "cien";
        }
        if (n >= 100) {
            String centena = CENTENAS[n / 100];
            int resto = n % 100;
            return resto == 0 ? centena : centena + " " + convertirHastaCien(resto);
        }
        return convertirHastaCien(n);
    }

    private static String convertirHastaCien(int n) {
        if (n <= 20) {
            return UNIDADES[n];
        }
        int decena = n / 10;
        int unidad = n % 10;
        if (unidad == 0) {
            return DECENAS[decena];
        }
        if (decena == 2) {
            // 21-29: veintiuno, veintidós, ... (una sola palabra)
            return "veinti" + UNIDADES[unidad];
        }
        return DECENAS[decena] + " y " + UNIDADES[unidad];
    }
}
