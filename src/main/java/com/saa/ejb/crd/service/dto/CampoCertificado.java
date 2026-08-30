package com.saa.ejb.crd.service.dto;

/**
 * Un valor impreso en un certificado, con su ORIGEN. Ver §3.1 de
 * docs/logica-negocio/crd/API-CERTIFICADOS-PARTICIPE.md.
 *
 * El origen lo calcula SIEMPRE el backend comparando lo que resolvió él mismo contra lo
 * que mandó el operador; el frontend nunca lo manda. Es la trazabilidad de qué se afirmó
 * con respaldo en la base y qué se afirmó por criterio de quien firma.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class CampoCertificado {

    /** El sistema lo resolvió y así se imprime. */
    public static final String ORIGEN_SISTEMA = "SISTEMA";
    /** El sistema no lo tenía; lo capturó el operador. */
    public static final String ORIGEN_MANUAL_REQUERIDO = "MANUAL_REQUERIDO";
    /** El sistema lo tenía y el operador lo cambió. */
    public static final String ORIGEN_MANUAL_EDITADO = "MANUAL_EDITADO";

    /** String, Number, Boolean o null. Las fechas van como "yyyy-MM-dd". */
    private Object valor;

    /** Cómo se imprime ("2005", "27 de octubre de 2020", "$145.728,15"). */
    private String valorTexto;

    /** SISTEMA | MANUAL_REQUERIDO | MANUAL_EDITADO */
    private String origen;

    /** false = el operador no lo puede tocar; el backend pisa lo que venga. */
    private boolean editable;

    /** De dónde salió cuando es SISTEMA ("CRD.CNTR.CNTRFCIN", "CRD.HPCS #204"). */
    private String fuente;

    public CampoCertificado() {
    }

    public CampoCertificado(Object valor, String valorTexto, String origen, boolean editable, String fuente) {
        this.valor = valor;
        this.valorTexto = valorTexto;
        this.origen = origen;
        this.editable = editable;
        this.fuente = fuente;
    }

    /** Campo resuelto por el sistema. */
    public static CampoCertificado sistema(Object valor, String valorTexto, boolean editable, String fuente) {
        return new CampoCertificado(valor, valorTexto, ORIGEN_SISTEMA, editable, fuente);
    }

    /** Campo que el sistema no pudo resolver: lo tiene que capturar el operador. */
    public static CampoCertificado requerido() {
        return new CampoCertificado(null, null, ORIGEN_MANUAL_REQUERIDO, true, null);
    }

    public Object getValor() { return valor; }
    public void setValor(Object valor) { this.valor = valor; }

    public String getValorTexto() { return valorTexto; }
    public void setValorTexto(String valorTexto) { this.valorTexto = valorTexto; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }
}
