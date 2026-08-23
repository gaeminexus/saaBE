package com.saa.ejb.cxp.service.dto;

import java.io.Serializable;

/**
 * Lo que devuelve el servicio de autorización del SRI para una clave de acceso.
 *
 * <p>
 * El campo que importa es {@link #getXmlAutorizacion()}: el sobre
 * {@code <autorizacion>} <b>completo</b>, no el {@code <comprobante>} interno.
 * Es el mismo archivo que el usuario baja del portal del SRI, y es el que
 * {@code registrarFacturaCompra} necesita para leer {@code fechaAutorizacion}
 * con {@code getXmlValueOuter} — si se guardara solo el comprobante interno ese
 * dato se perdería. Ver §8 regla 3 del plan.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@SuppressWarnings("serial")
public class ResultadoAutorizacionSri implements Serializable {

    private String claveAcceso;

    /** Ambiente deducido del dígito 24 de la clave: 1=pruebas 2=producción. */
    private Long ambiente;

    /** Cuántos comprobantes devolvió el SRI. 0 = no lo tiene (o está fuera de ventana). */
    private int numeroComprobantes;

    /** AUTORIZADO / NO AUTORIZADO / EN PROCESO / null si no vino el nodo. */
    private String estado;

    private String numeroAutorizacion;

    /** Tal como la devuelve el SRI, sin parsear. */
    private String fechaAutorizacion;

    /** El sobre {@code <autorizacion>} entero, listo para escribir a disco. */
    private String xmlAutorizacion;

    /** identificador + mensaje + informacionAdicional concatenados, para diagnóstico. */
    private String mensajes;

    /** La respuesta SOAP cruda. No se persiste; sirve para el log cuando algo no cuadra. */
    private String respuestaCompleta;

    /** @return true si el SRI dice AUTORIZADO y vino el sobre XML. */
    public boolean isAutorizado() {
        return "AUTORIZADO".equalsIgnoreCase(estado)
                && xmlAutorizacion != null && !xmlAutorizacion.isEmpty();
    }

    /** @return true si el SRI no tiene el comprobante. */
    public boolean isNoEncontrado() {
        return numeroComprobantes <= 0 && (xmlAutorizacion == null || xmlAutorizacion.isEmpty());
    }

    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }
    public Long getAmbiente() { return ambiente; }
    public void setAmbiente(Long ambiente) { this.ambiente = ambiente; }
    public int getNumeroComprobantes() { return numeroComprobantes; }
    public void setNumeroComprobantes(int numeroComprobantes) { this.numeroComprobantes = numeroComprobantes; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNumeroAutorizacion() { return numeroAutorizacion; }
    public void setNumeroAutorizacion(String numeroAutorizacion) { this.numeroAutorizacion = numeroAutorizacion; }
    public String getFechaAutorizacion() { return fechaAutorizacion; }
    public void setFechaAutorizacion(String fechaAutorizacion) { this.fechaAutorizacion = fechaAutorizacion; }
    public String getXmlAutorizacion() { return xmlAutorizacion; }
    public void setXmlAutorizacion(String xmlAutorizacion) { this.xmlAutorizacion = xmlAutorizacion; }
    public String getMensajes() { return mensajes; }
    public void setMensajes(String mensajes) { this.mensajes = mensajes; }
    public String getRespuestaCompleta() { return respuestaCompleta; }
    public void setRespuestaCompleta(String respuestaCompleta) { this.respuestaCompleta = respuestaCompleta; }
}
