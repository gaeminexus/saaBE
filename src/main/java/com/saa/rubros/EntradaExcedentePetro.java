package com.saa.rubros;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D-010 corrección (28-oct-2026): producto Petro por el que ENTRÓ el dinero de un aporte
 * {@link CrdTipoMovimientoAporte#EXCEDENTE_PETRO}. El reparto de la carga acredita
 * {@code 1.4.05.05} por las filas AH y {@code 1.4.05.10} por todas las demás (PE, PH, PQ, PP, HS);
 * la aplicación tiene que debitar el puente del MISMO producto de entrada ({@code 2.3.02.05} o
 * {@code 2.3.02.10}), aunque el haber vaya a la cuenta individual del partícipe. Un excedente
 * que vino de una fila de préstamo y se devuelve a aportes debita {@code 2.3.02.10}.
 *
 * <p>El aporte no tiene columna para el producto de origen: se estampa en la glosa
 * ({@code APRTGLSA}) con la marca {@code [Entrada Petro XX]}. Para las glosas D-010 anteriores a
 * la marca se reconoce también el formato {@code "(D-010) - XX - "}.</p>
 */
public final class EntradaExcedentePetro {

    /** Código Petro del producto de aportes (filas AH): entran por 1.4.05.05. */
    public static final String PRODUCTO_APORTES = "AH";

    private static final Pattern MARCA = Pattern.compile("\\[Entrada Petro ([A-Za-z0-9]+)\\]");
    private static final Pattern D010 = Pattern.compile("\\(D-010\\) - ([A-Za-z0-9]+) - ");

    private EntradaExcedentePetro() {
    }

    /** Marca a agregar a la glosa del aporte. */
    public static String marca(String codigoProducto) {
        return "[Entrada Petro " + (codigoProducto != null ? codigoProducto.trim() : "?") + "]";
    }

    /** Producto de entrada leído de la glosa, o {@code null} si la glosa no lo dice. */
    public static String productoDeGlosa(String glosa) {
        if (glosa == null) {
            return null;
        }
        Matcher m = MARCA.matcher(glosa);
        if (m.find()) {
            return m.group(1);
        }
        m = D010.matcher(glosa);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /** ¿El dinero entró por el lado préstamos del reparto (1.4.05.10)? */
    public static boolean entroPorPrestamos(String glosa) {
        String producto = productoDeGlosa(glosa);
        return producto != null && !PRODUCTO_APORTES.equalsIgnoreCase(producto);
    }
}
