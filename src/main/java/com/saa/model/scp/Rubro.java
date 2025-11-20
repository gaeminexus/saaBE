package com.saa.model.scp;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "PRBR", schema = "SCP")
@SequenceGenerator(name = "SQ_PRBRCDGO", sequenceName = "SCP.SQ_PRBRCDGO")
@NamedQueries({
		@NamedQuery(name = "RubroAll", query = "select e from Rubro e"),
		@NamedQuery(name = "RubroId", query = "select e from Rubro e where e.codigo = :id")
})
public class Rubro implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "PRBRCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRBRCDGO")
	private Long codigo;

	@Column(name = "PRBRDSCR")
	private String descripcion;

	@Column(name = "PRBRFCHA")
	@Temporal(TemporalType.DATE)
	private Date fechaIngreso;

	@Column(name = "PRBRALTR")
	private Long codigoAlterno;

	@Column(name = "PRBRTPOO")
	private Long tipo;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
	}

	public Long getTipo() {
		return tipo;
	}

	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}
}
