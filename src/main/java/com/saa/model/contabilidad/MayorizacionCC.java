package com.saa.model.contabilidad;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
@SuppressWarnings("serial")
@Entity
@Table(name = "MYRC", schema = "CNT")
//@SequenceGenerator(name = "SQ_MYRCCDGO", sequenceName = "CNT.SQ_MYRCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "MayorizacionCCAll", query = "select e from MayorizacionCC e"),
	@NamedQuery(name = "MayorizacionCCId", query = "select e from MayorizacionCC e where e.codigo = :id")
})
public class MayorizacionCC implements Serializable {

	/**
	 * id de la tabla.  codigo.
	 */
	@Basic
	@Id
	@Column(name = "MYRCCDGO", precision = 0)
//	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MYRCCDGO")
	private Long codigo;
	
	/**
	 * id del período a mayorizar. periodo
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;	

	/**
	 * fecha en que se realiza la mayorización.  fecha.
	 */
	@Basic
	@Column(name = "MYRCFCHA")
	private LocalDateTime fecha;
	

	/**
	 * Devuelve codigo 
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo valor nuevo para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve  periodo
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
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna fecha
	 * @param LocalDateTime valor nuevo para fecha 
	 */
	public void setFecha(LocalDateTime LocalDateTime) {
		this.fecha = LocalDateTime;
	}
	
   
}
