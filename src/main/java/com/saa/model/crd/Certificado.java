package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRTF (Certificado).
 *
 * Registro de cada certificado de partícipe EMITIDO por el módulo de créditos (al día,
 * haber recibido aportes, no adeudar, licitud de fondos, aportes patronales). Guarda el
 * número asignado, a quién, de qué tipo, con qué datos y el PDF tal como salió, para que
 * la reimpresión devuelva el mismo documento bit a bit.
 *
 * <b>Numeración</b>: una sola serie por año compartida por todos los tipos
 * ({@code anio} + {@code numero}, UNIQUE en la base). El número lo asigna
 * {@code CertificadoServiceImpl} como MAX+1 del año bajo lock, en la misma transacción que
 * genera el PDF: si el PDF falla, la transacción se revierte y el número nunca existió.
 * NO hay secuencia de Oracle a propósito (no se reinicia por año y no participa del rollback).
 *
 * <b>Snapshot</b>: {@code datos} es un JSON con todo lo que se imprimió (calidad, valores,
 * banderas, firmante, cargo y qué campos capturó el operador a mano). Es la fuente de la
 * reimpresión y de la auditoría; la base "viva" puede haber cambiado después.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRTF", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CertificadoAll", query = "select e from Certificado e"),
    @NamedQuery(name = "CertificadoId",  query = "select e from Certificado e where e.codigo = :id")
})
public class Certificado implements Serializable {

    /** Código del certificado. */
    @Id
    @Basic
    @Column(name = "CRTFCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Año de la serie (el de la fecha de emisión). */
    @Basic
    @Column(name = "CRTFANIO")
    private Long anio;

    /** Secuencial dentro del año. Único junto con {@code anio}. */
    @Basic
    @Column(name = "CRTFNMRO")
    private Long numero;

    /** Número impreso, ej. "ASOPREP-FCPC-PARTICIPE-099-2026". */
    @Basic
    @Column(name = "CRTFNMAL", length = 60)
    private String numeroAlterno;

    /** Tipo de certificado. Ver {@link com.saa.rubros.CrdTipoCertificado}. */
    @Basic
    @Column(name = "CRTFTPCR")
    private Long tipoCertificado;

    /** FK - Partícipe al que se emite. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Préstamo certificado. Solo en el tipo NO_ADEUDAR_CREDITO; nulo en los demás. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /**
     * Calidad del partícipe efectivamente impresa (código alterno de CRD.ESPR, ver
     * {@link com.saa.rubros.EstadoParticipeEntidad}). La base la propone desde
     * {@code ENTDIDST} y el operador puede cambiarla: por eso se guarda aparte.
     */
    @Basic
    @Column(name = "CRTFCLDD")
    private Long calidad;

    /** Fecha de emisión impresa en el certificado. */
    @Basic
    @Column(name = "CRTFFCEM")
    private LocalDate fechaEmision;

    /** Usuario que emitió. */
    @Basic
    @Column(name = "CRTFUSEM", length = 50)
    private String usuarioEmision;

    /** Snapshot JSON de todo lo impreso. */
    @Lob
    @Basic
    @Column(name = "CRTFDTOS")
    private String datos;

    /**
     * PDF emitido. No viaja en el JSON de la entidad (sería un arreglo de bytes enorme);
     * se descarga por el endpoint de reimpresión.
     */
    @Lob
    @Basic
    @Column(name = "CRTFPDFF")
    private byte[] pdf;

    /** Estado. Ver {@link com.saa.rubros.EstadoCertificado}: 1 EMITIDO, 2 ANULADO. */
    @Basic
    @Column(name = "CRTFESTD")
    private Long estado;

    /** Usuario que anuló. */
    @Basic
    @Column(name = "CRTFUSAN", length = 50)
    private String usuarioAnulacion;

    /** Fecha y hora de la anulación. */
    @Basic
    @Column(name = "CRTFFCAN")
    private LocalDateTime fechaAnulacion;

    /** Motivo de la anulación. */
    @Basic
    @Column(name = "CRTFMTAN", length = 500)
    private String motivoAnulacion;

    /** Fecha y hora de registro en el sistema. */
    @Basic
    @Column(name = "CRTFFCRG")
    private LocalDateTime fechaRegistro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }

    public Long getNumero() { return numero; }
    public void setNumero(Long numero) { this.numero = numero; }

    public String getNumeroAlterno() { return numeroAlterno; }
    public void setNumeroAlterno(String numeroAlterno) { this.numeroAlterno = numeroAlterno; }

    public Long getTipoCertificado() { return tipoCertificado; }
    public void setTipoCertificado(Long tipoCertificado) { this.tipoCertificado = tipoCertificado; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }

    public Long getCalidad() { return calidad; }
    public void setCalidad(Long calidad) { this.calidad = calidad; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getUsuarioEmision() { return usuarioEmision; }
    public void setUsuarioEmision(String usuarioEmision) { this.usuarioEmision = usuarioEmision; }

    public String getDatos() { return datos; }
    public void setDatos(String datos) { this.datos = datos; }

    @JsonIgnore
    public byte[] getPdf() { return pdf; }
    @JsonIgnore
    public void setPdf(byte[] pdf) { this.pdf = pdf; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getUsuarioAnulacion() { return usuarioAnulacion; }
    public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
