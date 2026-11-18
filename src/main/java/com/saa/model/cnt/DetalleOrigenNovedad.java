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
 * C34 — Detalle de origen de una novedad (CNT.DONV): cuotas, pagos, préstamos, movimientos; solo lectura, lo cargan scripts del expediente.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-01-novedades-v2.sql. Las FK se guardan como id (no relación), como en C25.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DONV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "DetalleOrigenNovedadAll", query = "select e from DetalleOrigenNovedad e"),
	@NamedQuery(name = "DetalleOrigenNovedadId", query = "select e from DetalleOrigenNovedad e where e.codigo = :id")
})
public class DetalleOrigenNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "DONVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Novedad. */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Título del bloque. */
	@Basic
	@Column(name = "DONVGRPO", length = 300)
	private String grupo;

	/** Orden. */
	@Basic
	@Column(name = "DONVSCNC")
	private Long secuencia;

	/** CUOTA, PAGO, PRESTAMO, APORTE, COBRO, MOV_BANCO, OTRO. */
	@Basic
	@Column(name = "DONVTIPO", length = 20)
	private String tipo;

	/** Clave en la fuente. */
	@Basic
	@Column(name = "DONVCLVE", length = 100)
	private String clave;

	/** Préstamo. */
	@Basic
	@Column(name = "PRSTCDGO")
	private Long idPrestamo;

	/** Cuota. */
	@Basic
	@Column(name = "DTPRCDGO")
	private Long idCuota;

	/** Pago. */
	@Basic
	@Column(name = "PGPRCDGO")
	private Long idPago;

	/** Partícipe. */
	@Basic
	@Column(name = "ENTDCDGO")
	private Long idEntidad;

	/** Fila del extracto. */
	@Basic
	@Column(name = "DEXBCDGO")
	private Long idFilaExtracto;

	/** Asiento. */
	@Basic
	@Column(name = "ASNTCDGO")
	private Long idAsiento;

	/** Número de cuota. */
	@Basic
	@Column(name = "DONVNMCT")
	private Long numeroCuota;

	/** Fecha principal. */
	@Basic
	@Column(name = "DONVFCH1")
	private LocalDate fecha1;

	/** Fecha secundaria. */
	@Basic
	@Column(name = "DONVFCH2")
	private LocalDate fecha2;

	/** Valor principal. */
	@Basic
	@Column(name = "DONVVLR1")
	private Double valor1;

	/** Valor secundario. */
	@Basic
	@Column(name = "DONVVLR2")
	private Double valor2;

	/** Estado antes. */
	@Basic
	@Column(name = "DONVESAN", length = 50)
	private String estadoAntes;

	/** Estado después. */
	@Basic
	@Column(name = "DONVESDS", length = 50)
	private String estadoDespues;

	/** Descripción. */
	@Basic
	@Column(name = "DONVDSCR", length = 1000)
	private String descripcion;

	/** Fuente. */
	@Basic
	@Column(name = "DONVFNTE", length = 200)
	private String fuente;

	/** Fecha de registro. */
	@Basic
	@Column(name = "DONVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "DONVUSAR", length = 50)
	private String usuarioRegistro;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public String getGrupo() { return grupo; }
	public void setGrupo(String grupo) { this.grupo = grupo; }
	public Long getSecuencia() { return secuencia; }
	public void setSecuencia(Long secuencia) { this.secuencia = secuencia; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public String getClave() { return clave; }
	public void setClave(String clave) { this.clave = clave; }
	public Long getIdPrestamo() { return idPrestamo; }
	public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
	public Long getIdCuota() { return idCuota; }
	public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }
	public Long getIdPago() { return idPago; }
	public void setIdPago(Long idPago) { this.idPago = idPago; }
	public Long getIdEntidad() { return idEntidad; }
	public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
	public Long getIdFilaExtracto() { return idFilaExtracto; }
	public void setIdFilaExtracto(Long idFilaExtracto) { this.idFilaExtracto = idFilaExtracto; }
	public Long getIdAsiento() { return idAsiento; }
	public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
	public Long getNumeroCuota() { return numeroCuota; }
	public void setNumeroCuota(Long numeroCuota) { this.numeroCuota = numeroCuota; }
	public LocalDate getFecha1() { return fecha1; }
	public void setFecha1(LocalDate fecha1) { this.fecha1 = fecha1; }
	public LocalDate getFecha2() { return fecha2; }
	public void setFecha2(LocalDate fecha2) { this.fecha2 = fecha2; }
	public Double getValor1() { return valor1; }
	public void setValor1(Double valor1) { this.valor1 = valor1; }
	public Double getValor2() { return valor2; }
	public void setValor2(Double valor2) { this.valor2 = valor2; }
	public String getEstadoAntes() { return estadoAntes; }
	public void setEstadoAntes(String estadoAntes) { this.estadoAntes = estadoAntes; }
	public String getEstadoDespues() { return estadoDespues; }
	public void setEstadoDespues(String estadoDespues) { this.estadoDespues = estadoDespues; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public String getFuente() { return fuente; }
	public void setFuente(String fuente) { this.fuente = fuente; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
