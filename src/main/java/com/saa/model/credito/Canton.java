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
 * Representa la tabla CNTN (Cantones).
 * Catálogo de cantones prueba.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNTN", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CantonAll", query = "select e from Canton e"),
    @NamedQuery(name = "CantonId", query = "select e from Canton e where e.codigo = :id")
})
public class Canton implements Serializable {
    
    /**
     * Código del cantón.  
     */
    @Id
    @Basic
    @Column(name = "CNTNCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Nombre del cantón.
     */
    @Basic
    @Column(name = "CNTNNMBR", length = 2000)
    private String nombre;
    
    /**
     * Código auxiliar.
     */
    @Basic
    @Column(name = "CNTNCDGX", length = 50)
    private String codigoAuxiliar;
    
    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "CNTNIDST", nullable = false)
    private Long idEstado;
    
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
     * Devuelve codigoAuxiliar.
     * @return codigoAuxiliar.
     */
    public String getCodigoAuxiliar() {
        return codigoAuxiliar;
    }
    
    /**
     * Asigna codigoAuxiliar.
     * @param codigoAuxiliar nuevo valor para codigoAuxiliar.
     */
    public void setCodigoAuxiliar(String codigoAuxiliar) {
        this.codigoAuxiliar = codigoAuxiliar;
    }
    
    /**
     * Devuelve idEstado.
     * @return idEstado.
     */
    public Long getIdEstado() {
        return idEstado;
    }
    
    /**
     * Asigna idEstado.
     * @param idEstado nuevo valor para idEstado.
     */
    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }
}

