package com.saa.model.credito;

import java.io.Serializable;

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
 * Representa la tabla TPRQ (TipoRequisitoPrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPRQ", schema = "CRD")
@SequenceGenerator(name = "SQ_TPRQCDGO", sequenceName = "CRD.SQ_TPRQCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TipoRequisitoPrestamoAll", query = "select e from TipoRequisitoPrestamo e"),
    @NamedQuery(name = "TipoRequisitoPrestamoId", query = "select e from TipoRequisitoPrestamo e where e.codigo = :id")
})
public class TipoRequisitoPrestamo implements Serializable {

    /**
     * Código del requisito.
     */
    @Id
    @Basic
    @Column(name = "TPRQCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TPRQCDGO")
    private Long codigo;

    /**
     * Nombre del requisito.
     */
    @Basic
    @Column(name = "TPRQNMBR", length = 2000)
    private String nombre;

    /**
     * Valor del requisito.
     */
    @Basic
    @Column(name = "TPRQVLRR")
    private Long valor;

    /**
     * Estado del requisito.
     */
    @Basic
    @Column(name = "TPRQESTD")
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getValor() {
        return valor;
    }

    public void setValor(Long valor) {
        this.valor = valor;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
