package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
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
 * Marcaciones de asistencia (entrada/salida).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MRCC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "MarcacionesId", query = "select e from Marcaciones e where e.codigo=:id"),
    @NamedQuery(name = "MarcacionesAll", query = "select e from Marcaciones e")
})
public class Marcaciones implements Serializable {

    /**
     * Código único de la marcación.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "MRCCCDGO")
    private Long codigo;

    /**
     * Empleado que marcó.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Fecha y hora de la marcación.
     */
    @Basic
    @Column(name = "MRCCFCHH", nullable = false)
    private LocalDateTime fechaHora;

    /**
     * Tipo de marcación (ENTRADA / SALIDA).
     */
    @Basic
    @Column(name = "MRCCTPOO")
    private String tipo;

    /**
     * Origen (RELOJ / WEB / MOVIL / etc.).
     */
    @Basic
    @Column(name = "MRCCORGN")
    private String origen;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "MRCCOBSR")
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "MRCCFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "MRCCUSRR")
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

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
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
