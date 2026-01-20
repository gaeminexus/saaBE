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
 * Detalle del detalleTurno por día de la semana.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTLL", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleTurnoId", query = "select e from DetalleTurno e where e.codigo=:id"),
    @NamedQuery(name = "DetalleTurnoAll", query = "select e from DetalleTurno e")
})
public class DetalleTurno implements Serializable {

    /**
     * Código único del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DTLLCDGO")
    private Long codigo;

    /**
     * DetalleTurno al que pertenece.
     */
    @ManyToOne
    @JoinColumn(name = "TRNOCDGO", referencedColumnName = "TRNOCDGO", nullable = false)
    private Turno turno;

    /**
     * Día de la semana (1=Lunes ... 7=Domingo).
     */
    @Basic
    @Column(name = "DTLLDIAA", nullable = false)
    private Integer diaSemana;

    /**
     * Hora de entrada (HH24:MI).
     */
    @Basic
    @Column(name = "DTLLENTR")
    private String horaEntrada;

    /**
     * Hora de salida (HH24:MI).
     */
    @Basic
    @Column(name = "DTLLSLDA")
    private String horaSalida;

    /**
     * Indica si el día es laborable (S/N).
     */
    @Basic
    @Column(name = "DTLLLBRB")
    private String laborable;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DTLLFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "DTLLUSRR")
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

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public Integer getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(Integer diaSemana) {
        this.diaSemana = diaSemana;
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

    public String getLaborable() {
        return laborable;
    }

    public void setLaborable(String laborable) {
        this.laborable = laborable;
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
