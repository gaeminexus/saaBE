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
        letras = apocopar(letras);
        return letras + " con " + String.format("%02d", centavos) + "/100 " + (dolares == 1L ? "dólar" : "dólares");
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
