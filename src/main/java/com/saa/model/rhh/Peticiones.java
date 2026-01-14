package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Solicitudes de permisos/licencias.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTCN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "Peticiones.findAll",
                query = "select p from Peticiones p"),
    @NamedQuery(name = "Peticiones.findById",
                query = "select p from Peticiones p where p.codigo = :id"),
    @NamedQuery(name = "Peticiones.findByEmpleado",
                query = "select p from Peticiones p where p.empleado.codigo = :empleadoId"),
    @NamedQuery(name = "Peticiones.findByEstado",
                query = "select p from Peticiones p where p.estado = :estado")
})
public class Peticiones implements Serializable {

    /**
     * Código único de la solicitud.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PTCNCDGO")
    private Long codigo;

    /**
     * Empleado solicitante.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Tipo de permiso/licencia.
     */
    @ManyToOne
    @JoinColumn(name = "CTLGCDGO", referencedColumnName = "CTLGCDGO", nullable = false)
    private Catalogo catalogo;

    /**
     * Fecha desde.
     */
    @Basic
    @Column(name = "PTCNFCHD", nullable = false)
    private LocalDate fechaDesde;

    /**
     * Fecha hasta.
     */
    @Basic
    @Column(name = "PTCNFCHH", nullable = false)
    private LocalDate fechaHasta;

    /**
     * Horas solicitadas (si aplica).
     */
    @Basic
    @Column(name = "PTCNHRAS", precision = 6, scale = 2)
    private Double horas;

    /**
     * Motivo del permiso.
     */
    @Basic
    @Column(name = "PTCNMTVO", length = 500)
    private String motivo;

    /**
     * Documento de respaldo (ruta o referencia).
     */
    @Basic
    @Column(name = "PTCNDOCC", length = 400)
    private String documento;

    /**
     * Estado de la solicitud (SOLICITADO / APROBADO / RECHAZADO / ANULADO).
     */
    @Basic
    @Column(name = "PTCNESTD", length = 12, nullable = false)
    private String estado;

    /**
     * Usuario que aprueba o rechaza.
     */
    @Basic
    @Column(name = "PTCNAPRB", length = 60)
    private String usuarioAprobador;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "PTCNOBSR", length = 500)
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PTCNFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "PTCNUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Catalogo getCatalogo() {
        return catalogo;
    }

    public void setCatalogo(Catalogo catalogo) {
        this.catalogo = catalogo;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Double getHoras() {
        return horas;
    }

    public void setHoras(Double horas) {
        this.horas = horas;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUsuarioAprobador() {
        return usuarioAprobador;
    }

    public void setUsuarioAprobador(String usuarioAprobador) {
        this.usuarioAprobador = usuarioAprobador;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
