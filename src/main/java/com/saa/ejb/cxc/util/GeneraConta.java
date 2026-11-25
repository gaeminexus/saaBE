package com.saa.ejb.cxc.util;

import com.saa.model.cxc.Facturador;

/**
 * VCN #6 (dudoso): decide si un comprobante se asienta según la bandera {@code generaConta} del facturador. La regla es la de siempre (solo con el valor 1);
 * lo nuevo es el aviso: un facturador con la bandera NULA no asienta y antes no decía nada, así que el comprobante quedaba sin contabilizar en silencio.
 */
public final class GeneraConta {

    private GeneraConta() {
    }

    /** {@code true} solo si {@code generaConta == 1}. Con la bandera nula deja un aviso en el log (no cambia cuándo se asienta). */
    public static boolean asienta(Facturador facturador) {
        Long bandera = facturador != null ? facturador.getGeneraConta() : null;
        if (bandera == null) {
            System.err.println("⚠ El facturador " + (facturador != null ? facturador.getId() : null)
                + " tiene generaConta NULO: sus comprobantes NO se asientan. Configure 1 (asienta) o 0 (no asienta) en el facturador.");
        }
        return Long.valueOf(1L).equals(bandera);
    }
}
