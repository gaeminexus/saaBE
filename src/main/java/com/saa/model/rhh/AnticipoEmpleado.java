package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cxp.PagoProgramado;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entity AnticipoEmpleado.
 * Anticipo de sueldo entregado a un colaborador, con su plan de descuento en
 * cuotas. Tabla: RHH.ANTE.
 * <p>
 * Ciclo: SOLICITADO -&gt; APROBADO (dispara el {@link PagoProgramado} de
 * origen externo {@code RHH_ANTICIPO_EMPLEADO}) -&gt; PAGADO (al confirmarse
 * el pago se genera el asiento y el {@link DescuentoRecurrente}) -&gt;
 * EN_DESCUENTO (el rol cobra cuotas) -&gt; CANCELADO (saldo en cero). O
 * ANULADO desde SOLICITADO/APROBADO sin pago confirmado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ANTE", schema = "RHH")
@NamedQueries({
	@NamedQuery(name = "AnticipoEmpleadoAll", query = "select e from AnticipoEmpleado e"),
	@NamedQuery(name = "AnticipoEmpleadoId",  query = "select e from AnticipoEmpleado e where e.codigo = :id")
})
public class AnticipoEmpleado implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Basic
	@Column(name = "ANTECDGO", precision = 0)
	private Long codigo;

	/**
	 * Colaborador que recibe el anticipo.
	 */
	@ManyToOne
	@JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
	private Empleado empleado;

	/**
	 * Fecha de la solicitud.
	 */
	@Basic
	@Column(name = "ANTEFCHA")
	private LocalDate fecha;

	/**
	 * Valor total entregado.
	 */
	@Basic
	@Column(name = "ANTEVLOR")
	private Double valor;

	/**
	 * Numero de cuotas en que se descuenta del rol.
	 */
	@Basic
	@Column(name = "ANTENMRO")
	private Long numeroCuotas;

	/**
	 * Valor de cada cuota.
	 */
	@Basic
	@Column(name = "ANTEVLCT")
	private Double valorCuota;

	/**
	 * Saldo pendiente por descontar; al llegar a 0 el anticipo queda CANCELADO.
	 */
	@Basic
	@Column(name = "ANTESLDD")
	private Double saldo;

	/**
	 * Mes desde el que empieza a descontarse en el rol.
	 */
	@Basic
	@Column(name = "ANTEFCHI")
	private LocalDate fechaInicioDescuento;

	/**
	 * Motivo por el que se solicita.
	 */
	@Basic
	@Column(name = "ANTEMTVO", length = 500)
	private String motivo;

	/**
	 * Observaciones.
	 */
	@Basic
	@Column(name = "ANTEOBSR", length = 2000)
	private String observacion;

	/**
	 * Estado (rubro 234): 1=Solicitado, 2=Aprobado, 3=Pagado, 4=En descuento,
	 * 5=Cancelado, 6=Anulado.
	 */
	@Basic
	@Column(name = "ANTEESTD")
	private Long estado;

	/**
	 * Pago programado con el que se entrego el dinero.
	 */
	@ManyToOne
	@JoinColumn(name = "PGTRCDGO", referencedColumnName = "PGTRCDGO")
	private PagoProgramado pagoProgramado;

	/**
	 * Descuento recurrente generado al confirmarse el pago.
	 */
	@ManyToOne
	@JoinColumn(name = "DSRCCDGO", referencedColumnName = "DSRCCDGO")
	private DescuentoRecurrente descuentoRecurrente;

	/**
	 * Usuario que aprobo (id, sin FK formal declarada en el DDL).
	 */
	@Basic
	@Column(name = "ANTEUSAP")
	private Long usuarioAprueba;

	/**
	 * Fecha de aprobacion.
	 */
	@Basic
	@Column(name = "ANTEFCAP")
	private LocalDate fechaAprobacion;

	/**
	 * Motivo de anulacion.
	 */
	@Basic
	@Column(name = "ANTEMTAN", length = 500)
	private String motivoAnulacion;

	/**
	 * Fecha de registro.
	 */
	@Basic
	@Column(name = "ANTEFCHR")
	private LocalDateTime fechaRegistro;

	/**
	 * Usuario que registra.
	 */
	@Basic
	@Column(name = "ANTEUSRR", length = 60)
	private String usuarioRegistro;

	// Getters y Setters

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }

	public Empleado getEmpleado() { return empleado; }
	public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

	public LocalDate getFecha() { return fecha; }
	public void setFecha(LocalDate fecha) { this.fecha = fecha; }

	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }

	public Long getNumeroCuotas() { return numeroCuotas; }
	public void setNumeroCuotas(Long numeroCuotas) { this.numeroCuotas = numeroCuotas; }

	public Double getValorCuota() { return valorCuota; }
	public void setValorCuota(Double valorCuota) { this.valorCuota = valorCuota; }

	public Double getSaldo() { return saldo; }
	public void setSaldo(Double saldo) { this.saldo = saldo; }

	public LocalDate getFechaInicioDescuento() { return fechaInicioDescuento; }
	public void setFechaInicioDescuento(LocalDate fechaInicioDescuento) { this.fechaInicioDescuento = fechaInicioDescuento; }

	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }

	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }

	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }

	public PagoProgramado getPagoProgramado() { return pagoProgramado; }
	public void setPagoProgramado(PagoProgramado pagoProgramado) { this.pagoProgramado = pagoProgramado; }

	public DescuentoRecurrente getDescuentoRecurrente() { return descuentoRecurrente; }
	public void setDescuentoRecurrente(DescuentoRecurrente descuentoRecurrente) { this.descuentoRecurrente = descuentoRecurrente; }

	public Long getUsuarioAprueba() { return usuarioAprueba; }
	public void setUsuarioAprueba(Long usuarioAprueba) { this.usuarioAprueba = usuarioAprueba; }

	public LocalDate getFechaAprobacion() { return fechaAprobacion; }
	public void setFechaAprobacion(LocalDate fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }

	public String getMotivoAnulacion() { return motivoAnulacion; }
	public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
