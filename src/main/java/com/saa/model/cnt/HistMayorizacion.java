package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@Table(name = "MYRH", schema = "CNT")
@SequenceGenerator(name = "SQ_MYRHCDGO", sequenceName = "CNT.SQ_MYRHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "HistMayorizacionAll", query = "select e from HistMayorizacion e"),
	@NamedQuery(name = "HistMayorizacionId", query = "select e from HistMayorizacion e where e.codigo = :id")
})
public class HistMayorizacion implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "MYRHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MYRHCDGO")
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
	@Column(name = "MYRHFCHA")
	private LocalDateTime fecha;	
	
	/**
	 * Id de la mayorizacion que origina el respaldo
	 */
	@Basic
	@Column(name = "MYRZCDGO")
	private Long idMayorizacion;
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
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
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna fecha
	 * @param localDateTime nuevo valor para fecha 
	 */
	public void setFecha(LocalDateTime localDateTime) {
		this.fecha = localDateTime;
	}

	/**
	 * Devuelve idMayorizacion
	 * @return idMayorizacion
	 */
	public Long getIdMayorizacion() {
		return idMayorizacion;
	}

	/**
	 * Asigna idMayorizacion
	 * @param idMayorizacion
	 */
	public void setIdMayorizacion(Long idMayorizacion) {
		this.idMayorizacion = idMayorizacion;
	}

}
