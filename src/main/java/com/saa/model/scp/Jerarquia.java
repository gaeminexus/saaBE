package com.saa.model.scp;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.NamedQuery;

@Entity
@Table(name = "PGSP", schema = "SCP")
@SequenceGenerator(name = "SQ_PGSPCDGO", sequenceName = "SCP.SQ_PGSPCDGO")
@NamedQueries({
		@NamedQuery(name = "JerarquiaAll", query = "select e from Jerarquia e"),
		@NamedQuery(name = "JerarquiaId", query = "select e from Jerarquia e where e.codigo = :id")
})
public class Jerarquia implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3522664762088215258L;

	@Id
	@Column(name = "PGSPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PGSPCDGO")
	private Long codigo;

	@Column(name = "PGSPNMBR")
	private String nombre;

	@Column(name = "PGSPNVLL")
	private Long nivel;

	@Column(name = "PGSPCDPD")
	private Long codigoPadre;

	@Column(name = "PGSPDSCP")
	private String descripcion;

	@Column(name = "PGSPULNV")
	private Long ultimoNivel;

	@Column(name = "PGSPRYYA")
	private Long rubroTipoEstructuraP;

	@Column(name = "PGSPRZZA")
	private Long rubroTipoEstructuraH;

	@Column(name = "PGSPCDAL")
	private Long codigoAlterno;

	@Column(name = "PGSPRYYB")
	private Long rubroNivelCaracteristicaP;

	@Column(name = "PGSPRZZB")
	private Long rubroNivelCaracteristicaH;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
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

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public Long getUltimoNivel() {
		return ultimoNivel;
	}

	public void setUltimoNivel(Long ultimoNivel) {
		this.ultimoNivel = ultimoNivel;
	}

	public Long getRubroTipoEstructuraP() {
		return rubroTipoEstructuraP;
	}

	public void setRubroTipoEstructuraP(Long rubroTipoEstructuraP) {
		this.rubroTipoEstructuraP = rubroTipoEstructuraP;
	}

	public Long getRubroTipoEstructuraH() {
		return rubroTipoEstructuraH;
	}

	public void setRubroTipoEstructuraH(Long rubroTipoEstructuraH) {
		this.rubroTipoEstructuraH = rubroTipoEstructuraH;
	}

	public Long getCodigoAlterno() {
		return codigoAlterno;
	}

	public void setCodigoAlterno(Long codigoAlterno) {
		this.codigoAlterno = codigoAlterno;
	}

	public Long getRubroNivelCaracteristicaP() {
		return rubroNivelCaracteristicaP;
	}

	public void setRubroNivelCaracteristicaP(Long rubroNivelCaracteristicaP) {
		this.rubroNivelCaracteristicaP = rubroNivelCaracteristicaP;
	}

	public Long getRubroNivelCaracteristicaH() {
		return rubroNivelCaracteristicaH;
	}

	public void setRubroNivelCaracteristicaH(Long rubroNivelCaracteristicaH) {
		this.rubroNivelCaracteristicaH = rubroNivelCaracteristicaH;
	}
}
