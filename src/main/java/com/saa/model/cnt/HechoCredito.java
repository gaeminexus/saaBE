package com.saa.model.cnt;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * C34d — Hecho de crédito (CNT.HCNV): con qué resuelve Crédito una o varias novedades (operación, tipo, fecha real, valor,
 * cuota desde la que se recalcula). Lo aplica la pasada v8 con el motor de crédito en su mes; aquí solo se registra.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-07-hechos-credito.sql.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HCNV", schema = "CNT")
public class HechoCredito implements Serializable {

	@Id
	@Basic
	@Column(name = "HCNVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;
	@Basic
	@Column(name = "PJRQCDGO")
	private Long idEmpresa;
	@Basic
	@Column(name = "PRSTCDGO")
	private Long idPrestamo;
	/** ABONO_CAPITAL, PAGO_CUOTAS, PRECANCELACION, REAPERTURA_CUOTAS, PARTICIPE_DEBE. */
	@Basic
	@Column(name = "HCNVTIPO", length = 20)
	private String tipo;
	@Basic
	@Column(name = "HCNVFCHA")
	private LocalDate fecha;
	@Basic
	@Column(name = "HCNVVLOR")
	private Double valor;
	/** Cuota desde la que se recalcula la tabla. */
	@Basic
	@Column(name = "DTPRCDGO")
	private Long idCuota;
	@Basic
	@Column(name = "HCNVTXTO", length = 4000)
	private String texto;
	@Basic
	@Column(name = "HCNVTPDC", length = 30)
	private String tipoDocumento;
	@Basic
	@Column(name = "HCNVNMDC", length = 100)
	private String numeroDocumento;
	/** 1 VIGENTE, 2 ANULADO. */
	@Basic
	@Column(name = "HCNVESTD")
	private Long estado;
	@Basic
	@Column(name = "HCNVFCRG")
	private LocalDateTime fechaRegistro;
	@Basic
	@Column(name = "HCNVUSAR", length = 50)
	private String usuarioRegistro;
	@Basic
	@Column(name = "HCNVFCAN")
	private LocalDateTime fechaAnulacion;
	@Basic
	@Column(name = "HCNVUSAN", length = 50)
	private String usuarioAnulacion;
	@Basic
	@Column(name = "HCNVMTAN", length = 1000)
	private String motivoAnulacion;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdEmpresa() { return idEmpresa; }
	public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
	public Long getIdPrestamo() { return idPrestamo; }
	public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public LocalDate getFecha() { return fecha; }
	public void setFecha(LocalDate fecha) { this.fecha = fecha; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public Long getIdCuota() { return idCuota; }
	public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public String getTipoDocumento() { return tipoDocumento; }
	public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
	public String getNumeroDocumento() { return numeroDocumento; }
	public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
	public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
	public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
	public String getUsuarioAnulacion() { return usuarioAnulacion; }
	public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }
	public String getMotivoAnulacion() { return motivoAnulacion; }
	public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

	/** C34-07b: mes desde (primer día) de un hecho en serie o de las cuotas pendientes de una precancelación. */
	@Basic
	@Column(name = "HCNVMSDS")
	private LocalDate mesDesde;
	/** C34-07b: NO_SE_DEBITA, SE_DEVUELVE o PARTICIPE_DEBE. */
	@Basic
	@Column(name = "HCNVTRDF", length = 20)
	private String tratamientoDiferencia;
	@Basic
	@Column(name = "HCNVVLDF")
	private Double valorDiferencia;
	/** C34-07b: reparto calculado por mes (JSON). */
	@Lob
	@Column(name = "HCNVDTLL")
	private String detalle;
	/** C34-07c: tipo de aporte con que se cruza la diferencia que falta (CRUCE_APORTES). */
	@Basic
	@Column(name = "HCNVTPAP")
	private Long tipoAporteDiferencia;
	/** C34-07d: condonación del faltante (JSON por cuota y concepto). */
	@Lob
	@Column(name = "HCNVCNDN")
	private String condonacion;
	/** C34-07d: decisión de ASOPREP que respalda la condonación. */
	@Basic
	@Column(name = "HCNVDCSN")
	private Long idDecision;
	public String getCondonacion() { return condonacion; }
	public void setCondonacion(String condonacion) { this.condonacion = condonacion; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public Long getTipoAporteDiferencia() { return tipoAporteDiferencia; }
	public void setTipoAporteDiferencia(Long tipoAporteDiferencia) { this.tipoAporteDiferencia = tipoAporteDiferencia; }
	public LocalDate getMesDesde() { return mesDesde; }
	public void setMesDesde(LocalDate mesDesde) { this.mesDesde = mesDesde; }
	public String getTratamientoDiferencia() { return tratamientoDiferencia; }
	public void setTratamientoDiferencia(String tratamientoDiferencia) { this.tratamientoDiferencia = tratamientoDiferencia; }
	public Double getValorDiferencia() { return valorDiferencia; }
	public void setValorDiferencia(Double valorDiferencia) { this.valorDiferencia = valorDiferencia; }
	public String getDetalle() { return detalle; }
	public void setDetalle(String detalle) { this.detalle = detalle; }
}
