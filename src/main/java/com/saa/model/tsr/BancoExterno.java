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
    
    /**
     * ⚠ Nombre histórico y engañoso: {@code BEXTTRJT} NO es un indicador de
     * tarjeta de crédito. Es el código de institución financiera del BCE que
     * exigen los formatos bancarios de pagos (campo 12 del Internacional,
     * columna B del Pacífico). Confirmado con el {@code e2-17} (2026-09-07):
     * 389 filas, 387 valores distintos (mínimo 10, máximo 9997), anclas
     * Banco de Machala=25 y Banco del Pacífico=30 verificadas sin repetirse
     * en ningún otro banco, y {@code BEXTTRJT=32} coincide con
     * {@code BANCO INTERNACIONAL} tal como exige su propia especificación
     * de archivo. NO renombrar: la entidad se serializa directo a JSON y el
     * frontend de bancos lee la clave {@code tarjeta}.
     */
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
	 * ⚠ Pese al nombre, es el código de institución financiera del BCE
	 * ({@code BEXTTRJT}), no un indicador de tarjeta de crédito. Ver el
	 * javadoc del campo.
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
