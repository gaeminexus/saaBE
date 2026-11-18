package com.saa.model.cnt;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * C34 — Línea por justificar de una novedad (CNT.LNNV): un movimiento que pasó por una cuenta por regularizar.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-01-novedades-v2.sql. Las FK se guardan como id (no relación), como en C25.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LNNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "LineaNovedadAll", query = "select e from LineaNovedad e"),
	@NamedQuery(name = "LineaNovedadId", query = "select e from LineaNovedad e where e.codigo = :id")
})
public class LineaNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "LNNVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Novedad. */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Orden dentro de la novedad. */
	@Basic
	@Column(name = "LNNVSCNC")
	private Long secuencia;

	/** CONTRAPARTIDA, LINEA_PR o MANUAL. */
	@Basic
	@Column(name = "LNNVORGN", length = 20)
	private String origen;

	/** Asiento de origen. */
	@Basic
	@Column(name = "ASNTCDGO")
	private Long idAsiento;

	/** Línea del asiento que se muestra. */
	@Basic
	@Column(name = "DTASCDGO")
	private Long idDetalleAsiento;

	/** Línea del asiento en la cuenta por regularizar. */
	@Basic
	@Column(name = "DTASCDPR")
	private Long idDetalleCuentaRegularizar;

	/** Cuenta por regularizar afectada (CNT.PLNN). */
	@Basic
	@Column(name = "PLNNCDGO")
	private Long idCuenta;

	/** Fecha del asiento. */
	@Basic
	@Column(name = "LNNVFCHA")
	private LocalDate fecha;

	/** Glosa de la línea. */
	@Basic
	@Column(name = "LNNVDSCR", length = 1000)
	private String descripcion;

	/** Valor (positivo). */
	@Basic
	@Column(name = "LNNVVLOR")
	private Double valor;

	/** D débito / H crédito sobre la cuenta afectada. */
	@Basic
	@Column(name = "LNNVSNTD", length = 1)
	private String sentido;

	/** Tipo de referencia (MOV_BANCO...). */
	@Basic
	@Column(name = "LNNVTPRF", length = 20)
	private String tipoReferencia;

	/** Referencia. */
	@Basic
	@Column(name = "LNNVRFRN", length = 100)
	private String referencia;

	/** Fila del extracto (TSR.DEXB). */
	@Basic
	@Column(name = "DEXBCDGO")
	private Long idFilaExtracto;

	/** Préstamo (CRD.PRST). */
	@Basic
	@Column(name = "PRSTCDGO")
	private Long idPrestamo;

	/** Partícipe (CRD.ENTD). */
	@Basic
	@Column(name = "ENTDCDGO")
	private Long idEntidad;

	/** Saldo por justificar. */
	@Basic
	@Column(name = "LNNVSLDO")
	private Double saldo;

	/** 1 PENDIENTE, 2 PARCIAL, 3 JUSTIFICADA, 4 ANULADA. */
	@Basic
	@Column(name = "LNNVESTD")
	private Long estado;

	/** Fecha de registro. */
	@Basic
	@Column(name = "LNNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "LNNVUSAR", length = 50)
	private String usuarioRegistro;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Long getSecuencia() { return secuencia; }
	public void setSecuencia(Long secuencia) { this.secuencia = secuencia; }
	public String getOrigen() { return origen; }
	public void setOrigen(String origen) { this.origen = origen; }
	public Long getIdAsiento() { return idAsiento; }
	public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
	public Long getIdDetalleAsiento() { return idDetalleAsiento; }
	public void setIdDetalleAsiento(Long idDetalleAsiento) { this.idDetalleAsiento = idDetalleAsiento; }
	public Long getIdDetalleCuentaRegularizar() { return idDetalleCuentaRegularizar; }
	public void setIdDetalleCuentaRegularizar(Long idDetalleCuentaRegularizar) { this.idDetalleCuentaRegularizar = idDetalleCuentaRegularizar; }
	public Long getIdCuenta() { return idCuenta; }
	public void setIdCuenta(Long idCuenta) { this.idCuenta = idCuenta; }
	public LocalDate getFecha() { return fecha; }
	public void setFecha(LocalDate fecha) { this.fecha = fecha; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public String getSentido() { return sentido; }
	public void setSentido(String sentido) { this.sentido = sentido; }
	public String getTipoReferencia() { return tipoReferencia; }
	public void setTipoReferencia(String tipoReferencia) { this.tipoReferencia = tipoReferencia; }
	public String getReferencia() { return referencia; }
	public void setReferencia(String referencia) { this.referencia = referencia; }
	public Long getIdFilaExtracto() { return idFilaExtracto; }
	public void setIdFilaExtracto(Long idFilaExtracto) { this.idFilaExtracto = idFilaExtracto; }
	public Long getIdPrestamo() { return idPrestamo; }
	public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
	public Long getIdEntidad() { return idEntidad; }
	public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
	public Double getSaldo() { return saldo; }
	public void setSaldo(Double saldo) { this.saldo = saldo; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
