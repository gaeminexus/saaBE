package com.saa.ejb.cxc.util;

import com.saa.basico.util.IncomeException;
import com.saa.model.tsr.Titular;

/**
 * Lote 12 (VCN #18): el nombre con que un titular sale en un comprobante, en su XML, en el RIDE y en los mensajes: el nombre y, si está vacío, la
 * razón social. Un cliente o proveedor que solo tiene razón social (una empresa) es válido; con {@code titular.getNombre()} a secas el XML salía con
 * la razón social vacía y el SRI lo rechazaba, y los mensajes decían «proveedor 'null'». Puro: no modifica el titular.
 */
public final class NombreTitular {

    private NombreTitular() {
    }

    /** El nombre, o la razón social si el nombre está vacío; {@code null} si el titular es nulo o no tiene ninguno de los dos. */
    public static String de(Titular titular) {
        if (titular == null) {
            return null;
        }
        if (titular.getNombre() != null && !titular.getNombre().trim().isEmpty()) {
            return titular.getNombre();
        }
        return titular.getRazonSocial() != null && !titular.getRazonSocial().trim().isEmpty() ? titular.getRazonSocial() : null;
    }

    /**
     * Igual que {@link #de(Titular)} pero falla con un mensaje claro si el titular no tiene ni nombre ni razón social: se prefiere detener la
     * emisión antes de armar el XML que enviar al SRI un comprobante sin razón social.
     *
     * @param rol «cliente», «proveedor», «sujeto retenido»… para el texto del error
     */
    public static String requerido(Titular titular, String rol) {
        String nombre = de(titular);
        if (nombre == null) {
            throw new IncomeException("El " + rol + (titular != null && titular.getCodigo() != null ? " (código " + titular.getCodigo() + ")" : "")
                + " no tiene nombre ni razón social: complete sus datos antes de emitir el comprobante.");
        }
        return nombre;
    }
}
