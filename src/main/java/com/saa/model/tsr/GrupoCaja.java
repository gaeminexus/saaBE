package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.CJIN. 
 *  Entity GrupoCaja. 
 *  Grupo de cajas lógicas.
 *  Permite la clasificación de las cajas lógicas.
 *  Es maestro de la entidad cajaLogica.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CJIN", schema = "TSR")
@SequenceGenerator(name = "SQ_CJINCDGO", sequenceName = "TSR.SQ_CJINCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "GrupoCajaAll", query = "select e from GrupoCaja e"),
	@NamedQuery(name = "GrupoCajaId", query = "select e from GrupoCaja e where e.codigo = :id")
})
public class GrupoCaja implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "CJINCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CJINCDGO")
    private Long codigo;
    
    /**
     * Nombre.
     */
    @Basic
    @Column(name = "CJINNMBR", length = 500)
    private String nombre;
    
    /**
     * Empresa a la que pertenece el grupo de cajas
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;    

    /**
     * Fecha de ingreso al sistema.
     */
    @Basic
    @Column(name = "CJINFCIN")
    private LocalDateTime fechaIngreso;
    
    /**
     * Fecha de desactivacion.
     */
    @Basic
    @Column(name = "CJINFCDS")
    private LocalDateTime fechaInactivo;
    
    /**
     * Attribute estado.
     */
    @Basic
    @Column(name = "CJINESTD")
    private Long estado;
    
    /**
     * Devuelve codigo
     * @return codigo
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo
     * @param codigo Nuevo valor para codigo 
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    /**
     * Devuelve nombre
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna nombre
     * @param nombre Nuevo valor para nombre 
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    /**
     * Devuelve empresa
     */
    public Empresa getEmpresa() {
        return this.empresa;
    }
    
    /**
     * Asigna empresa
     */
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    /**
     * Devuelve fechaIngreso
     * @return fechaIngreso
     */
    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    /**
     * Asigna fechaIngreso
     * @param fechaIngreso Nuevo valor para fechaIngreso 
     */
    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
    
    /**
     * Devuelve fechaInactivo
     * @return fechaInactivo
     */
    public LocalDateTime getFechaInactivo() {
        return fechaInactivo;
    }

    /**
     * Asigna fechaInactivo
     * @param fechaInactivo Nuevo valor para fechaInactivo 
     */
    public void setFechaInactivo(LocalDateTime fechaInactivo) {
        this.fechaInactivo = fechaInactivo;
    }
    
    /**
     * Devuelve estado
     * @return estado
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado
     * @param estado Nuevo valor para estado 
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
}
