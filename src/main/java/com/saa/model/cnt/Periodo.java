package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;

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

@SuppressWarnings("serial")
@Entity
@Table(name = "PRDO", schema = "CNT")
@SequenceGenerator(name = "SQ_PRDOCDGO", sequenceName = "CNT.SQ_PRDOCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PeriodoAll", query = "select e from Periodo e"),
	@NamedQuery(name = "PeriodoId", query = "select e from Periodo e where e.codigo = :id"),
	@NamedQuery(name = "PeriodoByEmpresaMesAnio",
	            query = "select e from Periodo e where e.empresa.codigo = :idEmpresa and e.mes = :mes and e.anio = :anio")
})

public class Periodo implements Serializable {
	
    /**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "PRDOCDGO")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRDOCDGO")
	private Long codigo;
	
	/**
	 * codigo del nivel de empresa tomado de scp.pjrq con pgspcdgo 12.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * número de mes.
	 */
	@Basic
	@Column(name = "PRDOMSSS")
	private Long mes;
	
	/**
	 * Numero año.
	 */
	@Basic
	@Column(name = "PRDOANNN")
	private Long anio;
	
	/**
	 * nombre del periodo.
	 */
	@Basic
	@Column(name = "PRDONMBR", length = 100)
	private String nombre;
	
	/**
	 * estado del periodo 1 = abierto, 2 = mayorizado, 3 = desmayorizado.
	 */
	@Basic
	@Column(name = "PRDOESTD")
	private Long estado;
	
	/**
	 * id de la mayorización actual.
	 */
	@Basic
	@Column(name = "PRDOMYRZ")
	private Long idMayorizacion;
	
	/**
	 * id de la ultima desmayorización del período.
	 */
	@Basic
	@Column(name = "PRDODSMY")
	private Long idDesmayorizacion;
	
	/**
	 * id de mayorización de cierre (igual al normal + asiento cierre) idMayorizacionCierre.
	 */
	@Basic
	@Column(name = "PRDOMYCR")
	private Long idMayorizacionCierre;
	
	/**
	 * id de desmayorizacion de cierre.
	 */
	@Basic
	@Column(name = "PRDODMCR")
	private Long idDesmayorizacionCierre;
	
	/**
	 * Attribute periodoCierre.
	 */
	@Basic
	@Column(name = "PRDOCRRE")
	private Long periodoCierre;
	
	/**
	 * Fecha del primer día del periodo
	 */
	@Basic
	@Column(name = "PRDOINCO")
	private LocalDate primerDia;
	
	/**
	 * Fecha del último día del periodo
	 */
	@Basic
	@Column(name = "PRDOFNN")
	private LocalDate ultimoDia;

	/**
	 * C17 — Cierre contable bloqueante (PRDOCNCR): 1 = período CERRADO para contabilidad, 0 o nulo = abierto.
	 * Es independiente de {@link #estado} (mayorizado/desmayorizado): el cierre exige el período mayorizado y
	 * lo deja mayorizado, para no romper la cadena "último mayorizado anterior" de saldos. Solo lo cambian
	 * {@code CierrePeriodoContableService.cerrar/reabrir}; {@code PeriodoService.saveSingle} conserva el valor
	 * de la base aunque el cliente mande otro. docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md.
	 */
	@Basic
	@Column(name = "PRDOCNCR")
	private Long cerradoContable;

	/** C17 — Fecha y hora del cierre contable vigente (PRDOFCCN); nula si está abierto. */
	@Basic
	@Column(name = "PRDOFCCN")
	private LocalDateTime fechaCierreContable;

