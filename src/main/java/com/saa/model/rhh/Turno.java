package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Catálogo de turnos de trabajo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TRNO", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "TurnoId", query = "select e from Turno e where e.codigo=:id"),
    @NamedQuery(name = "TurnoAll", query = "select e from Turno e")
})
public class Turno implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del turno.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "TRNOCDGO")
    private Long codigo;

    /**
     * Nombre del turno.
     */
    @Basic
    @Column(name = "TRNONMBR")
    private String nombre;

    /**
     * Hora de entrada (HH24:MI).
     */
    @Basic
    @Column(name = "TRNOENTR")
    private String horaEntrada;

    /**
     * Hora de salida (HH24:MI).
     */
    @Basic
    @Column(name = "TRNOSLDA")
    private String horaSalida;

    /**
     * Minutos de tolerancia.
     */
    @Basic
    @Column(name = "TRNOMNTS")
    private Integer minutosTolerancia;

    /**
     * Minutos de descanso no remunerado de la jornada -- el almuerzo. Nulo = sin descanso.
     *
     * <p>Se resta del intervalo entre la hora de entrada y la de salida para obtener la jornada
     * <b>efectiva</b>. Sin esta columna, un turno de 08:30 a 17:30 con una hora de almuerzo
     * declaraba nueve horas de jornada teorica cuando el trabajador solo puede acumular ocho, y
     * las horas suplementarias no empezaban a contar hasta las 18:30 en vez de las 17:30.</p>
     */
    @Basic
    @Column(name = "TRNOMNDS")
    private Integer minutosDescanso;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "TRNOESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TRNOFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "TRNOUSRR")
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public Integer getMinutosTolerancia() {
        return minutosTolerancia;
    }

    public void setMinutosTolerancia(Integer minutosTolerancia) {
        this.minutosTolerancia = minutosTolerancia;
    }

    public Integer getMinutosDescanso() {
        return minutosDescanso;
    }

    public void setMinutosDescanso(Integer minutosDescanso) {
        this.minutosDescanso = minutosDescanso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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
