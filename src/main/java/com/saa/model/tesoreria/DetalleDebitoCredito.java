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
 * <p>Pojo mapeo de tabla TSR.DTDC.
 * Entity Detalle Debito Credito.
 * Almacena el detalle de los debitos/creditos realizados a una cuenta bancaria.
 * Cada detalle esta relacionado con un detalle de plantilla para
 * obtener la cuenta contable y generar el asiento.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTDC", schema = "TSR")
@SequenceGenerator(name = "SQ_DTDCCDGO", sequenceName = "TSR.SQ_DTDCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleDebitoCreditoAll", query = "select e from DetalleDebitoCredito e"),
	@NamedQuery(name = "DetalleDebitoCreditoId", query = "select e from DetalleDebitoCredito e where e.codigo = :id")
})
public class DetalleDebitoCredito implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DTDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTDCCDGO")
    private Long codigo;
    
    /**
     * Debito/Credito al que pertenece el detalle
     */
    @ManyToOne
    @JoinColumn(name = "DBCRCDGO", referencedColumnName = "DBCRCDGO")
    private DebitoCredito debitoCredito;    

    /**
     * Detalle plantilla de la que se obtiene la cuenta contable.
     */
    @ManyToOne
    @JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
    private DetallePlantilla detallePlantilla;
    
    /**
     * Descripcion.
     */
    @Basic
    @Column(name = "DTDCDSCR", length = 500)
    private String descripcion;
    
    /**
     * Valor.
     */
    @Basic
    @Column(name = "DTDCVLRR")
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
     * Devuelve debitoCredito
     */
    public DebitoCredito getDebitoCredito() {
        return this.debitoCredito;
    }
    
    /**
     * Asigna debitoCredito
     */
    public void setDebitoCredito(DebitoCredito debitoCredito) {
        this.debitoCredito = debitoCredito;
    }
    
    /**
     * Devuelve detallePlantilla
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
}
