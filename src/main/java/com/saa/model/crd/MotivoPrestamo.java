package com.saa.model.crd;

import java.io.Serializable;

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
 * Representa la tabla MTVP (Catálogo de motivos de préstamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MTVP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "MotivoPrestamoAll", query = "select e from MotivoPrestamo e"),
    @NamedQuery(name = "MotivoPrestamoId", query = "select e from MotivoPrestamo e where e.codigo = :id")
})
public class MotivoPrestamo implements Serializable {

    /**
     * Código del motivo de préstamo.
     */
    @Id
    @Basic
    @Column(name = "MTVPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre del motivo de préstamo.
     */
    @Basic
    @Column(name = "MTVPNMBR", length = 2000)
    private String nombre;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "MTVPIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    /**
     * Devuelve codigo.
     * @return codigo.
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo.
     * @param codigo nuevo valor para codigo.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve nombre.
     * @return nombre.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna nombre.
     * @param nombre nuevo valor para nombre.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve estado.
     * @return estado.
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado.
     * @param estado nuevo valor para estado.
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

