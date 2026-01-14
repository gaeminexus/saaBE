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
    @NamedQuery(name = "DetalleDetalleTurno.findAll", query = "select d from DetalleDetalleTurno d"),
    @NamedQuery(name = "DetalleDetalleTurno.findById", query = "select d from DetalleDetalleTurno d where d.codigo = :id"),
    @NamedQuery(name = "DetalleDetalleTurno.findByDetalleTurno", query = "select d from DetalleDetalleTurno d where d.detalleTurno.codigo = :detalleTurnoId")
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
    private DetalleTurno detalleTurno;

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
    @Column(name = "DTLLENTR", length = 5)
    private String horaEntrada;

    /**
     * Hora de salida (HH24:MI).
     */
    @Basic
    @Column(name = "DTLLSLDA", length = 5)
    private String horaSalida;

    /**
     * Indica si el día es laborable (S/N).
     */
    @Basic
    @Column(name = "DTLLLBRB", length = 1, nullable = false)
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
    @Column(name = "DTLLUSRR", length = 60)
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

    public DetalleTurno getDetalleTurno() {
        return detalleTurno;
    }

    public void setDetalleTurno(DetalleTurno detalleTurno) {
        this.detalleTurno = detalleTurno;
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
