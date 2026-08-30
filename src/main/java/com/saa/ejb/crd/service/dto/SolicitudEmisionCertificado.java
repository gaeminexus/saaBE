package com.saa.ejb.crd.service.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Body de POST /rest/crtf/emitir. Ver §3.2 de API-CERTIFICADOS-PARTICIPE.md.
 *
 * {@code campos} trae SOLO el valor por clave (no el CampoCertificado entero): el origen lo
 * recalcula el backend, no confía en el que pudiera mandar el frontend.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudEmisionCertificado {

    private Long idEntidad;
    private Long tipo;
    /** Obligatorio en el tipo 3; ignorado en los demás. */
    private Long idPrestamo;
    /** Opcional en 2 y 5: la fila de HPCS que se usó en la precarga. */
    private Long idLiquidacion;
    /** ESPRCDEX que se va a imprimir (el propuesto o el corregido). */
    private Long calidad;
    /** Valor por clave. Fechas como "yyyy-MM-dd". */
    private Map<String, Object> campos = new LinkedHashMap<>();
    private String usuario;

    public SolicitudEmisionCertificado() {
    }

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }

    public Long getTipo() { return tipo; }
    public void setTipo(Long tipo) { this.tipo = tipo; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Long getIdLiquidacion() { return idLiquidacion; }
    public void setIdLiquidacion(Long idLiquidacion) { this.idLiquidacion = idLiquidacion; }

    public Long getCalidad() { return calidad; }
    public void setCalidad(Long calidad) { this.calidad = calidad; }

    public Map<String, Object> getCampos() { return campos; }
    public void setCampos(Map<String, Object> campos) { this.campos = campos; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
