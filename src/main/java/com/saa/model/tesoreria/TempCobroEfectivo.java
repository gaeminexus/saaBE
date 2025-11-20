/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tesoreria;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.TCEF.
 *  Entity Temporal de cobro con efectivo.
 *  Almacena el cobro con efectivo de manera temporal.
 *  Luego pasara a la tabla TSR.CEFC.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCEF", schema = "TSR")
@SequenceGenerator(name = "SQ_TCEFCDGO", sequenceName = "TSR.SQ_TCEFCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCobroEfectivoAll", query = "select e from TempCobroEfectivo e"),
	@NamedQuery(name = "TempCobroEfectivoId", query = "select e from TempCobroEfectivo e where e.codigo = :id")
})
public class TempCobroEfectivo implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "TCEFCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCEFCDGO")
    private Long codigo;
    
    /**
     * Cobro temporal al que pertenece
     */
    @ManyToOne
    @JoinColumn(name = "TCBRCDGO", referencedColumnName = "TCBRCDGO")
    private TempCobro tempCobro;    

    /**
     * Valor.
     */
    @Basic
    @Column(name = "TCEFVLRR")
    private Double valor;
    
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
     * Devuelve tempCobro
     */
    public TempCobro getTempCobro() {
        return this.tempCobro;
    }
    
    /**
     * Asigna tempCobro
     */
    public void setTempCobro(TempCobro tempCobro) {
        this.tempCobro = tempCobro;
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
}
