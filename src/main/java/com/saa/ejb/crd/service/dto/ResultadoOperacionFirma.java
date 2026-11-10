package com.saa.ejb.crd.service.dto;

/**
 * Resultado de una operación sobre un trámite de firma que ya tocó la base. Si el PROVEEDOR falla, el trámite queda
 * guardado con el error y la operación NO lanza (lanzar deshacería la transacción): {@code exito = false} con el código y
 * el mensaje, y el trámite tal como quedó. Los errores de validación sí se lanzan antes de tocar nada.
 */
public class ResultadoOperacionFirma {

    private boolean exito;
    /** Código del error del proveedor (PROVEEDOR_NO_DISPONIBLE, PROVEEDOR_NO_CONFIGURADO); null si hubo éxito. */
    private String codigoError;
    private String mensaje;
    private TramiteFirma tramite;

    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }
    public String getCodigoError() { return codigoError; }
    public void setCodigoError(String codigoError) { this.codigoError = codigoError; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public TramiteFirma getTramite() { return tramite; }
    public void setTramite(TramiteFirma tramite) { this.tramite = tramite; }
}
