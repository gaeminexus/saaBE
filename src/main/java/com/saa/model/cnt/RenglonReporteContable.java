package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * C19b (D-031) — Renglon de un reporte contable de renglones propios (CNT.RNRP, detalle de CNT.RPRT): un codigo
 * que no es una cuenta del plan (110105 del B17, FC18 del flujo de caja). Lo alimentan las reglas de CNT.DTRP del
 * mismo reporte. Ver docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RNRP", schema = "CNT")
@SequenceGenerator(name = "SQ_RNRPCDGO", sequenceName = "CNT.SQ_RNRPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "RenglonReporteContableAll", query = "select e from RenglonReporteContable e"),
	@NamedQuery(name = "RenglonReporteContableId", query = "select e from RenglonReporteContable e where e.codigo = :id")
})
public class RenglonReporteContable implements Serializable {

	/** Suma de lo que aportan las reglas (y, con acumulacion por prefijo, sus renglones hijos). */
	public static final long CALCULO_SUMA = 1L;
	/** Neto deudor (debe - haber) de sus cuentas, sin el signo de la regla (el "7" del B17). */
	public static final long CALCULO_NETO_DEUDOR = 2L;
	/** Formula sobre otros renglones: "+A -B". */
	public static final long CALCULO_FORMULA = 3L;

	@Basic
	@Id
	@Column(name = "RNRPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RNRPCDGO")
	private Long codigo;

	@ManyToOne
	@JoinColumn(name = "RPRTCDGO", referencedColumnName = "RPRTCDGO")
	private ReporteContable reporteContable;

	/** Codigo del renglon tal como se reporta. */
	@Basic
	@Column(name = "RNRPCDRN", length = 30)
	private String codigoRenglon;

	@Basic
	@Column(name = "RNRPNMBR", length = 200)
	private String nombre;

	@Basic
	@Column(name = "RNRPNVLL")
	private Long nivel;

	/** Orden en el reporte y en el archivo. */
	@Basic
	@Column(name = "RNRPORDN")
	private Long orden;

	/** 1 suma · 2 neto deudor · 3 formula. */
	@Basic
	@Column(name = "RNRPTPCL")
	private Long tipoCalculo;

	/** Base del valor propia (NULL = la del reporte): 1 saldo al corte · 2 movimiento del periodo · 3 saldo al inicio. */
	@Basic
	@Column(name = "RNRPBSVL")
	private Long baseValor;

	@Basic
	@Column(name = "RNRPFRML", length = 1000)
	private String formula;

	/** 1 va en el archivo · 0 solo presentacion/control. */
	@Basic
	@Column(name = "RNRPRPAR")
	private Long vaEnArchivo;

	@Basic
	@Column(name = "RNRPFCDS")
	private LocalDate vigenciaDesde;

	@Basic
	@Column(name = "RNRPFCHS")
	private LocalDate vigenciaHasta;

	/** 1 activo · 2 inactivo. */
	@Basic
	@Column(name = "RNRPESTD")
	private Long estado;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public ReporteContable getReporteContable() { return reporteContable; }
	public void setReporteContable(ReporteContable reporteContable) { this.reporteContable = reporteContable; }
	public String getCodigoRenglon() { return codigoRenglon; }
	public void setCodigoRenglon(String codigoRenglon) { this.codigoRenglon = codigoRenglon; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public Long getNivel() { return nivel; }
	public void setNivel(Long nivel) { this.nivel = nivel; }
	public Long getOrden() { return orden; }
	public void setOrden(Long orden) { this.orden = orden; }
	public Long getTipoCalculo() { return tipoCalculo; }
	public void setTipoCalculo(Long tipoCalculo) { this.tipoCalculo = tipoCalculo; }
	public Long getBaseValor() { return baseValor; }
	public void setBaseValor(Long baseValor) { this.baseValor = baseValor; }
	public String getFormula() { return formula; }
	public void setFormula(String formula) { this.formula = formula; }
	public Long getVaEnArchivo() { return vaEnArchivo; }
	public void setVaEnArchivo(Long vaEnArchivo) { this.vaEnArchivo = vaEnArchivo; }
	public LocalDate getVigenciaDesde() { return vigenciaDesde; }
	public void setVigenciaDesde(LocalDate vigenciaDesde) { this.vigenciaDesde = vigenciaDesde; }
	public LocalDate getVigenciaHasta() { return vigenciaHasta; }
	public void setVigenciaHasta(LocalDate vigenciaHasta) { this.vigenciaHasta = vigenciaHasta; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
}
