package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de GET /rest/crtf/precarga/{idEntidad}/{tipo}: todo lo que el sistema pudo
 * resolver para el certificado, con el origen de cada campo y los bloqueos.
 * Ver §3.1 de API-CERTIFICADOS-PARTICIPE.md.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class PrecargaCertificado {

    private Long idEntidad;
    private Long tipo;
    private String tipoTexto;
    private String nombre;
    private String cedula;
    /** ESPRCDEX propuesto desde ENTDIDST. */
    private Long calidadSistema;
    private String calidadSistemaTexto;
    /** bloqueos vacío y ningún MANUAL_REQUERIDO sin valor. */
    private boolean puedeEmitir;
    private List<MotivoBloqueoCertificado> bloqueos = new ArrayList<>();
    /** Claves por tipo, ver §4 del contrato. LinkedHashMap para conservar el orden de impresión. */
    private Map<String, CampoCertificado> campos = new LinkedHashMap<>();
    /** Solo tipos 3 y 4. */
    private List<PrestamoCertificado> prestamos = new ArrayList<>();
    /** Solo tipos 2 y 5. */
    private List<LiquidacionCertificado> liquidaciones = new ArrayList<>();

    public PrecargaCertificado() {
    }

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }

    public Long getTipo() { return tipo; }
    public void setTipo(Long tipo) { this.tipo = tipo; }

    public String getTipoTexto() { return tipoTexto; }
    public void setTipoTexto(String tipoTexto) { this.tipoTexto = tipoTexto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public Long getCalidadSistema() { return calidadSistema; }
    public void setCalidadSistema(Long calidadSistema) { this.calidadSistema = calidadSistema; }

    public String getCalidadSistemaTexto() { return calidadSistemaTexto; }
    public void setCalidadSistemaTexto(String calidadSistemaTexto) { this.calidadSistemaTexto = calidadSistemaTexto; }

    public boolean isPuedeEmitir() { return puedeEmitir; }
    public void setPuedeEmitir(boolean puedeEmitir) { this.puedeEmitir = puedeEmitir; }

    public List<MotivoBloqueoCertificado> getBloqueos() { return bloqueos; }
    public void setBloqueos(List<MotivoBloqueoCertificado> bloqueos) { this.bloqueos = bloqueos; }

    public Map<String, CampoCertificado> getCampos() { return campos; }
    public void setCampos(Map<String, CampoCertificado> campos) { this.campos = campos; }

    public List<PrestamoCertificado> getPrestamos() { return prestamos; }
    public void setPrestamos(List<PrestamoCertificado> prestamos) { this.prestamos = prestamos; }

    public List<LiquidacionCertificado> getLiquidaciones() { return liquidaciones; }
    public void setLiquidaciones(List<LiquidacionCertificado> liquidaciones) { this.liquidaciones = liquidaciones; }
}
