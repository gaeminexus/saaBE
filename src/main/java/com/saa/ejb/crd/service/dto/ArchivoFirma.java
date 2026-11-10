package com.saa.ejb.crd.service.dto;

/** PDF de un trámite de firma (original o firmado) listo para descargar. */
public class ArchivoFirma {

    private String nombre;
    private String mimeType;
    private byte[] contenido;

    public ArchivoFirma(String nombre, String mimeType, byte[] contenido) {
        this.nombre = nombre;
        this.mimeType = mimeType;
        this.contenido = contenido;
    }

    public String getNombre() { return nombre; }
    public String getMimeType() { return mimeType; }
    public byte[] getContenido() { return contenido; }
}
