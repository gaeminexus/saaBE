package com.saa.model.credito;

import java.io.Serializable;
import java.math.BigDecimal;

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
 * Representa la tabla CRDT (Créditos del sistema).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRDT", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CreditoMontoAprobacionAll", query = "select e from CreditoMontoAprobacion e"),
    @NamedQuery(name = "CreditoMontoAprobacionId", query = "select e from CreditoMontoAprobacion e where e.codigo = :id")
})
public class CreditoMontoAprobacion implements Serializable {
    
    /**
     * Código del crédito.
     */
    @Id
    @Basic
    @Column(name = "CRDTCDGO", precision = 10, scale = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Estado.
     */
    @Basic
    @Column(name = "CRDTIDST", precision = 1, scale = 0)
    private Long estado;
    
    /**
     * ID Proceso.
     */
    @Basic
    @Column(name = "CRDTIDPR")
    private BigDecimal idProceso;
    
    /**
     * Monto Máximo.
     */
    @Basic
    @Column(name = "CRDTMNMX")
    private BigDecimal montoMaximo;
    
    /**
     * Monto Mínimo.
     */
    @Basic
    @Column(name = "CRDTMNMN")
    private BigDecimal montoMinimo;
    
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
    
    /**
     * Devuelve idProceso.
     * @return idProceso.
     */
    public BigDecimal getIdProceso() {
        return idProceso;
    }
    
    /**
     * Asigna idProceso.
     * @param idProceso nuevo valor para idProceso.
     */
    public void setIdProceso(BigDecimal idProceso) {
        this.idProceso = idProceso;
    }
    
    /**
     * Devuelve montoMaximo.
     * @return montoMaximo.
     */
    public BigDecimal getMontoMaximo() {
        return montoMaximo;
    }
    
    /**
     * Asigna montoMaximo.
     * @param montoMaximo nuevo valor para montoMaximo.
     */
    public void setMontoMaximo(BigDecimal montoMaximo) {
        this.montoMaximo = montoMaximo;
    }
    
    /**
     * Devuelve montoMinimo.
     * @return montoMinimo.
     */
    public BigDecimal getMontoMinimo() {
        return montoMinimo;
    }
    
    /**
     * Asigna montoMinimo.
     * @param montoMinimo nuevo valor para montoMinimo.
     */
    public void setMontoMinimo(BigDecimal montoMinimo) {
        this.montoMinimo = montoMinimo;
    }
}