/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tsr;

import java.io.Serializable;

import com.saa.model.cnt.DetallePlantilla;

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
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.CMTV.
 *  Entity Motivo de cobro.
 *  Almacena el motivo de caja cobro.
 *  Es una descripción y desglose de valores del cobro.
 *  Detalle de la entidad cobro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CMTV", schema = "TSR")
@SequenceGenerator(name = "SQ_CMTVCDGO", sequenceName = "TSR.SQ_CMTVCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MotivoCobroAll", query = "select e from MotivoCobro e"),
	@NamedQuery(name = "MotivoCobroId", query = "select e from MotivoCobro e where e.codigo = :id")
})
public class MotivoCobro implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "CMTVCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CMTVCDGO")
    private Long codigo;
    
    /**
     * Cobro al que pertence
     */
    @ManyToOne
    @JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
    private Cobro cobro;    

    /**
     * Descripcion.
     */
    @Basic
    @Column(name = "CMTVDSCR", length = 200)
    private String descripcion;
    
    /**
     * Valor.
     */
    @Basic
    @Column(name = "CMTVVLRR")
    private Double valor;
    
    /**
     * Detalle plantilla para el motivo de pago.
     */
    @ManyToOne
    @JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
    private DetallePlantilla detallePlantilla;
    
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
     * Devuelve Cobro
     */
    public Cobro getCobro() {
        return this.cobro;
    }
    
    /**
     * Asigna Cobro
     */
    public void setCobro(Cobro cobro) {
        this.cobro = cobro;
    }

    /**
     * Devuelve descripcion
     * @return descripcion
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Asigna descripcion
     * @param descripcion Nuevo valor para descripcion 
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    /**
     * Devuelve valor
     * @return valor
     */
    public Double getValor() {
        return valor;
    }

    /**
     * Asigna valor
     * @param valor Nuevo valor para valor 
     */
    public void setValor(Double valor) {
        this.valor = valor;
    }
    
    /**
     * Devuelve detallePlantilla
     * @return detallePlantilla
     */
    public DetallePlantilla getDetallePlantilla() {
        return detallePlantilla;
    }

    /**
     * Asigna detallePlantilla
     * @param detallePlantilla Nuevo valor para detallePlantilla 
     */
    public void setDetallePlantilla(DetallePlantilla detallePlantilla) {
        this.detallePlantilla = detallePlantilla;
    }
}
