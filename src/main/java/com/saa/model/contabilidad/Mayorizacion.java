package com.saa.model.contabilidad;

import java.io.Serializable;
import java.util.Date;

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

@SuppressWarnings("serial")
@Entity
@Table(name = "MYRZ", schema = "CNT")
@SequenceGenerator(name = "SQ_MYRZCDGO", sequenceName = "CNT.SQ_MYRZCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MayorizacionAll", query = "select e from Mayorizacion e"),
	@NamedQuery(name = "MayorizacionId", query = "select e from Mayorizacion e where e.codigo = :id")
})
public class Mayorizacion implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "MYRZCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MYRZCDGO")
	private Long codigo;
	
	/**
	 * id del período a mayorizar.
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;	

	/**
	 * fecha en que se realiza la mayorización.
	 */
	@Basic
	@Column(name = "MYRZFCHA")
	private Date fecha;	
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para  codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve periodo
	 */
	public Periodo getPeriodo() {
		return this.periodo;
	}
	
	/**
	 * Asigna periodo
	 */
	public void setPeriodo(Periodo periodo) {
		this.periodo = periodo;
	}

	/**
	 * Devuelve fecha
	 * @return fecha
	 */
	public Date getFecha() {
		return fecha;
	}

	/**
	 * Asigna fecha
	 * @param fecha nuevo valor para  fecha 
	 */
	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

   
}
