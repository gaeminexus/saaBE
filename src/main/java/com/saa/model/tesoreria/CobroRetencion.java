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

import com.saa.model.contabilidad.DetallePlantilla;
import com.saa.model.contabilidad.Plantilla;

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
 * <p>Pojo mapeo de tabla TSR.CRTN.
 *  Entity cobro con retencion.
 *  Almacena los cobros con retencion realizados.
 *  Es detalle de la entidad cobro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRTN", schema = "TSR")
@SequenceGenerator(name = "SQ_CRTNCDGO", sequenceName = "TSR.SQ_CRTNCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CobroRetencionAll", query = "select e from CobroRetencion e"),
	@NamedQuery(name = "CobroRetencionId", query = "select e from CobroRetencion e where e.codigo = :id")
})
public class CobroRetencion implements Serializable {

    @Basic
    @Id
    @Column(name = "CRTNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CRTNCDGO")
    private Long codigo;
    
    @ManyToOne
    @JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
    private Cobro cobro;
    
    @ManyToOne
    @JoinColumn(name = "PLNSCDGO", referencedColumnName = "PLNSCDGO")
    private Plantilla plantilla;
    
    @ManyToOne
    @JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
    private DetallePlantilla detallePlantilla;

    @Basic
    @Column(name = "CRTNVLRR")
    private Double valor;

    @Basic
    @Column(name = "CRTNNMRO", length = 50)
    private String numero;
	
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
	 * Devuelve plantilla
	 */
	public Plantilla getPlantilla() {
		return plantilla;
	}

	/**
	 * Asigna plantilla
	 */
	public void setPlantilla(Plantilla plantilla) {
		this.plantilla = plantilla;
	}

	/**
	 * Devuelve DetallePlantilla
	 */
	public DetallePlantilla getDetallePlantilla() {
		return detallePlantilla;
	}

	/**
	 * Asigna detallePlantilla
	 */
	public void setDetallePlantilla(DetallePlantilla detallePlantilla) {
		this.detallePlantilla = detallePlantilla;
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
	 * Devuelve numero
	 * @return numero
	 */
	public String getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero Nuevo valor para numero 
	 */
	public void setNumero(String numero) {
		this.numero = numero;
	}
	
}
