package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.APPR (AprobacionPrestamo); DDL en {@code crd/sql/370}.
 * Es la aprobación (o negación) de un préstamo en un nivel; bitácora nivel por nivel. Vigente = estado 1; invalidada = 0.
 * Las llaves foráneas van como código simple (no {@code @ManyToOne}): la entidad se serializa directo a JSON y los EAGER
 * arrastrarían el grafo completo. Diseño: DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B4.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "APPR", schema = "CRD")
@SequenceGenerator(name = "SQ_APPRCDGO", sequenceName = "CRD.SQ_APPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AprobacionPrestamoAll", query = "select e from AprobacionPrestamo e"),
    @NamedQuery(name = "AprobacionPrestamoId",  query = "select e from AprobacionPrestamo e where e.codigo = :id")
})
public class AprobacionPrestamo implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "APPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_APPRCDGO")
    private Long codigo;

    /** Préstamo (CRD.PRST). */
    @Basic
    @Column(name = "PRSTCDGO")
    private Long idPrestamo;

    /** Regla con la que se aprobó (no se re-resuelve). */
    @Basic
    @Column(name = "RGAPCDGO")
    private Long idRegla;

    /** Nivel aprobado (CRD.NVAP). */
    @Basic
    @Column(name = "NVAPCDGO")
    private Long idNivel;

    /** Orden del nivel aprobado (copia de NVAPORDN). */
    @Basic
    @Column(name = "APPRORDN")
    private Long orden;

    /** Nombre del nivel al aprobar (copia). */
    @Basic
    @Column(name = "APPRNMNV")
    private String nombreNivel;

    /** Usuario que decidió (SCP.PJRQ.PJRQCDGO). */
    @Basic
    @Column(name = "APPRPJRQ")
    private Long idUsuario;

    /** Nombre del usuario que decidió (SCP.PJRQ.PJRQNMBR). */
    @Basic
    @Column(name = "APPRUSRO")
    private String usuario;

    /** 1 aprobado, 2 negado. */
    @Basic
    @Column(name = "APPRDCSN")
    private Long decision;

    /** Fecha de la decisión. */
    @Basic
    @Column(name = "APPRFCHA")
    private LocalDateTime fecha;

    /** Observación. */
    @Basic
    @Column(name = "APPROBSR")
    private String observacion;

    /** 1 vigente, 0 invalidada. */
    @Basic
    @Column(name = "APPRESTD")
    private Long estado;

    /** Quién la invalidó. */
    @Basic
    @Column(name = "APPRUSMD")
    private String usuarioModificacion;

    /** Cuándo se invalidó. */
    @Basic
    @Column(name = "APPRFCMD")
    private LocalDateTime fechaModificacion;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getIdRegla() { return idRegla; }
    public void setIdRegla(Long idRegla) { this.idRegla = idRegla; }
    public Long getIdNivel() { return idNivel; }
    public void setIdNivel(Long idNivel) { this.idNivel = idNivel; }
    public Long getOrden() { return orden; }
    public void setOrden(Long orden) { this.orden = orden; }
    public String getNombreNivel() { return nombreNivel; }
    public void setNombreNivel(String nombreNivel) { this.nombreNivel = nombreNivel; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Long getDecision() { return decision; }
    public void setDecision(Long decision) { this.decision = decision; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }
    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
