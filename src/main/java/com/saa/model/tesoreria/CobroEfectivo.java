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
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.CEFC.
 *  Entity Cobro con efectivo.
 *  Almacena los cobros en efectivo realizados.
 *  Es detalle de la entidad cobro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CEFC", schema = "TSR")
@SequenceGenerator(name = "SQ_CEFCCDGO", sequenceName = "TSR.SQ_CEFCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CobroEfectivoAll", query = "select e from CobroEfectivo e"),
	@NamedQuery(name = "CobroEfectivoId", query = "select e from CobroEfectivo e where e.codigo = :id")
})
public class CobroEfectivo implements Serializable {

    @Basic
    @Id
    @Column(name = "CEFCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CEFCCDGO")
    private Long codigo;
    
    @ManyToOne
    @JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
    private Cobro cobro;    

    @Basic
    @Column(name = "CEFCVLRR")
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
     * Devuelve cobro
     */
    public Cobro getCobro() {
        return this.cobro;
    }
    
    /**
     * Asigna cobro
     */
    public void setCobro(Cobro cobro) {
        this.cobro = cobro;
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