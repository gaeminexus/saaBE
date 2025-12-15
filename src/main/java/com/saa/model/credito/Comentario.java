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
 * Representa la tabla CMNT (Comentario).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CMNT", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ComentarioAll", query = "select e from Comentario e"),
    @NamedQuery(name = "ComentarioId", query = "select e from Comentario e where e.codigo = :id")
})
public class Comentario implements Serializable {

    /**
     * Código del comentario.
     */
    @Id
    @Basic
    @Column(name = "CMNTCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Fecha del comentario.
     */
    @Basic
    @Column(name = "CMNTFCHA")
    private LocalDateTime fecha;

    /**
     * Funcionario.
     */
    @Basic
    @Column(name = "CMNTFNCN", length = 250)
    private String funcionario;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "CMNTOBSR", length = 2000)
    private String observacion;

    /**
     * Estado textual.
     */
    @Basic
    @Column(name = "CMNTESTD", length = 100)
    private String estadoTexto;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "CMNTIDST")
    private Long estado;

    /**
     * Entidad (Partícipe).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Préstamo.
     */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(String funcionario) {
        this.funcionario = funcionario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public void setEstadoTexto(String estadoTexto) {
        this.estadoTexto = estadoTexto;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }
}

