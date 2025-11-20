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
@Table(name = "PDTR", schema = "SCP")
@SequenceGenerator(name = "SQ_PDTRCDGO", sequenceName = "SCP.SQ_PDTRCDGO")
@NamedQueries({
		@NamedQuery(name = "DetalleRubroAll", query = "SELECT e FROM DetalleRubro e"),
		@NamedQuery(name = "DetalleRubroId", query = "SELECT e from DetalleRubro e where e.codigo = :id")
})
public class DetalleRubro implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "PDTRCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PDTRCDGO")
	private Long codigo;

	@ManyToOne
	@JoinColumn(name = "PRBRCDGO", referencedColumnName = "PRBRCDGO")
	private Rubro rubro;

	@Column(name = "PDTRDSCR")
	private String descripcion;

	@Column(name = "PDTRVLRN")
	private Double valorNumerico;

	@Column(name = "PDTRVLRV")
	private String valorAlfanumerico;

	@Column(name = "PDTRALTR")
	private Long codigoAlterno;

	@Column(name = "PDTRESTD")
	private Long estado;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public Rubro getRubro() {
		return rubro;
	}

	public void setRubro(Rubro rubro) {
		this.rubro = rubro;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public Double getValorNumerico() {
		return valorNumerico;
	}

	public void setValorNumerico(Double valorNumerico) {
		this.valorNumerico = valorNumerico;
	}

	public String getValorAlfanumerico() {
		return valorAlfanumerico;
	}

	public void setValorAlfanumerico(String valorAlfanumerico) {
		this.valorAlfanumerico = valorAlfanumerico;
	}

	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}

}
