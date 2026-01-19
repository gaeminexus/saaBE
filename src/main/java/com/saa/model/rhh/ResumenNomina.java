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
import jakarta.persistence.UniqueConstraint;

/**
 * Resumen diario de asistencia por empleado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RSMN",schema = "RHH",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "MPLDCDGO", "RSMNFCHA" })
    }
)
@NamedQueries({
    @NamedQuery(name = "ResumenNomina.findAll",
                query = "select r from ResumenNomina r"),
    @NamedQuery(name = "ResumenNomina.findById",
                query = "select r from ResumenNomina r where r.codigo = :id"),
    @NamedQuery(name = "ResumenNomina.findByEmpleado",
                query = "select r from ResumenNomina r where r.empleado.codigo = :idEmpleado"),
    @NamedQuery(name = "ResumenNomina.findByFecha",
                query = "select r from ResumenNomina r where r.fecha = :fecha")
})
public class ResumenNomina implements Serializable {

    /**
     * Código único del resumen.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RSMNCDGO")
    private Long codigo;

    /**
     * Empleado.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Fecha del resumen.
     */
    @Basic
    @Column(name = "RSMNFCHA", nullable = false)
    private LocalDate fecha;

    /**
     * Hora de entrada (HH24:MI).
     */
    @Basic
    @Column(name = "RSMNENTR")
    private String horaEntrada;

    /**
     * Hora de salida (HH24:MI).
     */
    @Basic
    @Column(name = "RSMNSLDA")
    private String horaSalida;

    /**
     * Minutos de tardanza.
     */
    @Basic
    @Column(name = "RSMNTRDE")
    private Integer minutosTarde;

    /**
     * Minutos extra.
     */
    @Basic
    @Column(name = "RSMNEXTR")
    private Integer minutosExtra;

    /**
     * Indica ausencia (S/N).
     */
    @Basic
    @Column(name = "RSMNASNT")
    private String ausencia;

    /**
     * Indica justificación (S/N).
     */
    @Basic
    @Column(name = "RSMNJSTF")
    private String justificado;

    /**
     * Fuente del registro (CALCULO / AJUSTE).
     */
    @Basic
    @Column(name = "RSMNFNTE")
    private String fuente;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RSMNFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "RSMNUSRR")
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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(String horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public String getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(String horaSalida) {
        this.horaSalida = horaSalida;
    }

    public Integer getMinutosTarde() {
        return minutosTarde;
    }

    public void setMinutosTarde(Integer minutosTarde) {
        this.minutosTarde = minutosTarde;
    }

    public Integer getMinutosExtra() {
        return minutosExtra;
    }

    public void setMinutosExtra(Integer minutosExtra) {
        this.minutosExtra = minutosExtra;
    }

    public String getAusencia() {
        return ausencia;
    }

    public void setAusencia(String ausencia) {
        this.ausencia = ausencia;
    }

    public String getJustificado() {
        return justificado;
    }

    public void setJustificado(String justificado) {
        this.justificado = justificado;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
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
