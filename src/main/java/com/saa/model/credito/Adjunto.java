package com.saa.model.credito;

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
 * Representa la tabla ADJN (Adjunto).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ADJN", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "AdjuntoAll", query = "select e from Adjunto e"),
    @NamedQuery(name = "AdjuntoId", query = "select e from Adjunto e where e.codigo = :id")
})
public class Adjunto implements Serializable {

    /**
     * Código del adjunto.
     */
    @Id
    @Basic
    @Column(name = "ADJNCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Entidad (Partícipe).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * ID referencia bancaria.
     */
    @Basic
    @Column(name = "ADJNIDRF")
    private Long idReferencia;

    /**
     * Préstamo.
     */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /**
     * ID solicitud cambio aporte.
     */
    @Basic
    @Column(name = "ADJNISCA")
    private Long idSolicitudCambio;

    /**
     * Tipo de adjunto.
     */
    @ManyToOne
    @JoinColumn(name = "TPDJCDGO", referencedColumnName = "TPDJCDGO")
    private TipoAdjunto tipoAdjunto;

    /**
     * Nombre del archivo.
     */
    @Basic
    @Column(name = "ADJNNMAR", length = 2000)
    private String nombreArchivo;

    /**
     * URL del archivo.
     */
    @Basic
    @Column(name = "ADJNURLA", length = 2000)
    private String urlArchivo;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "ADJNOBSR", length = 2000)
    private String observacion;

    /**
     * Mime type.
     */
    @Basic
    @Column(name = "ADJNMMTY", length = 200)
    private String mimeType;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "ADJNIDST")
    private Long estado;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "ADJNFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario registro.
     */
    @Basic
    @Column(name = "ADJNUSRG", length = 200)
    private String usuarioRegistro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Long getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(Long idReferencia) {
        this.idReferencia = idReferencia;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Long getIdSolicitudCambio() {
        return idSolicitudCambio;
    }

    public void setIdSolicitudCambio(Long idSolicitudCambio) {
        this.idSolicitudCambio = idSolicitudCambio;
    }

    public TipoAdjunto getTipoAdjunto() {
        return tipoAdjunto;
    }

    public void setTipoAdjunto(TipoAdjunto tipoAdjunto) {
        this.tipoAdjunto = tipoAdjunto;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
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
}

