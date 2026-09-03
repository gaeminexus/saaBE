package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Representa CRD.ESCR (EscalaCalificacionRiesgo) — DDL en {@code sql/177}.
 * Un renglón por calificación (A1..E) de una {@link ConfiguracionCalificacionRiesgo}: rango de
 * días y porcentaje de provisión. Ver PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ESCR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "EscalaCalificacionRiesgoAll", query = "select e from EscalaCalificacionRiesgo e"),
    @NamedQuery(name = "EscalaCalificacionRiesgoId",  query = "select e from EscalaCalificacionRiesgo e where e.codigo = :id")
})
public class EscalaCalificacionRiesgo implements Serializable {

    @Id
    @Basic
    @Column(name = "ESCRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne
    @JoinColumn(name = "CFCRCDGO", referencedColumnName = "CFCRCDGO")
    private ConfiguracionCalificacionRiesgo configuracion;

    /** A1, A2, A3, B1, B2, C1, C2, D, E. */
    @Basic
    @Column(name = "ESCRCLFC", length = 5)
    private String calificacion;

    /** Día desde, inclusive. */
    @Basic
    @Column(name = "ESCRDSDE")
    private Long diaDesde;

    /** Día hasta, inclusive. {@code null} = sin tope (la última calificación de la escala). */
    @Basic
    @Column(name = "ESCRHSTA")
    private Long diaHasta;

    /** Porcentaje de provisión en TANTO POR UNO (0.0099 = 0,99%). */
    @Basic
    @Column(name = "ESCRPRVS")
    private Double porcentajeProvision;

    @Basic
    @Column(name = "ESCRORDN")
    private Long orden;

    @Basic
    @Column(name = "ESCRFCRG")
    private LocalDateTime fechaRegistro;

    @Basic
    @Column(name = "ESCRUSAR", length = 50)
    private String usuarioRegistro;

    @Basic
    @Column(name = "ESCRESTD")
    private Long estado;

    public EscalaCalificacionRiesgo() {
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public ConfiguracionCalificacionRiesgo getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionCalificacionRiesgo configuracion) {
        this.configuracion = configuracion;
    }

    public String getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(String calificacion) {
        this.calificacion = calificacion;
    }

    public Long getDiaDesde() {
        return diaDesde;
    }

    public void setDiaDesde(Long diaDesde) {
        this.diaDesde = diaDesde;
    }

    public Long getDiaHasta() {
        return diaHasta;
    }

    public void setDiaHasta(Long diaHasta) {
        this.diaHasta = diaHasta;
    }

    public Double getPorcentajeProvision() {
        return porcentajeProvision;
    }

    public void setPorcentajeProvision(Double porcentajeProvision) {
        this.porcentajeProvision = porcentajeProvision;
    }

    public Long getOrden() {
        return orden;
    }

    public void setOrden(Long orden) {
        this.orden = orden;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
