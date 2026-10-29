package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * C17 — Bitácora de cierres y reaperturas de período (CNT.HCPR).
 *
 * <p>Una fila por cada cierre o reapertura, contable o de cartera. Nunca se actualiza ni se borra: es la
 * auditoría de quién cerró/reabrió qué mes, cuándo y por qué. El estado vigente NO se lee de aquí (se lee de
 * {@code CNT.PRDO.PRDOCNCR} para contabilidad y de la corrida EJECUTADA de {@code CRD.CRCT} para cartera).</p>
 *
 * <p>Ver docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md y el DDL docs/logica-negocio/cnt/sql/C17-01-*.sql.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HCPR", schema = "CNT")
public class HistorialCierrePeriodo implements Serializable {

	/** Tipo de cierre: contable. */
	public static final String TIPO_CONTABLE = "CONTABLE";
	/** Tipo de cierre: cartera. */
	public static final String TIPO_CARTERA = "CARTERA";
	/** Acción: cierre. */
	public static final String ACCION_CIERRE = "CIERRE";
	/** Acción: reapertura. */
	public static final String ACCION_REAPERTURA = "REAPERTURA";

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "HCPRCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Empresa (SCP.PJRQ). Se guarda el id, no la relación: la bitácora no se navega. */
	@Basic
	@Column(name = "PJRQCDGO")
	private Long idEmpresa;

	@Basic
	@Column(name = "HCPRANNO")
	private Long anio;

	@Basic
	@Column(name = "HCPRMESS")
	private Long mes;

	/** CONTABLE o CARTERA. */
	@Basic
	@Column(name = "HCPRTPOO", length = 10)
	private String tipo;

	/** CIERRE o REAPERTURA. */
	@Basic
	@Column(name = "HCPRACCN", length = 12)
	private String accion;

	@Basic
	@Column(name = "HCPRFCHA")
	private LocalDateTime fecha;

	@Basic
	@Column(name = "HCPRUSRO", length = 100)
	private String usuario;

	@Basic
	@Column(name = "HCPRIPPP", length = 50)
	private String ip;

	@Basic
	@Column(name = "HCPRMTVO", length = 1000)
	private String motivo;

	/** Período contable (CNT.PRDO) afectado, si aplica. */
	@Basic
	@Column(name = "PRDOCDGO")
	private Long idPeriodo;

	/** Corrida de cierre de cartera (CRD.CRCT) afectada, si aplica. */
	@Basic
	@Column(name = "CRCTCDGO")
	private Long idCorrida;

	/** 1 si la operación se hizo fuera de orden porque el parámetro de re-proceso lo permitía. */
	@Basic
	@Column(name = "HCPRFRDN")
	private Long fueraDeOrden;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdEmpresa() { return idEmpresa; }
	public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
	public Long getAnio() { return anio; }
	public void setAnio(Long anio) { this.anio = anio; }
	public Long getMes() { return mes; }
	public void setMes(Long mes) { this.mes = mes; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public String getAccion() { return accion; }
	public void setAccion(String accion) { this.accion = accion; }
	public LocalDateTime getFecha() { return fecha; }
	public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
	public Long getIdPeriodo() { return idPeriodo; }
	public void setIdPeriodo(Long idPeriodo) { this.idPeriodo = idPeriodo; }
	public Long getIdCorrida() { return idCorrida; }
	public void setIdCorrida(Long idCorrida) { this.idCorrida = idCorrida; }
	public Long getFueraDeOrden() { return fueraDeOrden; }
	public void setFueraDeOrden(Long fueraDeOrden) { this.fueraDeOrden = fueraDeOrden; }
}
