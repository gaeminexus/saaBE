package com.saa.model.cnt;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C34d — Origen del dinero de un hecho de crédito (CNT.OHNV): línea del extracto, aporte o débito de cuenta individual,
 * partida pendiente por regularizar o SIN_DINERO. Guarda cómo está aplicado hoy y qué es en realidad (para la v8).
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-07-hechos-credito.sql.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "OHNV", schema = "CNT")
public class OrigenHecho implements Serializable {

	@Id
	@Basic
	@Column(name = "OHNVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;
	@Basic
	@Column(name = "HCNVCDGO")
	private Long idHecho;
	/** EXTRACTO, APORTE, PENDIENTE, SIN_DINERO. */
	@Basic
	@Column(name = "OHNVTIPO", length = 20)
	private String tipo;
	@Basic
	@Column(name = "DEXBCDGO")
	private Long idFilaExtracto;
	@Basic
	@Column(name = "APRTCDGO")
	private Long idAporte;
	@Basic
	@Column(name = "LNNVCDGO")
	private Long idLinea;
	@Basic
	@Column(name = "OHNVVLOR")
	private Double valor;
	/** Cómo está aplicado hoy (foto al elegirlo; marcas {entidad:N} en vez de nombres). */
	@Basic
	@Column(name = "OHNVCNAN", length = 1000)
	private String conceptoAnterior;
	/** Qué es en realidad. */
	@Basic
	@Column(name = "OHNVCNRL", length = 1000)
	private String conceptoReal;
	/** Entidad a la que está aplicado hoy, si es otra que la del préstamo. */
	@Basic
	@Column(name = "ENTDANTR")
	private Long idEntidadAnterior;
	@Basic
	@Column(name = "OHNVTXTO", length = 1000)
	private String texto;
	@Basic
	@Column(name = "OHNVESTD")
	private Long estado;
	@Basic
	@Column(name = "OHNVFCRG")
	private LocalDateTime fechaRegistro;
	@Basic
	@Column(name = "OHNVUSAR", length = 50)
	private String usuarioRegistro;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdHecho() { return idHecho; }
	public void setIdHecho(Long idHecho) { this.idHecho = idHecho; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public Long getIdFilaExtracto() { return idFilaExtracto; }
	public void setIdFilaExtracto(Long idFilaExtracto) { this.idFilaExtracto = idFilaExtracto; }
	public Long getIdAporte() { return idAporte; }
	public void setIdAporte(Long idAporte) { this.idAporte = idAporte; }
	public Long getIdLinea() { return idLinea; }
	public void setIdLinea(Long idLinea) { this.idLinea = idLinea; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public String getConceptoAnterior() { return conceptoAnterior; }
	public void setConceptoAnterior(String conceptoAnterior) { this.conceptoAnterior = conceptoAnterior; }
	public String getConceptoReal() { return conceptoReal; }
	public void setConceptoReal(String conceptoReal) { this.conceptoReal = conceptoReal; }
	public Long getIdEntidadAnterior() { return idEntidadAnterior; }
	public void setIdEntidadAnterior(Long idEntidadAnterior) { this.idEntidadAnterior = idEntidadAnterior; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
