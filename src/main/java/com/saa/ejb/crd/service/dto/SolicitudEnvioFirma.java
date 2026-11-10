package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo JSON de «enviar a firmar» y «reintentar»: {@code {"codigoDactilar":"XXXXXX"}}. Va en el cuerpo y no en la URL
 * para que no quede en los logs de acceso. El código dactilar no se guarda en ninguna tabla ni se escribe en un log, y por
 * eso su toString no lo incluye.
 */
public class SolicitudEnvioFirma {

    private String codigoDactilar;

    public String getCodigoDactilar() {
        return codigoDactilar;
    }

    public void setCodigoDactilar(String codigoDactilar) {
        this.codigoDactilar = codigoDactilar;
    }

    @Override
    public String toString() {
        return "SolicitudEnvioFirma[codigoDactilar=***]";
    }
}
