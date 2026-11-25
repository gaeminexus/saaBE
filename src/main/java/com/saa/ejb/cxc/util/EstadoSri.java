package com.saa.ejb.cxc.util;

/**
 * Lote 12 (VCN #8): clasifica el {@code <estado>} que devuelve el WS2 de autorización del SRI. Antes todo lo que no fuera exactamente AUTORIZADO se
 * guardaba como NO AUTORIZADA (estado 6), incluso «en proceso» o una respuesta sin {@code <autorizacion>}, y el comprobante quedaba rechazado aunque el
 * SRI lo fuera a autorizar un momento después.
 */
public final class EstadoSri {

    public enum Clasificacion {
        /** El SRI autorizó el comprobante. */
        AUTORIZADO,
        /** El SRI dijo expresamente que no lo autoriza: rechazo definitivo. */
        RECHAZADO,
        /** Respuesta intermedia, vacía o desconocida (en proceso, sin autorización todavía): no es un rechazo, se vuelve a consultar. */
        PENDIENTE
    }

    private EstadoSri() {
    }

    public static Clasificacion clasificar(String estado) {
        if (estado == null) {
            return Clasificacion.PENDIENTE;
        }
        String e = estado.trim().toUpperCase();
        if (e.equals("AUTORIZADO") || e.equals("AUTORIZADA")) {
            return Clasificacion.AUTORIZADO;
        }
        if (e.equals("NO AUTORIZADO") || e.equals("NO AUTORIZADA") || e.equals("NO_AUTORIZADO") || e.equals("RECHAZADA")
                || e.equals("RECHAZADO")) {
            return Clasificacion.RECHAZADO;
        }
        return Clasificacion.PENDIENTE;
    }

    /** {@code true} si hay que dejar el comprobante en espera (estado 4) y volver a consultar, en vez de marcarlo rechazado. */
    public static boolean esPendiente(String estado) {
        return clasificar(estado) == Clasificacion.PENDIENTE;
    }
}
