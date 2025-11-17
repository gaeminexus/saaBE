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
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.BEXT.
 * Entity BancoExterno.
 * Contiene el listado de todas las entidades financieras que se utilizan en el sistema.
 * Son diferentes al listado de entidades financieras manejadas por la empresa.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "BEXT", schema = "TSR")
@SequenceGenerator(name = "SQ_BEXTCDGO", sequenceName = "TSR.SQ_BEXTCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "BancoExternoAll", query = "select e from BancoExterno e order by e.nombre"),
    @NamedQuery(name = "BancoExternoId", query = "select e from BancoExterno e where e.codigo = :id")
})
public class BancoExterno implements Serializable {    
    
    @Basic
    @Id
    @Column(name = "BEXTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_BEXTCDGO")
    private Long codigo;    
    
    @Basic
    @Column(name = "BEXTNMBR")
    private String nombre;    
    
    @Basic
    @Column(name = "BEXTTRJT")
    private Long tarjeta;    
    
    @Basic
    @Column(name = "BEXTESTD")
    private Long estado;    
    
    @Basic
    @Column(name = "BEXTFCIN")
    private LocalDateTime fechaIngreso;
    
    /**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}
	
	/**
	 * Asigna codigo
	 * @param codigo Nuevo valor de codigo
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
	 * @param nombre nuevo valor de nombre
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve tarjeta
	 * @return tarjeta
	 */
	public Long getTarjeta() {
		return tarjeta;
	}
	
	/**
	 * Asigna tarjeta
	 * @param tarjeta Nuevo valor de tarjeta
	 */
	public void setTarjeta(Long tarjeta) {
		this.tarjeta = tarjeta;
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
	 * @param estado nuevo valor de estado
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Devuelve fecha ingreso
	 * @return fecha ingreso
	 */
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fecha ingreso
	 * @param fechaIngreso Nuevo valor de fecha de ingreso
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}	
	
}
