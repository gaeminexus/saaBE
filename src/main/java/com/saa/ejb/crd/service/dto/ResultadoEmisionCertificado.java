package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuesta de POST /rest/crtf/emitir. Ver §3.2 de API-CERTIFICADOS-PARTICIPE.md.
 * {@code campos} es lo que efectivamente se imprimió, con el origen FINAL de cada uno.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class ResultadoEmisionCertificado {

    private Long idCertificado;
    private Long numero;
    private Long anio;
    /** "ASOPREP-FCPC-PARTICIPE-099-2026" */
    private String numeroAlterno;
    private LocalDate fechaEmision;
    private Long tipo;
    private String tipoTexto;
    private Long calidad;
    private String calidadTexto;
    private Map<String, CampoCertificado> campos = new LinkedHashMap<>();
    /** "/rest/crtf/pdf/{idCertificado}" */
    private String urlPdf;

    public ResultadoEmisionCertificado() {
    }

    public Long getIdCertificado() { return idCertificado; }
    public void setIdCertificado(Long idCertificado) { this.idCertificado = idCertificado; }

    public Long getNumero() { return numero; }
    public void setNumero(Long numero) { this.numero = numero; }

    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }

    public String getNumeroAlterno() { return numeroAlterno; }
    public void setNumeroAlterno(String numeroAlterno) { this.numeroAlterno = numeroAlterno; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public Long getTipo() { return tipo; }
    public void setTipo(Long tipo) { this.tipo = tipo; }

    public String getTipoTexto() { return tipoTexto; }
    public void setTipoTexto(String tipoTexto) { this.tipoTexto = tipoTexto; }

    public Long getCalidad() { return calidad; }
    public void setCalidad(Long calidad) { this.calidad = calidad; }

    public String getCalidadTexto() { return calidadTexto; }
    public void setCalidadTexto(String calidadTexto) { this.calidadTexto = calidadTexto; }

    public Map<String, CampoCertificado> getCampos() { return campos; }
    public void setCampos(Map<String, CampoCertificado> campos) { this.campos = campos; }

    public String getUrlPdf() { return urlPdf; }
    public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }
}
