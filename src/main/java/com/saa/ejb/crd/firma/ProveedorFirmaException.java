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

    /** Security Data pide más datos del firmante (STY-0002): el mensaje trae los campos y adjuntos que faltan. */
    public static final String FIRMANTE_REQUIERE_DATOS = "FIRMANTE_REQUIERE_DATOS";
    /** Security Data rechazó al firmante (STY-0003, STY-0013, STY-0014): el mensaje trae su errorReason literal. */
    public static final String FIRMANTE_RECHAZADO = "FIRMANTE_RECHAZADO";
    /** El proveedor respondió algo que no se puede interpretar. */
    public static final String RESPUESTA_INVALIDA = "RESPUESTA_PROVEEDOR_INVALIDA";
    /** El proveedor respondió con un error (HTTP o status=false); el mensaje trae el suyo. */
    public static final String ERROR_PROVEEDOR = "ERROR_PROVEEDOR";

    private final String codigo;

    public ProveedorFirmaException(String codigo, String mensaje) {
        super(codigo + ": " + mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
