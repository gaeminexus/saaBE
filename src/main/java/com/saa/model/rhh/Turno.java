package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

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
    @NamedQuery(name = "Turno.findAll",
                query = "select t from Turno t"),
    @NamedQuery(name = "Turno.findActivos",
                query = "select t from Turno t where t.estado = 'A'")
})
public class Turno implements Serializable {

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
    @Column(name = "TRNONMBR", nullable = false, length = 120)
    private String nombre;

    /**
     * Hora de entrada (HH24:MI).
     */
    @Basic
    @Column(name = "TRNOENTR", nullable = false, length = 5)
    private String horaEntrada;

    /**
     * Hora de salida (HH24:MI).
     */
    @Basic
    @Column(name = "TRNOSLDA", nullable = false, length = 5)
    private String horaSalida;

    /**
     * Minutos de tolerancia.
     */
    @Basic
    @Column(name = "TRNOMNTS", nullable = false)
    private Integer minutosTolerancia;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "TRNOESTD", nullable = false, length = 1)
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TRNOFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "TRNOUSRR", length = 60)
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
