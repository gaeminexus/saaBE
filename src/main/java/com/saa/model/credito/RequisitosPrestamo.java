package com.saa.model.credito;

import java.io.Serializable;
import java.sql.Timestamp;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla RQPR (Requisitos del préstamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RQPR", schema = "CRD")
@SequenceGenerator(name = "SQ_RQPRCDGO", sequenceName = "CRD.SQ_RQPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "RequisitoPrestamoAll", query = "select e from RequisitosPrestamo e"),
    @NamedQuery(name = "RequisitoPrestamoId", query = "select e from RequisitosPrestamo e where e.codigo = :id")
})
public class RequisitosPrestamo implements Serializable {

    /**
     * Código del requisito.
     */
    @Id
    @Basic
    @Column(name = "RQPRCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RQPRCDGO")
    private Long codigo;

    /**
     * Código del préstamo.
     */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /**
     * Código de tipo requisito.
     */
    @ManyToOne
    @JoinColumn(name = "TPRQCDGO", referencedColumnName = "TPRQCDGO")
    private TipoRequisitoPrestamo tipoRequisitoPrestamo;

    /**
     * Indica si está validado.
     */
    @Basic
    @Column(name = "RQPRVLDO")
    private Long validado;

    /**
     * Indica si requiere alerta.
     */
    @Basic
    @Column(name = "RQPRALRT")
    private Long alerta;

    /**
     * Descripción.
     */
    @Basic
    @Column(name = "RQPRDSCR", length = 2000)
    private String descripcion;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "RQPROBSR", length = 2000)
    private String observacion;

    /**
     * Usuario registro.
     */
    @Basic
    @Column(name = "RQPRUSRG", length = 200)
    private String usuarioRegistro;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "RQPRFCRG")
    private Timestamp fechaRegistro;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "RQPRESTD")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public TipoRequisitoPrestamo getTipoRequisito() {
        return tipoRequisitoPrestamo;
    }

    public void setTipoRequisito(TipoRequisitoPrestamo tipoRequisito) {
        this.tipoRequisitoPrestamo = tipoRequisito;
    }

    public Long getValidado() {
        return validado;
    }

    public void setValidado(Long validado) {
        this.validado = validado;
    }

    public Long getAlerta() {
        return alerta;
    }

    public void setAlerta(Long alerta) {
        this.alerta = alerta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
