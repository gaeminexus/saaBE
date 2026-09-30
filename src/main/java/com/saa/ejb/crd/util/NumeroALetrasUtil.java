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
