package com.saa.ejb.crd.firma;

/**
 * Falla del proveedor de firma electrónica. Es una excepción comprobada y NO un {@code IncomeException} a propósito: el
 * servicio la atrapa para dejar el error en el trámite y confirmar la transacción; un {@code IncomeException}
 * (rollback = true) que cruzara un EJB marcaría la transacción para deshacer y se perdería el trámite.
 *
 * <p>El mensaje nunca contiene credenciales ni tokens.</p>
 */
public class ProveedorFirmaException extends Exception {

    private static final long serialVersionUID = 1L;

    /** El proveedor no tiene integración operativa todavía (fase 1) o no responde. */
    public static final String PROVEEDOR_NO_DISPONIBLE = "PROVEEDOR_NO_DISPONIBLE";
    /** Falta una propiedad de sistema con la configuración del proveedor. */
    public static final String PROVEEDOR_NO_CONFIGURADO = "PROVEEDOR_NO_CONFIGURADO";

    private final String codigo;

    public ProveedorFirmaException(String codigo, String mensaje) {
        super(codigo + ": " + mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
