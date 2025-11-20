package com.saa.model.credito;

import java.io.Serializable;

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
 * Representa la tabla TPHD (TipoHidrocarburifica).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPHD", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TipoHidrocarburificaAll", query = "select e from TipoHidrocarburifica e"),
    @NamedQuery(name = "TipoHidrocarburificaId", query = "select e from TipoHidrocarburifica e where e.codigo = :id")
})
public class TipoHidrocarburifica implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "TPHDCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /** FK - Código Filial */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Código Externo.
     */
    @Basic
    @Column(name = "TPHDCDEX")
    private Long codigoExterno;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "TPHDIDST")
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
     * Devuelve codigoEntidad.
     * @return codigoEntidad.
     */
    public Entidad getCodigoEntidad() {
        return entidad;
    }

    /**
     * Asigna codigoEntidad.
     * @param codigoEntidad nuevo valor para codigoEntidad.
     */
    public void setCodigoEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    /**
     * Devuelve codigoExterno.
     * @return codigoExterno.
     */
    public Long getCodigoExterno() {
        return codigoExterno;
    }

    /**
     * Asigna codigoExterno.
     * @param codigoExterno nuevo valor para codigoExterno.
     */
    public void setCodigoExterno(Long codigoExterno) {
        this.codigoExterno = codigoExterno;
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
