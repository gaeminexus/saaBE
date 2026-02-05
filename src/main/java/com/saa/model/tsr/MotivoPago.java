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
import com.saa.model.cnt.Plantilla;

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
 * <p>Pojo mapeo de tabla TSR.PMTV.
 *  Entity Motivo de pago.
 *  Almacena el motivo de cada pago.
 *  Detalle de la entidad Pago.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PMTV", schema = "TSR")
@SequenceGenerator(name = "SQ_PMTVCDGO", sequenceName = "TSR.SQ_PMTVCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MotivoPagoAll", query = "select e from MotivoPago e"),
	@NamedQuery(name = "MotivoPagoId", query = "select e from MotivoPago e where e.codigo = :id")
})
public class MotivoPago implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "PMTVCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PMTVCDGO")
    private Long codigo;
    
    /**
     * Pago al que pertenece.
     */
    @ManyToOne
    @JoinColumn(name = "PGSSCDGO", referencedColumnName = "PGSSCDGO")
    private Pago pago;    

    /**
     * Plantilla para el motivo de pago.
     */
    @ManyToOne
    @JoinColumn(name = "PLNSCDGO", referencedColumnName = "PLNSCDGO")
    private Plantilla plantilla;
    
    /**
     * Detalle plantilla para el motivo de pago.
     */
    @ManyToOne
    @JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
    private DetallePlantilla detallePlantilla;
    
    /**
     * Descripcion del motivo de pago.
     */
    @Basic
    @Column(name = "PMTVDSCR", length = 200)
    private String descripcion;
    
    /**
     * Valor por el motivo de pago.
     */
    @Basic
    @Column(name = "PMTVVLRR")
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
     * Devuelve tempPago
     */
    public Pago getPago() {
        return this.pago;
    }
    
    /**
     * Asigna tempPago
     */
    public void setPago(Pago pago) {
        this.pago = pago;
    }

    /**
     * Devuelve plantilla
     * @return plantilla
     */
    public Plantilla getPlantilla() {
        return plantilla;
    }

    /**
     * Asigna plantilla
     * @param plantilla Nuevo valor para plantilla 
     */
    public void setPlantilla(Plantilla plantilla) {
        this.plantilla = plantilla;
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
