package com.saa.model.scp;

import java.io.Serializable;
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

@Entity
@Table(name = "PJRQ", schema = "SCP")
@SequenceGenerator(name = "SQ_PJRQCDGO1", sequenceName = "SCP.SQ_PJRQCDGO")
@NamedQueries({
		@NamedQuery(name = "EmpresaAll", query = "select e from Empresa e"),
		@NamedQuery(name = "EmpresaId", query = "select e from Empresa e where e.codigo = :id")
})
public class Empresa implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "PJRQCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PJRQCDGO")
	private Long codigo;

	@ManyToOne
	@JoinColumn(name = "PGSPCDGO", referencedColumnName = "PGSPCDGO")
	private Jerarquia jerarquia;

	@Column(name = "PJRQNMBR")
	private String nombre;

	@Column(name = "PJRQNVLL")
	private Long nivel;

	@Column(name = "PJRQCDPD")
	private Long codigoPadre;

	@Column(name = "PJRQINGR")
	private Long ingresado;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public Jerarquia getJerarquia() {
		return jerarquia;
	}

	public void setJerarquia(Jerarquia jerarquia) {
		this.jerarquia = jerarquia;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Long getNivel() {
		return nivel;
	}

	public void setNivel(Long nivel) {
		this.nivel = nivel;
	}

	public Long getCodigoPadre() {
		return codigoPadre;
	}

	public void setCodigoPadre(Long codigoPadre) {
		this.codigoPadre = codigoPadre;
	}

	public Long getIngresado() {
		return ingresado;
	}

	public void setIngresado(Long ingresado) {
		this.ingresado = ingresado;
	}

}