	/** C17 — Usuario que cerró el período (PRDOUSCN); nulo si está abierto. */
	@Basic
	@Column(name = "PRDOUSCN", length = 100)
	private String usuarioCierreContable;

	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}	
	
	/**
	 * Asigna empresa
	 * @param empresa nuevo valor para empresa 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve mes
	 * @return mes
	 */
	public Long getMes() {
		return mes;
	}

	/**
	 * Asigna mes
	 * @param mes nuevo valor para mes 
	 */
	public void setMes(Long mes) {
		this.mes = mes;
	}
	
	/**
	 * Devuelve anio
	 * @return anio
	 */
	public Long getAnio() {
		return anio;
	}

	/**
	 * Asigna anio
	 * @param anio nuevo valor para anio 
	 */
	public void setAnio(Long anio) {
		this.anio = anio;
	}
	
	/**
	 * Devuelve nombre
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre nuevo valor para nombre 
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve estado
	 * @return estado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Devuelve idMayorizacion
	 * @return idMayorizacion
	 */
	public Long getIdMayorizacion() {
		return idMayorizacion;
	}

	/**
	 * Asigna idMayorizacion
	 * @param idMayorizacion nuevo valor para idMayorizacion 
	 */
	public void setIdMayorizacion(Long idMayorizacion) {
		this.idMayorizacion = idMayorizacion;
	}
	
	/**
	 * Devuelve idDesmayorizacion
	 * @return idDesmayorizacion
	 */
	public Long getIdDesmayorizacion() {
		return idDesmayorizacion;
	}

	/**
	 * Asigna idDesmayorizacion
	 * @param idDesmayorizacion nuevo valor para idDesmayorizacion 
	 */
	public void setIdDesmayorizacion(Long idDesmayorizacion) {
		this.idDesmayorizacion = idDesmayorizacion;
	}
	
	/**
	 * Devuelve idMayorizacionCierre
	 * @return idMayorizacionCierre
	 */
	public Long getIdMayorizacionCierre() {
		return idMayorizacionCierre;
	}

	/**
	 * Asigna idMayorizacionCierre
	 * @param idMayorizacionCierre nuevo valor para idMayorizacionCierre 
	 */
	public void setIdMayorizacionCierre(Long idMayorizacionCierre) {
		this.idMayorizacionCierre = idMayorizacionCierre;
	}
	
	/**
	 * Devuelve idDesmayorizacionCierre
	 * @return idDesmayorizacionCierre
	 */
	public Long getIdDesmayorizacionCierre() {
		return idDesmayorizacionCierre;
	}

	/**
	 * Asigna idDesmayorizacionCierre
	 * @param idDesmayorizacionCierre nuevo valor para idDesmayorizacionCierre 
	 */
	public void setIdDesmayorizacionCierre(Long idDesmayorizacionCierre) {
		this.idDesmayorizacionCierre = idDesmayorizacionCierre;
	}
	
	/**
	 * Devuelve periodoCierre
	 * @return periodoCierre
	 */
	public Long getPeriodoCierre() {
		return periodoCierre;
	}

	/**
	 * Asigna periodoCierre
	 * @param periodoCierre nuevo valor para periodoCierre 
	 */
	public void setPeriodoCierre(Long periodoCierre) {
		this.periodoCierre = periodoCierre;
	}
	
	/**
	 * Devuelve el primer día del periodo 
	 * @return	: primer dia
	 */
	public LocalDate getPrimerDia() {
		return primerDia;
	}

	public void setPrimerDia(LocalDate primerDia) {
		this.primerDia = primerDia;
	}

	public LocalDate getUltimoDia() {
		return ultimoDia;
	}

	public void setUltimoDia(LocalDate ultimoDia) {
		this.ultimoDia = ultimoDia;
	}

	public Long getCerradoContable() {
		return cerradoContable;
	}

	public void setCerradoContable(Long cerradoContable) {
		this.cerradoContable = cerradoContable;
	}

	public LocalDateTime getFechaCierreContable() {
		return fechaCierreContable;
	}

	public void setFechaCierreContable(LocalDateTime fechaCierreContable) {
		this.fechaCierreContable = fechaCierreContable;
	}

	public String getUsuarioCierreContable() {
		return usuarioCierreContable;
	}

	public void setUsuarioCierreContable(String usuarioCierreContable) {
		this.usuarioCierreContable = usuarioCierreContable;
	}
	}

