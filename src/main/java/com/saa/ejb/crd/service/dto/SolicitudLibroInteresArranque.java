package com.saa.ejb.crd.service.dto;

import java.io.Serializable;

/**
 * Lote 21: cuerpo de {@code POST /rest/prov/semillaDevengoInteres} (A2) y {@code POST /rest/prov/ajusteLibroProvision} (A1). Nada del cuadre va fijo:
 * el corte y el asiento de referencia son parámetros. Contrato: docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md.
 */
public class SolicitudLibroInteresArranque implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    /** Fecha de corte, {@code yyyy-MM-dd}. */
    private String corte;
    /** Asiento manual del cuadre al que queda ligado lo que se graba (no se genera asiento contable). Es la clave de idempotencia. */
    private Long idAsientoReferencia;
    private String usuario;
    /** {@code true} graba; {@code false} (o nulo) solo previsualiza. */
    private Boolean grabar;
    /**
     * Solo A2 (lote 24): {@code SEMILLA} (por defecto) siembra el interés devengado y no cobrado de las cuotas que el libro aún no tiene; {@code DIFERENCIA} graba solo la
     * diferencia por cuota entre el objetivo al corte y el libro (MVIC 11 si falta, 12 si sobra).
     */
    private String modo;

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getCorte() { return corte; }
    public void setCorte(String corte) { this.corte = corte; }
    public Long getIdAsientoReferencia() { return idAsientoReferencia; }
    public void setIdAsientoReferencia(Long idAsientoReferencia) { this.idAsientoReferencia = idAsientoReferencia; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public Boolean getGrabar() { return grabar; }
    public void setGrabar(Boolean grabar) { this.grabar = grabar; }
}
