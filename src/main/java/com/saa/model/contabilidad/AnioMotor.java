package com.saa.model.contabilidad;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "ANIO", schema = "AOT")
@SequenceGenerator(name = "SQ_ANIOCDGO", sequenceName = "AOT.SQ_ANIOCDGO", allocationSize = 1)
@NamedQueries({
		@NamedQuery(name = "AnioMotorId", query = "select e from AnioMotor e where e.codigo=:id"),
		@NamedQuery(name = "AnioMotorAll", query = "select e from AnioMotor e")
})
public class AnioMotor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ANIOCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ANIOCDGO")
	private Long codigo;

	@Column(name = "ANIOANIO")
	private Long anio;

	@Column(name = "ANIOESTD")
	private Long estado;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public Long getAnio() {
		return anio;
	}

	public void setAnio(Long anio) {
		this.anio = anio;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
