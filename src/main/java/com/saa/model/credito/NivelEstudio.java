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
import jakarta.persistence.Table;

/**
 * Representa la tabla NVLS (Catálogo de niveles de estudio).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NVLS", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "NivelEstudioAll", query = "select e from NivelEstudio e"),
    @NamedQuery(name = "NivelEstudioId", query = "select e from NivelEstudio e where e.codigo = :id")
})
public class NivelEstudio implements Serializable {

    /**
     * Código del nivel de estudio.
     */
    @Id
    @Basic
    @Column(name = "NVLSCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre del nivel de estudio.
     */
    @Basic
    @Column(name = "NVLSNMBR", length = 2000)
    private String nombre;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "NVLSIDST")
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
